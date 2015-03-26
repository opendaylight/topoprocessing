package org.opendaylight.topoprocessing.impl.operator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyAggregatorTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar");
    private static final QName LIST_TOPOID_QNAME = QName.create(ROOT_QNAME, "topology");
    private static final QName LEAF_TOPOID_QNAME = QName.create(ROOT_QNAME, "topology-id");
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip");
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id");
    private static final QName LIST_MAC_QNAME = QName.create(ROOT_QNAME, "mac");
    private static final QName LEAF_MAC_QNAME = QName.create(ROOT_QNAME, "mac-id");

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";

    private TopologyAggregator aggregator;
    private List<TopologyStore> topologyStores;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();

    @Mock
    private NormalizedNode mockNormalizedNode;

    @Before
    public void setUp() throws Exception {

        // fill topology stores
        QName qnameLeafIp = QName.create(ROOT_QNAME, "ip");
//        QName qnameLeafMac = QName.create(ROOT_QNAME, "mac");
        YangInstanceIdentifier yiidLeafIp = YangInstanceIdentifier.builder()
//                .nodeWithKey(LIST_TOPOID_QNAME, LEAF_TOPOID_QNAME, TOPO1)
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "1")
                .build();
//        YangInstanceIdentifier yiidLeafMac = YangInstanceIdentifier.builder().node(qnameLeafMac).build();
        LeafNode<Object> leafNode1 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1")
                .build();
//        LeafNode<Object> leafNode2 = ImmutableLeafNodeBuilder.create()
//                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
//                .withValue("192.168.1.1")
//                .build();
//        LeafNode<Object> leafNode3 = ImmutableLeafNodeBuilder.create()
//                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
//                .withValue("192.168.1.1")
//                .build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode, leafNode1);
//        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode, leafNode2);
//        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode, leafNode3);

        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes1 = new HashMap<>();
        physicalNodes1.put(yiidLeafIp, physicalNode1);
//        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes2 = new HashMap<>();
//        physicalNodes2.put(yiidLeafIp, physicalNode2);
//        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes3 = new HashMap<>();
//        physicalNodes3.put(yiidLeafIp, physicalNode3);

        TopologyStore topo1 = new TopologyStore(TOPO1, physicalNodes1);
//        TopologyStore topo2 = new TopologyStore(TOPO2, physicalNodes2);
//        TopologyStore topo3 = new TopologyStore(TOPO3, physicalNodes3);

        topologyStores = new ArrayList<>();
        topologyStores.add(topo1);
//        topologyStores.add(topo2);
//        topologyStores.add(topo3);
        CorrelationItemEnum correlationItem = CorrelationItemEnum.Node;
        aggregator = new TopologyAggregator(correlationItem, topologyStores, idGenerator);
    }

    @Test
    public void testProcessCreatedChanges() throws Exception {

        QName qnameLeafIp = QName.create(ROOT_QNAME, "ip");
        YangInstanceIdentifier leafYiid1 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "10").build();
        LeafNode<Object> leafNode1 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1").build();
        YangInstanceIdentifier leafYiid2 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "11").build();
        LeafNode<Object> leafNode2 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode, leafNode1);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode, leafNode2);
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes1 = new HashMap<>();
        physicalNodes1.put(leafYiid1, physicalNode1);
        physicalNodes1.put(leafYiid2, physicalNode2);

        AggregationMap created1 = aggregator.processCreatedChanges(physicalNodes1, TOPO2);
        Assert.assertEquals(1, created1.getCreatedData().size());
        Assert.assertEquals(0, created1.getUpdatedData().size());
        Assert.assertEquals(0, created1.getRemovedData().size());

        List<PhysicalNode> createdPhysicalNodes1 = created1.entrySet().iterator().next().getValue().getPhysicalNodes();
        Assert.assertEquals("Count of physical nodes in aggregation should be 2", 2, createdPhysicalNodes1.size());

        YangInstanceIdentifier leafYiid3 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "12").build();
        LeafNode<Object> leafNode3 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1").build();
        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode, leafNode3);
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes2 = new HashMap<>();
        physicalNodes2.put(leafYiid3, physicalNode3);

        AggregationMap created2 = aggregator.processCreatedChanges(physicalNodes2, TOPO3);
        Assert.assertEquals(0, created2.getCreatedData().size());
        Map<YangInstanceIdentifier, LogicalNode> updatedData = created2.getUpdatedData();
        Assert.assertEquals(1, updatedData.size());
        Assert.assertEquals(0, created2.getRemovedData().size());

        List<PhysicalNode> createdPhysicalNodes2 = updatedData.entrySet().iterator().next().getValue().getPhysicalNodes();
        Assert.assertEquals("Count of physical nodes in aggregation should be 2", 3, createdPhysicalNodes2.size());
        Assert.assertEquals("1. physical node in the aggregated point doesn't equals to the original one",
                createdPhysicalNodes2.get(0), physicalNode1);
        Assert.assertEquals("2. physical node in the aggregated point doesn't equals to the original one",
                createdPhysicalNodes2.get(1), topologyStores.get(0).getPhysicalNodes().entrySet().iterator().next().getValue());
        Assert.assertEquals("3. physical node in ths aggregated point doesn't equals to the original one",
                createdPhysicalNodes2.get(2), physicalNode3);
    }

    @Test
    public void testProcessRemovedChanges() throws Exception {
//        aggregator.processRemovedChanges()
    }

    @Test
    public void testProcessUpdatedChanges() throws Exception {

    }
}