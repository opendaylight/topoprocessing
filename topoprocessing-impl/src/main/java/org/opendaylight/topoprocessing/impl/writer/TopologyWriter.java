/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.writer;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.IgnoreAddQueue;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.polkorab
 *
 */
public class TopologyWriter implements TransactionChainListener {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TopologyWriter.class);
    private static final int MAXIMUM_OPERATIONS = 50;
    private static final int EXECUTOR_POOL_THREADS = 1;
    private static final int SHUTDOWN_SUBMIT_TIMEOUT = 500;
    private String topologyId;
    private OverlayItemTranslator translator;
    private DOMTransactionChain transactionChain;
    private YangInstanceIdentifier topologyIdentifier;
    private YangInstanceIdentifier nodeIdentifier;
    private YangInstanceIdentifier linkIdentifier;
    private Queue<TransactionOperation> preparedOperations;
    private ThreadPoolExecutor pool;
    private Class<? extends Model> model;

    private static final AtomicIntegerFieldUpdater<TopologyWriter> WRITE_SCHEDULED_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(TopologyWriter.class, "writeScheduled");
    private volatile int writeScheduled = 0; //do not delete it is used above in WRITE_SCHEDULED_UPDATER

    private final Runnable writeTask = new Runnable() {
        @Override
        public void run() {
            TopologyWriter.this.write();
        }
    };

    /**
     * Default constructor
     * @param topologyId topologyId of overlay topology
     */
    public TopologyWriter(String topologyId, Class<? extends Model> model) {
        this.topologyId = topologyId;
        this.model = model;
        if (model.equals(I2rsModel.class)) {
            topologyIdentifier = YangInstanceIdentifier.builder(InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER)
                    .nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, topologyId).build();
            nodeIdentifier = YangInstanceIdentifier.builder(topologyIdentifier)
                    .node(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
                            .network.Node.QNAME)
                    .build();
            linkIdentifier = YangInstanceIdentifier.builder(topologyIdentifier)
                    .node(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608
                            .network.Link.QNAME)
                    .build();
        } else {
            topologyIdentifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                    .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, topologyId).build();
            nodeIdentifier = YangInstanceIdentifier.builder(topologyIdentifier).node(Node.QNAME).build();
            linkIdentifier = YangInstanceIdentifier.builder(topologyIdentifier).node(Link.QNAME).build();
        }
        preparedOperations = new ConcurrentLinkedQueue<>();
        pool = new ScheduledThreadPoolExecutor(EXECUTOR_POOL_THREADS);
    }


    /**
     * Writes empty overlay topology with provided topologyId. Also writes empty {@link Link}
     * and {@link Node} mapnode.
     */
    public void initOverlayTopology() {
        if (model.equals(I2rsModel.class)) {
            YangInstanceIdentifier networkId = YangInstanceIdentifier.of(Network.QNAME);

            MapNode nodeMapNode = ImmutableNodes.mapNodeBuilder(org.opendaylight.yang.gen.v1.urn.ietf.params
                    .xml.ns.yang.ietf.network.rev150608.network.Node.QNAME).build();
            MapNode linkMapNode = ImmutableNodes.mapNodeBuilder(org.opendaylight.yang.gen.v1.urn.ietf.params
                    .xml.ns.yang.ietf.network.topology.rev150608.network.Link.QNAME).build();
            MapNode networkNode = ImmutableNodes.mapNodeBuilder(Network.QNAME)
                            .withChild(ImmutableNodes.mapEntryBuilder(Network.QNAME,
                                    TopologyQNames.I2RS_NETWORK_ID_QNAME, topologyId)
                                    .withChild(nodeMapNode)
                                    .withChild(linkMapNode).build())
                            .build();
            preparedOperations.add(new MergeOperation(networkId, networkNode));
        } else {
            YangInstanceIdentifier networkId = YangInstanceIdentifier.of(NetworkTopology.QNAME);

            MapNode nodeMapNode = ImmutableNodes.mapNodeBuilder(Node.QNAME).build();
            MapNode linkMapNode = ImmutableNodes.mapNodeBuilder(Link.QNAME).build();
            ContainerNode networkNode = ImmutableContainerNodeBuilder.create()
                    .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(NetworkTopology.QNAME))
                    .withChild(ImmutableNodes.mapNodeBuilder(Topology.QNAME)
                            .withChild(ImmutableNodes.mapEntryBuilder(Topology.QNAME,
                                    TopologyQNames.TOPOLOGY_ID_QNAME, topologyId)
                                    .withChild(nodeMapNode)
                                    .withChild(linkMapNode).build())
                            .build())
                    .build();
            preparedOperations.add(new MergeOperation(networkId, networkNode));
        }
        scheduleWrite();
    }

    /**
     * @param wrapper LogicalNodeWrapper to be written into datastore
     * @param itemType item type
     */
    public void writeItem(final OverlayItemWrapper wrapper, CorrelationItemEnum itemType) {
        NormalizedNode<?, ?> node = translator.translate(wrapper);
        preparedOperations.add(new PutOperation(createItemIdentifier(wrapper, itemType), node));
        scheduleWrite();
    }

    /**
     * @param wrapper LogicalNodeWrapper to be removed from datastore
     * @param itemType item type
     */
    public void deleteItem(final OverlayItemWrapper wrapper, CorrelationItemEnum itemType) {
        preparedOperations.add(new DeleteOperation(createItemIdentifier(wrapper, itemType)));
        scheduleWrite();
    }

    private YangInstanceIdentifier createItemIdentifier(OverlayItemWrapper wrapper, CorrelationItemEnum itemType) {
        YangInstanceIdentifier itemWithItemIdIdentifier = null;
        if (wrapper != null) {
            itemWithItemIdIdentifier = buildDatastoreIdentifier(wrapper.getId(), itemType);
        }
        return itemWithItemIdIdentifier;
    }


    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
            Throwable cause) {
        LOGGER.warn("Unexpected transaction failure in transaction {}", transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transaction successfully finished", chain);
        }
    }

    /**
     * @param transactionChain Sets {@link TransactionChain}
     */
    public void setTransactionChain(DOMTransactionChain transactionChain) {
        this.transactionChain = transactionChain;
    }

    public void setTranslator(OverlayItemTranslator translator) {
        this.translator = translator;
    }


    /**
     * Writes topology-types
     * @param topologyTypes - taken from overlay topology request
     */
    public void writeTopologyTypes(DataContainerChild<? extends PathArgument, ?> topologyTypes) {
        YangInstanceIdentifier topologyTypesYiid = topologyIdentifier.node(TopologyTypes.QNAME);
        preparedOperations.add(new PutOperation(topologyTypesYiid, topologyTypes));
        scheduleWrite();
    }

    private void scheduleWrite() {
        if (preparedOperations.isEmpty()) {
            LOGGER.trace("No operations prepared - no write needed");
            return;
        }
        if (WRITE_SCHEDULED_UPDATER.compareAndSet(this, 0, 1)) {
            LOGGER.trace("Scheduling write task");
            pool.execute(writeTask);
        } else {
            LOGGER.trace("Write task is already present");
        }
    }

    void write() {
        LOGGER.trace("Writing prepared operations.");
        DOMDataWriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        int operation = 0;
        boolean shutdown = false;
        while ((operation < MAXIMUM_OPERATIONS) && (preparedOperations.peek() != null)) {
            TransactionOperation currentOperation = preparedOperations.poll();
            currentOperation.addOperationIntoTransaction(transaction);
            operation++;
            if(currentOperation instanceof ShutdownOperation) {
                preparedOperations.clear();
                preparedOperations = new IgnoreAddQueue<TransactionOperation>();
                shutdown = true;
                break;
            }
        }
        LOGGER.debug("Submitting {} prepared operations.", operation);
        CheckedFuture<Void,TransactionCommitFailedException> submit = transaction.submit();

        Futures.addCallback(submit, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void empty) {
                LOGGER.debug("Transaction successfully written.");
            }
            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.warn("Transaction failed.");
            }
        });

        if(shutdown) {
            if(! submit.isDone()) {
                try {
                    submit.get(SHUTDOWN_SUBMIT_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    LOGGER.error("An error occurred while waiting for transaction submit: {}", submit, e);
                }
            }
            LOGGER.trace("Shutting down writer");
            try {
                transactionChain.close();
            } catch (Exception e) {
                LOGGER.error("An error occurred while closing transaction chain: {}", transactionChain, e);
            }
            pool.shutdownNow();
        } else{
            if (! WRITE_SCHEDULED_UPDATER.compareAndSet(this, 1, 0)) {
                LOGGER.warn("Writer found unscheduled");
            }
            scheduleWrite();
        }
    }

    /**
     * Signals that allocated resources should be released
     */
    public void tearDown() {
        LOGGER.trace("Tear down signaled.");
        preparedOperations.add(new ShutdownOperation(topologyIdentifier));
        scheduleWrite();
    }

    /**
     * Wait until all threads/writes are finished
     * @param timeOut timeout to wait for threads/writes to finish in ms
     * @throws InterruptedException
     */
    public void waitForTearDownCompletion(int timeOut)throws InterruptedException {
        pool.awaitTermination(timeOut,TimeUnit.MILLISECONDS);
    }


    private YangInstanceIdentifier buildDatastoreIdentifier(String itemId, CorrelationItemEnum correlationItem) {
        InstanceIdentifierBuilder builder = null;
        if (model.equals(I2rsModel.class)) {
            switch (correlationItem) {
            case Node:
            case TerminationPoint:
                builder = YangInstanceIdentifier.builder(nodeIdentifier)
                            .nodeWithKey(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
                                    .network.Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, itemId);
                break;
            case Link:
                builder = YangInstanceIdentifier.builder(linkIdentifier)
                        .nodeWithKey(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                                .rev150608.network.Link.QNAME, TopologyQNames.I2RS_LINK_ID_QNAME, itemId);
                break;
            default:
                throw new IllegalArgumentException("Wrong Correlation item set: "
                        + correlationItem);
            }

        } else {
            switch (correlationItem) {
            case Node:
            case TerminationPoint:
                builder = YangInstanceIdentifier.builder(nodeIdentifier)
                            .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, itemId);
                break;
            case Link:
                builder = YangInstanceIdentifier.builder(linkIdentifier)
                        .nodeWithKey(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, itemId);
                break;
            default:
                throw new IllegalArgumentException("Wrong Correlation item set: "
                        + correlationItem);
            }
        }
        return builder.build();
    }
}
