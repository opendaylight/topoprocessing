/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.nt.request;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.model.ModelAdapter;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.nt.model.NTModelAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractCheckedFuture;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class NTTopologyRequestListenerTest {

    private TopologyRequestListener listener;
    private static final String TOPO_NAME = "mytopo:1";

    @Mock private DOMDataBroker mockBroker;
    @Mock private DOMDataTreeChangeService mockDataTreeChangeService;
    @Mock private BindingNormalizedNodeSerializer mockNodeSerializer;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private RpcServices mockRpcServices;
    @Mock private DOMTransactionChain mockTransactionChain;
    @Mock private DOMDataWriteTransaction mockTransaction;
    @Mock private FiltratorFactory userDefinedFiltratorFactory;
    @Mock private DataTreeCandidate mockRootChange;
    @Mock private DataTreeCandidateNode mockRootNode;
    @Mock private DataTreeCandidateNode mockTopology;
    @Mock private PathArgument mockTopologyPathArgument;
    private Collection<DataTreeCandidate> changes;
    private Collection<DataTreeCandidateNode> topologies;

    private final YangInstanceIdentifier rootYangID = YangInstanceIdentifier.of(NetworkTopology.QNAME)
            .node(Topology.QNAME);
    private final Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();

    private class UserDefinedFilter extends FilterBase {
        // testing class for testing purpose
    }

    @Before
    public void setUp() {
        topologies = new LinkedList<>();
        when(mockRootNode.getChildNodes()).thenReturn(topologies);
        when(mockRootChange.getRootNode()).thenReturn(mockRootNode);
        when(mockRootChange.getRootPath()).thenReturn(rootYangID);
        when(mockTopology.getIdentifier()).thenReturn(mockTopologyPathArgument);
        changes = new LinkedList<>();
        changes.add(mockRootChange);

        modelAdapters.put( NetworkTopologyModel.class, new NTModelAdapter());
        listener = new NTTopologyRequestListener(mockBroker, mockDataTreeChangeService, mockNodeSerializer,
                mockSchemaHolder, mockRpcServices, modelAdapters);
        listener.setDatastoreType(LogicalDatastoreType.OPERATIONAL);

        when(mockRpcServices.getRpcService()).thenReturn(Mockito.mock(DOMRpcService.class));
        when(mockBroker.createTransactionChain((TransactionChainListener) Matchers.any())).thenReturn(
                mockTransactionChain);
        when(mockTransactionChain.newWriteOnlyTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.submit()).thenReturn(Mockito.mock(AbstractCheckedFuture.class));
    }

    @Test
    public void testCreateCorrectNode() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME).node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME).build();
        MapEntryNode node = ImmutableNodes.mapEntryBuilder(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME)
                .addChild(createAugNode()).build();
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        DataTreeCandidateNode topologyCandidateNode = DataTreeCandidateNodes.fromNormalizedNode(node);
        topologies.add(topologyCandidateNode);
        // augmentation
        CorrelationAugment mockCorrelationAugument = Mockito.mock(CorrelationAugment.class);
        Correlations mockCorrelations = Mockito.mock(Correlations.class);
        when(mockCorrelationAugument.getCorrelations()).thenReturn(mockCorrelations);
        when(mockCorrelations.getCorrelation()).thenReturn(new ArrayList<Correlation>());
        Answer<Class<? extends Model>> answer = new Answer<Class<? extends Model>>() {
            @Override
            public Class<? extends Model> answer(InvocationOnMock invocation)
                    throws Throwable {
                return NetworkTopologyModel.class;
            }
        };
        when(mockCorrelations.getOutputModel()).then(answer);
        // topology
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topoId = TopologyId.getDefaultInstance(TOPO_NAME);
        Topology topology = topoBuilder.setKey(new TopologyKey(topoId))
                .setTopologyId(topoId)
                .addAugmentation(CorrelationAugment.class, mockCorrelationAugument)
                .build();
        Map.Entry<? extends InstanceIdentifier<?>, DataObject> topoEntry = Maps.immutableEntry(
                (InstanceIdentifier<?>) Mockito.mock(InstanceIdentifier.class), (DataObject) topology);
        when(mockNodeSerializer.fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any())).thenReturn(
                (Map.Entry<InstanceIdentifier<?>, DataObject>) topoEntry);

        listener.onDataTreeChanged(changes);
        TopologyRequestHandler handler = listener.getTopoRequestHandlers().get(yiid);
        Assert.assertNotNull("RequestHandler should be created", handler);
    }

    private AugmentationNode createAugNode() {
        Set<QName> qnames = new HashSet<>();
        qnames.add(TopologyQNames.TOPOLOGY_CORRELATION_AUGMENT);
        AugmentationIdentifier augId = new AugmentationIdentifier(qnames);
        AugmentationNode augNode = ImmutableAugmentationNodeBuilder.create()
                .withNodeIdentifier(augId).withChild(ImmutableNodes.containerNode(Correlations.QNAME))
                .build();
        return augNode;
    }

    @Test
    public void testCreateWrongNode1() {
        MapNode node = ImmutableNodes.mapNodeBuilder(Topology.QNAME).withChild(
                ImmutableNodes.mapEntry(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME)).build();
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        DataTreeCandidateNode topologyCandidateNode = DataTreeCandidateNodes.fromNormalizedNode(node);
        topologies.add(topologyCandidateNode);
        listener.onDataTreeChanged(changes);
        Mockito.verify(mockNodeSerializer, Mockito.times(0)).fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any());
    }

    @Test
    public void testCreateWrongNode2() {
        MapNode node = ImmutableNodes.mapNodeBuilder(Node.QNAME).build();
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        DataTreeCandidateNode topologyCandidateNode = DataTreeCandidateNodes.fromNormalizedNode(node);
        topologies.add(topologyCandidateNode);

        listener.onDataTreeChanged(changes);
        Mockito.verify(mockNodeSerializer, Mockito.times(0)).fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any());
    }

    @Test
    public void testRemoveNode() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME).node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME).build();
        Map<YangInstanceIdentifier, TopologyRequestHandler> handlers = listener.getTopoRequestHandlers();
        MapEntryNode node = ImmutableNodes.mapEntryBuilder(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME)
                .addChild(createAugNode()).build();
        // pre insert topology request handler
        TopologyRequestHandler mockRequestHandler = Mockito.mock(TopologyRequestHandler.class);
        handlers.put(yiid, mockRequestHandler);
        // process removal
        when(mockRootNode.getModificationType()).thenReturn(ModificationType.SUBTREE_MODIFIED);
        when(mockTopology.getDataAfter()).thenReturn(Optional.absent());
        when(mockTopology.getIdentifier()).thenReturn(node.getIdentifier());
        topologies.add(mockTopology);
        listener.onDataTreeChanged(changes);
        Mockito.verify(mockRequestHandler).processDeletionRequest(0);
        Assert.assertEquals("RequestHandlersMap should be empty", 0, handlers.size());
    }

    @Test
    public void testRegisterFiltrator() {
        listener.registerFiltrator(UserDefinedFilter.class, userDefinedFiltratorFactory);
        Assert.assertEquals("Listener's map should contain default filtrators plus one (the registrated)",
                DefaultFiltrators.getDefaultFiltrators().size() + 1, listener.getFiltrators().size());
    }

    @Test
    public void testUnregisterFiltrator() {
        testRegisterFiltrator();
        listener.unregisterFiltrator(UserDefinedFilter.class);
        Assert.assertEquals("The map should contain default filtrators again -"
                + "after the user filtrator was added and removed.",
                DefaultFiltrators.getDefaultFiltrators().size() , listener.getFiltrators().size());
    }

    /**
     * Unregistering of non-existing filtrator should not cause any problem.
     */
    @Test
    public void testUnregisterNonExistingFiltrator() {
        listener.unregisterFiltrator(UserDefinedFilter.class);
        Assert.assertEquals("Default filtrators should remain untouched",
                DefaultFiltrators.getDefaultFiltrators().size() , listener.getFiltrators().size());
    }
}