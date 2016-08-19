/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventoryRendering.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.util.AbstractSchemaContext;

/**
 * @author matej.perina
 */
@RunWith(MockitoJUnitRunner.class)
public class IRTopoProcessingProviderImplTest {

    private TopoProcessingProviderImpl topoProcessingProvider;

    @Mock private SchemaService schemaService;
    @Mock private DOMDataBroker dataBroker;
    @Mock private BindingNormalizedNodeSerializer nodeSerializer;
    @Mock private RpcServices rpcServices;
    @Mock private ListenerRegistration<SchemaContextListener> schemaContextListenerRegistration;
    @Mock private ListenerRegistration<DOMDataChangeListener> topologyRequestListenerRegistration;

    @Test
    public void testStartup() throws Exception {
        Mockito.when(rpcServices.getRpcService()).thenReturn(Mockito.mock(DOMRpcService.class));
        Mockito.when(rpcServices.getRpcProviderService()).thenReturn(Mockito.mock(DOMRpcProviderService.class));
        AbstractSchemaContext schemaContext = Mockito.mock(AbstractSchemaContext.class);
        Mockito.when(schemaContext.getQName()).thenReturn(SchemaContext.NAME);
        Mockito.when(schemaService.getGlobalContext()).thenReturn(schemaContext);
        Mockito.when(schemaService.registerSchemaContextListener((SchemaContextListener) Matchers.any()))
                .thenReturn(schemaContextListenerRegistration);
        Mockito.when(dataBroker.registerDataChangeListener((LogicalDatastoreType) Matchers.any(),
                (YangInstanceIdentifier) Matchers.any(), (DOMDataChangeListener) Matchers.any(),
                (DataChangeScope) Matchers.any())).thenReturn(topologyRequestListenerRegistration);

        // startup
        topoProcessingProvider = new TopoProcessingProviderImpl();
        topoProcessingProvider.startup();
        TopoProcessingProviderIR InvProvider = new TopoProcessingProviderIR();
        InvProvider.startup(topoProcessingProvider);
        Mockito.verify(schemaService).registerSchemaContextListener((SchemaContextListener) Matchers.any());

        // close
        topoProcessingProvider.close();
        Mockito.verify(schemaContextListenerRegistration).close();
    }
}
