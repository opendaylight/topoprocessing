/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.testUtilities;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.AbstractDOMDataBroker;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * @author martin.uhlir
 *
 */
public class TestingDOMDataBroker extends AbstractDOMDataBroker implements DOMDataTreeChangeService {

    private boolean listenerClosed = false;
    private final Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> supportedExtensions;

    public TestingDOMDataBroker() {
        super(Collections.singletonMap(LogicalDatastoreType.CONFIGURATION, Mockito.mock(DOMStore.class)));
        supportedExtensions = new HashMap<>();
        supportedExtensions.put(DOMDataTreeChangeService.class, this);
    }

    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return supportedExtensions;
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

    @Override
    protected CheckedFuture<Void, TransactionCommitFailedException> submit(DOMDataWriteTransaction transaction,
            Collection<DOMStoreThreePhaseCommitCohort> cohorts) {
        return Futures.immediateCheckedFuture(null);
    }
}

