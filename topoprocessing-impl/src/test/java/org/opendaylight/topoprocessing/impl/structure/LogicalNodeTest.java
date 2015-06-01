/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.structure;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LogicalNodeTest {

    private static final String TOPOLOGY1 = "pcep-topology:1";
    private static final String NODE_ID1 = "pcep:1";
    private static final String TOPOLOGY2 = "pcep-topology:2";
    private static final String NODE_ID2 = "pcep:2";
    private static final String NODE_ID3 = "pcep:3";
    private LogicalNode logicalNode;
    private PhysicalNode physicalNode, physicalNode2;

    @Mock private NormalizedNode<?,?> mockNormalizedNode1;
    @Mock private NormalizedNode<?,?> mockLeafNode1;
    @Mock private NormalizedNode<?,?> mockNormalizedNode2;
    @Mock private NormalizedNode<?,?> mockLeafNode2;

    @Test
    public void testLogicalNodeCreation() {
        physicalNode = new PhysicalNode(mockNormalizedNode1, mockLeafNode1, TOPOLOGY1, NODE_ID1);
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode);
        logicalNode = new LogicalNode(physicalNodes);
        Assert.assertEquals(1, logicalNode.getPhysicalNodes().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode, logicalNode.getPhysicalNodes().get(0)));
    }

    @Test
    public void testAddPhysicalNode() {
        physicalNode = new PhysicalNode(mockNormalizedNode1, mockLeafNode1, TOPOLOGY1, NODE_ID1);
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode);
        logicalNode = new LogicalNode(physicalNodes);

        physicalNode2 = new PhysicalNode(mockNormalizedNode2, mockLeafNode2, TOPOLOGY2, NODE_ID2);
        logicalNode.addPhysicalNode(physicalNode2);
        Assert.assertEquals(2, logicalNode.getPhysicalNodes().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getPhysicalNodes().get(1)));
    }

    @Test
    public void testRemovePhysicalNode() {
        testAddPhysicalNode();

        logicalNode.removePhysicalNode(physicalNode);
        Assert.assertEquals(1, logicalNode.getPhysicalNodes().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getPhysicalNodes().get(0)));
    }

    @Test
    public void testUpdatePhysicalNode() {
        testLogicalNodeCreation();

        physicalNode2 = new PhysicalNode(mockNormalizedNode2, mockLeafNode2, TOPOLOGY2, NODE_ID2);
        logicalNode.updatePhysicalNode(physicalNode, physicalNode2);
        Assert.assertEquals(1, logicalNode.getPhysicalNodes().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getPhysicalNodes().get(0)));
    }

    @Test
    public void testGetPhysicalNodes() {
        testLogicalNodeCreation();

        List<PhysicalNode> physicalNodes = logicalNode.getPhysicalNodes();
        Assert.assertEquals(1, physicalNodes.size());
        Assert.assertTrue(physicalNodeEquals(physicalNode, physicalNodes.get(0)));
    }

    @Test
    public void testSetPhysicalNodes() {
        testAddPhysicalNode();

        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode1, mockLeafNode1, TOPOLOGY2, NODE_ID3);
        List<PhysicalNode> newPhysicalNodes = new ArrayList<>();
        newPhysicalNodes.add(physicalNode3);

        logicalNode.setPhysicalNodes(newPhysicalNodes);

        Assert.assertEquals(1, logicalNode.getPhysicalNodes().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode3, logicalNode.getPhysicalNodes().get(0)));
    }

    @Test(expected=NullPointerException.class)
    public void testCreationWithNull() {
        LogicalNode logicalNode = new LogicalNode(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRemovingUnexistingNode() {
        testLogicalNodeCreation();

        logicalNode.removePhysicalNode(physicalNode2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUpdatingUnexistingNode() {
        testLogicalNodeCreation();
        PhysicalNode physicalNode3 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY2, NODE_ID3);

        logicalNode.updatePhysicalNode(physicalNode2, physicalNode3);
    }

    private static boolean physicalNodeEquals(PhysicalNode node1, PhysicalNode node2) {
        if (node1.getNode().equals(node2.getNode()) &&
                node1.getLeafNode().equals(node2.getLeafNode()) && 
                node1.getTopologyId().equals(node2.getTopologyId()) && 
                node1.getNodeId().equals(node2.getNodeId())) {
            return true;
        } else {
            return false;
        }
    }
}
