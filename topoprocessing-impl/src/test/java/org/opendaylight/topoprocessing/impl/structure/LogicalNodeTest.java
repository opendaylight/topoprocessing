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
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
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
    private OverlayItem logicalNode;
    private UnderlayItem physicalNode, physicalNode2;

    @Mock private NormalizedNode<?,?> mockNormalizedNode1;
    @Mock private NormalizedNode<?,?> mockLeafNode1;
    @Mock private NormalizedNode<?,?> mockNormalizedNode2;
    @Mock private NormalizedNode<?,?> mockLeafNode2;

    @Test
    public void testLogicalNodeCreation() {
        physicalNode = new UnderlayItem(mockNormalizedNode1, mockLeafNode1, TOPOLOGY1, NODE_ID1, CorrelationItemEnum.Node);
        List<UnderlayItem> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode);
        logicalNode = new OverlayItem(physicalNodes);
        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode, logicalNode.getUnderlayItems().get(0)));
    }

    @Test
    public void testAddPhysicalNode() {
        physicalNode = new UnderlayItem(mockNormalizedNode1, mockLeafNode1, TOPOLOGY1, NODE_ID1, CorrelationItemEnum.Node);
        List<UnderlayItem> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode);
        logicalNode = new OverlayItem(physicalNodes);

        physicalNode2 = new UnderlayItem(mockNormalizedNode2, mockLeafNode2, TOPOLOGY2, NODE_ID2, CorrelationItemEnum.Node);
        logicalNode.addUnderlayItem(physicalNode2);
        Assert.assertEquals(2, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getUnderlayItems().get(1)));
    }

    @Test
    public void testRemovePhysicalNode() {
        testAddPhysicalNode();

        logicalNode.removeUnderlayItem(physicalNode);
        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getUnderlayItems().get(0)));
    }

    @Test
    public void testUpdatePhysicalNode() {
        testLogicalNodeCreation();

        physicalNode2 = new UnderlayItem(mockNormalizedNode2, mockLeafNode2, TOPOLOGY2, NODE_ID2, CorrelationItemEnum.Node);
        logicalNode.updateUnderlayItem(physicalNode, physicalNode2);
        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getUnderlayItems().get(0)));
    }

    @Test
    public void testGetPhysicalNodes() {
        testLogicalNodeCreation();

        List<UnderlayItem> physicalNodes = logicalNode.getUnderlayItems();
        Assert.assertEquals(1, physicalNodes.size());
        Assert.assertTrue(physicalNodeEquals(physicalNode, physicalNodes.get(0)));
    }

    @Test
    public void testSetPhysicalNodes() {
        testAddPhysicalNode();

        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode1, mockLeafNode1, TOPOLOGY2, NODE_ID3,
                CorrelationItemEnum.Node);
        List<UnderlayItem> newPhysicalNodes = new ArrayList<>();
        newPhysicalNodes.add(physicalNode3);

        logicalNode.setUnderlayItems(newPhysicalNodes);

        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode3, logicalNode.getUnderlayItems().get(0)));
    }

    @Test(expected=NullPointerException.class)
    public void testCreationWithNull() {
        OverlayItem logicalNode = new OverlayItem(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRemovingUnexistingNode() {
        testLogicalNodeCreation();

        logicalNode.removeUnderlayItem(physicalNode2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUpdatingUnexistingNode() {
        testLogicalNodeCreation();
        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode1, null, TOPOLOGY2, NODE_ID3,
                CorrelationItemEnum.Node);

        logicalNode.updateUnderlayItem(physicalNode2, physicalNode3);
    }

    private static boolean physicalNodeEquals(UnderlayItem node1, UnderlayItem node2) {
        if (node1.getNode().equals(node2.getNode()) &&
                node1.getLeafNode().equals(node2.getLeafNode()) && 
                node1.getTopologyId().equals(node2.getTopologyId()) && 
                node1.getItemId().equals(node2.getItemId())) {
            return true;
        } else {
            return false;
        }
    }
}
