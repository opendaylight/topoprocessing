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
import org.mockito.Mock;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;



/**
 * @author martin.uhlir
 *
 */
public class UnificationAggregatorTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar");
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip");
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id");
    private static final QName qnameLeafIp = QName.create(ROOT_QNAME, "ip");

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";
    private static final String TOPO3 = "topo3";
    private static final String TOPO4 = "topo4";

    private UnificationAggregator aggregator;
    private List<TopologyStore> topologyStores;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
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

        // initialize topology stores
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
        YangInstanceIdentifier leafYiid11, leafYiid12;
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
        aggregator = new UnificationAggregator(correlationItem, topologyStores, idGenerator);
        // creates logical nodes from physical nodes
        aggregator.initialize(topologyStores);
    }

    /**
     * Test that two disjuncted topologies create one aggregated topology containing same amount of nodes as
     * sum of nodes in TOPO1 and TOPO2
     *
     * Change 1 - add:
     * <pre>
     *     TOPO1 {
     *         node13: 192.168.1.3;
     *         node14: 192.168.1.4;
     *     }
     * </pre>
     *
     * Result:
     *  TOPO1 {
     *      logical node {logicalYIIDx, node11}
     *      logical node {logicalYIIDx, node13}
     *      logical node {logicalYIIDx, node14}
     *  }
     *  TOPO2 {
     *      logical node {logicalYIIDx, node12}
     *  }
     *  aggregated {
     *      logical node {logicalYIIDx, node12}
     *      logical node {logicalYIIDx, node11}
     *      logical node {logicalYIIDx, node13}
     *      logical node {logicalYIIDx, node14}
     *  }
     *
     * @throws Exception
     */
    @Test
    public void testProcessCreatedChanges1() throws Exception {
        YangInstanceIdentifier leafYiid13, leafYiid14;
        leafYiid13 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "13").build();
        LeafNode<Object> leafNode13 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.3").build();
        leafYiid14 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "14").build();
        LeafNode<Object> leafNode14 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.4").build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode13);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode14);
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes1 = new HashMap<>();
        physicalNodes1.put(leafYiid13, physicalNode1);
        physicalNodes1.put(leafYiid14, physicalNode2);

        AggregationMap created1 = aggregator.processCreatedChanges(physicalNodes1, TOPO1);
        Assert.assertEquals(2, created1.getCreatedData().size());
        Assert.assertEquals(0, created1.getUpdatedData().size());
        Assert.assertEquals(0, created1.getRemovedData().size());
    }

    /**
     * Test that two topologies with one node
     *
     * Change 1 - add:
     * <pre>
     *     TOPO2 {
     *         node21: 192.168.1.1;
     *         node22: 192.168.1.3;
     *     }
     * </pre>
     *
     * Result:
     *  TOPO1 {
     *      logical node {logicalYIIDx, node11}
     *  }
     *  TOPO2 {
     *      logical node {logicalYIIDx, node12}
     *      logical node {logicalYIIDx, node21}
     *      logical node {logicalYIIDx, node22}
     *  }
     *  aggregated {
     *      logical node {logicalYIIDx, node12}
     *      logical node {logicalYIIDx, {node11, node21}}
     *      logical node {logicalYIIDx, node22}
     *  }
     *
     * @throws Exception
     */
    @Test
    public void testProcessCreatedChanges2() throws Exception {
        YangInstanceIdentifier leafYiid21, leafYiid23;
        leafYiid21 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "21").build();
        LeafNode<Object> leafNode21 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.1").build();
        leafYiid23 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "23").build();
        LeafNode<Object> leafNode23 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qnameLeafIp))
                .withValue("192.168.1.3").build();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, leafNode21);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, leafNode23);
        Map<YangInstanceIdentifier, PhysicalNode> physicalNodes1 = new HashMap<>();
        physicalNodes1.put(leafYiid21, physicalNode1);
        physicalNodes1.put(leafYiid23, physicalNode2);

        AggregationMap created1 = aggregator.processCreatedChanges(physicalNodes1, TOPO2);
        Assert.assertEquals(1, created1.getCreatedData().size());
        Assert.assertEquals(1, created1.getUpdatedData().size());
        Assert.assertEquals(0, created1.getRemovedData().size());
    }
}
