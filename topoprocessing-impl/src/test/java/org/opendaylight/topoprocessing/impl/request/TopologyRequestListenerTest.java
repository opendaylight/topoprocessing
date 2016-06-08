/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestListenerTest {

    private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> changeMock;
    private HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData;
    private HashSet<YangInstanceIdentifier> removedPaths;
    private HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> updatedData;

    @Before
    public void before() {
        changeMock = mock(AsyncDataChangeEvent.class);
        createdData = new HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>>();
        removedPaths = new HashSet<YangInstanceIdentifier>();
        updatedData = new HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>>();

        when(changeMock.getCreatedData()).thenReturn(createdData);
        when(changeMock.getRemovedPaths()).thenReturn(removedPaths);
        when(changeMock.getUpdatedData()).thenReturn(updatedData);
    }

    @Test
    public void testOnDataChangedEmptyCreatedAndRemovedPaths() {
        TestListener testListener = new TestListener();
        testListener.onDataChanged(changeMock);

        assertEquals(testListener.getTopoRequestHandlers().size(), 0);
        verify(changeMock, times(1)).getUpdatedData();
        verify(changeMock, times(1)).getCreatedData();
        verify(changeMock, times(1)).getRemovedPaths();
    }

    @Test
    public void testProcessCreatedDataNodeIsNotTopologyOrCorrelationsAreMissing() {
        YangInstanceIdentifier yangId1 = mock(YangInstanceIdentifier.class);
        YangInstanceIdentifier yangId2 = mock(YangInstanceIdentifier.class);

        NormalizedNode<?, ?> nodeMock = mock(NormalizedNode.class);
        NormalizedNode<?, ?> mapEntryNodeMock = mock(NormalizedNode.class,
                        withSettings().extraInterfaces(MapEntryNode.class));

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
    public void testProcessCreatedDataNodeIsTopologyAndTopologyRequest() {
        // preparing nodeSerializer mock
        BindingNormalizedNodeSerializer nodeSerializerMock = mock(BindingNormalizedNodeSerializer.class);
        Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNodeMock = mock(Entry.class);
        when(nodeSerializerMock.fromNormalizedNode(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                        .thenReturn(fromNormalizedNodeMock);

        // preparing normalized node mock that "implements" MapEntryNode
        NormalizedNode<?, ?> mapEntryNodeMock = mock(NormalizedNode.class,
                        withSettings().extraInterfaces(MapEntryNode.class));
        Optional<DataContainerChild<? extends PathArgument, ?>> topologyTypesMock = mock(Optional.class);
        when(topologyTypesMock.isPresent()).thenReturn(false);
        when(((MapEntryNode) mapEntryNodeMock).getChild(any(YangInstanceIdentifier.PathArgument.class)))
                        .thenReturn(topologyTypesMock);

        // preparing createdData - filling it with mocks
        YangInstanceIdentifier yangId1 = mock(YangInstanceIdentifier.class);
        YangInstanceIdentifier yangId2 = mock(YangInstanceIdentifier.class);
        createdData.put(yangId1, mapEntryNodeMock);

        // preparing other mocks
        TopologyRequestHandler requestHandlerMock = mock(TopologyRequestHandler.class);
        DOMDataBroker dataBrokerMock = mock(DOMDataBroker.class);
        GlobalSchemaContextHolder schemaHolderMock = mock(GlobalSchemaContextHolder.class);
        RpcServices rpcServicesMock = mock(RpcServices.class);
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();

        // preparing tested TopologyRequestListener (testListener)
        TestListener testListener = new TestListener(dataBrokerMock, nodeSerializerMock, schemaHolderMock,
                        rpcServicesMock, modelAdapters);
        testListener.setCreateTopologyRequestHandlerReturnValue(requestHandlerMock);
        testListener.setTopologyRequestReturnValue(true);
        testListener.setTopologyReturnValue(true);

        // case1 - topologyTypes is not present
        testListener.onDataChanged(changeMock);

        verify(requestHandlerMock, times(1)).processNewRequest();
        assertEquals(testListener.getTopoRequestHandlers().size(), 1);
        assertEquals(testListener.getTopoRequestHandlers().get(yangId1), requestHandlerMock);

        // case2 - topologyTypes is present
        testListener.getTopoRequestHandlers().clear();
        createdData.clear();
        createdData.put(yangId2, mapEntryNodeMock);
        when(topologyTypesMock.isPresent()).thenReturn(true);
        testListener.onDataChanged(changeMock);

        verify(requestHandlerMock, times(2)).processNewRequest();
        verify(requestHandlerMock, times(1)).delegateTopologyTypes(any(DataContainerChild.class));
        assertEquals(testListener.getTopoRequestHandlers().size(), 1);
        assertEquals(testListener.getTopoRequestHandlers().get(yangId2), requestHandlerMock);
    }

    @Test
    public void testProcessRemovedData() {
        TestListener testListener = new TestListener();
        YangInstanceIdentifier yangId1 = mock(YangInstanceIdentifier.class);
        YangInstanceIdentifier yangId2 = mock(YangInstanceIdentifier.class);
        YangInstanceIdentifier yangId3 = mock(YangInstanceIdentifier.class);
        TopologyRequestHandler requestHandlerMock = mock(TopologyRequestHandler.class);

        removedPaths.add(yangId1);
        removedPaths.add(yangId2);
        removedPaths.add(yangId3);

        testListener.getTopoRequestHandlers().put(yangId1, requestHandlerMock);
        testListener.getTopoRequestHandlers().put(yangId2, null);

        testListener.onDataChanged(changeMock);

        assertEquals(testListener.getTopoRequestHandlers().size(), 0);
        verify(requestHandlerMock, times(1)).processDeletionRequest(0);
    }

    @Test
    public void testProcessUpdatedData() {
        DOMDataBroker domDataBrokerMock = mock(DOMDataBroker.class);
        BindingNormalizedNodeSerializer bindingNormalizedNodeSerializerMock =
                mock(BindingNormalizedNodeSerializer.class);
        GlobalSchemaContextHolder globalSchemaContextHolderMock = mock(GlobalSchemaContextHolder.class);
        RpcServices rpcServicesMock = mock(RpcServices.class);
        HashMap<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<Class<? extends Model>,
                ModelAdapter>();

        MapEntryNode mapEntryNodeMock = mock(MapEntryNode.class);
        Optional<DataContainerChild<? extends PathArgument, ?>> optionalMock = mock(Optional.class);

        TestListener testListener = new TestListener(domDataBrokerMock, bindingNormalizedNodeSerializerMock,
                globalSchemaContextHolderMock, rpcServicesMock, modelAdapters);

        testListener.setCreateTopologyRequestHandlerReturnValue(mock(TopologyRequestHandler.class));

        YangInstanceIdentifier yangInstanceIdentifierMock = mock(YangInstanceIdentifier.class);
        NormalizedNode<?, ?> normalizedNodeMock = mock(NormalizedNode.class);
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> change = new HashMap<YangInstanceIdentifier,
                NormalizedNode<?, ?>>();

        TopologyRequestHandler topologyRequestHandlerMock = mock(TopologyRequestHandler.class);
        testListener.getTopoRequestHandlers().put(yangInstanceIdentifierMock, topologyRequestHandlerMock);

        when(changeMock.getUpdatedData()).thenReturn(change);
        when(mapEntryNodeMock.getChild((PathArgument)any())).thenReturn((optionalMock));

        testListener.setTopologyRequestReturnValue(false);

        testListener.onDataChanged(changeMock);
        Assert.assertTrue(testListener.getTopoRequestHandlers().get(yangInstanceIdentifierMock)
                == topologyRequestHandlerMock);
        Assert.assertEquals(1, testListener.getTopoRequestHandlers().size());

        testListener.setTopologyRequestReturnValue(true);
        testListener.setTopologyReturnValue(false);

        testListener.onDataChanged(changeMock);
        Assert.assertTrue(testListener.getTopoRequestHandlers().get(yangInstanceIdentifierMock)
                == topologyRequestHandlerMock);
        Assert.assertEquals(1, testListener.getTopoRequestHandlers().size());

        testListener.setTopologyReturnValue(true);

        testListener.onDataChanged(changeMock);
        Assert.assertTrue(testListener.getTopoRequestHandlers().get(yangInstanceIdentifierMock)
                == topologyRequestHandlerMock);
        Assert.assertEquals(1, testListener.getTopoRequestHandlers().size());

        change.put(yangInstanceIdentifierMock, normalizedNodeMock);

        testListener.onDataChanged(changeMock);
        Assert.assertTrue(testListener.getTopoRequestHandlers().get(yangInstanceIdentifierMock)
                == topologyRequestHandlerMock);
        Assert.assertEquals(1, testListener.getTopoRequestHandlers().size());

        normalizedNodeMock = mapEntryNodeMock;
        change.put(yangInstanceIdentifierMock, normalizedNodeMock);

        testListener.onDataChanged(changeMock);
        Assert.assertFalse(testListener.getTopoRequestHandlers().get(yangInstanceIdentifierMock)
                == topologyRequestHandlerMock);
    }

    /**
     * Class just for testing purpose of abstract class TopologyRequestListener.
     */
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

        @Override
        protected boolean isLinkCalculation(NormalizedNode<?, ?> normalizedNode) {
            return false;
        }
    }
}