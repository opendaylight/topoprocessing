package org.opendaylight.topoprocessing.impl.request;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.any;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.Topology;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;

/**
 * @author samuel.kontris
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestListenerTest {

    AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> changeMock;

    @Before
    public void before() {
        changeMock = mock(AsyncDataChangeEvent.class);
    }

    @Test
    public void testOnDataChangedEmptyCreatedAndRemovedPaths() {
        when(changeMock.getCreatedData()).thenReturn(new HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>>());
        when(changeMock.getRemovedPaths()).thenReturn(new HashSet<YangInstanceIdentifier>());

        TestListener testListener = new TestListener();
        testListener.onDataChanged(changeMock);

        assertEquals(testListener.getTopoRequestHandlers().size(), 0);
        verify(changeMock, times(1)).getCreatedData();
        verify(changeMock, times(1)).getRemovedPaths();
    }

    @Test
    public void testProcessCreatedDataNodeIsNotTopologyOrCorrelationsAreMissing() {
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData =
                new HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>>();

        when(changeMock.getCreatedData()).thenReturn(createdData);
        when(changeMock.getRemovedPaths()).thenReturn(new HashSet<YangInstanceIdentifier>());

        YangInstanceIdentifier yangId1 = mock(YangInstanceIdentifier.class);
        YangInstanceIdentifier yangId2 = mock(YangInstanceIdentifier.class);

        NormalizedNode<?, ?> nodeMock = mock(NormalizedNode.class);
        NormalizedNode<?, ?> mapEntryNodeMock = mock(NormalizedNode.class, withSettings().extraInterfaces(MapEntryNode.class));

        TestListener testListener = new TestListener();

        // case 1
        testListener.setTopologyReturnValue(false);
        createdData.put(yangId1, mapEntryNodeMock);
        createdData.put(yangId2, nodeMock);

        testListener.onDataChanged(changeMock);
        assertEquals(testListener.getTopoRequestHandlers().size(), 0);

        // case 2
        testListener.setTopologyReturnValue(true);
        createdData.clear();
        createdData.put(yangId1, nodeMock);

        testListener.onDataChanged(changeMock);
        assertEquals(testListener.getTopoRequestHandlers().size(), 0);

        // case 3
        testListener.setTopologyReturnValue(true);
        testListener.setTopologyRequestReturnValue(false);
        createdData.clear();
        createdData.put(yangId1, mapEntryNodeMock);

        testListener.onDataChanged(changeMock);
        assertEquals(testListener.getTopoRequestHandlers().size(), 0);
    }

    @Test
    public void testProcessCreatedDataNodeIsTopologyAndTopologyRequest () {
        // preparing nodeSerializer mock
        BindingNormalizedNodeSerializer nodeSerializerMock = mock(BindingNormalizedNodeSerializer.class);
        Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNodeMock = mock(Entry.class);
        when(nodeSerializerMock.fromNormalizedNode(any(), any())).thenReturn(fromNormalizedNodeMock);

        // preparing normalized node mock
        NormalizedNode<?, ?> mapEntryNodeMock = mock(NormalizedNode.class, withSettings().extraInterfaces(MapEntryNode.class));
        Optional<DataContainerChild<? extends PathArgument, ?>> topologyTypesMock = mock(Optional.class);
        when(topologyTypesMock.isPresent()).thenReturn(false);
        when(((MapEntryNode) mapEntryNodeMock).getChild(any())).thenReturn(topologyTypesMock);

        // preparing createdData - filling it with mocks
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData = new HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>>();
        YangInstanceIdentifier yangId1 = mock(YangInstanceIdentifier.class);
        YangInstanceIdentifier yangId2 = mock(YangInstanceIdentifier.class);
        createdData.put(yangId1, mapEntryNodeMock);
        when(changeMock.getCreatedData()).thenReturn(createdData);
        when(changeMock.getRemovedPaths()).thenReturn(new HashSet<YangInstanceIdentifier>());

        // preparing other mocks
        TopologyRequestHandler requestHandlerMock = mock(TopologyRequestHandler.class);
        DOMDataBroker dataBrokerMock = mock(DOMDataBroker.class);
        GlobalSchemaContextHolder schemaHolderMock = mock(GlobalSchemaContextHolder.class);
        RpcServices rpcServicesMock = mock(RpcServices.class);
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();

        // preparing tested TopologyRequestListener (testListener)
        TestListener testListener = new TestListener(dataBrokerMock, nodeSerializerMock, schemaHolderMock, rpcServicesMock, modelAdapters);
        testListener.setCreateTopologyRequestHandlerReturnValue(requestHandlerMock);
        testListener.setTopologyRequestReturnValue(true);
        testListener.setTopologyReturnValue(true);

        // case1 - topologyTypes is not present
        testListener.onDataChanged(changeMock);

        verify(requestHandlerMock, times(1)).processNewRequest();
        assertEquals(testListener.getTopoRequestHandlers().size(), 1);
        assertEquals(testListener.getTopoRequestHandlers().get(yangId1), requestHandlerMock);

        // case2 - topologyTypes is
        testListener.getTopoRequestHandlers().clear();
        createdData.clear();
        createdData.put(yangId2, mapEntryNodeMock);
        when(topologyTypesMock.isPresent()).thenReturn(true);
        testListener.onDataChanged(changeMock);

        verify(requestHandlerMock, times(1)).delegateTopologyTypes(any());
        assertEquals(testListener.getTopoRequestHandlers().size(), 1);
        assertEquals(testListener.getTopoRequestHandlers().get(yangId2), requestHandlerMock);
    }


    // Class just for testing abstract class TopologyRequestListener
    private class TestListener extends TopologyRequestListener {

        boolean isTopologyReturnValue;
        boolean isTopologyRequestReturnValue;
        TopologyRequestHandler requestHandlerReturnValue;

        public TestListener() {
            this(null, null, null, null, null);
        }

        public TestListener(DOMDataBroker dataBroker, BindingNormalizedNodeSerializer nodeSerializer,
                GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
                Map<Class<? extends Model>, ModelAdapter> modelAdapters) {
            super(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
        }

        @Override
        protected boolean isTopology(NormalizedNode<?, ?> normalizeNode) {
            return isTopologyReturnValue;
        }

        @Override
        protected boolean isTopologyRequest(NormalizedNode<?, ?> normalizedNode) {
            return isTopologyRequestReturnValue;
        }

        @Override
        protected TopologyRequestHandler createTopologyRequestHandler(DOMDataBroker dataBroker,
                GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
                Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            return requestHandlerReturnValue;
        }

        public void setTopologyReturnValue(boolean isTopologyReturnValue) {
            this.isTopologyReturnValue = isTopologyReturnValue;
        }


        public void setTopologyRequestReturnValue(boolean isTopologyRequestReturnValue) {
            this.isTopologyRequestReturnValue = isTopologyRequestReturnValue;
        }

        public void setCreateTopologyRequestHandlerReturnValue(TopologyRequestHandler requestHandler) {
            this.requestHandlerReturnValue = requestHandler;
        }


    }
}