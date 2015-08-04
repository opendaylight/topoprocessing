/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author michal.polkorab
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryListenerTest {

    private static final String TOPOLOGY_ID = "test:1";

    @Mock private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockChange;
    @Mock private DOMDataBroker domDataBroker;
    private InventoryListener listener = new InventoryListener(TOPOLOGY_ID);
    private QName leafQname = QName.create(Node.QNAME, "leaf-node");
    private QName nodeIdQname = QName.create(Node.QNAME, "id");

    /** Successful scenario */
    @Test
    public void test() {
        String nodeName = "node:1";
        String leafValue = "10.0.0.1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.of(Node.QNAME);
        LeafNode<String> leafNodeValue = ImmutableNodes.leafNode(leafQname, leafValue);
        MapEntryNode testNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, nodeIdQname, nodeName)
                .addChild(leafNodeValue).build();
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, testNode);

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(leafQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        listener.setOperator(mockOperator);
        listener.setPathIdentifier(pathIdentifier);

        Map<YangInstanceIdentifier, UnderlayItem> createdEntries = new HashMap<>();
        UnderlayItem physicalNode = new UnderlayItem(testNode, leafNodeValue, TOPOLOGY_ID, nodeName, CorrelationItemEnum.Node);
        createdEntries.put(nodeYiid, physicalNode);

        // create
        Mockito.when(mockChange.getCreatedData()).thenReturn(mapCreated);
        listener.onDataChanged(mockChange);
        Mockito.verify(mockOperator).processCreatedChanges(Matchers.refEq(createdEntries), Matchers.eq(TOPOLOGY_ID));

        // update
        Mockito.when(mockChange.getUpdatedData()).thenReturn(mapCreated);
        listener.onDataChanged(mockChange);
        Mockito.verify(mockOperator).processUpdatedChanges(Matchers.refEq(createdEntries), Matchers.eq(TOPOLOGY_ID));

        // delete
        YangInstanceIdentifier nodeIdYiid = YangInstanceIdentifier.builder().node(Nodes.QNAME)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, nodeIdQname, nodeName).build();
        Mockito.when(mockChange.getRemovedPaths()).thenReturn(Collections.singleton(nodeIdYiid));
        listener.onDataChanged(mockChange);
        ArrayList<YangInstanceIdentifier> identifiers = new ArrayList<>();
        identifiers.add(nodeIdYiid);
        Mockito.verify(mockOperator).processRemovedChanges(Matchers.refEq(identifiers), Matchers.eq(TOPOLOGY_ID));
    }

    /** Missing specified LeafNode - no action should be taken */
    @Test
    public void testMissingLeafNode() {
        String nodeName = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.of(Node.QNAME);
        MapEntryNode testNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, nodeIdQname, nodeName).build();
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, testNode);

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(leafQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        listener.setOperator(mockOperator);
        listener.setPathIdentifier(pathIdentifier);

        Mockito.when(mockChange.getCreatedData()).thenReturn(mapCreated);
        listener.onDataChanged(mockChange);
        Mockito.verify(mockOperator, Mockito.times(0)).processCreatedChanges(
                (Map<YangInstanceIdentifier, UnderlayItem>) any(), Matchers.eq(TOPOLOGY_ID));
    }

    /** Verifies that no operations happen when received changes are empty */
    @Test
    public void testEmptyChanges() {
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> emptyMap = new HashMap<>();
        Mockito.when(mockChange.getCreatedData()).thenReturn(emptyMap);
        Mockito.when(mockChange.getUpdatedData()).thenReturn(emptyMap);
        Mockito.when(mockChange.getRemovedPaths()).thenReturn(Collections.<YangInstanceIdentifier> emptySet());
        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(leafQname);
        listener.setPathIdentifier(pathIdentifier);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        listener.setOperator(mockOperator);
        listener.onDataChanged(mockChange);
        Mockito.verify(mockOperator, Mockito.times(0)).processCreatedChanges(
                (Map<YangInstanceIdentifier, UnderlayItem>) any(), Matchers.eq(TOPOLOGY_ID));
        Mockito.verify(mockOperator, Mockito.times(0)).processUpdatedChanges(
                (Map<YangInstanceIdentifier, UnderlayItem>) any(), Matchers.eq(TOPOLOGY_ID));
        Mockito.verify(mockOperator, Mockito.times(0)).processRemovedChanges((List<YangInstanceIdentifier>) any(),
                (String) any());
    }

}
