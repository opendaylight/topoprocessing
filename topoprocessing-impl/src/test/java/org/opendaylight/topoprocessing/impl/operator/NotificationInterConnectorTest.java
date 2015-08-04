/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;

/**
 * @author michal.polkorab
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationInterConnectorTest {

    private static final String TOPOLOGY_ID = "test:1";
    private QName leafQname = QName.create(Node.QNAME, "leaf-node");
    private QName nodeIdQname = QName.create(Node.QNAME, "id");

    @Mock private TopologyOperator operator;
    private NotificationInterConnector connector;
    private TestNodeCreator creator = new TestNodeCreator();

    /** Sets up NotificationInterConnector */
    @Before
    public void setUp() {
        operator = Mockito.mock(TopologyOperator.class);
        connector = new NotificationInterConnector(TOPOLOGY_ID, CorrelationItemEnum.Node);
        connector.setOperator(operator);
    }

    /** Tests created changes - only topology node is received */
    @Test
    public void testOnlyTopologyNode() {
        String nodeId = "toponode:1";
        MapEntryNode topoNode = creator.createMapEntryNode(nodeId);
        YangInstanceIdentifier topoNodeId = createTopoNodeIdentifier(nodeId);
        UnderlayItem item = new UnderlayItem(topoNode, null, TOPOLOGY_ID, nodeId, CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> entries = new HashMap<>();
        entries.put(topoNodeId, item);
        connector.processCreatedChanges(entries, TOPOLOGY_ID);
        Mockito.verifyZeroInteractions(operator);
    }

    /** Tests created changes - only inventory node is received */
    @Test
    public void testOnlyInventoryNode() {
        String nodeId = "invnode:1";
        String leafValue = "10.0.0.1";
        YangInstanceIdentifier invNodeId = createInvNodeIdentifier(nodeId);
        LeafNode<String> leafNode = ImmutableNodes.leafNode(leafQname, leafValue);
        UnderlayItem item = new UnderlayItem(null, leafNode, TOPOLOGY_ID, null, CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> entries = new HashMap<>();
        entries.put(invNodeId, item);
        connector.processCreatedChanges(entries, TOPOLOGY_ID);
        Mockito.verifyZeroInteractions(operator);
    }

    /** Tests created changes - inventory node is received before topology node */
    @Test
    public void testCreateInventoryNodeFirst() {
        // create inventory node
        String node1 = "node:1";
        String leafValue = "10.0.0.1";
        YangInstanceIdentifier invNodeId = createInvNodeIdentifier(node1);
        LeafNode<String> leafNode = ImmutableNodes.leafNode(leafQname, leafValue);
        UnderlayItem item1 = new UnderlayItem(null, leafNode, TOPOLOGY_ID, null, CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> entries = new HashMap<>();
        entries.put(invNodeId, item1);
        connector.processCreatedChanges(entries, TOPOLOGY_ID);

        // create topology node
        String node2 = "node:2";
        YangInstanceIdentifier topoNodeId = createTopoNodeIdentifier(node2);
        AugmentationNode augNode = createInventoryRefNode(invNodeId);
        MapEntryNode topoNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, nodeIdQname, node2)
                .addChild(augNode).build();
        UnderlayItem item2 = new UnderlayItem(topoNode, null, TOPOLOGY_ID, node2, CorrelationItemEnum.Node);
        entries = new HashMap<>();
        entries.put(topoNodeId, item2);
        connector.processCreatedChanges(entries, TOPOLOGY_ID);

        // create expected result
        UnderlayItem comparationItem = new UnderlayItem(topoNode, leafNode, TOPOLOGY_ID, node2,
                CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> expectedItems = new HashMap<>();
        expectedItems.put(invNodeId, comparationItem);
        Mockito.verify(operator, Mockito.times(1)).processCreatedChanges(Matchers.refEq(expectedItems),
                Matchers.eq(TOPOLOGY_ID));
    }

    /** Tests created changes - topology node is received before inventory node */
    @Test
    public void testCreateTopologyNodeFirst() {
        // create inventory node
        String node1 = "node:1";
        String leafValue = "10.0.0.1";
        YangInstanceIdentifier invNodeId = createInvNodeIdentifier(node1);
        LeafNode<String> leafNode = ImmutableNodes.leafNode(leafQname, leafValue);
        UnderlayItem item1 = new UnderlayItem(null, leafNode, TOPOLOGY_ID, null, CorrelationItemEnum.Node);

        // create topology node
        String node2 = "node:2";
        YangInstanceIdentifier topoNodeId = createTopoNodeIdentifier(node2);
        AugmentationNode augNode = createInventoryRefNode(invNodeId);
        MapEntryNode topoNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, nodeIdQname, node2)
                .addChild(augNode).build();
        UnderlayItem item2 = new UnderlayItem(topoNode, null, TOPOLOGY_ID, node2, CorrelationItemEnum.Node);

        // enqueue and process topology node
        Map<YangInstanceIdentifier, UnderlayItem> entries = new HashMap<>();
        entries.put(topoNodeId, item2);
        connector.processCreatedChanges(entries, TOPOLOGY_ID);

        // enqueue and process inventory node
        Map<YangInstanceIdentifier, UnderlayItem> entries2 = new HashMap<>();
        entries2.put(invNodeId, item1);
        connector.processCreatedChanges(entries2, TOPOLOGY_ID);

        // create expected result
        UnderlayItem comparationItem = new UnderlayItem(topoNode, leafNode, TOPOLOGY_ID, node2,
                CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> expectedItems = new HashMap<>();
        expectedItems.put(invNodeId, comparationItem);
        Mockito.verify(operator, Mockito.times(1)).processCreatedChanges(Matchers.refEq(expectedItems),
                Matchers.eq(TOPOLOGY_ID));
    }

    /** Tests removed changes - remove with inventory identifier first */
    @Test
    public void testRemoveInventoryNode() {
        testCreateInventoryNodeFirst();
        String node1 = "node:1";
        YangInstanceIdentifier invNodeId = createInvNodeIdentifier(node1);
        List<YangInstanceIdentifier> identifiers = new ArrayList<>();
        identifiers.add(invNodeId);

        // remove with inventory identifier
        connector.processRemovedChanges(identifiers, TOPOLOGY_ID);
        Mockito.verify(operator, Mockito.times(1)).processRemovedChanges(
                Matchers.eq(Collections.singletonList(invNodeId)), Matchers.eq(TOPOLOGY_ID));
        Assert.assertNull("Item should have been removed",
                connector.getTopologyStore(TOPOLOGY_ID).getUnderlayItems().get(invNodeId));

        // remove with topology identifier
        String node2 = "node:2";
        YangInstanceIdentifier topoNodeId = createTopoNodeIdentifier(node2);
        identifiers = new ArrayList<>();
        identifiers.add(topoNodeId);
        // item should have been already removed, so no operator interactions are expected
        Mockito.verifyNoMoreInteractions(operator);
        connector.processRemovedChanges(identifiers, TOPOLOGY_ID);
    }

    /** Tests removed changes - remove with topology identifier first */
    @Test
    public void testRemoveTopologyNode() {
        testCreateInventoryNodeFirst();
        String node1 = "node:1";
        YangInstanceIdentifier invNodeId = createInvNodeIdentifier(node1);
        String node2 = "node:2";
        YangInstanceIdentifier topoNodeId = createTopoNodeIdentifier(node2);
        List<YangInstanceIdentifier> identifiers = new ArrayList<>();
        identifiers.add(topoNodeId);

        // remove with topology identifier
        connector.processRemovedChanges(identifiers, TOPOLOGY_ID);
        Mockito.verify(operator, Mockito.times(1)).processRemovedChanges(
                Matchers.eq(Collections.singletonList(invNodeId)), Matchers.eq(TOPOLOGY_ID));
        Assert.assertNull("Item should have been removed",
                connector.getTopologyStore(TOPOLOGY_ID).getUnderlayItems().get(invNodeId));

        // remove with inventory identifier
        identifiers = new ArrayList<>();
        identifiers.add(invNodeId);
        // item should have been already removed, so no operator interactions are expected
        Mockito.verifyNoMoreInteractions(operator);
        connector.processRemovedChanges(identifiers, TOPOLOGY_ID);
    }

    /** Tests update changes - update inventory node */
    @Test
    public void testUpdateInventoryNode() {
        testCreateInventoryNodeFirst();
        // create inventory node
        String node1 = "node:1";
        String leafValue = "10.0.0.2";
        YangInstanceIdentifier invNodeId = createInvNodeIdentifier(node1);
        LeafNode<String> leafNode = ImmutableNodes.leafNode(leafQname, leafValue);
        UnderlayItem item1 = new UnderlayItem(null, leafNode, TOPOLOGY_ID, null, CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> entries = new HashMap<>();
        entries.put(invNodeId, item1);
        connector.processUpdatedChanges(entries, TOPOLOGY_ID);

        // create expected result
        UnderlayItem item = connector.getTopologyStore(TOPOLOGY_ID).getUnderlayItems().get(invNodeId);
        UnderlayItem comparationItem = new UnderlayItem(item.getItem(), leafNode, TOPOLOGY_ID, item.getItemId(),
                CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> expectedItems = new HashMap<>();
        expectedItems.put(invNodeId, comparationItem);
        Mockito.verify(operator, Mockito.times(1)).processUpdatedChanges(Matchers.refEq(expectedItems),
                Matchers.eq(TOPOLOGY_ID));
    }

    /** Tests update changes - update topology node */
    @Test
    public void testUpdateTopologyNode() {
        testCreateInventoryNodeFirst();
        // create inventory node for expected data check
        String node1 = "node:1";
        String leafValue = "10.0.0.1";
        YangInstanceIdentifier invNodeId = createInvNodeIdentifier(node1);
        LeafNode<String> leafNode = ImmutableNodes.leafNode(leafQname, leafValue);

        // create topology node
        String node2 = "node:2";
        YangInstanceIdentifier topoNodeId = createTopoNodeIdentifier(node2);
        AugmentationNode augNode = createInventoryRefNode(invNodeId);
        MapEntryNode topoNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, nodeIdQname, node2)
                .addChild(augNode)
                .addChild(ImmutableNodes.leafNode(QName.create(Topology.QNAME, "topo-leaf"), "leafValue")).build();
        UnderlayItem item2 = new UnderlayItem(topoNode, null, TOPOLOGY_ID, node2, CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> entries = new HashMap<>();
        entries.put(topoNodeId, item2);
        connector.processUpdatedChanges(entries, TOPOLOGY_ID);

        // create expected result
        UnderlayItem comparationItem = new UnderlayItem(topoNode, leafNode, TOPOLOGY_ID, node2,
                CorrelationItemEnum.Node);
        Map<YangInstanceIdentifier, UnderlayItem> expectedItems = new HashMap<>();
        expectedItems.put(invNodeId, comparationItem);
        Mockito.verify(operator, Mockito.times(1)).processCreatedChanges(Matchers.refEq(expectedItems),
                Matchers.eq(TOPOLOGY_ID));
    }

    private YangInstanceIdentifier createInvNodeIdentifier(String nodeId) {
        return YangInstanceIdentifier.builder().node(Nodes.QNAME).node(Node.QNAME)
                .nodeWithKey(Node.QNAME, nodeIdQname, nodeId).build();
    }

    private YangInstanceIdentifier createTopoNodeIdentifier(String nodeId) {
        return creator.createNodeIdYiid(nodeId);
    }

    private AugmentationNode createInventoryRefNode(YangInstanceIdentifier invNodeId) {
        QName inventoryNodeRefQname = QName.create("(urn:opendaylight:model:topology:inventory?"
                + "revision=2013-10-30)inventory-node-ref");
        Set<QName> qnames = new HashSet<>();
        qnames.add(inventoryNodeRefQname);
        AugmentationIdentifier augId = new AugmentationIdentifier(qnames);
        AugmentationNode augNode = ImmutableAugmentationNodeBuilder.create()
                .withNodeIdentifier(augId).withChild(ImmutableNodes.leafNode(inventoryNodeRefQname, invNodeId))
                .build();
        return augNode;
    }
}
