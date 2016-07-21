package org.opendaylight.topoprocessing.impl.operator;

import java.util.Collections;
import java.util.HashMap;
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
import org.opendaylight.topoprocessing.impl.operator.filtrator.RangeNumberFiltrator;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TerminationPointFiltratorTest {

    private static final String NT_TOPOLOGY_ID = "topo:1";
    private static final String INV_TOPOLOGY_ID = "topo:2";
    private static final String NODE_ID = "node:1";
    private static final int FILTRATOR_MIN_VALUE = 10;
    private static final int FILTRATOR_MAX_VALUE = 20;
    private static final QName UNNUMBERED_QNAME =
        QName.create(TerminationPoint.QNAME, "unnumbered").intern();
    private static final QName INVENTORY_NUMBER_VALUE_QNAME =
        QName.create(NodeConnector.QNAME, "number-value").intern();

    private TerminationPointFiltrator networkTopoModelFiltrator;
    private TerminationPointFiltrator inventoryModelFiltrator;
    private YangInstanceIdentifier pathForNTFiltrator = YangInstanceIdentifier.of(UNNUMBERED_QNAME);
    private YangInstanceIdentifier pathForINVFiltrator = YangInstanceIdentifier.of(INVENTORY_NUMBER_VALUE_QNAME);
    private YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.NODE_IDENTIFIER)
            .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID).build();
    private YangInstanceIdentifier topologyIdentifierMock = YangInstanceIdentifier.EMPTY;

    @Mock private RpcServices rpcServicesMock;
    @Mock private GlobalSchemaContextHolder schemaHolderMock;
    @Mock private DOMRpcService domRpcServiceMock;
    private TestManager manager;

    class TestManager extends TopologyManager {

        private MapEntryNode output;

        public TestManager() {
            super(rpcServicesMock, schemaHolderMock, topologyIdentifierMock, NetworkTopologyModel.class);
        }

        public void setOutput(MapEntryNode output) {
            this.output = output;
        }

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            MapEntryNode inputNode = (MapEntryNode) newOverlayItem.getUnderlayItems().iterator().next().getItem();
            Assert.assertEquals("Nodes should be equal", output, inputNode);
        }
    }

    @Before
    public void setUp() {
        Mockito.when(rpcServicesMock.getRpcService()).thenReturn(domRpcServiceMock);
        manager = new TestManager();
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();

        //set up NT model filtration testing
        networkTopoModelFiltrator = new TerminationPointFiltrator(topoStoreProvider, NetworkTopologyModel.class);
        topoStoreProvider.initializeStore(NT_TOPOLOGY_ID, false);
        RangeNumberFiltrator ntFilter = new RangeNumberFiltrator(FILTRATOR_MIN_VALUE, FILTRATOR_MAX_VALUE,
                pathForNTFiltrator);
        networkTopoModelFiltrator.addFilter(ntFilter);
        networkTopoModelFiltrator.setTopologyManager(manager);

        //set up inventory model filtration testing
        inventoryModelFiltrator = new TerminationPointFiltrator(topoStoreProvider, OpendaylightInventoryModel.class);
        topoStoreProvider.initializeStore(INV_TOPOLOGY_ID, false);
        RangeNumberFiltrator inventoryFilter = new RangeNumberFiltrator(FILTRATOR_MIN_VALUE, FILTRATOR_MAX_VALUE,
                pathForINVFiltrator);
        inventoryModelFiltrator.addFilter(inventoryFilter);
        inventoryModelFiltrator.setTopologyManager(manager);
    }

    /**
     * Test the ProcessCreatedChanges method with input based on network topology model.
     */
    @Test
    public void testProcessCreatedChangesOnNTModel() {
        String tpId1 = "tp1";
        String value1 = "15";
        String tpId2 = "tp2";
        String value2 = "30";
        MapEntryNode tp1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                tpId1).withChild(ImmutableNodes.leafNode(UNNUMBERED_QNAME, value1)).build();
        MapEntryNode tp2 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                tpId2).withChild(ImmutableNodes.leafNode(UNNUMBERED_QNAME, value2)).build();

        // input
        MapEntryNode nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).withChild(tp2)
                .build()).build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, NT_TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);

        // output
        MapEntryNode nodeValueOutput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).build())
                .build();
        UnderlayItem underlayItemOutput = new UnderlayItem(nodeValueOutput, null, NT_TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);
        OverlayItem overlayItemOutput = new OverlayItem(Collections.singletonList(underlayItemOutput),
                CorrelationItemEnum.TerminationPoint);
        underlayItemOutput.setOverlayItem(overlayItemOutput);

        manager.setOutput(nodeValueOutput);
        networkTopoModelFiltrator.processCreatedChanges(nodeYiid, underlayItemInput, NT_TOPOLOGY_ID);
    }


    /**
     * Test the ProcessCreatedChanges method with input based on inventory model.
     */
    @Test
    public void testProcessCreatedChangesOnInventoryModel() {
        String nodeConnectorId1 = "tp1";
        String value1 = "15";
        String nodeConnectorId2 = "tp2";
        String value2 = "30";
        String tpId1 = "tp:1";
        String tpId2 = "tp:2";

        //create INV node
        LeafNode<String> connectorValueLeaf1 = ImmutableNodes.leafNode(INVENTORY_NUMBER_VALUE_QNAME, value1);
        LeafNode<String> connectorValueLeaf2 = ImmutableNodes.leafNode(INVENTORY_NUMBER_VALUE_QNAME, value2);

        MapEntryNode nodeConnector1 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME,
                TopologyQNames.NODE_CONNECTOR_ID_QNAME, nodeConnectorId1).withChild(connectorValueLeaf1).build();
        MapEntryNode nodeConnector2 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME,
                TopologyQNames.NODE_CONNECTOR_ID_QNAME, nodeConnectorId2).withChild(connectorValueLeaf2).build();

        MapNode nodeConnectorList = ImmutableNodes.mapNodeBuilder(NodeConnector.QNAME).withChild(nodeConnector1)
                .withChild(nodeConnector2).build();

        MapEntryNode inventoryNode = ImmutableNodes.mapEntryBuilder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME,
                TopologyQNames.INVENTORY_NODE_ID_QNAME, NODE_ID)
                .withChild(nodeConnectorList).build();

        Map<Integer, NormalizedNode<?, ?>> leafNodesMap = new HashMap<>();
        leafNodesMap.put(0,inventoryNode);


        //create NT node with refs to INV leaf nodes
        LeafNode<String> nodeConnectorRef1 = ImmutableNodes.leafNode(
                TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, nodeConnectorId1);
        LeafNode<String> nodeConnectorRef2 = ImmutableNodes.leafNode(
                TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, nodeConnectorId2);

        MapEntryNode terminationPoint1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                TopologyQNames.NETWORK_TP_ID_QNAME, tpId1).withChild(nodeConnectorRef1).build();
        MapEntryNode terminationPoint2 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                TopologyQNames.NETWORK_TP_ID_QNAME, tpId2).withChild(nodeConnectorRef2).build();
        MapNode terminationPointList = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                .withChild(terminationPoint1).withChild(terminationPoint2).build();

        LeafNode<String> invNodeRef = ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_REF_QNAME, NODE_ID);

        MapEntryNode ntNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID)
                .withChild(invNodeRef).withChild(terminationPointList).build();

        UnderlayItem invUnderlayItemInput = new UnderlayItem(ntNode , leafNodesMap, INV_TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);
        invUnderlayItemInput.setLeafNodes(leafNodesMap);

        //set expected output
        String tpRefPath = new StringBuilder("/network-topology:network-topology/topology/")
                .append(INV_TOPOLOGY_ID).append("/node/").append(NODE_ID).append("/termination-point/").append(tpId1)
                .toString();
        NodeWithValue<String> leafSetEntry = new NodeWithValue<String>(TopologyQNames.TP_REF, tpRefPath);
        ImmutableLeafSetEntryNodeBuilder<String> leafSetEntryNodeBuilder = new ImmutableLeafSetEntryNodeBuilder<>();
        LeafSetEntryNode<String> leafSetEntryNode = leafSetEntryNodeBuilder.withNodeIdentifier(leafSetEntry)
                .withValue(tpRefPath).build();

        ListNodeBuilder<String, LeafSetEntryNode<String>> listNodeBuilder = ImmutableLeafSetNodeBuilder.create();

        NodeIdentifier tpRefNodeIdentifier = NodeIdentifier.create(TopologyQNames.TP_REF);
        LeafSetNode<String> leafSetNode = listNodeBuilder.withNodeIdentifier(tpRefNodeIdentifier)
                .withChild(leafSetEntryNode).build();

        MapEntryNode outTerminationPoint = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                TopologyQNames.NETWORK_TP_ID_QNAME, tpId1).withChild(leafSetNode).build();
        MapNode outTerminationPointList = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                .withChild(outTerminationPoint).build();
        LeafNode<String> outInvNodeRef = ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_REF_QNAME, NODE_ID);
        MapEntryNode outNTNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(outInvNodeRef).withChild(outTerminationPointList).build();

        manager.setOutput(outNTNode);

        inventoryModelFiltrator.processCreatedChanges(nodeYiid, invUnderlayItemInput, INV_TOPOLOGY_ID);
    }

    @Test
    public void testProcessUpdatedChanges() {
        testProcessCreatedChangesOnNTModel();

        String tpId1 = "tp1";
        String value1 = "30";
        MapEntryNode tp1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                tpId1).withChild(ImmutableNodes.leafNode(UNNUMBERED_QNAME, value1)).build();

        // input
        MapEntryNode nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).build())
                .build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, NT_TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);

        // output
        MapEntryNode nodeValueOutput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).build()).build();

        manager.setOutput(nodeValueOutput);
        networkTopoModelFiltrator.processUpdatedChanges(nodeYiid, underlayItemInput, NT_TOPOLOGY_ID);
    }
}
