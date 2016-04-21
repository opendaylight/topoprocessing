package org.opendaylight.topoprocessing.impl.operator;

import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TerminationPointFiltratorTest {

    private static final String TOPOLOGY_ID = "topo:1";
    private static final String NODE_ID = "node:1";
    private static final int MIN = 10;
    private static final int MAX = 20;
    private static final QName UNNUMBERED_QNAME = QName.create(TerminationPoint.QNAME, "unnumbered");

    private TerminationPointFiltrator filtrator;
    private YangInstanceIdentifier path = YangInstanceIdentifier.of(UNNUMBERED_QNAME);
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
        filtrator = new TerminationPointFiltrator(topoStoreProvider, NetworkTopologyModel.class);
        topoStoreProvider.initializeStore(TOPOLOGY_ID, false);
        RangeNumberFiltrator filter = new RangeNumberFiltrator(MIN, MAX, path);
        filtrator.addFilter(filter);
        filtrator.setTopologyManager(manager);
    }

    @Test
    public void testProcessCreatedChanges() {
        String tpId1 = "tp1";
        String value1 = "15";
        String tpId2 = "tp2";
        String value2 = "30";
        MapEntryNode tp1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tpId1)
                .withChild(ImmutableNodes.leafNode(UNNUMBERED_QNAME, value1)).build();
        MapEntryNode tp2 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tpId2)
                .withChild(ImmutableNodes.leafNode(UNNUMBERED_QNAME, value2)).build();

        // input
        MapEntryNode nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).withChild(tp2)
                .build()).build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);

        // output
        MapEntryNode nodeValueOutput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).build()).build();
        UnderlayItem underlayItemOutput = new UnderlayItem(nodeValueOutput, null, TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);
        OverlayItem overlayItemOutput = new OverlayItem(Collections.singletonList(underlayItemOutput),
                CorrelationItemEnum.TerminationPoint);
        underlayItemOutput.setOverlayItem(overlayItemOutput);

        manager.setOutput(nodeValueOutput);
        filtrator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_ID);
    }

    @Test
    public void testProcessUpdatedChanges() {
        testProcessCreatedChanges();

        String tpId1 = "tp1";
        String value1 = "30";
        MapEntryNode tp1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tpId1)
                .withChild(ImmutableNodes.leafNode(UNNUMBERED_QNAME, value1)).build();

        // input
        MapEntryNode nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).build()).build();
        UnderlayItem underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);

        // output
        MapEntryNode nodeValueOutput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).build()).build();

        manager.setOutput(nodeValueOutput);
        filtrator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_ID);
    }
}
