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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.topoprocessing.impl.structure.LogicalNodeWrapper;
import org.opendaylight.topoprocessing.impl.translator.LogicalNodeToNodeTranslator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
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
    private String topologyId;
    private LogicalNodeToNodeTranslator translator;
    private YangInstanceIdentifier nodeIdentifier;
    private DOMTransactionChain transactionChain;
    private YangInstanceIdentifier topologyIdentifier;
    private Queue<TransactionOperation> preparedOperations;
    private ThreadPoolExecutor pool;

    private static final AtomicIntegerFieldUpdater<TopologyWriter> WRITE_SCHEDULED_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(TopologyWriter.class, "writeScheduled");
    private volatile int writeScheduled = 0;
    
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
    public TopologyWriter(String topologyId) {
        this.topologyId = topologyId;
        translator = new LogicalNodeToNodeTranslator();
        topologyIdentifier =
                YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, topologyId).build();
        nodeIdentifier = topologyIdentifier.node(Node.QNAME);
        preparedOperations = new ConcurrentLinkedQueue<>();
        pool = new ScheduledThreadPoolExecutor(EXECUTOR_POOL_THREADS);
    }

    /**
     * Updates existing data in operational DataStore
     * @param dataToUpdate data to be updated
     */
    public void updateData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> dataToUpdate) {
        DOMDataWriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        Iterator<Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator =
                dataToUpdate.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            transaction.merge(LogicalDatastoreType.OPERATIONAL, entry.getKey(), entry.getValue());
        }
        CheckedFuture<Void,TransactionCommitFailedException> commitFuture = transaction.submit();
        Futures.addCallback(commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LOGGER.debug("Data updated successfully");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.warn("Failed to update transaction data");
            }
        });
    }

    /**
     * Writes / creates new data in operational DataStore
     * @param dataToCreate data to be created
     */
    public void writeCreatedData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> dataToCreate) {
        DOMDataWriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        Iterator<Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator =
                dataToCreate.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            transaction.put(LogicalDatastoreType.OPERATIONAL, entry.getKey(), entry.getValue());
        }
        CheckedFuture<Void,TransactionCommitFailedException> commitFuture = transaction.submit();
        Futures.addCallback(commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LOGGER.debug("Data written successfully");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.warn("Failed to write new transaction data");
            }
        });
    }

    /**
     * Removed specified data from operational DataStore
     * @param dataToRemove data to be removed
     */
    public void deleteData(Set<YangInstanceIdentifier> dataToRemove) {
        DOMDataWriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        Iterator<YangInstanceIdentifier> iterator = dataToRemove.iterator();
        while (iterator.hasNext()) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, iterator.next());
        }
        CheckedFuture<Void,TransactionCommitFailedException> commitFuture = transaction.submit();
        Futures.addCallback(commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LOGGER.debug("Data successfully removed");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.warn("Failed to remove transaction data");
            }
        });
    }

    /**
     * Writes empty overlay topology with provided topologyId. Also writes empty {@link Link}
     * and {@link Node} mapnode.
     */
    public void initOverlayTopology() {
        MapEntryNode topologyMapEntryNode = ImmutableNodes
                .mapEntry(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, topologyId);

        MapNode nodeMapNode = ImmutableNodes.mapNodeBuilder(Node.QNAME).build();
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(topologyIdentifier)
                .node(Node.QNAME).build();
        MapNode linkMapNode = ImmutableNodes.mapNodeBuilder(Link.QNAME).build();
        YangInstanceIdentifier linkYiid = YangInstanceIdentifier.builder(topologyIdentifier)
                .node(Link.QNAME).build();

        preparedOperations.add(new PutOperation(topologyIdentifier, topologyMapEntryNode));
        preparedOperations.add(new PutOperation(nodeYiid, nodeMapNode));
        preparedOperations.add(new PutOperation(linkYiid, linkMapNode));
        scheduleWrite();
    }

    /**
     * @param wrapper
     */
    public void writeNode(final LogicalNodeWrapper wrapper) {
        NormalizedNode<?, ?> node = translator.translate(wrapper);
        preparedOperations.add(new PutOperation(createNodeIdentifier(wrapper.getNodeId()), node));
        scheduleWrite();
    }

    /**
     * @param wrapper
     */
    public void deleteNode(final LogicalNodeWrapper wrapper) {
        preparedOperations.add(new DeleteOperation(createNodeIdentifier(wrapper.getNodeId())));
        scheduleWrite();
    }

    private YangInstanceIdentifier createNodeIdentifier(String nodeId) {
        return YangInstanceIdentifier.builder(nodeIdentifier)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
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
     * @param transactionChain
     */
    public void setTransactionChain(DOMTransactionChain transactionChain) {
        this.transactionChain = transactionChain;
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
        while ((operation < MAXIMUM_OPERATIONS) && (preparedOperations.peek() != null)) {
            preparedOperations.poll().addOperationIntoTransaction(transaction);
            operation++;
        }
        LOGGER.debug("Submitting {} prepared operations.", operation);
        CheckedFuture<Void,TransactionCommitFailedException> submit = transaction.submit();

        Futures.addCallback(submit, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LOGGER.debug("Transaction successfully written.");
            }
            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.warn("Transaction failed.");
            }
        });

        if (! WRITE_SCHEDULED_UPDATER.compareAndSet(this, 1, 0)) {
            LOGGER.warn("Writer found unscheduled");
        }
        scheduleWrite();
    }

    /**
     * Deletes whole overlay {@link Topology}
     */
    public void deleteOverlayTopology() {
        preparedOperations.add(new DeleteOperation(topologyIdentifier));
        scheduleWrite();
    }
}
