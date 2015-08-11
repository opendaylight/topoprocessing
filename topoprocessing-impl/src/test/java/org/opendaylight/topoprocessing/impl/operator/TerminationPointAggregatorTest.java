package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.*;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TerminationPointAggregatorTest {

    private static final String TOPOLOGY_NAME = "topo:1";
    private static final QName IP_ADDRESS_QNAME = QName.create(TerminationPoint.QNAME, "ip-address");
    private static final QName NODE_FEATURE = QName.create(Node.QNAME, "node-feature");
    private static final String TP_ID1 = "tp1";
    private static final String TP_ID2 = "tp2";
    private static final String TP_ID3 = "tp3";
    private static final String TP_ID4 = "tp4";
    private static final String TP_ID5 = "tp5";

    private String tpRef1;
    private MapEntryNode tp1;
    private MapEntryNode tp2;
    private MapEntryNode tp3;

    private MapEntryNode tp4;
    private MapEntryNode tp5;
    private YangInstanceIdentifier targetField = YangInstanceIdentifier.of(IP_ADDRESS_QNAME);
    @Mock private RpcServices rpcServices;
    @Mock private DOMRpcService domRpcService;
    @Mock private GlobalSchemaContextHolder schemaHolder;
    @Mock private YangInstanceIdentifier topologyIdentifier = YangInstanceIdentifier.of(Node.QNAME);
    private TerminationPointAggregator aggregator;
    private TpTestTopologyManager topoManager;

    private class TpTestTopologyManager extends TopologyManager {
        public OverlayItem oldOverlayItem;
        public OverlayItem newOverlayItem;

        public TpTestTopologyManager() {
            super(rpcServices, schemaHolder, topologyIdentifier);
        }

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            this.newOverlayItem = newOverlayItem;
        }

        @Override
        public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
            this.oldOverlayItem = overlayItemIdentifier;
        }

        public OverlayItem getOldOverlayItem() {
            return oldOverlayItem;
        }

        public OverlayItem getNewOverlayItem() {
            return newOverlayItem;
        }
    }

    @Before
    public void setUp() {

        // setup topologyManager
        Mockito.when(rpcServices.getRpcService()).thenReturn(domRpcService);
        topoManager = new TpTestTopologyManager();

         /* exemplary Termination point */
        // leaf-list entry ip-address
        String ip1 = "192.168.1.10";
        tpRef1 = "/(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)network-topology" +
                "/topology/topology[{(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology-id=pcep-topology:1}]" +
                "/node/node[{(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)node-id=pcep:11}]" +
                "/termination-point/termination-point[{(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)tp-id=pcep:1:1}]";
        // leaf-list tp-ref entry
        LeafSetEntryNode<Object> tpRefEntry1 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new NodeWithValue(TopologyQNames.TP_REF, tpRef1)).withValue(tpRef1).build();
        // leaf-list tp-ref
        LeafSetNode<Object> lfTpRef1 = ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                new NodeIdentifier(TopologyQNames.TP_REF)).withChild(tpRefEntry1).build();
        // TP entry
        tp1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID1)
                .withChild(lfTpRef1)
                .withChild(ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ip1))
                .build();

        String ip2 = "192.168.1.11";
        tp2 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID2)
                .withChild(ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ip2)).build();

        String ip3 = "192.168.1.10";
        tp3 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID3)
                .withChild(ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ip3)).build();

        String ip4 = "192.168.1.11";
        tp4 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID4)
                .withChild(ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ip4)).build();

        String ip5 = "192.168.1.10";
        tp5 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID5)
                .withChild(ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ip5)).build();

        // aggregator
        aggregator = new TerminationPointAggregator();
        aggregator.setTargetField(targetField);
        aggregator.setTopologyManager(topoManager);
        aggregator.initializeStore(TOPOLOGY_NAME, false);
    }

    /**
     * Create 5 Termination points:
     * TP1, TP3, TP5 - with ip address 192.168.1.10
     * TP2, TP4 - with ip address 192.168.1.11
     *
     * after aggregation:
     * myTP1 with refs to TP1, TP3, TP5
     * myTP2 with refs to TP2, TP4
     */
    @Test
    public void testCreateNode() {
        // input item - 5 TPs
        String nodeId = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers
                .NODE_IDENTIFIER).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
        MapEntryNode nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                nodeId).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                        .withChild(tp1)
                        .withChild(tp2)
                        .withChild(tp3)
                        .withChild(tp4)
                        .withChild(tp5)
                        .build()
        ).build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node);
        aggregator.processCreatedChanges(Collections.singletonMap(nodeYiid, underlayItemInput), TOPOLOGY_NAME);

        // check results
        Assert.assertNotNull("Manager should contain some changes", topoManager.getNewOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes", topoManager.getNewOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getNewOverlayItem().getUnderlayItems().get(0).getItem();
        MapNode tpMapNodes = (MapNode) NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME)).get();
        Assert.assertEquals("Number of Termination Points", 2, tpMapNodes.getValue().size());

        // tp map node 1
        Optional<NormalizedNode<?, ?>> tp1Opt = NormalizedNodes.findNode(tpMapNodes, YangInstanceIdentifier.builder()
                .nodeWithKey(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, "tp:1").build());
        Assert.assertTrue("TP with id 'tp:1' should be presented", tp1Opt.isPresent());
        Optional<DataContainerChild<? extends PathArgument, ?>> tp3refs
                = ((MapEntryNode) tp1Opt.get()).getChild(new NodeIdentifier(TopologyQNames.TP_REF));
        Assert.assertTrue("TP Map1 should contains TP-REFS", tp3refs.isPresent());
        ArrayList<LeafSetEntryNode> refListNode3 = new ArrayList<>(((LeafSetNode) tp3refs.get()).getValue());
        Assert.assertEquals("number of Termination Point References in TP1", 2, refListNode3.size());
        Assert.assertEquals("TP1 reference1", TP_ID4, ((LeafSetEntryNode) refListNode3.get(0).getValue()).getValue());
        Assert.assertEquals("TP1 reference2", TP_ID2, ((LeafSetEntryNode) refListNode3.get(1).getValue()).getValue());

        // tp map node 2
        Optional<NormalizedNode<?, ?>> tp2Opt = NormalizedNodes.findNode(tpMapNodes, YangInstanceIdentifier.builder()
                .nodeWithKey(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, "tp:2").build());
        Assert.assertTrue("TP with id 'tp:2' should be presented", tp2Opt.isPresent());
        Optional<DataContainerChild<? extends PathArgument, ?>> tp2refs
                = ((MapEntryNode) tp2Opt.get()).getChild(new NodeIdentifier(TopologyQNames.TP_REF));

        Assert.assertTrue("TP2 should contains TP-REFS", tp2refs.isPresent());
        ArrayList<LeafSetEntryNode> refListNode2 = new ArrayList<>(((LeafSetNode) tp2refs.get()).getValue());
        Assert.assertEquals("number of Termination Point References in TP2", 3, refListNode2.size());
