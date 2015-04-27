package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.LogicalNodeWrapper;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
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
    private static final QName qnameLeafIp = QName.create(ROOT_QNAME, "ip");
//    private static final QName qnameLeafMac = QName.create(ROOT_QNAME, "mac");

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";

    private TopologyAggregator aggregator;
    private List<TopologyStore> topologyStores;
    private YangInstanceIdentifier leafYiid11, leafYiid12, leafYiid21, leafYiid22, leafYiid23;
    private LeafNode<Object> leafNode11;
//    private YangInstanceIdentifier testTopologyRef = YangInstanceIdentifier.builder()
//            .node(NetworkTopology.QNAME).node(Topology.QNAME)
//            .nodeWithKey(Topology.QNAME, TopologyQNames.topologyIdQName, "test").build();

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
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode11, leafYiid11);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode12, leafYiid12);
        topo1.getPhysicalNodes().put(leafYiid11, physicalNode1);
        topo2.getPhysicalNodes().put(leafYiid12, physicalNode2);

        CorrelationItemEnum correlationItem = CorrelationItemEnum.Node;
        aggregator = new TopologyAggregator();
        aggregator.setCorrelationItem(correlationItem);
        aggregator.setTopologyStores(topologyStores);
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
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode21, leafYiid21);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode22, leafYiid22);
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes1 = new HashMap<>();
        physicalNodes1.put(leafYiid21, physicalNode1);
        physicalNodes1.put(leafYiid22, physicalNode2);

        aggregator.processCreatedChanges(physicalNodes1, TOPO2);
        List<LogicalNodeWrapper> wrappers = aggregator.getTopologyManager().getWrappers();

        // one LogicalNodeWrapper should be created
        Assert.assertEquals(1, wrappers.size());
        // containing one LogicalNode
        List<LogicalNode> logicalNodes = wrappers.get(0).getLogicalNodes();
        Assert.assertEquals(1, logicalNodes.size());
        // containing two PhysicalNodes
        Assert.assertEquals(2, logicalNodes.get(0).getPhysicalNodes().size());

        // change 2
        leafYiid23 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "23").build();
        LeafNode<Object> leafNode23 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1").build();
        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode1, leafNode23, leafYiid23);
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
        Assert.assertEquals(1, aggregator.getTopologyStores().get(2).getPhysicalNodes().size());

        // still one LogicalNodeWrapper should be created
        Assert.assertEquals(1, wrappers.size());
        // still containing one LogicalNode
        logicalNodes = wrappers.get(0).getLogicalNodes();
        Assert.assertEquals(1, logicalNodes.size());
        // but containing three PhysicalNodes
        List<PhysicalNode> physicalNodes = logicalNodes.get(0).getPhysicalNodes();
        Assert.assertEquals(3, logicalNodes.get(0).getPhysicalNodes().size());
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
        // one logical node wrapper
        List<LogicalNodeWrapper> wrappers = aggregator.getTopologyManager().getWrappers();
        Assert.assertEquals(1, wrappers.size());
        // containing one LogicalNode
        List<LogicalNode> logicalNodes = wrappers.get(0).getLogicalNodes();
        Assert.assertEquals(1, logicalNodes.size());

        // case 2
        ArrayList<YangInstanceIdentifier> remove2 = new ArrayList<>();
        remove2.add(leafYiid21);
        aggregator.processRemovedChanges(remove2, TOPO2);

        // two physical nodes in topology store TOPO2 (=get(1))
        Assert.assertEquals(2, aggregator.getTopologyStores().get(1).getPhysicalNodes().size());

        // logical node has been removed, and so the logical node wrapper has been removed
        Assert.assertEquals(0, wrappers.size());
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
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode31 = new PhysicalNode(mockNormalizedNode1, leafNode31, leafYiid11);
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid11, physicalNode31);
        aggregator.processUpdatedChanges(update, TOPO1);
        
        // two LogicalNodeWrappers should be created
        List<LogicalNodeWrapper> wrappers = aggregator.getTopologyManager().getWrappers();
        Assert.assertEquals(2, wrappers.size());
        // each containing one logical node
        List<LogicalNode> logicalNodes1 = wrappers.get(0).getLogicalNodes();
        Assert.assertEquals(1, logicalNodes1.size());
        List<LogicalNode> logicalNodes2 = wrappers.get(1).getLogicalNodes();
        Assert.assertEquals(1, logicalNodes2.size());
        
        // both containing two PhysicalNodes
        List<PhysicalNode> physicalNodes1 = logicalNodes1.get(0).getPhysicalNodes();
        Assert.assertEquals(2, physicalNodes1.size());
        List<PhysicalNode> physicalNodes2 = logicalNodes2.get(0).getPhysicalNodes();
        Assert.assertEquals(2, physicalNodes2.size());
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
        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode1, leafNode32, leafYiid23);
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid23, physicalNode);
        aggregator.processUpdatedChanges(update, TOPO3);
        
        // one LogicalNodeWrappers existing (one LogicalNodeWrapper was removed)
        List<LogicalNodeWrapper> wrappers = aggregator.getTopologyManager().getWrappers();
        Assert.assertEquals(1, wrappers.size());
        // containing one logical node
        List<LogicalNode> logicalNodes1 = wrappers.get(0).getLogicalNodes();
        Assert.assertEquals(1, logicalNodes1.size());
        // containing three PhysicalNodes
        List<PhysicalNode> physicalNodes1 = logicalNodes1.get(0).getPhysicalNodes();
        Assert.assertEquals(3, physicalNodes1.size());
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
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.2").build();
        PhysicalNode physicalNode31 = new PhysicalNode(mockNormalizedNode1, leafNode31, leafYiid22);
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid22, physicalNode31);
        aggregator.processUpdatedChanges(update, TOPO2);
        
        // there shall be no impact on logical node wrapper and its logical node
        // it means still one logical node wrappper present
        List<LogicalNodeWrapper> wrappers = aggregator.getTopologyManager().getWrappers();
        Assert.assertEquals(1, wrappers.size());
        // containing one logical node
        List<LogicalNode> logicalNodes1 = wrappers.get(0).getLogicalNodes();
        Assert.assertEquals(1, logicalNodes1.size());
    }
}