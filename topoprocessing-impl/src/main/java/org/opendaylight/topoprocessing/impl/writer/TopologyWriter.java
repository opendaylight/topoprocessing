/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.writer;

import java.util.concurrent.Future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import com.google.common.util.concurrent.SettableFuture;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
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
    private OverlayItemTranslator translator;
    private DOMTransactionChain transactionChain;
    private YangInstanceIdentifier topologyIdentifier;
    private YangInstanceIdentifier nodeIdentifier;
    private YangInstanceIdentifier linkIdentifier;
    private Queue<TransactionOperation> preparedOperations;
    private ThreadPoolExecutor pool;

    private static final AtomicIntegerFieldUpdater<TopologyWriter> WRITE_SCHEDULED_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(TopologyWriter.class, "writeScheduled");
    private static final int MAX_DELETE_TOPOLOGY_WAIT_TIME = 1000;
    private volatile int writeScheduled = 0;
    private boolean shutdownInitiated = false;
    private SettableFuture<Boolean> tearDownFuture = SettableFuture.create();

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
        translator = new OverlayItemTranslator();
        topologyIdentifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, topologyId).build();
        nodeIdentifier = YangInstanceIdentifier.builder(topologyIdentifier).node(Node.QNAME).build();
        linkIdentifier = YangInstanceIdentifier.builder(topologyIdentifier).node(Link.QNAME).build();
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
            public void onSuccess(Void empty) {
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
            public void onSuccess(Void empty) {
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
            public void onSuccess(Void empty) {
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
     * @param wrapper LogicalNodeWrapper to be written into datastore
     * @param itemType item type
     */
    public void writeItem(final OverlayItemWrapper wrapper, CorrelationItemEnum itemType) {
        if (! shutdownInitiated) {
            NormalizedNode<?, ?> node = translator.translate(wrapper);
            preparedOperations.add(new PutOperation(createItemIdentifier(wrapper, itemType), node));
            scheduleWrite();
        }
    }

    /**
     * @param wrapper LogicalNodeWrapper to be removed from datastore
     * @param itemType item type
     */
    public void deleteItem(final OverlayItemWrapper wrapper, CorrelationItemEnum itemType) {
        if (! shutdownInitiated) {
            preparedOperations.add(new DeleteOperation(createItemIdentifier(wrapper, itemType)));
            scheduleWrite();
        }
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
            public void onSuccess(Void empty) {
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
        if (shutdownInitiated) {
            LOGGER.trace("Shutting down writer");
            releaseResources(transactionChain.newWriteOnlyTransaction());
            return;
        }
        scheduleWrite();
    }

    /**
     * Signals that allocated resources should be released
     * @return tear down finished future
     */
    public Future<Boolean> tearDown() {
        LOGGER.trace("Tear down signaled.");
        shutdownInitiated = true;
        return tearDownFuture;
    }

    /**
     * Clears list of unexecuted operations, deletes overlay topology from datastore
     * and shuts down {@link ThreadPoolExecutor}. Then sets the tearDownFuture - notifying
     * that the tearDown action has been completed.
     * @param transaction used for overlay topology deletion
     */
    private void releaseResources(DOMDataWriteTransaction transaction) {
        LOGGER.trace("Releasing allocated resources.");
        preparedOperations.clear();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, topologyIdentifier);
        try {
            transaction.submit().get(MAX_DELETE_TOPOLOGY_WAIT_TIME, TimeUnit.MILLISECONDS);
            pool.shutdownNow();
            tearDownFuture.set(true);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warn("Exception during overlay topology removal: {}", e);
        }
    }

    private YangInstanceIdentifier buildDatastoreIdentifier(String itemId, CorrelationItemEnum correlationItem) {
        InstanceIdentifierBuilder builder = null;
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
        return builder.build();
    }
}
