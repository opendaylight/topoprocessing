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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
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
    private static final QName I2RS_TERMINATION_POINT_QNAME = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
            .yang.ietf.network.topology.rev150608.network.node.TerminationPoint.QNAME;
    private static final QName I2RS_IP_ADDRESS_QNAME =
        QName.create(I2RS_TERMINATION_POINT_QNAME, "ip-address").intern();
    private static final QName NT_IP_ADDRESS_QNAME =
        QName.create(TerminationPoint.QNAME, "ip-address").intern();
    private static final QName INV_IP_ADDRESS_QNAME =
        QName.create(NodeConnector.QNAME, "ip-address").intern();
    private static final QName NODE_FEATURE = QName.create(Node.QNAME, "node-feature").intern();
    private static final String TOPOLOGY_NAME = "topo:1";
    private static final String TP_ID1 = "tp1";
    private static final String TP_ID2 = "tp2";
    private static final String TP_ID3 = "tp3";
    private static final String TP_ID4 = "tp4";
    private static final String TP_ID5 = "tp5";
    private static final String IP1 = "192.168.1.10";
    private static final String IP2 = "192.168.1.11";
    private static final String IP3 = "192.168.1.10";
    private static final String IP4 = "192.168.1.11";
    private static final String IP5 = "192.168.1.10";

    private String tpRef1;
    private MapEntryNode tp1;
    private MapEntryNode tp2;
    private MapEntryNode tp3;
    private MapEntryNode tp4;
    private MapEntryNode tp5;
    private YangInstanceIdentifier targetField;
    @Mock
    private RpcServices rpcServices;
    @Mock
    private DOMRpcService domRpcService;
    @Mock
    private GlobalSchemaContextHolder schemaHolder;
    @Mock
    private YangInstanceIdentifier topologyIdentifier = YangInstanceIdentifier.of(Node.QNAME);
    private TerminationPointAggregator aggregator;
    private TpTestTopologyManager topoManager;

    private class TpTestTopologyManager extends TopologyManager {
        private OverlayItem oldOverlayItem;
        private OverlayItem newOverlayItem;

        public TpTestTopologyManager() {
            super(rpcServices, schemaHolder, topologyIdentifier, NetworkTopologyModel.class);
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
    }

    private void setInvTps() {
        tp1 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME, TopologyQNames.NODE_CONNECTOR_ID_QNAME, TP_ID1)
                .withChild(ImmutableNodes.leafNode(INV_IP_ADDRESS_QNAME, IP1)).build();
        tp2 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME, TopologyQNames.NODE_CONNECTOR_ID_QNAME, TP_ID2)
                .withChild(ImmutableNodes.leafNode(INV_IP_ADDRESS_QNAME, IP2)).build();
        tp3 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME, TopologyQNames.NODE_CONNECTOR_ID_QNAME, TP_ID3)
                .withChild(ImmutableNodes.leafNode(INV_IP_ADDRESS_QNAME, IP3)).build();
        tp4 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME, TopologyQNames.NODE_CONNECTOR_ID_QNAME, TP_ID4)
                .withChild(ImmutableNodes.leafNode(INV_IP_ADDRESS_QNAME, IP4)).build();
        tp5 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME, TopologyQNames.NODE_CONNECTOR_ID_QNAME, TP_ID5)
                .withChild(ImmutableNodes.leafNode(INV_IP_ADDRESS_QNAME, IP5)).build();
    }

    private void setI2rsTps() {
        tp1 = ImmutableNodes.mapEntryBuilder(I2RS_TERMINATION_POINT_QNAME, TopologyQNames.I2RS_TP_ID_QNAME, TP_ID1)
                .withChild(ImmutableNodes.leafNode(I2RS_IP_ADDRESS_QNAME, IP1)).build();
        tp2 = ImmutableNodes.mapEntryBuilder(I2RS_TERMINATION_POINT_QNAME, TopologyQNames.I2RS_TP_ID_QNAME, TP_ID2)
                .withChild(ImmutableNodes.leafNode(I2RS_IP_ADDRESS_QNAME, IP2)).build();
        tp3 = ImmutableNodes.mapEntryBuilder(I2RS_TERMINATION_POINT_QNAME, TopologyQNames.I2RS_TP_ID_QNAME, TP_ID3)
                .withChild(ImmutableNodes.leafNode(I2RS_IP_ADDRESS_QNAME, IP3)).build();
        tp4 = ImmutableNodes.mapEntryBuilder(I2RS_TERMINATION_POINT_QNAME, TopologyQNames.I2RS_TP_ID_QNAME, TP_ID4)
                .withChild(ImmutableNodes.leafNode(I2RS_IP_ADDRESS_QNAME, IP4)).build();
        tp5 = ImmutableNodes.mapEntryBuilder(I2RS_TERMINATION_POINT_QNAME, TopologyQNames.I2RS_TP_ID_QNAME, TP_ID5)
                .withChild(ImmutableNodes.leafNode(I2RS_IP_ADDRESS_QNAME, IP5)).build();
    }

    private void setNtTps() {
        /* exemplary Termination point */
        tpRef1 = "/(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)network-topology/topology/topology"
                + "[{(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology-id=pcep-topology:1}]"
                + "/node/node[{(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)node-id=pcep:11}]"
                + "/termination-point/termination-point[{(urn:TBD:params:xml:ns:yang:network-topology?"
                + "revision=2013-10-21)tp-id=pcep:1:1}]";
        // leaf-list tp-ref entry
        LeafSetEntryNode<Object> tpRefEntry1 = ImmutableLeafSetEntryNodeBuilder.create()
                .withNodeIdentifier(new NodeWithValue(TopologyQNames.TP_REF, tpRef1)).withValue(tpRef1).build();
        // leaf-list tp-ref
        LeafSetNode<Object> lfTpRef1 = ImmutableLeafSetNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(TopologyQNames.TP_REF)).withChild(tpRefEntry1).build();
        // TP entry
        tp1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID1)
                .withChild(lfTpRef1).withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, IP1)).build();
        tp2 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID2)
                .withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, IP2)).build();
        tp3 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID3)
                .withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, IP3)).build();
        tp4 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID4)
                .withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, IP4)).build();
        tp5 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID5)
                .withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, IP5)).build();
    }

    private void init(Class<? extends Model> model) {
        // aggregator
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        aggregator = new TerminationPointAggregator(topoStoreProvider, model);
        if (model.equals(I2rsModel.class)) {
            setI2rsTps();
            targetField = YangInstanceIdentifier.of(I2RS_IP_ADDRESS_QNAME);
        } else if (model.equals(NetworkTopologyModel.class)) {
            setNtTps();
            targetField = YangInstanceIdentifier.of(NT_IP_ADDRESS_QNAME);
        } else {
            setInvTps();
            targetField = YangInstanceIdentifier.of(INV_IP_ADDRESS_QNAME);
        }
        Map<Integer, YangInstanceIdentifier> targetFields = new HashMap<>(1);
        targetFields.put(0, targetField);
        aggregator.setTargetField(targetFields);
        aggregator.setTopologyManager(topoManager);
        aggregator.getTopoStoreProvider().initializeStore(TOPOLOGY_NAME, false);
    }

    /**
     * INVENTORY MODEL Create 5 Termination points: TP1, TP3, TP5 - with ip
     * address 192.168.1.10 TP2, TP4 - with ip address 192.168.1.11
     *
     * after aggregation: myTP1 with refs to TP1, TP3, TP5 myTP2 with refs to
     * TP2, TP4
     */
    @Test
    public void testCreateNodeInv() {
        init(OpendaylightInventoryModel.class);
        // input item - 5 TPs
        String nodeId = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder()
                .nodeWithKey(TopologyQNames.INVENTORY_NODE_QNAME, TopologyQNames.INVENTORY_NODE_ID_QNAME, nodeId)
                .build();
        MapEntryNode invNode = ImmutableNodes
                .mapEntryBuilder(TopologyQNames.INVENTORY_NODE_QNAME, TopologyQNames.INVENTORY_NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.mapNodeBuilder(NodeConnector.QNAME).withChild(tp1).withChild(tp2)
                        .withChild(tp3).withChild(tp4).withChild(tp5).build())
                .build();
        Map<Integer, NormalizedNode<?, ?>> invMap = new HashMap<>();
        invMap.put(0, invNode);
        MapEntryNode ntTp1 = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID1)
                .withChild(ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, IP1)).build();
        MapEntryNode ntTp2 = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID2)
                .withChild(ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, IP2)).build();
        MapEntryNode ntTp3 = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID3)
                .withChild(ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, IP3)).build();
        MapEntryNode ntTp4 = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID4)
                .withChild(ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, IP4)).build();
        MapEntryNode ntTp5 = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID5)
                .withChild(ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, IP5)).build();
        MapEntryNode ntNodeInput = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(ntTp1).withChild(ntTp2)
                        .withChild(ntTp3).withChild(ntTp4).withChild(ntTp5).build())
                .build();
        UnderlayItem underlayItemInput = new UnderlayItem(ntNodeInput, invMap, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node);
        aggregator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);
        // check results
        Assert.assertNotNull("Manager should contain some changes", topoManager.getNewOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes",
                topoManager.getNewOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getNewOverlayItem().getUnderlayItems().peek().getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Assert.assertTrue("Node should contain TerminationPointMap", tpMapNodeOpt.isPresent());
        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNode.getValue());
        Assert.assertEquals("Number of Termination Points", 2, mapEntryNodes.size());
        for (MapEntryNode mapEntryNode : mapEntryNodes) {
            Optional<DataContainerChild<? extends PathArgument, ?>> tpRefsOpt = mapEntryNode
                    .getChild(new NodeIdentifier(TopologyQNames.TP_REF));
            Assert.assertTrue("TP Entry should have some REFs", tpRefsOpt.isPresent());
            Collection<LeafSetEntryNode<String>> tpRefs = ((LeafSetNode<String>) tpRefsOpt.get()).getValue();
            if (3 == tpRefs.size()) {
                // TP entry node 1
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID1);
                stack.add(TP_ID3);
                stack.add(TP_ID5);
                int i = 1;
                for (LeafSetEntryNode<String> tpRef : tpRefs) {
                    Assert.assertNotNull("TP1 reference" + i, stack.remove(((String) tpRef.getValue())));
                    i++;
                }
            } else if (2 == tpRefs.size()) {
                // TP entry map node 2
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID2);
                stack.add(TP_ID4);
                int i = 1;
                for (LeafSetEntryNode<String> tpRef : tpRefs) {
                    Assert.assertNotNull("TP2 reference" + i, stack.remove(((String) tpRef.getValue())));
                    i++;
                }
            } else {
                Assert.fail("Unexpected number of TP References");
            }
        }
    }

    /**
     * Create 5 Termination points: TP1, TP3, TP5 - with ip address 192.168.1.10
     * TP2, TP4 - with ip address 192.168.1.11
     *
     * after aggregation: myTP1 with refs to TP1, TP3, TP5 myTP2 with refs to
     * TP2, TP4
     */
    @Test
    public void testCreateNodeNt() {
        init(NetworkTopologyModel.class);
        // input item - 5 TPs
        String nodeId = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.NODE_IDENTIFIER)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
        MapEntryNode nodeValueInput = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).withChild(tp2)
                        .withChild(tp3).withChild(tp4).withChild(tp5).build())
                .build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node);
        aggregator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);
        // check results
        Assert.assertNotNull("Manager should contain some changes", topoManager.getNewOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes",
                topoManager.getNewOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getNewOverlayItem().getUnderlayItems().peek().getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Assert.assertTrue("Node should contain TerminationPointMap", tpMapNodeOpt.isPresent());
        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNode.getValue());
        Assert.assertEquals("Number of Termination Points", 2, mapEntryNodes.size());
        for (MapEntryNode mapEntryNode : mapEntryNodes) {
            Optional<DataContainerChild<? extends PathArgument, ?>> tpRefsOpt = mapEntryNode
                    .getChild(new NodeIdentifier(TopologyQNames.TP_REF));
            Assert.assertTrue("TP Entry should have some REFs", tpRefsOpt.isPresent());
            Collection<LeafSetEntryNode<String>> tpRefs = ((LeafSetNode<String>) tpRefsOpt.get()).getValue();
            if (3 == tpRefs.size()) {
                // TP entry node 1
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID1);
                stack.add(TP_ID3);
                stack.add(TP_ID5);
                int i = 1;
                for (LeafSetEntryNode<String> tpRef : tpRefs) {
                    Assert.assertNotNull("TP1 reference" + i, stack.remove(((String) tpRef.getValue())));
                    i++;
                }
            } else if (2 == tpRefs.size()) {
                // TP entry map node 2
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID2);
                stack.add(TP_ID4);
                int i = 1;
                for (LeafSetEntryNode<String> tpRef : tpRefs) {
                    Assert.assertNotNull("TP2 reference" + i, stack.remove(((String) tpRef.getValue())));
                    i++;
                }
            } else {
                Assert.fail("Unexpected number of TP References");
            }
        }
    }

    @Test
    public void testCreateInsideAggregatedNodesNT() {
        init(NetworkTopologyModel.class);
        String tpId6 = "tp6";
        String tpId7 = "tp7";
        String tpId8 = "tp8";

        String topoId6 = "topo1";
        String topoId7 = "topo2";
        String topoId8 = "topo2";

        String fakeNodeId = "fakeNode";
        String nodeId6 = "node1";
        String nodeId7 = "node2";
        String nodeId8 = "node2";

        String fullTpId6 = topoId6 + "/" + nodeId6 + "/" + tpId6;
        String fullTpId7 = topoId7 + "/" + nodeId7 + "/" + tpId7;
        String fullTpId8 = topoId8 + "/" + nodeId8 + "/" + tpId8;

        String ip6 = "192.168.1.10";
        String ip7 = "192.168.1.10";
        String ip8 = "192.168.1.11";

        MapEntryNode tp6 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                fullTpId6).withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, ip6)).build();
        MapEntryNode tp7 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                fullTpId7).withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, ip7)).build();
        MapEntryNode tp8 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                fullTpId8).withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, ip8)).build();

        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.NODE_IDENTIFIER)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, fakeNodeId).build();
        MapEntryNode nodeValueInput = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, fakeNodeId)
                .withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp6).withChild(tp7)
                        .withChild(tp8).build())
                .build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, fakeNodeId,
                CorrelationItemEnum.Node);

        Map<Integer, YangInstanceIdentifier> targetFields = new HashMap<Integer, YangInstanceIdentifier>();
        targetFields.put(0, targetField);
        Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTP = new HashMap<>();

        targetFieldsPerTP.put(fullTpId6, targetFields);
        targetFieldsPerTP.put(fullTpId7, targetFields);
        targetFieldsPerTP.put(fullTpId8, targetFields);

        aggregator.setAgregationInsideAggregatedNodes(true);
        aggregator.setTargetFieldsPerTP(targetFieldsPerTP);

        aggregator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);

        Assert.assertNotNull("Manager should contain some changes", topoManager.getNewOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes",
                topoManager.getNewOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getNewOverlayItem().getUnderlayItems().peek().getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Assert.assertTrue("Node should contain TerminationPointMap", tpMapNodeOpt.isPresent());
        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();

        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNode.getValue());
        Assert.assertEquals("Number of Termination Points", 2, mapEntryNodes.size());
        for (MapEntryNode mapEntryNode : mapEntryNodes) {
            Optional<DataContainerChild<? extends PathArgument, ?>> tpRefsOpt = mapEntryNode
                    .getChild(new NodeIdentifier(TopologyQNames.TP_REF));
            Assert.assertTrue("TP Entry should have some REFs", tpRefsOpt.isPresent());
            Collection<LeafSetEntryNode<String>> tpRefs = ((LeafSetNode<String>) tpRefsOpt.get()).getValue();
            if (1 == tpRefs.size()) {
                // TP 8
                String tpRef = tpRefs.iterator().next().getValue();
                Assert.assertTrue(tpRef.contains(topoId8));
                Assert.assertTrue(tpRef.contains(tpId8));
                Assert.assertTrue(tpRef.contains(nodeId8));
            } else if (2 == tpRefs.size()) {
                // TP 6 and 7
                for (LeafSetEntryNode<String> tpRef : tpRefs) {
                    if(tpRef.getValue().contains(tpId6)) {
                        Assert.assertTrue(tpRef.getValue().contains(tpId6));
                        Assert.assertTrue(tpRef.getValue().contains(nodeId6));
                        Assert.assertTrue(tpRef.getValue().contains(topoId6));
                    } else {
                        Assert.assertTrue(tpRef.getValue().contains(tpId7));
                        Assert.assertTrue(tpRef.getValue().contains(nodeId7));
                        Assert.assertTrue(tpRef.getValue().contains(topoId7));
                    }
                }
            } else {
                Assert.fail("Unexpected number of TP References");
            }
        }
    }

    /**
     * Create 5 Termination points: TP1, TP3, TP5 - with ip address 192.168.1.10
     * TP2, TP4 - with ip address 192.168.1.11
     *
     * after aggregation: myTP1 with refs to TP1, TP3, TP5 myTP2 with refs to
     * TP2, TP4
     */
    @Test
    public void testCreateNodeI2rs() {
        init(I2rsModel.class);
        // input item - 5 TPs
        String nodeId = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder()
                .nodeWithKey(TopologyQNames.I2RS_NODE_QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeId).build();
        MapEntryNode nodeValueInput = ImmutableNodes
                .mapEntryBuilder(TopologyQNames.I2RS_NODE_QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.mapNodeBuilder(I2RS_TERMINATION_POINT_QNAME).withChild(tp1).withChild(tp2)
                        .withChild(tp3).withChild(tp4).withChild(tp5).build())
                .build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node);
        aggregator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);
        // check results
        Assert.assertNotNull("Manager should contain some changes", topoManager.getNewOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes",
                topoManager.getNewOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getNewOverlayItem().getUnderlayItems().peek().getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(I2RS_TERMINATION_POINT_QNAME));
        Assert.assertTrue("Node should contain TerminationPointMap", tpMapNodeOpt.isPresent());
        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNode.getValue());
        Assert.assertEquals("Number of Termination Points", 2, mapEntryNodes.size());
        for (MapEntryNode mapEntryNode : mapEntryNodes) {
            MapNode suppTps = (MapNode) mapEntryNode.getChild(new NodeIdentifier(SupportingTerminationPoint.QNAME))
                    .get();
            Collection<MapEntryNode> colSuppTps = suppTps.getValue();
            if (3 == colSuppTps.size()) {
                // TP entry node 1
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID1);
                stack.add(TP_ID3);
                stack.add(TP_ID5);
                int i = 1;
                for (MapEntryNode tpSupp : colSuppTps) {
                    String tpRef = (String) tpSupp.getAttributeValue(TopologyQNames.I2RS_TP_REF);
                    Assert.assertNotNull("TP1 reference" + i, stack.remove(tpRef));
                    i++;
                }
            } else if (2 == colSuppTps.size()) {
                // TP entry map node 2
                ArrayList<String> stack = new ArrayList<>();
                stack.add(TP_ID2);
                stack.add(TP_ID4);
                int i = 1;
                for (MapEntryNode tpSupp : colSuppTps) {
                    String tpRef = (String) tpSupp.getAttributeValue(TopologyQNames.I2RS_TP_REF);
                    Assert.assertNotNull("TP1 reference" + i, stack.remove(tpRef));
                    i++;
                }
            } else {
                Assert.fail("Unexpected number of TP References");
            }
        }
    }

    /**
     * create 5 TPs and aggregate them change IP address of th 5th Termination
     * Point result should be: myTP1 with refs to TP1, TP3 myTP2 with refs to
     * TP2, TP4 myTP3 with refs to TP5 id's of myTPs should not change
     */
    @Test
    public void testUpdateNodeDifferentTPsNt() {
        testCreateNodeNt();
        String nodeId = "node:1";
        String ip = "192.168.1.13";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.NODE_IDENTIFIER)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
        MapEntryNode tp = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, TP_ID5)
                .withChild(ImmutableNodes.leafNode(NT_IP_ADDRESS_QNAME, ip)).build();
        MapEntryNode nodeValueInput = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).withChild(tp2)
                        .withChild(tp3).withChild(tp4).withChild(tp).build())
                .build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);

        Assert.assertNotNull("Manager should contain some changes", topoManager.getOldOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes",
                topoManager.getOldOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getOldOverlayItem().getUnderlayItems().peek().getItem();
        Collection<MapEntryNode> tpMapNodes = ((MapNode) NormalizedNodes
                .findNode(node, YangInstanceIdentifier.of(TerminationPoint.QNAME)).get()).getValue();
        Assert.assertEquals("Number of Termination Points", 3, tpMapNodes.size());
        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNodes);
        for (MapEntryNode mapEntryNode : mapEntryNodes) {
            Optional<DataContainerChild<? extends PathArgument, ?>> tpRefs = mapEntryNode
                    .getChild(new NodeIdentifier(TopologyQNames.TP_REF));
            Assert.assertTrue("TP should contain tp-id", tpRefs.isPresent());
            List<LeafSetEntryNode<String>> tpRefEntries = new ArrayList<>(
                    ((LeafSetNode<String>) tpRefs.get()).getValue());
            if (tpRefEntries.get(0).getValue().contains("tp:1")) {
                Assert.assertTrue(tpRefEntries.get(1).getValue().contains("tp:3"));
                Assert.assertEquals("", 2, tpRefEntries.size());
            } else if (tpRefEntries.get(0).getValue().contains("tp:3")) {
                Assert.assertTrue(tpRefEntries.get(1).getValue().contains("tp:1"));
                Assert.assertEquals("", 2, tpRefEntries.size());
            } else if (tpRefEntries.get(0).getValue().contains("tp:2")) {
                Assert.assertTrue(tpRefEntries.get(1).getValue().contains("tp:4"));
                Assert.assertEquals("", 2, tpRefEntries.size());
            } else if (tpRefEntries.get(0).getValue().contains("tp:4")) {
                Assert.assertTrue(tpRefEntries.get(1).getValue().contains("tp:2"));
                Assert.assertEquals("", 2, tpRefEntries.size());
            } else if (tpRefEntries.get(0).getValue().contains("tp:5")) {
                Assert.assertEquals("", 1, tpRefEntries.size());
            }
        }
    }

    @Test
    public void testUpdateNodeSameTPsNt() {
        testCreateNodeNt();
        String nodeId = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.NODE_IDENTIFIER)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
        MapEntryNode nodeValueInput = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.leafNode(NODE_FEATURE, "new-value"))
                .withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).withChild(tp2)
                        .withChild(tp3).withChild(tp4).withChild(tp5).build())
                .build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);

        MapEntryNode resultItem = (MapEntryNode) topoManager.getOldOverlayItem().getUnderlayItems().iterator().next()
                .getItem();
        Optional<DataContainerChild<? extends PathArgument, ?>> newValueLeaf = resultItem
                .getChild(new NodeIdentifier(NODE_FEATURE));
        Assert.assertTrue("Node should contain 'new-value' leaf", newValueLeaf.isPresent());
        Optional<NormalizedNode<?, ?>> oldTps = NormalizedNodes.findNode(
                topoManager.getNewOverlayItem().getUnderlayItems().iterator().next().getItem(),
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Optional<NormalizedNode<?, ?>> newTps = NormalizedNodes.findNode(resultItem,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        Assert.assertEquals("Termination Points Map Node in old and new node should be equals", oldTps, newTps);
    }

    @Test
    public void testUpdateI2rs() {
        testCreateNodeI2rs();
        String nodeId = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder()
                .nodeWithKey(TopologyQNames.I2RS_NODE_QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeId).build();
        MapEntryNode nodeValueInput = ImmutableNodes
                .mapEntryBuilder(TopologyQNames.I2RS_NODE_QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeId).build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_NAME);
        Assert.assertNotNull("Manager should contain some changes", topoManager.getNewOverlayItem());
        Assert.assertNotNull("OverlayItem should contain some nodes",
                topoManager.getNewOverlayItem().getUnderlayItems());
        NormalizedNode<?, ?> node = topoManager.getNewOverlayItem().getUnderlayItems().peek().getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(I2RS_TERMINATION_POINT_QNAME));
        Assert.assertTrue("Node should contain TerminationPointMap", tpMapNodeOpt.isPresent());
        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
        ArrayList<MapEntryNode> mapEntryNodes = new ArrayList<>(tpMapNode.getValue());
        Assert.assertEquals("Number of Termination Points", 0, mapEntryNodes.size());
    }
}
