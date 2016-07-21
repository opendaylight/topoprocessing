/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import static org.mockito.Matchers.any;

import com.google.common.base.Optional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.testUtilities.TestDataTreeCandidateNode;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author michal.polkorab
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryListenerTest {

    private static final String TOPOLOGY_ID = "test:1";

    @Mock private Collection<DataTreeCandidate> mockCollection;
    @Mock private Iterator<DataTreeCandidate> mockIteratorCandidate;
    @Mock private DataTreeCandidate mockDataTreeCandidate;
    @Mock private DataTreeCandidateNode mockDataTreeCandidateNode;
    @Mock private Collection<DataTreeCandidateNode> mockDataTreeCandidateNodeCollection;
    @Mock private Iterator<DataTreeCandidateNode> mockDataTreeCandidateNodeIterator;

    private InventoryListener listener = new InventoryListener(TOPOLOGY_ID, CorrelationItemEnum.Node);
    private QName leafQname = QName.create(Node.QNAME, "leaf-node");
    private QName nodeIdQname = QName.create(Node.QNAME, "id");

    /** Successful scenario */
    @Test
    public void test() {
        String nodeName = "node:1";
        String leafValue = "10.0.0.1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder()
                .node(Nodes.QNAME)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, TopologyQNames.INVENTORY_NODE_ID_QNAME, nodeName)
                .build();

        LeafNode<String> leafNodeValue = ImmutableNodes.leafNode(leafQname, leafValue);
        MapEntryNode testNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, nodeIdQname, nodeName)
                .addChild(leafNodeValue).build();

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(leafQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        listener.setOperator(mockOperator);
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>(1);
        pathIdentifiers.put(0, pathIdentifier);
        listener.setPathIdentifier(pathIdentifiers);

        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, leafNodeValue);
        UnderlayItem physicalNode = new UnderlayItem(null, targetFields, TOPOLOGY_ID, null, CorrelationItemEnum.Node);
        NodeIdentifierWithPredicates nodePathArgument = new NodeIdentifierWithPredicates(Node.QNAME,
                TopologyQNames.INVENTORY_NODE_ID_QNAME, nodeName);
        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();

        // create
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.WRITE);
        Optional<NormalizedNode<?, ?>> dataAfter = Optional.<NormalizedNode<?, ?>>fromNullable(testNode);
        rootNode.setDataAfter(dataAfter);
        rootNode.setIdentifier(nodePathArgument);
        listener.onDataTreeChanged(mockCollection);
        Mockito.verify(mockOperator).processCreatedChanges(Matchers.eq(nodeYiid), Matchers.refEq(physicalNode),
                Matchers.eq(TOPOLOGY_ID));

        // update
        resetMocks();
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.SUBTREE_MODIFIED);
        listener.onDataTreeChanged(mockCollection);
        Mockito.verify(mockOperator).processUpdatedChanges(Matchers.eq(nodeYiid), Matchers.refEq(physicalNode), Matchers.eq(TOPOLOGY_ID));

        // delete
        resetMocks();
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.DELETE);
        rootNode.setIdentifier(nodePathArgument);
        YangInstanceIdentifier nodeIdYiid = YangInstanceIdentifier.builder()
                .node(Nodes.QNAME).node(Node.QNAME).nodeWithKey(Node.QNAME, nodeIdQname, nodeName).build();
        listener.onDataTreeChanged(mockCollection);
        Mockito.verify(mockOperator).processRemovedChanges(Matchers.eq(nodeIdYiid), Matchers.eq(TOPOLOGY_ID));
    }

    private void setUpMocks(TestDataTreeCandidateNode rootNode) {
        Mockito.when(mockCollection.iterator()).thenReturn(mockIteratorCandidate);
        Mockito.when(mockIteratorCandidate.hasNext()).thenReturn(true, false);
        Mockito.when(mockIteratorCandidate.next()).thenReturn(mockDataTreeCandidate);
        Mockito.when(mockDataTreeCandidate.getRootNode()).thenReturn(mockDataTreeCandidateNode);
        Mockito.when(mockDataTreeCandidateNode.getChildNodes()).thenReturn(mockDataTreeCandidateNodeCollection);
        Mockito.when(mockDataTreeCandidateNodeCollection.iterator()).thenReturn(mockDataTreeCandidateNodeIterator);
        Mockito.when(mockDataTreeCandidateNodeIterator.hasNext()).thenReturn(true, false);
        Mockito.when(mockDataTreeCandidateNodeIterator.next()).thenReturn(rootNode);
    }

    private void resetMocks() {
        Mockito.reset(mockCollection, mockIteratorCandidate, mockDataTreeCandidate, mockDataTreeCandidateNode,
                mockDataTreeCandidateNodeCollection, mockDataTreeCandidateNodeIterator);
    }

    /** Missing specified LeafNode - no action should be taken */
    @Test
    public void testMissingLeafNode() {
        String nodeName = "node:1";
        MapEntryNode testNode = ImmutableNodes.mapEntryBuilder(Node.QNAME, nodeIdQname, nodeName).build();

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(leafQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        listener.setOperator(mockOperator);
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>(1);
        pathIdentifiers.put(0, pathIdentifier);
        listener.setPathIdentifier(pathIdentifiers);
        NodeIdentifierWithPredicates nodePathArgument = new NodeIdentifierWithPredicates(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, leafQname);
        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();

        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.WRITE);
        rootNode.setIdentifier(nodePathArgument);
        Optional<NormalizedNode<?, ?>> dataAfter = Optional.<NormalizedNode<?, ?>>fromNullable(testNode);
        rootNode.setDataAfter(dataAfter);
        listener.onDataTreeChanged(mockCollection);
        Mockito.verify(mockOperator, Mockito.times(0)).processCreatedChanges(
                (YangInstanceIdentifier) any(), (UnderlayItem) any(), Matchers.eq(TOPOLOGY_ID));
    }
}
