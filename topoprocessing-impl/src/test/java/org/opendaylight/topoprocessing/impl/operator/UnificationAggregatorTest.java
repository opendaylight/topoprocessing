/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class UnificationAggregatorTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar");
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip");
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id");
    private static final QName QNAME_LEAF_IP = QName.create(ROOT_QNAME, "ip");

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";

    private static final String TOPO5 = "topo5";

    private static final String TOPOLOGY1 = "openflow:1";
    private static final String TOPOLOGY2 = "bgp:1";

    private TopologyAggregator aggregator;
    private List<TopologyStore> topologyStores;
    private YangInstanceIdentifier leafYiid11, leafYiid12, leafYiid21, leafYiid22, leafYiid23;
    private LeafNode<Object> leafNode11;

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
        // initialize topology stores
        TopologyStore topo1 = new TopologyStore(TOPO1, false, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        TopologyStore topo2 = new TopologyStore(TOPO2, false, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        TopologyStore topo3 = new TopologyStore(TOPO3, false, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        TopologyStore topo4 = new TopologyStore(TOPO4, false, new HashMap<YangInstanceIdentifier, PhysicalNode>());
        topologyStores = new ArrayList<>();
        topologyStores.add(topo1);
        topologyStores.add(topo2);
        topologyStores.add(topo3);
        topologyStores.add(topo4);

        aggregator = new UnificationAggregator();
        aggregator.topologyStores = topologyStores;
        aggregator.setTopologyManager(mockManager);
    }

    /**
     * change 1
     * Checks that for each new physical node, one logical node is created.
     * In this case two logical nodes are created, each for one physical node
     * LogicalNode(1): {node21}
     * LogicalNode(2): {node22}

     * change 2
     * Because newly created node23 in TOPO3 can create aggregated topology with node21,
     * it's logical node shall be updated
     * LogicalNode(1): {node21, node23}
     * LogicalNode(2): {node22}
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

        Assert.assertEquals(0, aggregator.topologyStores.get(0).getPhysicalNodes().size());
        // checks that two nodes have been correctly added into topology TOPO2   
        Assert.assertEquals(2, aggregator.topologyStores.get(1).getPhysicalNodes().size());
        Assert.assertEquals(0, aggregator.topologyStores.get(2).getPhysicalNodes().size());
        Assert.assertEquals(0, aggregator.topologyStores.get(3).getPhysicalNodes().size());

        // addLogicalNode method has been called twice
        Mockito.verify(mockManager, Mockito.times(2)).addLogicalNode((LogicalNode) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).updateLogicalNode((LogicalNode) Mockito.any());

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

        Assert.assertEquals(0, aggregator.topologyStores.get(0).getPhysicalNodes().size());
        Assert.assertEquals(2, aggregator.topologyStores.get(1).getPhysicalNodes().size());
        // checks that one node has been correctly added into topology TOPO3   
        Assert.assertEquals(1, aggregator.topologyStores.get(2).getPhysicalNodes().size());
        Assert.assertEquals(0, aggregator.topologyStores.get(3).getPhysicalNodes().size());

        Mockito.verify(mockManager, Mockito.times(2)).addLogicalNode((LogicalNode) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) Mockito.any());
        // updateLogicalNode method has been called once
        Mockito.verify(mockManager, Mockito.times(1)).updateLogicalNode((LogicalNode) Mockito.any());
    }

    /**
     * case 1
     * Verify that node23 is correctly removed from LogicalNode(1) by use of updateLogicalNode function.
     * result:
     * LogicalNode(1): {node21}
     * LogicalNode(2): {node22}
     * 
     * case2
     * Verify that node22 has been removed from LogicalNode(2) by use of removeLogicalNode function
     * (due to the fact, that it was the last node in that LogicalNode) 
     * @throws Exception
     */
    @Test
    public void testProcessRemovedChanges() throws Exception {
        testProcessCreatedChanges();
        // case 1
        ArrayList<YangInstanceIdentifier> remove1 = new ArrayList<>();
        remove1.add(leafYiid23);
        aggregator.processRemovedChanges(remove1, TOPO3);

        Assert.assertEquals(0, aggregator.topologyStores.get(0).getPhysicalNodes().size());
        Assert.assertEquals(2, aggregator.topologyStores.get(1).getPhysicalNodes().size());
        // no physical nodes in topology store TOPO3 (=get(2))
        Assert.assertEquals(0, aggregator.topologyStores.get(2).getPhysicalNodes().size());
        Assert.assertEquals(0, aggregator.topologyStores.get(3).getPhysicalNodes().size());

        Mockito.verify(mockManager, Mockito.times(2)).addLogicalNode((LogicalNode) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) Mockito.any());
        // one logical node has been updated
        Mockito.verify(mockManager, Mockito.times(2)).updateLogicalNode((LogicalNode) Mockito.any());

        // case 2
        ArrayList<YangInstanceIdentifier> remove2 = new ArrayList<>();
        remove2.add(leafYiid22);
        aggregator.processRemovedChanges(remove2, TOPO2);

        // one physical node left in topology store TOPO2 (=get(1))
        Assert.assertEquals(1, aggregator.topologyStores.get(1).getPhysicalNodes().size());

        Mockito.verify(mockManager, Mockito.times(2)).addLogicalNode((LogicalNode) Mockito.any());
        // one logical node has been removed
        Mockito.verify(mockManager, Mockito.times(1)).removeLogicalNode((LogicalNode) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(2)).updateLogicalNode((LogicalNode) Mockito.any());
    }

    /**
     * When IP address of one node has changed the way it can create aggregated node with some other existing
     * physical node (because their IP addresses are equal), this node shall be removed from original LogicalNode
     * (by calling updateLogicalNode method) and added into the other LogicalNode (by calling addLogicalNode method)
     * @throws Exception
     */
    @Test
    public void testProcessUpdatedChanges1() throws Exception {
        testProcessCreatedChanges();
        LeafNode<Object> leafNode31 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.3").build();
        PhysicalNode physicalNode31 = new PhysicalNode(mockNormalizedNode1, leafNode31, TOPO2, "31");
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        update.put(leafYiid21, physicalNode31);
        aggregator.processUpdatedChanges(update, TOPO2);

        // one new logical node has been created
        Mockito.verify(mockManager, Mockito.times(3)).addLogicalNode((LogicalNode) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeLogicalNode((LogicalNode) Mockito.any());
        // one logical nodes has been updated
        Mockito.verify(mockManager, Mockito.times(2)).updateLogicalNode((LogicalNode) Mockito.any());
    }

    /**
     * Testing processCreateChanges, processUpdatedChanges, processRemovedChanges with nonstandard inputs
     */
    @Test
    public void testFunctionsWithEmptyAndNullMaps() {
        // empty hash maps and array list
        Map<YangInstanceIdentifier, PhysicalNode> update = new HashMap<>();
        aggregator.processCreatedChanges(update, TOPO1);
        aggregator.processUpdatedChanges(update, TOPO1);
        ArrayList<YangInstanceIdentifier> remove = new ArrayList<>();
        aggregator.processRemovedChanges(remove, TOPO1);

        // null topology names
        Map<YangInstanceIdentifier, PhysicalNode> update2 = new HashMap<>();
        aggregator.processCreatedChanges(update2, null);
        aggregator.processUpdatedChanges(update2, null);
        ArrayList<YangInstanceIdentifier> remove2 = new ArrayList<>();
        aggregator.processRemovedChanges(remove2, null);

        // null hash maps and array list
        Map<YangInstanceIdentifier, PhysicalNode> update3 = null;
        aggregator.processCreatedChanges(update3, TOPO1);
        aggregator.processUpdatedChanges(update3, TOPO1);
        ArrayList<YangInstanceIdentifier> remove3 = null;
        aggregator.processRemovedChanges(remove3, TOPO1);

        // TOPO5 has never been used nor initialized, this shall not cause any problem
        Map<YangInstanceIdentifier, PhysicalNode> update4 = new HashMap<>();
        aggregator.processCreatedChanges(update4, TOPO5);
        aggregator.processUpdatedChanges(update4, TOPO5);
        ArrayList<YangInstanceIdentifier> remove4 = new ArrayList<>();
        aggregator.processRemovedChanges(remove4, TOPO5);

        Assert.assertTrue(true);
    }
}
