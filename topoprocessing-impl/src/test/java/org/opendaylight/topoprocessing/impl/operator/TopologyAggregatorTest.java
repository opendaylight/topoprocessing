package org.opendaylight.topoprocessing.impl.operator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
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
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.MappingBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyAggregatorTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar");
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip");
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id");
    private static final QName QNAME_LEAF_IP = QName.create(ROOT_QNAME, "ip");
//    private static final QName QNAME_LEAF_MAC = QName.create(ROOT_QNAME, "mac");

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";

    private static final String TOPOLOGY1 = "openflow:1";
    private static final String TOPOLOGY2 = "bgp:1";

    private TopologyAggregator aggregator;
    private List<TopologyStore> topologyStores;
    private YangInstanceIdentifier leafYiid11, leafYiid12, leafYiid21, leafYiid22, leafYiid23;
    private LeafNode<Object> leafNode11;
//    private YangInstanceIdentifier testTopologyRef = YangInstanceIdentifier.builder()
//            .node(NetworkTopology.QNAME).node(Topology.QNAME)
//            .nodeWithKey(Topology.QNAME, TopologyQNames.topologyIdQName, "test").build();

    @Mock
    private NormalizedNode<?,?> mockNormalizedNode1, mockNormalizedNode2;
    @Mock
    private TopologyManager mockManager;

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
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.1").build();
        leafYiid12 = YangInstanceIdentifier.builder().nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "12").build();
        LeafNode<Object> leafNode12 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.2")
                .build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode11, TOPO1, "11");
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode12, TOPO2, "12");
        topo1.getPhysicalNodes().put(leafYiid11, physicalNode1);
        topo2.getPhysicalNodes().put(leafYiid12, physicalNode2);

        aggregator = new EqualityAggregator();
        aggregator.setTopologyStores(topologyStores);
        aggregator.setTopologyManager(mockManager);
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
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.1").build();
        leafYiid22 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "22").build();
        LeafNode<Object> leafNode22 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.3").build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode21, TOPO2, "21");
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode22, TOPO2, "22");
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes1 = new HashMap<>();
        physicalNodes1.put(leafYiid21, physicalNode1);
        physicalNodes1.put(leafYiid22, physicalNode2);

        aggregator.processCreatedChanges(physicalNodes1, TOPO2);

        Mockito.verify(mockManager, Mockito.times(1)).addLogicalNode((LogicalNode) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) any());
        Mockito.verify(mockManager, Mockito.times(0)).updateLogicalNode((LogicalNode) any());

        // change 2
        leafYiid23 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "23").build();
        LeafNode<Object> leafNode23 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.1").build();
        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode1, leafNode23, TOPO3, "23");
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes2 = new HashMap<>();
        physicalNodes2.put(leafYiid23, physicalNode3);

        aggregator.processCreatedChanges(physicalNodes2, TOPO3);

        // one physical node in topology store TOPO1 (=get(0))
        Assert.assertEquals(1, aggregator.getTopologyStores().get(0).getPhysicalNodes().size());
        // three physical nodes in topology store TOPO2 (=get(1))
        Assert.assertEquals(3, aggregator.getTopologyStores().get(1).getPhysicalNodes().size());
        // one physical node in topology store TOPO3 (=get(2))
        Assert.assertEquals(1, aggregator.getTopologyStores().get(2).getPhysicalNodes().size());
        // one physical node in topology store TOPO4 (=get(3))
        Assert.assertEquals(0, aggregator.getTopologyStores().get(3).getPhysicalNodes().size());

        Mockito.verify(mockManager, Mockito.times(1)).addLogicalNode((LogicalNode) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) any());
        Mockito.verify(mockManager, Mockito.times(1)).updateLogicalNode((LogicalNode) any());

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
        testProcessCreatedChanges();

        // case 1
        ArrayList<YangInstanceIdentifier> remove1 = new ArrayList<>();
        remove1.add(leafYiid11);
        aggregator.processRemovedChanges(remove1, TOPO1);

        // no physical nodes in topology store TOPO1 (=get(0))
        Assert.assertEquals(0, aggregator.getTopologyStores().get(0).getPhysicalNodes().size());

        Mockito.verify(mockManager, Mockito.times(1)).addLogicalNode((LogicalNode) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) any());
        // one logical node was updated
        Mockito.verify(mockManager, Mockito.times(2)).updateLogicalNode((LogicalNode) any());

        // case 2
        ArrayList<YangInstanceIdentifier> remove2 = new ArrayList<>();
        remove2.add(leafYiid21);
        aggregator.processRemovedChanges(remove2, TOPO2);

        // two physical nodes in topology store TOPO2 (=get(1))
        Assert.assertEquals(2, aggregator.getTopologyStores().get(1).getPhysicalNodes().size());

        Mockito.verify(mockManager, Mockito.times(1)).addLogicalNode((LogicalNode) any());
        // one logical node has been removed
        Mockito.verify(mockManager, Mockito.times(1)).removeLogicalNode((LogicalNode) any());
        Mockito.verify(mockManager, Mockito.times(2)).updateLogicalNode((LogicalNode) any());
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
        testProcessCreatedChanges();
        LeafNode<Object> leafNode31 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode31 = new PhysicalNode(mockNormalizedNode1, leafNode31, TOPO1, "11");
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid11, physicalNode31);
        aggregator.processUpdatedChanges(update, TOPO1);

        // one new logical node has been created
        Mockito.verify(mockManager, Mockito.times(2)).addLogicalNode((LogicalNode) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) any());
        // one new logical node has been updated
        Mockito.verify(mockManager, Mockito.times(2)).updateLogicalNode((LogicalNode) any());
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
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode1, leafNode32, TOPO3, "23");
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid23, physicalNode);
        aggregator.processUpdatedChanges(update, TOPO3);

        Mockito.verify(mockManager, Mockito.times(2)).addLogicalNode((LogicalNode) any());
        // one logical node has been removed
        Mockito.verify(mockManager, Mockito.times(1)).removeLogicalNode((LogicalNode) any());
        // one new logical node has been updated
        Mockito.verify(mockManager, Mockito.times(3)).updateLogicalNode((LogicalNode) any());
    }

    /**
     * Update changes 3 schema
     * input data form createChanges
     *
     * Change 1 - update:
     * - modifying physical node not belonging to any logical node
     * <pre>
     *     TOPO2 {
     *         node22: 192.168.1.3 -> 192.168.1.2;
     *     }
     * </pre>
     *
     * Result:
     * <pre>
     *     LOGICAL1 {
     *          node11 (192.168.1.2, TOPO1);
     *          node21 (192.168.1.1, TOPO2);
     *          node23 (192.168.1.1, TOPO3);
     *     }
     *     physical not pertaining to any logical node
     *          node12 (192.168.1.2, TOPO2);
     *          node22 (192.168.1.2, TOPO2);
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testProcessUpdatedChanges3() throws Exception {
        testProcessCreatedChanges();

        LeafNode<Object> leafNode31 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode31 = new PhysicalNode(mockNormalizedNode1, leafNode31, TOPO2, "22");
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid22, physicalNode31);
        aggregator.processUpdatedChanges(update, TOPO2);

        Mockito.verify(mockManager, Mockito.times(2)).addLogicalNode((LogicalNode) any());
        // one logical node has been removed
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) any());
        // one new logical node has been updated
        Mockito.verify(mockManager, Mockito.times(1)).updateLogicalNode((LogicalNode) any());
    }

    /**
     * Checks that two topology stores are initialized for two different underlay topologies in one call
     */
    @Test
    public void testInitStructuresWithTwoDifferentTopologies() {
        List<Mapping> mappings = createMappings(TOPOLOGY1, TOPOLOGY2);
        TopologyAggregator aggregator = new EqualityAggregator();
        aggregator.initializeStructures(mappings);
        assertEquals(aggregator.getTopologyStores().size(), 2);
    }

    private static List<Mapping> createMappings(String... topologyIds) {
        List<Mapping> mappings = new ArrayList<>();
        for (String topologyId : topologyIds) {
            MappingBuilder mappingBuilder1 = new MappingBuilder();
            mappingBuilder1.setUnderlayTopology(topologyId);
            mappings.add(mappingBuilder1.build());
        }
        return mappings;
    }

    /**
     * Checks that two topology stores are initialized for two different underly topologies in
     * two different calls of initializeStructures method
     */
    @Test
    public void testInitStructuresWithTwoDifferentTopologiesInTwoCalls() {
        List<Mapping> mappings = createMappings(TOPOLOGY1);
        TopologyAggregator aggregator = new EqualityAggregator();
        aggregator.initializeStructures(mappings);

        List<Mapping> mappings2 = createMappings(TOPOLOGY2);
        aggregator.initializeStructures(mappings2);

        assertEquals(aggregator.getTopologyStores().size(), 2);
    }

    /**
     * Checks that in two different calls of initializeStructures method
     * only one topology store is initialized for two underly topologies with the same id 
     */
    @Test
    public void testInitStructuresWithTwoSameTopologiesInTwoCalls() {
        List<Mapping> mappings = createMappings(TOPOLOGY1);
        TopologyAggregator aggregator = new EqualityAggregator();
        aggregator.initializeStructures(mappings);

        List<Mapping> mappings2 = createMappings(TOPOLOGY1);
        aggregator.initializeStructures(mappings2);

        assertEquals(aggregator.getTopologyStores().size(), 1);
    }

    /**
     * Checks that correlation with mappings set to null is handled correctly 
     * (no topology store is created, no NullPointerException)
     */
    @Test
    public void testMappingNull() {
        List<Mapping> mappings = null;
        aggregator.initializeStructures(mappings);
    }

}