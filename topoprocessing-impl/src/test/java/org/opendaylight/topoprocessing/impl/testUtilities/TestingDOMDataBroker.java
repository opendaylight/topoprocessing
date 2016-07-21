/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.testUtilities;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
public class TestingDOMDataBroker implements DOMDataBroker, DOMDataTreeChangeService {

    private boolean listenerClosed = false;
    private Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> supportedExtensions;

    public TestingDOMDataBroker() {
        supportedExtensions = new HashMap<>();
        supportedExtensions.put(DOMDataTreeChangeService.class, this);
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(LogicalDatastoreType store,
            YangInstanceIdentifier path, DOMDataChangeListener listener,
            org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope triggeringScope) {
        return null;
    }

    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return supportedExtensions;
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return null;
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return null;
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return null;
    }

    @Override
    public DOMTransactionChain createTransactionChain(
            TransactionChainListener listener) {
        return null;
    }

    public boolean getListenerClosed() {
        return listenerClosed;
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(
            DOMDataTreeIdentifier treeId, L listener) {
        return new ListenerRegistration<L>() {
            @Override
            public L getInstance() {
                return null;
            }

            @Override
            public void close() {
                listenerClosed = true;
            }
        };
    }
}

