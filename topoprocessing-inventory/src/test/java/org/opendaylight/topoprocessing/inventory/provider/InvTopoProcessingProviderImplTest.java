/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventory.provider;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.inventory.adapter.InvModelAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.util.AbstractSchemaContext;

import com.google.common.collect.SetMultimap;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class InvTopoProcessingProviderImplTest {

    private TopoProcessingProviderImpl topoProcessingProvider;

    @Mock private SchemaService schemaService;
    @Mock private DOMDataBroker dataBroker;
    @Mock private BindingNormalizedNodeSerializer nodeSerializer;
    @Mock private RpcServices rpcServices;
    @Mock private ListenerRegistration<SchemaContextListener> schemaContextListenerRegistration;
    @Mock private ListenerRegistration<DOMDataChangeListener> topologyRequestListenerRegistration;
    @Mock private InvModelAdapter mockInvModelAdapter;
    @Mock private TopologyRequestListener mockTopoRequestListener;

    private class SchemaContextTmp extends AbstractSchemaContext {

        @Override
        protected Map<ModuleIdentifier, String> getIdentifiersToSources() {
            return null;
        }

        @Override
        protected SetMultimap<URI, Module> getNamespaceToModules() {
            return null;
        }

        @Override
        protected SetMultimap<String, Module> getNameToModules() {
            return null;
        }

        @Override
        public Set<Module> getModules() {
            return null;
        }
    }

    @Before
    public void setUp() {
        Mockito.when(rpcServices.getRpcService()).thenReturn(Mockito.mock(DOMRpcService.class));
        Mockito.when(rpcServices.getRpcProviderService()).thenReturn(Mockito.mock(DOMRpcProviderService.class));
        SchemaContextTmp schemaContext = new SchemaContextTmp();
        Mockito.when(schemaService.getGlobalContext()).thenReturn(schemaContext);
    }

    @Test
    public void testStartup() {
        Mockito.when(schemaService.registerSchemaContextListener((SchemaContextListener) Matchers.any()))
                .thenReturn(schemaContextListenerRegistration);
        Mockito.when(dataBroker.registerDataChangeListener((LogicalDatastoreType) Matchers.any(),
                (YangInstanceIdentifier) Matchers.any(), (DOMDataChangeListener) Matchers.any(),
                (DataChangeScope) Matchers.any())).thenReturn(topologyRequestListenerRegistration);

        // startup
        topoProcessingProvider = new TopoProcessingProviderImpl(
                schemaService, dataBroker, nodeSerializer, rpcServices, DatastoreType.OPERATIONAL);
        topoProcessingProvider.startup();
        TopoProcessingProviderInv invProvider = new TopoProcessingProviderInv();
        invProvider.startup(topoProcessingProvider);
        Mockito.verify(schemaService).registerSchemaContextListener((SchemaContextListener) Matchers.any());
    }

    @Test
    public void testClose() throws Exception {
        testStartup();
        // close
        topoProcessingProvider.close();
        Mockito.verify(schemaContextListenerRegistration).close();
    }

}
