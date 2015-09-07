/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
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
    public void setUp() {
        // initialize and set up topology stores
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        aggregator = new UnificationAggregator(topoStoreProvider);
        topoStoreProvider.initializeStore(TOPO1, false);
        topoStoreProvider.initializeStore(TOPO2, false);
        topoStoreProvider.initializeStore(TOPO3, false);
        topoStoreProvider.initializeStore(TOPO4, false);

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
        TestNodeCreator testNodeCreator = new TestNodeCreator();
        leafYiid21 = testNodeCreator.createNodeIdYiid("21");
        LeafNode<String> leafNode21 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");

        leafYiid22 = testNodeCreator.createNodeIdYiid("22");
        LeafNode<String> leafNode22 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.3");
        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode1, leafNode21, TOPO2, "21", CorrelationItemEnum.Node);
        UnderlayItem physicalNode2 = new UnderlayItem(mockNormalizedNode1, leafNode22, TOPO2, "22", CorrelationItemEnum.Node);

        aggregator.processCreatedChanges(leafYiid21, physicalNode1, TOPO2);
        aggregator.processCreatedChanges(leafYiid22, physicalNode2, TOPO2);
        
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        // checks that two nodes have been correctly added into topology TOPO2
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO3).getUnderlayItems().size());
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

        // addLogicalNode method has been called twice
        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).updateOverlayItem((OverlayItem) Mockito.any());

        // change 2
        leafYiid23 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "23").build();
        LeafNode<Object> leafNode23 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.1").build();
        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode1, leafNode23, TOPO3, "23", CorrelationItemEnum.Node);

        aggregator.processCreatedChanges(leafYiid23, physicalNode3, TOPO3);

        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());
        // checks that one node has been correctly added into topology TOPO3
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO3).getUnderlayItems().size());
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

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
     * @throws Exception
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
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

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
     * @throws Exception
     */
    @Test
    public void testProcessUpdatedChanges1() throws Exception {
        testProcessCreatedChanges();
        LeafNode<Object> leafNode31 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.3").build();
        UnderlayItem physicalNode31 = new UnderlayItem(mockNormalizedNode1, leafNode31, TOPO2, "31", CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(leafYiid21, physicalNode31, TOPO2);

        // one new logical node has been created
        Mockito.verify(mockManager, Mockito.times(3)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        // one logical nodes has been updated
        Mockito.verify(mockManager, Mockito.times(2)).updateOverlayItem((OverlayItem) Mockito.any());
    }

}
