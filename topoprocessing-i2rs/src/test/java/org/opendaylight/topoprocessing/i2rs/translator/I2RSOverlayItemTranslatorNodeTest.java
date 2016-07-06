/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import com.google.common.base.Optional;

/**
 * @author andrej.zan
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class I2RSOverlayItemTranslatorNodeTest {

    private OverlayItemTranslator translator = new OverlayItemTranslator(new I2RSNodeTranslator(), new I2RSLinkTranslator());;
    private static final String TOPOLOGY_NAME = "topology:1";

    @Mock
    private NormalizedNode<?, ?> mockNormalizedNode;

    @Mock
    private UnderlayItem mockPhysicalNode;

    /**
     * Test case: NodeId translation
     */
    @Test
    public void testNodeId() {
        String logicalName = "node:1";
        UnderlayItem physicalNode = new UnderlayItem(mockNormalizedNode, null, TOPOLOGY_NAME, "node:1",
                CorrelationItemEnum.Node);
        OverlayItem logicalNode = new OverlayItem(Collections.singletonList(physicalNode), CorrelationItemEnum.Node);
        OverlayItemWrapper wrapper = new OverlayItemWrapper(logicalName, logicalNode);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        NormalizedNode<?, ?> nodeId = NormalizedNodes.findNode(normalizedNode,
                YangInstanceIdentifier.of(TopologyQNames.I2RS_NODE_ID_QNAME)).get();
        Assert.assertEquals("NormalizedNode ID should be the same as the LogicalNodeWrapper ID",
                logicalName, nodeId.getValue());
    }

    /**
     * Wrap the same node more times to the wrapper, translate it, and check result for the duplicity
     *
     * @throws Exception Exception
     */
    @Test
    public void testSameNodePassed() throws Exception {
        String logicalName = "node:1";
        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode, null, TOPOLOGY_NAME, "node:1",
                CorrelationItemEnum.Node);
        OverlayItem logicalNode = new OverlayItem(Collections.singletonList(physicalNode1), CorrelationItemEnum.Node);
        UnderlayItem physicalNode2 = new UnderlayItem(mockNormalizedNode, null, TOPOLOGY_NAME, "node:2",
                CorrelationItemEnum.Node);
        OverlayItemWrapper wrapper = new OverlayItemWrapper(logicalName, logicalNode);
        List<UnderlayItem> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode1);
        physicalNodes.add(physicalNode2);
        wrapper.addOverlayItem(new OverlayItem(physicalNodes, CorrelationItemEnum.Node));
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        Collection supportingNodes = (Collection) ((MapEntryNode) normalizedNode)
                .getChild(new NodeIdentifier(SupportingNode.QNAME)).get().getValue();
        Assert.assertEquals("Incorrect amount of supporting nodes included", 2, supportingNodes.size());
    }

    /**
     * <p>Test case: one SupportingNode translation</p>
     * <p>- includes TopologyRef and NodeRef</p>
     */
    @Test
    public void testSupportingNode() {
        String logicalName = "node:1";
        String physicalName = "node:11";

        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode, null, TOPOLOGY_NAME, physicalName,
                CorrelationItemEnum.Node);
        OverlayItem logicalNode = new OverlayItem(Collections.singletonList(physicalNode1), CorrelationItemEnum.Node);
        OverlayItemWrapper wrapper = new OverlayItemWrapper(logicalName, logicalNode);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);
        Map<QName, Object> keyValues = new HashMap<>();
        keyValues.put(TopologyQNames.I2RS_NETWORK_REF, TOPOLOGY_NAME);
        keyValues.put(TopologyQNames.I2RS_NODE_REF, physicalName);

        // topologyRef
        YangInstanceIdentifier yiidTopoRef = YangInstanceIdentifier.builder().node(SupportingNode.QNAME)
                .nodeWithKey(SupportingNode.QNAME, keyValues)
                .node(TopologyQNames.I2RS_NETWORK_REF).build();
        Optional<NormalizedNode<?, ?>> networkRef = NormalizedNodes.findNode(normalizedNode, yiidTopoRef);
        Assert.assertTrue("NetworkRef should be provided", networkRef.isPresent());
        Assert.assertEquals("NetworkRef from SupportingNodes should be the same as the PhysicalNode's Topology",
                TOPOLOGY_NAME, networkRef.get().getValue());

        // nodeRef
        YangInstanceIdentifier yiidNodeRef = YangInstanceIdentifier.builder().node(SupportingNode.QNAME)
                .nodeWithKey(SupportingNode.QNAME, keyValues)
                .node(TopologyQNames.I2RS_NODE_REF).build();
        Optional<NormalizedNode<?, ?>> nodeRef = NormalizedNodes.findNode(normalizedNode, yiidNodeRef);
        Assert.assertTrue("NodeRef should be provided", nodeRef.isPresent());
        Assert.assertEquals("NodeRef from SupportingNodes should be the same as the PhysicalNode's Id",
                physicalName, nodeRef.get().getValue());
    }

    /**
     * Test case: more supporting nodes translation
     */
    @Test
    public void testSupportingNodes() {
        String topologyName = "myTopo:1";
        String wrapperName = "myNode:1";
        String physicalName1 = "node:11";
        String physicalName2 = "node:12";
        String physicalName3 = "node:13";
        String physicalName4 = "node:14";
        String physicalName5 = "node:15";

        // logical 1
        List<UnderlayItem> toAggregate1 = new ArrayList<>();
        UnderlayItem physicalNode1 = new UnderlayItem(mockNormalizedNode, null, topologyName, physicalName1,
                CorrelationItemEnum.Node);
        UnderlayItem physicalNode2 = new UnderlayItem(mockNormalizedNode, null, topologyName, physicalName2,
                CorrelationItemEnum.Node);
        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode, null, topologyName, physicalName3,
                CorrelationItemEnum.Node);
        toAggregate1.add(physicalNode1);
        toAggregate1.add(physicalNode2);
        toAggregate1.add(physicalNode3);
        OverlayItem logicalNode1 = new OverlayItem(toAggregate1, CorrelationItemEnum.Node);

        // logical 2
        List<UnderlayItem> toAggregate2 = new ArrayList<>();
        UnderlayItem physicalNode4 = new UnderlayItem(mockNormalizedNode, null, topologyName, physicalName4,
                CorrelationItemEnum.Node);
        UnderlayItem physicalNode5 = new UnderlayItem(mockNormalizedNode, null, topologyName, physicalName5,
                CorrelationItemEnum.Node);
        toAggregate2.add(physicalNode4);
        toAggregate2.add(physicalNode5);
        OverlayItem logicalNode2 = new OverlayItem(toAggregate2, CorrelationItemEnum.Node);

        // process
        OverlayItemWrapper wrapper = new OverlayItemWrapper(wrapperName, logicalNode1);
        wrapper.addOverlayItem(logicalNode2);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        // supporting-nodes
        Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> value =
              ((MapEntryNode) normalizedNode).getChild(new NodeIdentifier(SupportingNode.QNAME));
        Assert.assertTrue("OverlayNode should contain UnderlayNodes", value.isPresent());
        Assert.assertEquals("OverlayNode contains wrong amount of UnderlayNodes", 5,
                ((Collection) value.get().getValue()).size());
    }

    /**
     * Test case: TerminationPoint translation
     */
    @Test
    public void testTerminationPoint() {
        String logicalName = "node:1";

        final String tpId1  = "tpId:1";
        String tpRef1 = "tpRef:1";
        String tpId2  = "tpId:2";
        String tpRef2 = "tpRef:2";
        String tpId3  = "tpId:3";
        String tpRef3 = "tpRef:3";
        String tpId4  = "tpId:4";
        String tpRef4 = "tpRef:4";

        final String physicalName1 = "node:1";
        MapNode terminationPoints1 = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId1)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_ID_QNAME, tpId1))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_REF, tpRef1))
                                .build())
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId2)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_ID_QNAME, tpId2))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_REF, tpRef2))
                                .build())
                .build();
        final MapEntryNode node1 = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, physicalName1)
                .withChild(terminationPoints1).build();

        final String physicalName2 = "node:2";
        MapNode terminationPoints2 = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId1)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_ID_QNAME, tpId1))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_REF, tpRef1))
                                .build())
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId3)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_ID_QNAME, tpId3))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_REF, tpRef3))
                                .build())
                .build();
        final MapEntryNode node2 = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, physicalName2)
                .withChild(terminationPoints2).build();

        final String physicalName3 = "node:3";
        MapNode terminationPoints3 = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId2)
                        .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_ID_QNAME, tpId2))
                        .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_REF, tpRef2))
                        .build())
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId4)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_ID_QNAME, tpId4))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.I2RS_TP_REF, tpRef4))
                                .build())
                .build();
        final MapEntryNode node3 = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, physicalName3)
                .withChild(terminationPoints3).build();
        final Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, mockNormalizedNode);
        OverlayItem logicalNode1 = new OverlayItem(new ArrayList<UnderlayItem>() {{
            add(new UnderlayItem(node1, targetFields, TOPOLOGY_NAME, physicalName1, CorrelationItemEnum.Node));
            add(new UnderlayItem(node2, targetFields, TOPOLOGY_NAME, physicalName2, CorrelationItemEnum.Node));
        }}, CorrelationItemEnum.Node);
        OverlayItem logicalNode2 = new OverlayItem(Collections.singletonList(
                new UnderlayItem(node3, targetFields, TOPOLOGY_NAME, physicalName3, CorrelationItemEnum.Node)
        ), CorrelationItemEnum.Node);
        OverlayItemWrapper wrapper = new OverlayItemWrapper(logicalName, logicalNode1);
        wrapper.addOverlayItem(logicalNode2);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        Collection value = (Collection) ((MapEntryNode) normalizedNode).getChild(
                new NodeIdentifier(TerminationPoint.QNAME)).get().getValue();
        Assert.assertEquals("OverlayNode contains wrong amount of TerminationPoints", 6, value.size());
    }
}
