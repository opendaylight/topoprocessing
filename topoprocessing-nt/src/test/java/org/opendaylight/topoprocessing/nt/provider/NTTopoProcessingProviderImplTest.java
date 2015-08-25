package org.opendaylight.topoprocessing.nt.provider;

import java.net.URI;
import java.util.Map;
import java.util.Set;

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
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
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
public class NTTopoProcessingProviderImplTest {

    private TopoProcessingProviderImpl topoProcessingProvider;

    @Mock private SchemaService schemaService;
    @Mock private DOMDataBroker dataBroker;
    @Mock private BindingNormalizedNodeSerializer nodeSerializer;
    @Mock private RpcServices rpcServices;
    @Mock private ListenerRegistration<SchemaContextListener> schemaContextListenerRegistration;
    @Mock private ListenerRegistration<DOMDataChangeListener> topologyRequestListenerRegistration;
    @Mock private Model model;
    @Mock private ModelAdapter modelAdapter;
    @Mock private TopologyRequestListener listener;

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

    @Test
    public void testStartup() throws Exception {
        Mockito.when(rpcServices.getRpcService()).thenReturn(Mockito.mock(DOMRpcService.class));
        Mockito.when(rpcServices.getRpcProviderService()).thenReturn(Mockito.mock(DOMRpcProviderService.class));
        SchemaContextTmp schemaContext = new SchemaContextTmp();
        Mockito.when(schemaService.getGlobalContext()).thenReturn(schemaContext);
        Mockito.when(schemaService.registerSchemaContextListener((SchemaContextListener) Matchers.any()))
                .thenReturn(schemaContextListenerRegistration);
        Mockito.when(dataBroker.registerDataChangeListener((LogicalDatastoreType) Matchers.any(),
                (YangInstanceIdentifier) Matchers.any(), (DOMDataChangeListener) Matchers.any(),
                (DataChangeScope) Matchers.any())).thenReturn(topologyRequestListenerRegistration);

        // startup
        topoProcessingProvider = new TopoProcessingProviderImpl(
                schemaService, dataBroker, nodeSerializer, rpcServices, DatastoreType.OPERATIONAL);
        topoProcessingProvider.startup();
        TopoProcessingProviderNT NTProvider = new TopoProcessingProviderNT();
        NTProvider.startup(topoProcessingProvider);
        Mockito.verify(schemaService).registerSchemaContextListener((SchemaContextListener) Matchers.any());
        Mockito.verify(dataBroker).registerDataChangeListener(
                Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.eq(InstanceIdentifiers.TOPOLOGY_IDENTIFIER),
                Matchers.any(TopologyRequestListener.class),
                Matchers.eq(DataChangeScope.ONE));

        // close
        topoProcessingProvider.close();
        Mockito.verify(schemaContextListenerRegistration).close();
        Mockito.verify(topologyRequestListenerRegistration).close();
    }
}
