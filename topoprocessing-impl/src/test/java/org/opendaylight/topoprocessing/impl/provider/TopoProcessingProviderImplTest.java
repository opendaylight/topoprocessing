/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.GlobalSchemaContextListener;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.InventoryRenderingModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.util.AbstractSchemaContext;

import com.google.common.collect.SetMultimap;

/**
 * @author marek.korenciak
 */
@RunWith(MockitoJUnitRunner.class)
public class TopoProcessingProviderImplTest {

    @Mock
    private DOMDataBroker domDataBrokerMock;
    @Mock
    private DOMDataTreeChangeService domDataTreeChangeServiceMock;
    @Mock
    private SchemaService schemaMock;
    @Mock
    private BindingNormalizedNodeSerializer serializerMock;
    @Mock
    private RpcServices rpcServicesMock;
    @Mock
    private ListenerRegistration<SchemaContextListener> listenerSchemaRegistrationMock;
    @Mock
    private ListenerRegistration<TopologyRequestListener> listenerRegistrationMock;
    @Mock
    private ModelAdapter modelAdapterMock;
    @Mock
    private TopologyRequestListener topologyRequestListenerMock;
    @Mock
    private Broker brokerMock;
    @Mock
    private ProviderSession sessionMock;
    @Mock
    private DOMRpcService rpcServiceMock;
    @Mock
    private DOMRpcProviderService rpcProviderServiceMock;

    private TopoProcessingProviderImpl provider;
    private SchemaContextTmp schemaContext = new SchemaContextTmp();

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
    public void init() {
        when(rpcServicesMock.getRpcService()).thenReturn(rpcServiceMock);
        when(rpcServicesMock.getRpcProviderService()).thenReturn(rpcProviderServiceMock);
        when(schemaMock.getGlobalContext()).thenReturn(schemaContext);
        when(brokerMock.registerProvider(any())).thenReturn(sessionMock);
        when(sessionMock.getService(DOMRpcService.class)).thenReturn(rpcServiceMock);
        when(sessionMock.getService(DOMRpcProviderService.class)).thenReturn(rpcProviderServiceMock);
        provider = new TopoProcessingProviderImpl();
        provider.setSchemaService(schemaMock);
        provider.setDataBroker(domDataBrokerMock);
        provider.setNodeSerializer(serializerMock);
        provider.setRpcServices(rpcServicesMock);
        provider.setDataStoreType(LogicalDatastoreType.CONFIGURATION);
        provider.setBroker(brokerMock);
        provider.setDomDataTreeChangeService(domDataTreeChangeServiceMock);
        Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> brokerExtensions = new HashMap<>();
        brokerExtensions.put(DOMDataTreeChangeService.class, domDataTreeChangeServiceMock);
        Mockito.when(domDataBrokerMock.getSupportedExtensions()).thenReturn(brokerExtensions);
    }

    @Test
    public void startupTest() {
        when(schemaMock.registerSchemaContextListener((GlobalSchemaContextListener)any()))
            .thenReturn(listenerSchemaRegistrationMock);

        provider.startup();
        verify(schemaMock,times(1)).registerSchemaContextListener((GlobalSchemaContextListener)any());
    }

    @Test
    public void closeTest() throws Exception {
        setTopologyRequestListener();

        when(schemaMock.registerSchemaContextListener((GlobalSchemaContextListener)any()))
            .thenReturn(listenerSchemaRegistrationMock);
        provider.startup();
        provider.registerModelAdapter(I2rsModel.class, modelAdapterMock);
        provider.close();
        verify(listenerSchemaRegistrationMock,times(1)).close();
        verify(listenerRegistrationMock,times(1)).close();
    }

    @Test
    public void registerFiltratorFactoryTest() {
        setTopologyRequestListener();

        provider.registerModelAdapter(I2rsModel.class, modelAdapterMock);
        FiltratorFactory filtratorFactoryMock = mock(FiltratorFactory.class);
        provider.registerFiltratorFactory(FilterBase.class, filtratorFactoryMock);
        verify(topologyRequestListenerMock, times(1)).registerFiltrator(FilterBase.class, filtratorFactoryMock);
    }

    @Test
    public void unregisterFiltratorFactoryTest() {
        setTopologyRequestListener();

        provider.registerModelAdapter(I2rsModel.class, modelAdapterMock);
        provider.unregisterFiltratorFactory(FilterBase.class);
        verify(topologyRequestListenerMock, times(1)).unregisterFiltrator(FilterBase.class);
    }

    @Test
    public void registerModelAdapterAndRegisterTopologyRequestListenerTest() {
        setTopologyRequestListener();

        int iterator = 0;

        try {
            provider.registerModelAdapter(I2rsModel.class, null);
        } catch (IllegalStateException e) {
            iterator++;
        }

        provider.registerModelAdapter(I2rsModel.class, modelAdapterMock);
        provider.registerModelAdapter(NetworkTopologyModel.class, modelAdapterMock);
        verify(modelAdapterMock, times(2)).createTopologyRequestListener((DOMDataBroker)any(),
                (DOMDataTreeChangeService) any(), (BindingNormalizedNodeSerializer)any(),
                (GlobalSchemaContextHolder)any(), (RpcServices)any(), (Map<Class<? extends Model>, ModelAdapter>)any());

        provider.registerModelAdapter(InventoryRenderingModel.class, modelAdapterMock);
        verify(modelAdapterMock, times(2)).createTopologyRequestListener((DOMDataBroker)any(),
                (DOMDataTreeChangeService) any(), (BindingNormalizedNodeSerializer)any(),
                (GlobalSchemaContextHolder)any(), (RpcServices)any(), (Map<Class<? extends Model>, ModelAdapter>)any());

        assertEquals(1, iterator);
    }

    private void setTopologyRequestListener() {
        when(modelAdapterMock.createTopologyRequestListener((DOMDataBroker)any(), (DOMDataTreeChangeService) any(),
                (BindingNormalizedNodeSerializer)any(), (GlobalSchemaContextHolder)any(), (RpcServices)any(),
                (Map<Class<? extends Model>, ModelAdapter>)any())).thenReturn(topologyRequestListenerMock);
        when(domDataTreeChangeServiceMock.registerDataTreeChangeListener((DOMDataTreeIdentifier) any(),
                (TopologyRequestListener) any())).thenReturn(listenerRegistrationMock);
    }
}