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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

import com.google.common.base.Optional;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestListenerTest {

    @Mock
    private DataTreeCandidate mockRootChange;
    @Mock
    private DataTreeCandidateNode mockRootNode;
    @Mock
    private DataTreeCandidateNode mockTopology;
    @Mock
    private TopologyRequestHandler requestHandlerMock;
    @Mock
    private Optional<DataContainerChild<? extends PathArgument, ?>> topologyTypesMock;

    private Collection<DataTreeCandidate> changes;
    private Collection<DataTreeCandidateNode> topologies;
    private TestListener testListener;

    private final YangInstanceIdentifier rootYangID = YangInstanceIdentifier.of(NetworkTopology.QNAME)
            .node(Topology.QNAME);
    @Mock
    private PathArgument mockTopologyPathArgument;

    @Before
    public void before() {
        topologies = new LinkedList<>();
        topologies.add(mockTopology);
        when(mockRootNode.getChildNodes()).thenReturn(topologies);
        when(mockRootChange.getRootNode()).thenReturn(mockRootNode);
        when(mockRootChange.getRootPath()).thenReturn(rootYangID);
        when(mockTopology.getIdentifier()).thenReturn(mockTopologyPathArgument);
        changes = new LinkedList<>();
        changes.add(mockRootChange);

        // preparing other mocks
        DOMDataBroker dataBrokerMock = mock(DOMDataBroker.class);
        GlobalSchemaContextHolder schemaHolderMock = mock(GlobalSchemaContextHolder.class);
        RpcServices rpcServicesMock = mock(RpcServices.class);
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        DOMDataTreeChangeService mockDomDataTreeChangeService = mock(DOMDataTreeChangeService.class);
        // preparing nodeSerializer mock
        BindingNormalizedNodeSerializer nodeSerializerMock = mock(BindingNormalizedNodeSerializer.class);
        Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNodeMock = mock(Entry.class);
        when(nodeSerializerMock.fromNormalizedNode(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(fromNormalizedNodeMock);
        // preparing tested TopologyRequestListener (testListener)
        testListener = new TestListener(dataBrokerMock, mockDomDataTreeChangeService, nodeSerializerMock,
                schemaHolderMock, rpcServicesMock, modelAdapters);
        testListener.setCreateTopologyRequestHandlerReturnValue(requestHandlerMock);
    }

    private NormalizedNode<?, ?> createTopologyMapNodeEntry() {
        // preparing normalized node mock that "implements" MapEntryNode
        NormalizedNode<?, ?> mapEntryNodeMock = mock(NormalizedNode.class,
                withSettings().extraInterfaces(MapEntryNode.class));
        when(((MapEntryNode) mapEntryNodeMock).getChild(any(YangInstanceIdentifier.PathArgument.class)))
                .thenReturn(topologyTypesMock);
        return mapEntryNodeMock;
    }

    @Test
    public void testOnDataTreeChangedUnmodified() {
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.UNMODIFIED);
        testListener.onDataTreeChanged(changes);
        assertEquals(0, testListener.getTopoRequestHandlers().size());
    }

    /**
     * Tests writing a new topology request into empty datastore.
     */
    @Test
    public void testWritingTopologyRequest() {
        Optional<NormalizedNode<?, ?>> optionalMapEntryNodeMock = Optional.of(createTopologyMapNodeEntry());
        when(mockTopology.getDataAfter()).thenReturn(optionalMapEntryNodeMock);
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.WRITE);
        when(topologyTypesMock.isPresent()).thenReturn(true);

        testListener.setTopologyReturnValue(true);
        testListener.setTopologyRequestReturnValue(true);

        // case 1 - no topologyTypes defined
        testListener.onDataTreeChanged(changes);
        assertEquals(1, testListener.getTopoRequestHandlers().size());
        verify(requestHandlerMock).processNewRequest();
        verify(requestHandlerMock, times(1)).delegateTopologyTypes(any());

        // case 2 - when topologyTypes is not present
        testListener.getTopoRequestHandlers().clear();
        when(topologyTypesMock.isPresent()).thenReturn(false);
        testListener.onDataTreeChanged(changes);
        verify(requestHandlerMock, times(2)).processNewRequest();
        verify(requestHandlerMock, times(1)).delegateTopologyTypes(any());
        assertEquals(testListener.getTopoRequestHandlers().size(), 1);
    }

    /**
     * Tests adding a new topology request alongside others.
     */
    @Test
    public void testAddingTopologyRequest() {
        Optional<NormalizedNode<?, ?>> optionalMapEntryNodeMock = Optional.of(createTopologyMapNodeEntry());
        when(mockTopology.getDataAfter()).thenReturn(optionalMapEntryNodeMock);
        when(mockTopology.getDataBefore()).thenReturn(Optional.absent());
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(topologyTypesMock.isPresent()).thenReturn(true);

        testListener.setTopologyReturnValue(true);
        testListener.setTopologyRequestReturnValue(true);
        testListener.onDataTreeChanged(changes);
        assertEquals(1, testListener.getTopoRequestHandlers().size());
        verify(requestHandlerMock).processNewRequest();
        verify(requestHandlerMock, times(1)).delegateTopologyTypes(any());
    }

    /**
     * Tests the handling of writing a data topology (no request).
     */
    @Test
    public void testWritingDataTopology() {
        Optional<NormalizedNode<?, ?>> optionalMapEntryNodeMock = Optional.of(createTopologyMapNodeEntry());
        when(mockTopology.getDataAfter()).thenReturn(optionalMapEntryNodeMock);
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.WRITE);
        testListener.setTopologyReturnValue(true);
        testListener.setTopologyRequestReturnValue(false);

        testListener.onDataTreeChanged(changes);
        assertEquals(0, testListener.getTopoRequestHandlers().size());
        verify(requestHandlerMock, never()).processNewRequest();
        verify(requestHandlerMock, never()).delegateTopologyTypes(any());
    }

    @Test
    public void testRemovingTopologyRequestFromOthers() {
        YangInstanceIdentifier yangIDToRemove = YangInstanceIdentifier.builder(mockRootChange.getRootPath())
                .node(mockTopologyPathArgument).build();
        testListener.getTopoRequestHandlers().put(yangIDToRemove, requestHandlerMock);

        // preparing topology mock
        when(mockTopology.getDataAfter()).thenReturn(Optional.absent());
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        testListener.setTopologyReturnValue(true);
        testListener.setTopologyRequestReturnValue(true);

        testListener.onDataTreeChanged(changes);

        assertEquals(0, testListener.getTopoRequestHandlers().size());
        verify(requestHandlerMock, times(1)).processDeletionRequest(Mockito.anyInt());
    }

    @Test
    public void testRemovingDataTopologyFromOthers() {
        YangInstanceIdentifier yangIDToRemove = mock(YangInstanceIdentifier.class);
        testListener.getTopoRequestHandlers().put(yangIDToRemove, requestHandlerMock);

        // preparing topology mock
        when(mockTopology.getDataAfter()).thenReturn(Optional.absent());
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        testListener.setTopologyReturnValue(true);
        testListener.setTopologyRequestReturnValue(true);

        testListener.onDataTreeChanged(changes);
        assertEquals(1, testListener.getTopoRequestHandlers().size());
        verify(requestHandlerMock, never()).processDeletionRequest(anyInt());
    }

    @Test
    public void testUpdatingTopologyRequest() {
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        Optional<NormalizedNode<?, ?>> optionalMapEntryNodeMock = Optional.of(createTopologyMapNodeEntry());
        when(mockTopology.getDataAfter()).thenReturn(optionalMapEntryNodeMock);
        when(mockTopology.getDataBefore()).thenReturn(optionalMapEntryNodeMock);
        testListener.setTopologyReturnValue(true);
        testListener.setTopologyRequestReturnValue(true);
        YangInstanceIdentifier yangIDToUpdate = YangInstanceIdentifier.builder(mockRootChange.getRootPath())
                .node(mockTopologyPathArgument).build();
        testListener.getTopoRequestHandlers().put(yangIDToUpdate, requestHandlerMock);

        testListener.onDataTreeChanged(changes);
        assertEquals(requestHandlerMock, testListener.getTopoRequestHandlers().get(yangIDToUpdate));
        assertEquals(1, testListener.getTopoRequestHandlers().size());
        verify(requestHandlerMock).processDeletionRequest(anyInt());
        verify(requestHandlerMock).processNewRequest();
    }

    /**
     * Class just for testing purpose of abstract class TopologyRequestListener.
     */
    private static class TestListener extends TopologyRequestListener {

        private boolean isTopologyReturnValue;
        private boolean isTopologyRequestReturnValue;
        TopologyRequestHandler requestHandlerReturnValue;

        public TestListener(DOMDataBroker dataBroker, DOMDataTreeChangeService domDataTreeChangeService,
                BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
                RpcServices rpcServices, Map<Class<? extends Model>, ModelAdapter> modelAdapters) {
            super(dataBroker, domDataTreeChangeService, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
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
                DOMDataTreeChangeService domDataTreeChangeService, GlobalSchemaContextHolder schemaHolder,
                RpcServices rpcServices, Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
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