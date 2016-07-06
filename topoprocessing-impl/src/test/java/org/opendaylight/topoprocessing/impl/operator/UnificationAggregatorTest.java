/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.ComputedLink;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.Scripting;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class UnificationAggregatorTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar").intern();
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip").intern();
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id").intern();
    private static final QName QNAME_LEAF_IP = QName.create(ROOT_QNAME, "ip").intern();

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";

    public static final String script = "aggregable.setResult(true);";

    private TopologyAggregator aggregator;
    private YangInstanceIdentifier leafYiid21, leafYiid22, leafYiid23, leafYiid24;

    @Mock
    private NormalizedNode<?,?> mockNormalizedNode1, mockNormalizedNode2;
    @Mock
    private TopologyManager mockManager;
    @Mock
    private Scripting mockScripting;

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
     * @throws Exception Exception
     */
    @Before
    public void setUp() throws Exception {
        // initialize and set up topology stores
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        aggregator = new UnificationAggregator(topoStoreProvider);
        topoStoreProvider.initializeStore(TOPO1, false);
        topoStoreProvider.initializeStore(TOPO2, false);
        topoStoreProvider.initializeStore(TOPO3, false);
        topoStoreProvider.initializeStore(TOPO4, false);
        aggregator.setTopologyManager(mockManager);
        // setup script mock
        Mockito.when(mockScripting.getLanguage()).thenReturn("javascript");
        Mockito.when(mockScripting.getScript()).thenReturn(script);
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
     * @throws Exception Exception
     */
    @Test
    public void testProcessCreatedChanges() throws Exception {
        // change 1
        TestNodeCreator testNodeCreator = new TestNodeCreator();
        leafYiid21 = testNodeCreator.createNodeIdYiid("21");
        LeafNode<String> leafNode21 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");

        leafYiid22 = testNodeCreator.createNodeIdYiid("22");
        LeafNode<String> leafNode22 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.3");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode21);
        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO2, "21",
                CorrelationItemEnum.Node);
        Map<Integer, NormalizedNode<?, ?>> targetFields2 = new HashMap<>(1);
        targetFields2.put(0, leafNode22);
        UnderlayItem physicalNode2 = new UnderlayItem(mockNormalizedNode1, targetFields2, TOPO2, "22",
                CorrelationItemEnum.Node);

        leafYiid24 = testNodeCreator.createNodeIdYiid("24");
        LeafNode<String> leafNode24 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");
        Map<Integer, NormalizedNode<?, ?>> targetFields4 = new HashMap<>(1);
        targetFields4.put(0, leafNode24);
        UnderlayItem physicalNode4 = new UnderlayItem(mockNormalizedNode1, targetFields4, TOPO2, "24",
                CorrelationItemEnum.Node);

        // add to topostrore node that wasnt processed with operator to test if agregator process this correctly
        aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().put(leafYiid24,physicalNode4);

        aggregator.processCreatedChanges(leafYiid21, physicalNode1, TOPO2);
        aggregator.processCreatedChanges(leafYiid22, physicalNode2, TOPO2);

        // enable scripting, next tests will be performed with script
        aggregator.initCustomAggregation(mockScripting);

        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        // checks that two nodes have been correctly added into topology TOPO2
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO3).getUnderlayItems().size());
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

        // addLogicalNode method has been called twice
        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).updateOverlayItem((OverlayItem) Mockito.any());

        // test if script was initailized
        Mockito.verify(mockScripting).getScript();

        // change 2
        leafYiid23 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "23").build();
        LeafNode<Object> leafNode23 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.1").build();
        Map<Integer, NormalizedNode<?, ?>> targetFields3 = new HashMap<>(1);
        targetFields3.put(0, leafNode23);
        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode1, targetFields3, TOPO3, "23",
                CorrelationItemEnum.Node);

        aggregator.processCreatedChanges(leafYiid23, physicalNode3, TOPO3);

        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());
        // checks that one node has been correctly added into topology TOPO3
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO3).getUnderlayItems().size());
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        // updateLogicalNode method has been called once
        Mockito.verify(mockManager, Mockito.times(1)).updateOverlayItem((OverlayItem) Mockito.any());
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
     * @throws Exception Exception
     */
    @Test
    public void testProcessRemovedChanges() throws Exception {
        testProcessCreatedChanges();
        // case 1
        aggregator.processRemovedChanges(leafYiid23, TOPO3);

        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());
        // no physical nodes in topology store TOPO3 (=get(2))
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO3).getUnderlayItems().size());
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        // one logical node has been updated
        Mockito.verify(mockManager, Mockito.times(2)).updateOverlayItem((OverlayItem) Mockito.any());

        // case 2
        aggregator.processRemovedChanges(leafYiid22, TOPO2);

        // one physical node left in topology store TOPO2 (=get(1))
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        // one logical node has been removed
        Mockito.verify(mockManager, Mockito.times(1)).removeOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(2)).updateOverlayItem((OverlayItem) Mockito.any());
    }

    /**
     * When IP address of one node has changed the way it can create aggregated node with some other existing
     * physical node (because their IP addresses are equal), this node shall be removed from original LogicalNode
     * (by calling updateLogicalNode method) and added into the other LogicalNode (by calling addLogicalNode method)
     * @throws Exception Exception
     */
    @Test
    public void testProcessUpdatedChanges1() throws Exception {
        testProcessCreatedChanges();
        LeafNode<Object> leafNode31 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.3").build();
        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, leafNode31);
        UnderlayItem physicalNode31 = new UnderlayItem(mockNormalizedNode1, targetFields, TOPO2, "31",
                CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(leafYiid21, physicalNode31, TOPO2);

        // one new logical node has been created
        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        // one logical nodes has been updated
        Mockito.verify(mockManager, Mockito.times(3)).updateOverlayItem((OverlayItem) Mockito.any());
    }



    @Test
    public void testProcessCreatedChangesOnLinks() throws Exception {
        TestNodeCreator testNodeCreator = new TestNodeCreator();
        leafYiid21 = testNodeCreator.createNodeIdYiid("21");
        LeafNode<String> leafNode21 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");
        leafYiid22 = testNodeCreator.createNodeIdYiid("22");
        LeafNode<String> leafNode22 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.3");
        Map<Integer, NormalizedNode<?, ?>> targetFieldsLinks1 = new HashMap<>(1);
        targetFieldsLinks1.put(0, leafNode21);
        targetFieldsLinks1.put(1, leafNode22);

        leafYiid21 = testNodeCreator.createNodeIdYiid("31");
        LeafNode<String> leafNode31 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.2.1");
        leafYiid22 = testNodeCreator.createNodeIdYiid("32");
        LeafNode<String> leafNode32 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.2.3");
        Map<Integer, NormalizedNode<?, ?>> targetFieldsLinks2 = new HashMap<>(1);
        targetFieldsLinks2.put(0, leafNode31);
        targetFieldsLinks2.put(1, leafNode32);

        leafYiid21 = testNodeCreator.createNodeIdYiid("41");
        LeafNode<String> leafNode41 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.3.1");
        leafYiid22 = testNodeCreator.createNodeIdYiid("42");
        LeafNode<String> leafNode42 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.3.3");
        Map<Integer, NormalizedNode<?, ?>> targetFieldsLinks3 = new HashMap<>(1);
        targetFieldsLinks3.put(0, leafNode41);
        targetFieldsLinks3.put(1, leafNode42);
        ComputedLink physicalLink1 = new ComputedLink(mockNormalizedNode1, targetFieldsLinks1,leafNode21,leafNode22,
                TOPO2, "link1", CorrelationItemEnum.Link);
        ComputedLink physicalLink2 = new ComputedLink(mockNormalizedNode2, targetFieldsLinks2,leafNode31,leafNode32,
                TOPO2, "link2", CorrelationItemEnum.Link);
        ComputedLink physicalLink3 = new ComputedLink(mockNormalizedNode2, targetFieldsLinks3,leafNode41,leafNode42,
                TOPO2, "link3", CorrelationItemEnum.Link);

        // process creation of two links
        aggregator.processCreatedChanges(leafYiid21, physicalLink1, TOPO2);
        aggregator.processCreatedChanges(leafYiid22, physicalLink2, TOPO2);
        // process update of link
        aggregator.processUpdatedChanges(leafYiid22, physicalLink3, TOPO2);

        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO3).getUnderlayItems().size());
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(3)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(1)).removeOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).updateOverlayItem((OverlayItem) Mockito.any());
    }

}