//        Assert.assertEquals("Check if previous reference stayed in node", refListNode2.get(0).getValue(), tpRef1);
        Assert.assertEquals("TP2 reference1", TP_ID5, ((LeafSetEntryNode) refListNode2.get(0).getValue()).getValue());
        Assert.assertEquals("TP2 reference2", TP_ID1, ((LeafSetEntryNode) refListNode2.get(1).getValue()).getValue());
        Assert.assertEquals("TP2 reference3", TP_ID3, ((LeafSetEntryNode) refListNode2.get(2).getValue()).getValue());
    }

    /**
     * create 5 TPs and aggregate them
     * change IP address of th 5th Termination Point
     * result should be:
     * myTP1 with refs to TP1, TP3
     * myTP2 with refs to TP2, TP4
     * myTP3 with refs to TP5
     * id's of myTPs should not change
     */
    @Test
    public void testUpdateNodeDifferentTPs() {
        testCreateNode();

        String nodeId = "node:1";
        String ip = "192.168.1.13";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers
                .NODE_IDENTIFIER).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
        MapEntryNode tp = ImmutableNodes.mapEntryBuilder(
                TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID5)
                .withChild(ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ip)).build();
        MapEntryNode nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                nodeId).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                        .withChild(tp1)
                        .withChild(tp2)
                        .withChild(tp3)
                        .withChild(tp4)
                        .withChild(tp)
                        .build()
                ).build();

        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(Collections.singletonMap(nodeYiid, underlayItemInput), TOPOLOGY_NAME);

        Assert.assertNotNull("Manager should contain some changes", topoManager.getOldOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes", topoManager.getOldOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getOldOverlayItem().getUnderlayItems().get(0).getItem();
        Collection<MapEntryNode> tpMapNodes = ((MapNode) NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME)).get()).getValue();
        Assert.assertEquals("Number of Termination Points", 3, tpMapNodes.size());

        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNodes);

        // TP1
        Assert.assertEquals("Termination Point 1 ID differs", "tp:1",
                mapEntryNodes.get(2).getChild(new NodeIdentifier(TopologyQNames.NETWORK_TP_ID_QNAME)).get().getValue());
        // TP2
        Assert.assertEquals("Termination Point 2 ID differs", "tp:2",
                mapEntryNodes.get(0).getChild(new NodeIdentifier(TopologyQNames.NETWORK_TP_ID_QNAME)).get().getValue());
        // TP3
        Assert.assertEquals("Termination Point 3 ID differs", "tp5",
                mapEntryNodes.get(1).getChild(new NodeIdentifier(TopologyQNames.NETWORK_TP_ID_QNAME)).get().getValue());
        Optional<DataContainerChild<? extends PathArgument, ?>> tp3refs
                = mapEntryNodes.get(1).getChild(new NodeIdentifier(TopologyQNames.TP_REF));
        Assert.assertFalse("Termination Point 3 should not contains TP-REFS", tp3refs.isPresent());
        Optional<DataContainerChild<? extends PathArgument, ?>> tpId
                = mapEntryNodes.get(1).getChild(new NodeIdentifier(TopologyQNames.NETWORK_TP_ID_QNAME));
        Assert.assertTrue("Termination Point 3 should contain ID ", tpId.isPresent());
        Assert.assertEquals("Termination Point should be equal", TP_ID5, tpId.get().getValue());
    }

    @Test
    public void testUpdateNodeSameTPs() {
        String nodeId = "node:1";
//        String ip = "192.168.1.13";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers
                .NODE_IDENTIFIER).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
        MapEntryNode nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                nodeId)
                .withChild(ImmutableNodes.leafNode(NODE_FEATURE, "new-value"))
                .withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                                .withChild(tp1)
                                .withChild(tp2)
                                .withChild(tp3)
                                .withChild(tp4)
                                .withChild(tp5)
                                .build()
                ).build();

        testCreateNode();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(Collections.singletonMap(nodeYiid, underlayItemInput), TOPOLOGY_NAME);

        MapEntryNode resultItem = (MapEntryNode) topoManager.getOldOverlayItem().getUnderlayItems().iterator().next().getItem();
        Optional<DataContainerChild<? extends PathArgument, ?>> newValueLeaf = resultItem.getChild(new NodeIdentifier(NODE_FEATURE));
        Assert.assertTrue("Node should contain 'new-value' leaf", newValueLeaf.isPresent());

        Optional<NormalizedNode<?, ?>> oldTps = NormalizedNodes.findNode(topoManager.getNewOverlayItem()
                .getUnderlayItems().iterator().next().getItem(), YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Optional<NormalizedNode<?, ?>> newTps = NormalizedNodes.findNode(resultItem, YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Assert.assertEquals("Termination Points Map Node in old and new node should be equals", oldTps, newTps);
    }
}
