/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import static org.mockito.Matchers.any;

import java.util.HashMap;
import java.util.Map;
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

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class EqualityAggregatorTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar").intern();
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip").intern();
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id").intern();
    private static final QName QNAME_LEAF_IP = QName.create(ROOT_QNAME, "ip").intern();

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";
    private static final String TOPO5 = "topo5";
    private static final String TOPO6 = "topo6";
    private static final boolean AGGREGATE_INSIDE = false;

    private TopologyAggregator aggregator;
    private YangInstanceIdentifier leafYiid11, leafYiid12, leafYiid21, leafYiid22, leafYiid23;
    private LeafNode<String> leafNode11;
    private TestNodeCreator testNodeCreator;

    @Mock private NormalizedNode<?,?> mockNormalizedNode1, mockNormalizedNode2;
    @Mock private TopologyManager mockManager;

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
        // initialize and set up topology stores
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        aggregator = new EqualityAggregator(topoStoreProvider);
        topoStoreProvider.initializeStore(TOPO1, AGGREGATE_INSIDE);
        topoStoreProvider.initializeStore(TOPO2, AGGREGATE_INSIDE);
        topoStoreProvider.initializeStore(TOPO3, AGGREGATE_INSIDE);
        topoStoreProvider.initializeStore(TOPO4, AGGREGATE_INSIDE);

        TopologyStore topo1 = aggregator.getTopoStoreProvider().getTopologyStore(TOPO1);
        TopologyStore topo2 = aggregator.getTopoStoreProvider().getTopologyStore(TOPO2);

        // fill topology stores
        testNodeCreator = new TestNodeCreator();
        leafYiid11 = testNodeCreator.createNodeIdYiid("11");
        leafNode11 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");
        leafYiid12 = testNodeCreator.createNodeIdYiid("12");
        LeafNode<String> leafNode12 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.2");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode11);
        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO1, "11", CorrelationItemEnum.Node);
        Map<Integer, NormalizedNode<?, ?>> targetFields2 = new HashMap<>(1);
        targetFields2.put(0, leafNode12);
        UnderlayItem physicalNode2 = new UnderlayItem(mockNormalizedNode1, targetFields2, TOPO2, "12", CorrelationItemEnum.Node);
        topo1.getUnderlayItems().put(leafYiid11, physicalNode1);
        topo2.getUnderlayItems().put(leafYiid12, physicalNode2);

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
        leafYiid21 = testNodeCreator.createNodeIdYiid("21");
        LeafNode<String> leafNode21 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");
        leafYiid22 = testNodeCreator.createNodeIdYiid("22");
        LeafNode<String> leafNode22 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.3");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode21);
        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO2, "21", CorrelationItemEnum.Node);
        Map<Integer, NormalizedNode<?, ?>> targetFields2 = new HashMap<>(1);
        targetFields2.put(0, leafNode22);
        UnderlayItem physicalNode2 = new UnderlayItem(mockNormalizedNode1, targetFields2, TOPO2, "22", CorrelationItemEnum.Node);

        aggregator.processCreatedChanges(leafYiid21, physicalNode1, TOPO2);
        aggregator.processCreatedChanges(leafYiid22, physicalNode2, TOPO2);

        Mockito.verify(mockManager, Mockito.times(1)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).updateOverlayItem((OverlayItem) any());

        // change 2
        leafYiid23 = testNodeCreator.createNodeIdYiid("23");
        LeafNode<String> leafNode23 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");
        Map<Integer, NormalizedNode<?, ?>> targetFields3 = new HashMap<>(1);
        targetFields3.put(0, leafNode23);
        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode1, targetFields3, TOPO3, "23", CorrelationItemEnum.Node);
        aggregator.processCreatedChanges(leafYiid23, physicalNode3, TOPO3);

        // one physical node in topology store TOPO1
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        // three physical nodes in topology store TOPO2
        Assert.assertEquals(3, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());
        // one physical node in topology store TOPO3
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO3).getUnderlayItems().size());
        // no physical node in topology store TOPO4
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO4).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(1)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(1)).updateOverlayItem((OverlayItem) any());
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
        aggregator.processRemovedChanges(leafYiid11, TOPO1);

        // no physical nodes in topology store TOPO1
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(1)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        // one logical node was updated
        Mockito.verify(mockManager, Mockito.times(2)).updateOverlayItem((OverlayItem) any());

        // case 2
        aggregator.processRemovedChanges(leafYiid21, TOPO2);

        // two physical nodes in topology store TOPO2
        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(1)).addOverlayItem((OverlayItem) any());
        // one logical node has been removed
        Mockito.verify(mockManager, Mockito.times(1)).removeOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(2)).updateOverlayItem((OverlayItem) any());
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
        LeafNode<String> leafNode31 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.2");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode31);
        UnderlayItem physicalNode31 = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO1, "11", CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(leafYiid11, physicalNode31, TOPO1);

        // one new logical node has been created
        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        // two logical nodes have been updated
        Mockito.verify(mockManager, Mockito.times(2)).updateOverlayItem((OverlayItem) any());
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

        LeafNode<String> leafNode32 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.2");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode32);
        UnderlayItem physicalNode = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO3, "23", CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(leafYiid23, physicalNode, TOPO3);

        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) any());
        // one logical node has been removed
        Mockito.verify(mockManager, Mockito.times(1)).removeOverlayItem((OverlayItem) any());
        // three logical node have been updated
        Mockito.verify(mockManager, Mockito.times(3)).updateOverlayItem((OverlayItem) any());
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
     *          node11 (192.168.1.1, TOPO1);
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
        LeafNode<String> leafNode31 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.2");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode31);
        UnderlayItem physicalNode31 = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO2, "22", CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(leafYiid22, physicalNode31, TOPO2);

        // Even though nodes node12 and node22 have now the same IP 192.168.1.2, they cannot create aggregated
        // node because they belong to the same topology TOPO2
        Mockito.verify(mockManager, Mockito.times(1)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(1)).updateOverlayItem((OverlayItem) any());
    }

    /**
     * If in an aggregated (logical) node a Node value has been changed (it means LeafNode value remained unchanged),
     * only this Node value will be updated
     * @throws Exception
     */
    @Test
    public void testProcessUpdatedChanges4() throws Exception {
        testProcessCreatedChanges();

        leafYiid23 = testNodeCreator.createNodeIdYiid("23");
        LeafNode<String> leafNode23 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode23);
        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode2, targetFields1, TOPO3, "23", CorrelationItemEnum.Node);
        aggregator.processUpdatedChanges(leafYiid23, physicalNode3, TOPO3);

        Mockito.verify(mockManager, Mockito.times(1)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        // updateLogicalNode method has been called
        Mockito.verify(mockManager, Mockito.times(2)).updateOverlayItem((OverlayItem) any());
    }

    /**
     * AggregateInside in the topologies TOPO5 and TOPO6 is set to true.
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        boolean aggregateInside = true;
        aggregator.getTopoStoreProvider().initializeStore(TOPO5, aggregateInside);
        aggregator.getTopoStoreProvider().initializeStore(TOPO6, aggregateInside);

        YangInstanceIdentifier leafYiid51 = testNodeCreator.createNodeIdYiid("51");
        LeafNode<String> leafNode51 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.5");
        YangInstanceIdentifier leafYiid61 = testNodeCreator.createNodeIdYiid("61");
        LeafNode<String> leafNode61 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.6");
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode51);
        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO5, "51", CorrelationItemEnum.Node);
        Map<Integer, NormalizedNode<?, ?>> targetFields2 = new HashMap<>(1);
        targetFields2.put(0, leafNode61);
        UnderlayItem physicalNode2 = new UnderlayItem(mockNormalizedNode1, targetFields2, TOPO6, "61", CorrelationItemEnum.Node);
        aggregator.processCreatedChanges(leafYiid51, physicalNode1, TOPO5);
        aggregator.processCreatedChanges(leafYiid61, physicalNode2, TOPO5);

        Mockito.verify(mockManager, Mockito.times(0)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).updateOverlayItem((OverlayItem) any());

        // adds node and creates logical node inside the same topology
        YangInstanceIdentifier leafYiid52 = testNodeCreator.createNodeIdYiid("52");
        LeafNode<String> leafNode52 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.5");
        Map<Integer, NormalizedNode<?, ?>> targetFields3 = new HashMap<>(1);
        targetFields3.put(0, leafNode52);
        UnderlayItem physicalNode = new UnderlayItem(mockNormalizedNode1, targetFields3, TOPO5, "52", CorrelationItemEnum.Node);
        aggregator.processCreatedChanges(leafYiid52, physicalNode, TOPO5);
        // logical node have been created over two physical nodes having the same IP partaining to the same topology
        Mockito.verify(mockManager, Mockito.times(1)).addOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) any());
        Mockito.verify(mockManager, Mockito.times(0)).updateOverlayItem((OverlayItem) any());
    }

}
