/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.writer;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * @author michal.polkorab
 *
 */
public class TopologyWriter {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TopologyWriter.class);
    private DOMDataBroker dataBroker;

    /**
     * Default constructor
     * @param dataBroker broker used for transaction operations
     */
    public TopologyWriter(DOMDataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Updates existing data in operational DataStore
     * @param dataToUpdate data to be updated
     */
    public void updateData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> dataToUpdate) {
        DOMDataWriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
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
                LOGGER.debug("Failed to update transaction data");
            }
        });
    }

    /**
     * Writes / creates new data in operational DataStore
     * @param dataToCreate data to be created
     */
    public void writeCreatedData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> dataToCreate) {
        DOMDataWriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
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
                LOGGER.debug("Failed to write new transaction data");
            }
        });
    }

    /**
     * Removed specified data from operational DataStore
     * @param dataToRemove data to be removed
     */
    public void deleteData(Set<YangInstanceIdentifier> dataToRemove) {
        DOMDataWriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
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
                LOGGER.debug("Failed to remove transaction data");
            }
        });
    }
}
