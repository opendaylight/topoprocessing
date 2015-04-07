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
public class EqualityAggregatorTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar");
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip");
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id");
    private static final QName qnameLeafIp = QName.create(ROOT_QNAME, "ip");
//    private static final QName qnameLeafMac = QName.create(ROOT_QNAME, "mac");

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";

    private EqualityAggregator aggregator;
    private List<TopologyStore> topologyStores;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private YangInstanceIdentifier leafYiid11, leafYiid12, leafYiid21, leafYiid22, leafYiid23;
    private LeafNode<Object> leafNode11;

    @Mock
    private NormalizedNode mockNormalizedNode1, mockNormalizedNode2;

    /**
     * Setup schema
     *
     * <pre>
     * TOPO1 {
     *     node11: 192.168.1.1;
     * }
     *
     * TOPO2 {
     *     node12: 192.168.1.2;
     * }
     * </pre>
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        // make topology stores
        TopologyStore topo1 = new TopologyStore(TOPO1, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        TopologyStore topo2 = new TopologyStore(TOPO2, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        TopologyStore topo3 = new TopologyStore(TOPO3, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        TopologyStore topo4 = new TopologyStore(TOPO4, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        topologyStores = new ArrayList<>();
        topologyStores.add(topo1);
        topologyStores.add(topo2);
        topologyStores.add(topo3);
        topologyStores.add(topo4);

        // fill topology stores
        leafYiid11 = YangInstanceIdentifier.builder().nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "11").build();
        leafNode11 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1").build();
        leafYiid12 = YangInstanceIdentifier.builder().nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "12").build();
        LeafNode<Object> leafNode12 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.2")
                .build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode11);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode12);
        topo1.getPhysicalNodes().put(leafYiid11, physicalNode1);
        topo2.getPhysicalNodes().put(leafYiid12, physicalNode2);

        CorrelationItemEnum correlationItem = CorrelationItemEnum.Node;
        aggregator = new EqualityAggregator(correlationItem, topologyStores, idGenerator);
    }

    /**
     * Create changes schema
     *
     * Change 1 - add:
     * <pre>
     *     TOPO2 {
     *         node21: 192.168.1.1;
     *         node22: 192.168.1.3;
     *     }
     * </pre>
     *
     * Change 2 - add:
     * <pre>
     *     TOPO3 {
     *         node23: 192.168.1.1;
     *     }
     * </pre>
     *
     * Result:
     * <pre>
     *     LOGICAL1 {
     *          node11 (192.168.1.1, TOPO1);
     *          node21 (192.168.1.1, TOPO2);
     *          node23 (192.168.1.1, TOPO3);
     *     }
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testProcessCreatedChanges() throws Exception {
        // change 1
        leafYiid21 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "21").build();
        LeafNode<Object> leafNode21 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1").build();
        leafYiid22 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "22").build();
        LeafNode<Object> leafNode22 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.3").build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode21);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode22);
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes1 = new HashMap<>();
        physicalNodes1.put(leafYiid21, physicalNode1);
        physicalNodes1.put(leafYiid22, physicalNode2);

        AggregationMap created1 = aggregator.processCreatedChanges(physicalNodes1, TOPO2);
        //expecting: one created aggregated topology
        Assert.assertEquals(1, created1.getCreatedData().size());
        Assert.assertEquals(0, created1.getUpdatedData().size());
        Assert.assertEquals(0, created1.getRemovedData().size());

        List<PhysicalNode> createdPhysicalNodes1 = created1.entrySet().iterator().next().getValue().getPhysicalNodes();
        //expecting: newly created logical node contains 2 physical nodes (node21 and node11)
        Assert.assertEquals("Count of physical nodes in aggregation should be 2", 2, createdPhysicalNodes1.size());

        // change 2 - adding new physical node to previously created logical node
        leafYiid23 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "23").build();
        LeafNode<Object> leafNode23 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1").build();
        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode1, leafNode23);
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes2 = new HashMap<>();
        physicalNodes2.put(leafYiid23, physicalNode3);

        AggregationMap created2 = aggregator.processCreatedChanges(physicalNodes2, TOPO3);
        //expecting: no new logical node created
        Assert.assertEquals(0, created2.getCreatedData().size());
        Map<YangInstanceIdentifier, LogicalNode> updatedData = created2.getUpdatedData();
        //expecting: 1 logical node updated
        Assert.assertEquals(1, updatedData.size());
        //expecting: no logical node removed
        Assert.assertEquals(0, created2.getRemovedData().size());

        List<PhysicalNode> createdPhysicalNodes2 = updatedData.entrySet().iterator().next().getValue().getPhysicalNodes();
        Assert.assertEquals("Count of physical nodes in aggregation should be 3", 3, createdPhysicalNodes2.size());
        Assert.assertEquals("1st physical node in the logical node is not equal with the original one",
                createdPhysicalNodes2.get(0), physicalNode1);
        Assert.assertEquals("2nd physical node in the logical node is not equal with the node created in Change 1",
                createdPhysicalNodes2.get(1), topologyStores.get(0).getPhysicalNodes().get(leafYiid11));
        Assert.assertEquals("3rd physical node in the logical node is not equal with the node created in Change 2",
                createdPhysicalNodes2.get(2), physicalNode3);
    }

    /**
     * Remove changes schema
     * input data form createChanges
     *
     * Change 1 - remove:
     * <pre>
     *     TOPO1 {
     *         node11: 192.168.1.1;
     *     }
     * </pre>
     *
     * * Change 2 - remove:
     * <pre>
     *     TOPO2 {
     *         node21: 192.168.1.1;
     *     }
     * </pre>
     *
     * Result: Logical node removed
     *
     * @throws Exception
     */
    @Test
    public void testProcessRemovedChanges() throws Exception {
        // in order to remove nodes, aggregated topology from "create" method will be used
        testProcessCreatedChanges();

        ArrayList<YangInstanceIdentifier> remove1 = new ArrayList<>();
        remove1.add(leafYiid11);
        AggregationMap resultMap1 = aggregator.processRemovedChanges(remove1, TOPO1);
        //expecting: no new created node
        Assert.assertEquals(0, resultMap1.getCreatedData().size());
        Map<YangInstanceIdentifier, LogicalNode> updatedData = resultMap1.getUpdatedData();
        //expecting: one updated logical node
        Assert.assertEquals(1, updatedData.size());
        //expecting: no removed logical node
        Assert.assertEquals(0, resultMap1.getRemovedData().size());
        List<PhysicalNode> updatedPhysicalNodes = updatedData.entrySet().iterator().next().getValue().getPhysicalNodes();
        //expecting: logical node containing 3 physical nodes minus 1 = now containing 2 physical nodes
        Assert.assertEquals("Count of physical nodes in aggregation should be 2", 2, updatedPhysicalNodes.size());

        ArrayList<YangInstanceIdentifier> remove2 = new ArrayList<>();
        remove2.add(leafYiid21);
        AggregationMap resultMap2 = aggregator.processRemovedChanges(remove2, TOPO2);
        //expecting: no new created node
        Assert.assertEquals(0, resultMap2.getCreatedData().size());
        //expecting: no new updated node
        Assert.assertEquals(0, resultMap2.getUpdatedData().size());
        //expecting: from logical node originally containing 2 physical nodes one node was removed = logical node with
        //one physical node should not exist and should be removed
        Assert.assertEquals(1, resultMap2.getRemovedData().size());
    }

    /**
     * Update changes 1 schema
     * input data form createChanges
     *
     * Change 1 - update:
     * <pre>
     *     TOPO1 {
     *         node11: 192.168.1.1 -> 192.168.1.2;
     *     }
     * </pre>
     *
     * Result:
     * <pre>
     *     LOGICAL1 {
     *          node21 (192.168.1.1, TOPO2);
     *          node23 (192.168.1.1, TOPO3);
     *     }
     *     LOGICAL2 {
     *          node11 (192.168.1.2, TOPO1);
     *          node12 (192.168.1.2, TOPO2);
     *     }
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testProcessUpdatedChanges1() throws Exception {
        // in order to update nodes, aggregated topology from "create" method will be used
        testProcessCreatedChanges();

        LeafNode<Object> leafNode31 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode1, leafNode31);
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid11, physicalNode);
        AggregationMap resultMap1 = aggregator.processUpdatedChanges(update, TOPO1);
        //expecting: 1 new logical node created, containing two physical nodes with IP 192.168.1.2
        Assert.assertEquals(1, resultMap1.getCreatedData().size());
        //expecting: 1 updated logical node (containing two physical nodes with IP 192.168.1.1)
        Assert.assertEquals(1, resultMap1.getUpdatedData().size());
        //expecting: no logical node removed
        Assert.assertEquals(0, resultMap1.getRemovedData().size());
    }

    /**
     * Update changes 2 schema
     * input data form updateChanges1
     *
     * Change 1 - update:
     * <pre>
     *     TOPO3 {
     *         node23: 192.168.1.1 -> 192.168.1.2;
     *     }
     * </pre>
     *
     * Result:
     * <pre>
     *     LOGICAL1 - removed
     *     LOGICAL2 {
     *          node11 (192.168.1.2, TOPO1);
     *          node12 (192.168.1.2, TOPO2);
     *          node23 (192.168.1.2, TOPO3);
     *     }
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testProcessUpdatedChanges2() throws Exception {
        testProcessUpdatedChanges1();

        LeafNode<Object> leafNode32 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode1, leafNode32);
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid23, physicalNode);
        AggregationMap resultMap1 = aggregator.processUpdatedChanges(update, TOPO3);
        //expecting: no created logical node
        Assert.assertEquals(0, resultMap1.getCreatedData().size());
        //expecting: 1 updated logical node (containing three physical nodes with IP 192.168.1.2)
        Assert.assertEquals(1, resultMap1.getUpdatedData().size());
        //expecting: 1 removed logical node (originally containing two physical nodes with IP 192.168.1.1)
        Assert.assertEquals(1, resultMap1.getRemovedData().size());
    }

    /**
     * In physical node, leafnode attribute remains the same, node attribute is modified
     * @throws Exception
     */
    @Test
    public void testProcessUpdatedChanges3() throws Exception {
        testProcessCreatedChanges();

        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode2, leafNode11);
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid11, physicalNode);
        AggregationMap resultMap1 = aggregator.processUpdatedChanges(update, TOPO1);
        Assert.assertEquals(0, resultMap1.getCreatedData().size());
        Map<YangInstanceIdentifier, LogicalNode> updatedData = resultMap1.getUpdatedData();
        //expecting: 1 updated logical node
        Assert.assertEquals(1, updatedData.size());
        Assert.assertEquals(0, resultMap1.getRemovedData().size());

        //expecting: the physical node in logical node has been modified
        boolean equals = false;
        List<PhysicalNode> physicalNodes = updatedData.entrySet().iterator().next().getValue().getPhysicalNodes();
        for (PhysicalNode node : physicalNodes) {
            if (node.getNode().equals(mockNormalizedNode2)) {
                equals = true;
            }
        }
        Assert.assertTrue("Set nodes doesn't equal", equals);
    }
}