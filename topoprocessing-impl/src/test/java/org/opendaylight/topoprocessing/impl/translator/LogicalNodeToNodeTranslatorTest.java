package org.opendaylight.topoprocessing.impl.translator;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.LogicalNodeWrapper;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.*;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import java.util.*;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class LogicalNodeToNodeTranslatorTest {

    private LogicalNodeToNodeTranslator translator = new LogicalNodeToNodeTranslator();
    private static final String TOPOLOGY_NAME = "topology:1";

    @Mock
    private NormalizedNode<?, ?> mockNormalizedNode;

    @Mock
    private PhysicalNode mockPhysicalNode;

    /**
     * Test case: NodeId translation
     */
    @Test
    public void testNodeId() {
        String logicalName = "node:1";
        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode, null, TOPOLOGY_NAME, "node:1");
        LogicalNode logicalNode = new LogicalNode(Collections.singletonList(physicalNode));
        LogicalNodeWrapper wrapper = new LogicalNodeWrapper(logicalName, logicalNode);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        NormalizedNode<?, ?> nodeId = NormalizedNodes.findNode(normalizedNode,
                YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME)).get();
        Assert.assertEquals("NormalizedNode ID should be the same as the LogicalNodeWrapper ID",
                logicalName, nodeId.getValue());
    }

    /**
     * Test case: one SupportingNode translation<br/>
     * - includes TopologyRef and NodeRef
     */
    @Test
    public void testSupportingNode() {
        String logicalName = "node:1";
        String physicalName = "node:11";

        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode, null, TOPOLOGY_NAME, physicalName);
        LogicalNode logicalNode = new LogicalNode(Collections.singletonList(physicalNode1));
        LogicalNodeWrapper wrapper = new LogicalNodeWrapper(logicalName, logicalNode);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);
        Map<QName, Object> keyValues = new HashMap<>();
        keyValues.put(TopologyQNames.TOPOLOGY_REF, TOPOLOGY_NAME);
        keyValues.put(TopologyQNames.NODE_REF, physicalName);

        // topologyRef
        YangInstanceIdentifier yiidTopoRef = YangInstanceIdentifier.builder().node(SupportingNode.QNAME)
                .nodeWithKey(SupportingNode.QNAME, keyValues)
                .node(TopologyQNames.TOPOLOGY_REF).build();
        Optional<NormalizedNode<?, ?>> topologyRef = NormalizedNodes.findNode(normalizedNode, yiidTopoRef);
        Assert.assertTrue("TopologyRef should be provided", topologyRef.isPresent());
        Assert.assertEquals("TopologyRef from SupportingNodes should be the same as the PhysicalNode's Topology",
                TOPOLOGY_NAME, topologyRef.get().getValue());

        // nodeRef
        YangInstanceIdentifier yiidNodeRef = YangInstanceIdentifier.builder().node(SupportingNode.QNAME)
                .nodeWithKey(SupportingNode.QNAME, keyValues)
                .node(TopologyQNames.NODE_REF).build();
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
        List<PhysicalNode> toAggregate1 = new ArrayList<>();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode, null, topologyName, physicalName1);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode, null, topologyName, physicalName2);
        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode, null, topologyName, physicalName3);
        toAggregate1.add(physicalNode1);
        toAggregate1.add(physicalNode2);
        toAggregate1.add(physicalNode3);
        LogicalNode logicalNode1 = new LogicalNode(toAggregate1);

        // logical 2
        List<PhysicalNode> toAggregate2 = new ArrayList<>();
        PhysicalNode physicalNode4 = new PhysicalNode(mockNormalizedNode, null, topologyName, physicalName4);
        PhysicalNode physicalNode5 = new PhysicalNode(mockNormalizedNode, null, topologyName, physicalName5);
        toAggregate2.add(physicalNode4);
        toAggregate2.add(physicalNode5);
        LogicalNode logicalNode2 = new LogicalNode(toAggregate2);

        // process
        LogicalNodeWrapper wrapper = new LogicalNodeWrapper(wrapperName, logicalNode1);
        wrapper.addLogicalNode(logicalNode2);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        // supporting-nodes
        Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> value =
              ((MapEntryNode) normalizedNode).getChild(new YangInstanceIdentifier.NodeIdentifier(SupportingNode.QNAME));
        Assert.assertTrue("OverlayNode should contain UnderlayNodes", value.isPresent());
        Assert.assertEquals("OverlayNode contains wrong amount of UnderlayNodes", 5,
                ((Collection) value.get().getValue()).size());
    }

    /**
     * Test case: TerminationPoint translation
     */
    @Test
    public void testTerminationPoint() {
        String topologyName = "topology:1";
        String logicalName = "node:1";
        String physicalName = "node:11";

        String tpId1 = "tp:1:1";
        String tpRef1= "tp:1:1";
        String tpId2 = "tp:1:2";
        String tpRef2= "tp:1:2";
        String tpId3 = "tp:1:3";
        String tpRef3 = "tp:1:3";
        Map<QName, Object> tpKeyValues1 = new HashMap<>();
        tpKeyValues1.put(TopologyQNames.TP_ID, tpId1);
        tpKeyValues1.put(TopologyQNames.TP_REF, tpRef1);
        Map<QName, Object> tpKeyValues2 = new HashMap<>();
        tpKeyValues2.put(TopologyQNames.TP_ID, tpId2);
        tpKeyValues2.put(TopologyQNames.TP_REF, tpRef2);
        Map<QName, Object> tpKeyValues3 = new HashMap<>();
        tpKeyValues3.put(TopologyQNames.TP_ID, tpId3);
        tpKeyValues3.put(TopologyQNames.TP_REF, tpRef3);

        MapNode terminationPoints = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME)
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.TP_ID, tpId1)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.TP_ID, tpId1))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.TP_REF, tpRef1))
                                .build()
                )
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.TP_ID, tpId2)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.TP_ID, tpId2))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.TP_REF, tpRef2))
                                .build()
                )
                .withChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.TP_ID, tpId3)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.TP_ID, tpId3))
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.TP_REF, tpRef3))
                                .build()
                ).build();

        MapEntryNode node = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, physicalName)
                .withChild(terminationPoints).build();
        PhysicalNode physicalNode = new PhysicalNode(node, mockNormalizedNode, TOPOLOGY_NAME, physicalName);

        LogicalNode logicalNode = new LogicalNode(Collections.singletonList(physicalNode));
        LogicalNodeWrapper wrapper = new LogicalNodeWrapper(logicalName, logicalNode);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        // termination-points
        Collection value = (Collection) ((MapEntryNode) normalizedNode).getChild(
                new YangInstanceIdentifier.NodeIdentifier(TerminationPoint.QNAME)).get().getValue();
        Assert.assertEquals("OverlayNode should contain 3 TerminationPoints", 3, value.size());
    }
}
