package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;

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
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        aggregator = new TerminationPointAggregator(topoStoreProvider, NetworkTopologyModel.class);
        Map<Integer, YangInstanceIdentifier> targetFields = new HashMap<>(1);
        targetFields.put(0, targetField);
        aggregator.setTargetField(targetFields);
        aggregator.setTopologyManager(topoManager);
        aggregator.getTopoStoreProvider().initializeStore(TOPOLOGY_NAME, false);
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
        aggregator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);

        // check results
        Assert.assertNotNull("Manager should contain some changes", topoManager.getNewOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes", topoManager.getNewOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getNewOverlayItem().getUnderlayItems().peek().getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TopologyQNames.I2RS_TERMINATION_POINT_QNAME));
        Assert.assertTrue("Node should contain TerminationPointMap", tpMapNodeOpt.isPresent());
        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNode.getValue());
        Assert.assertEquals("Number of Termination Points", 2, mapEntryNodes.size());

        for (MapEntryNode mapEntryNode : mapEntryNodes) {
            Optional<DataContainerChild<? extends PathArgument, ?>> tpRefsOpt = mapEntryNode.getChild(new NodeIdentifier(SupportingTerminationPoint.QNAME));
            Assert.assertTrue("TP Entry should have some REFs", tpRefsOpt.isPresent());
            Collection<MapEntryNode> tpRefs = ((MapNode) tpRefsOpt.get()).getValue();
            if (3 == tpRefs.size()) {
//                TP entry node 1
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID1);
                stack.add(TP_ID3);
                stack.add(TP_ID5);
                int i = 1;
                for (MapEntryNode tpRef : tpRefs) {
                    Assert.assertNotNull("TP1 reference" + i, stack.remove(((String) tpRef.getValue().iterator().next().getValue())));
                    i++;
                }
            } else if (2 == tpRefs.size()) {
//                TP entry map node 2
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID2);
                stack.add(TP_ID4);
                int i = 1;
                for (MapEntryNode tpRef : tpRefs) {
                    Assert.assertNotNull("TP2 reference" + i, stack.remove(((String) tpRef.getValue().iterator().next().getValue())));
                    i++;
                }
            } else {
                Assert.fail("Unexpected number of TP References");
            }
        }
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
        aggregator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);

        Assert.assertNotNull("Manager should contain some changes", topoManager.getOldOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes", topoManager.getOldOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getOldOverlayItem().getUnderlayItems().peek().getItem();
        Collection<MapEntryNode> tpMapNodes = ((MapNode) NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TopologyQNames.I2RS_TERMINATION_POINT_QNAME)).get()).getValue();
        Assert.assertEquals("Number of Termination Points", 3, tpMapNodes.size());
        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNodes);
        for (MapEntryNode mapEntryNode : mapEntryNodes) {
            Optional<DataContainerChild<? extends PathArgument, ?>> tpId
                    = mapEntryNode.getChild(new NodeIdentifier(TopologyQNames.I2RS_TP_ID_QNAME));
            Assert.assertTrue("TP should contain tp-id", tpId.isPresent());
            Optional<DataContainerChild<? extends PathArgument, ?>> tpRefsOpt = mapEntryNode.getChild(new NodeIdentifier(SupportingTerminationPoint.QNAME));
            Assert.assertTrue("TP Entry should have some REFs", tpRefsOpt.isPresent());
        }
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
        aggregator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);

        MapEntryNode resultItem = (MapEntryNode) topoManager.getOldOverlayItem().getUnderlayItems().iterator().next().getItem();
        Optional<DataContainerChild<? extends PathArgument, ?>> newValueLeaf = resultItem.getChild(new NodeIdentifier(NODE_FEATURE));
        Assert.assertTrue("Node should contain 'new-value' leaf", newValueLeaf.isPresent());

        Optional<NormalizedNode<?, ?>> oldTps = NormalizedNodes.findNode(topoManager.getNewOverlayItem()
                .getUnderlayItems().iterator().next().getItem(), YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Optional<NormalizedNode<?, ?>> newTps = NormalizedNodes.findNode(resultItem, YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Assert.assertEquals("Termination Points Map Node in old and new node should be equals", oldTps, newTps);
    }
}
