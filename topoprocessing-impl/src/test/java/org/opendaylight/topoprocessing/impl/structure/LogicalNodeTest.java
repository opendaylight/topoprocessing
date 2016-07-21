/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, mockLeafNode1);
        physicalNode = new UnderlayItem(mockNormalizedNode1, targetFields, TOPOLOGY1, NODE_ID1, CorrelationItemEnum.Node);
        List<UnderlayItem> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode);
        logicalNode = new OverlayItem(physicalNodes, CorrelationItemEnum.Node);
        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode, logicalNode.getUnderlayItems().peek()));
    }

    @Test
    public void testAddPhysicalNode() {
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, mockLeafNode1);
        physicalNode = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPOLOGY1, NODE_ID1, CorrelationItemEnum.Node);
        List<UnderlayItem> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode);
        logicalNode = new OverlayItem(physicalNodes, CorrelationItemEnum.Node);

        Map<Integer, NormalizedNode<?, ?>> targetFields2 = new HashMap<>(1);
        targetFields2.put(0, mockLeafNode2);
        physicalNode2 = new UnderlayItem(mockNormalizedNode2, targetFields2, TOPOLOGY2, NODE_ID2, CorrelationItemEnum.Node);
        logicalNode.addUnderlayItem(physicalNode2);
        Assert.assertEquals(2, logicalNode.getUnderlayItems().size());
        Iterator<UnderlayItem> iterator = logicalNode.getUnderlayItems().iterator();
        iterator.next();
        Assert.assertTrue(physicalNodeEquals(physicalNode2, iterator.next()));
    }

    @Test
    public void testRemovePhysicalNode() {
        testAddPhysicalNode();

        logicalNode.removeUnderlayItem(physicalNode);
        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getUnderlayItems().peek()));
    }

    @Test
    public void testUpdatePhysicalNode() {
        testLogicalNodeCreation();

        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, mockLeafNode2);
        physicalNode2 = new UnderlayItem(mockNormalizedNode2, targetFields, TOPOLOGY2, NODE_ID2, CorrelationItemEnum.Node);
        logicalNode.updateUnderlayItem(physicalNode, physicalNode2);
        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode2, logicalNode.getUnderlayItems().peek()));
    }

    @Test
    public void testGetPhysicalNodes() {
        testLogicalNodeCreation();

        Queue<UnderlayItem> physicalNodes = logicalNode.getUnderlayItems();
        Assert.assertEquals(1, physicalNodes.size());
        Assert.assertTrue(physicalNodeEquals(physicalNode, physicalNodes.peek()));
    }

    @Test
    public void testSetPhysicalNodes() {
        testAddPhysicalNode();

        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, mockLeafNode1);
        UnderlayItem physicalNode3 = new UnderlayItem(mockNormalizedNode1, targetFields, TOPOLOGY2, NODE_ID3,
                CorrelationItemEnum.Node);
        Queue<UnderlayItem> newPhysicalNodes = new ConcurrentLinkedQueue<>();
        newPhysicalNodes.add(physicalNode3);

        logicalNode.setUnderlayItems(newPhysicalNodes);

        Assert.assertEquals(1, logicalNode.getUnderlayItems().size());
        Assert.assertTrue(physicalNodeEquals(physicalNode3, logicalNode.getUnderlayItems().peek()));
    }

    @Test(expected=NullPointerException.class)
    public void testCreationWithNull() {
        OverlayItem logicalNode = new OverlayItem(null, CorrelationItemEnum.Node);
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
        if (node1.getItem().equals(node2.getItem())
                && node1.getLeafNodes().equals(node2.getLeafNodes())
                && node1.getTopologyId().equals(node2.getTopologyId())
                && node1.getItemId().equals(node2.getItemId())) {
            return true;
        } else {
            return false;
        }
    }
}
