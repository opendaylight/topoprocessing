package org.opendaylight.topoprocessing.impl.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractCheckedFuture;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestListenerTest {

    private TopologyRequestListener listener;
    private static final String TOPO_NAME = "mytopo:1";

    @Mock private DOMDataBroker mockBroker;
    @Mock private BindingNormalizedNodeSerializer mockNodeSerializer;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private RpcServices mockRpcServices;
    @Mock private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockChange;
    @Mock private DOMTransactionChain mockTransactionChain;
    @Mock private DOMDataWriteTransaction mockTransaction;

    @Before
    public void setUp() {
        listener = new TopologyRequestListener(mockBroker, mockNodeSerializer, mockSchemaHolder, mockRpcServices);
        listener.setDatastoreType(DatastoreType.OPERATIONAL);

        Mockito.when(mockRpcServices.getRpcService()).thenReturn(Mockito.mock(DOMRpcService.class));
        Mockito.when(mockBroker.createTransactionChain((TransactionChainListener) Matchers.any())).thenReturn(
                mockTransactionChain);
        Mockito.when(mockTransactionChain.newWriteOnlyTransaction()).thenReturn(mockTransaction);
        Mockito.when(mockTransaction.submit()).thenReturn(Mockito.mock(AbstractCheckedFuture.class));
    }

    @Test
    public void testCreateCorrectNode() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME).node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME).build();
        MapEntryNode node = ImmutableNodes.mapEntryBuilder(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME)
                .addChild(ImmutableNodes.containerNode(TopologyTypes.QNAME)).build();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = new HashMap<>();
        map.put(yiid, node);
        Mockito.when(mockChange.getCreatedData()).thenReturn(map);

        // augmentation
        CorrelationAugment mockCorrelationAugument = Mockito.mock(CorrelationAugment.class);
        Correlations mockCorrelations = Mockito.mock(Correlations.class);
        Mockito.when(mockCorrelationAugument.getCorrelations()).thenReturn(mockCorrelations);
        Mockito.when(mockCorrelations.getCorrelation()).thenReturn(new ArrayList<Correlation>());
        // topology
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topoId = TopologyId.getDefaultInstance(TOPO_NAME);
        Topology topology = topoBuilder.setKey(new TopologyKey(topoId))
                .setTopologyId(topoId)
                .addAugmentation(CorrelationAugment.class, mockCorrelationAugument)
                .build();
        Map.Entry<? extends InstanceIdentifier<?>, DataObject> topoEntry = Maps.immutableEntry(
                (InstanceIdentifier<?>) Mockito.mock(InstanceIdentifier.class), (DataObject) topology);
        Mockito.when(mockNodeSerializer.fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any())).thenReturn(
                (Map.Entry<InstanceIdentifier<?>, DataObject>) topoEntry);
        listener.onDataChanged(mockChange);

        TopologyRequestHandler handler = listener.getTopoRequestHandlers().get(yiid);
        Assert.assertNotNull("RequestHandler should be created", handler);
    }

    @Test
    public void testCreateWrongNode1() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME).build();

        MapNode node = ImmutableNodes.mapNodeBuilder(Topology.QNAME).withChild(
                ImmutableNodes.mapEntry(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME)).build();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = new HashMap<>();
        map.put(yiid, node);
        Mockito.when(mockChange.getCreatedData()).thenReturn(map);

        listener.onDataChanged(mockChange);
        Mockito.verify(mockNodeSerializer, Mockito.times(0)).fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any());
    }

    @Test
    public void testCreateWrongNode2() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME)
                .node(Node.QNAME)
                .build();
        MapNode node = ImmutableNodes.mapNodeBuilder(Node.QNAME).build();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = new HashMap<>();
        map.put(yiid, node);
        Mockito.when(mockChange.getCreatedData()).thenReturn(map);

        listener.onDataChanged(mockChange);
        Mockito.verify(mockNodeSerializer, Mockito.times(0)).fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any());
    }

    @Test
    public void testRemoveNode() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME).node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPO_NAME).build();
        Map<YangInstanceIdentifier, TopologyRequestHandler> handlers = listener.getTopoRequestHandlers();
        // pre insert topology request handler
        TopologyRequestHandler mockRequestHandler = Mockito.mock(TopologyRequestHandler.class);
        handlers.put(yiid, mockRequestHandler);
        // process removal
        Set<YangInstanceIdentifier> removedPaths = new HashSet<>();
        removedPaths.add(yiid);
        Mockito.when(mockChange.getRemovedPaths()).thenReturn(removedPaths);
        listener.onDataChanged(mockChange);
        Mockito.verify(mockRequestHandler).processDeletionRequest();
        Assert.assertEquals("RequestHandlersMap should be empty", 0, handlers.size());
    }
}
