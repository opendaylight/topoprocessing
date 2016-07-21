/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.observer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.topology.mlmt.utility.MlmtDataChangeObserver;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MlmtDataChangeEventListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtDataChangeEventListener.class);
    private LogicalDatastoreType storageType;
    private InstanceIdentifier<? extends DataObject> path;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private MlmtDataChangeObserver observer;
    private DataBroker dataBroker;

    public void init(final LogicalDatastoreType type, final DataBroker dataBroker,
            final InstanceIdentifier<? extends DataObject> path) {
        this.storageType = type;
        this.path = path;
        this.dataBroker = dataBroker;
    }

    public void registerObserver(final MlmtDataChangeObserver observer) {
        this.observer = observer;
        this.listenerRegistration = dataBroker.registerDataChangeListener(storageType, path,
                this, DataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        try {
            if (observer != null) {
                observer.onDataChanged(storageType, change);
            }
        } catch (final Exception e) {
            LOG.error("MlmtDataChangeEventListener.onDataChanged: ", e);
        }
    }

    @Override
    public void close() {
        LOG.info("MlmtDataChangeEventListener stopped.");
        if (this.listenerRegistration != null) {
            try {
                this.listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("MlmtDataChangeEventListener.close: Failed to close listener registration", e);
            }
            listenerRegistration = null;
        }
    }
}
