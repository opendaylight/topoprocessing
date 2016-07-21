/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.listener;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.testUtilities.TestDataTreeCandidateNode;
import org.opendaylight.topoprocessing.impl.testUtilities.TestingDOMDataBroker;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
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
 * @author andrej.zan
 */
@RunWith(MockitoJUnitRunner.class)
public class I2RSUnderlayTopologyListenerTest {

    private static final String TOPOLOGY_ID = "mytopo:1";

    @Mock private Collection<DataTreeCandidate> mockCollection;
    @Mock private Iterator<DataTreeCandidate> mockIteratorCandidate;
    @Mock private DataTreeCandidate mockDataTreeCandidate;
    @Mock private DataTreeCandidateNode mockDataTreeCandidateNode;
    @Mock private Collection<DataTreeCandidateNode> mockDataTreeCandidateNodeCollection;
    @Mock private Iterator<DataTreeCandidateNode> mockDataTreeCandidateNodeIterator;

    private PingPongDataBroker dataBroker;
    private TestingDOMDataBroker testingDOMDataBroker;

    @Before
    public void setUp() {
        testingDOMDataBroker = new TestingDOMDataBroker();
        dataBroker = new PingPongDataBroker(testingDOMDataBroker);
    }

    @Test
    public void testAggregation() {
        QName ipAddressQname = QName.create(Node.QNAME, "ip-address");
        String nodeName = "node:1";
        String ipAddress = "192.168.1.1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER)
                .nodeWithKey(Network.QNAME,TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPOLOGY_ID)
                .node(Node.QNAME).build();
        LeafNode<String> nodeIpValue = ImmutableNodes.leafNode(ipAddressQname, ipAddress);
        MapEntryNode nodeValueWithIp = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME,
                nodeName).addChild(nodeIpValue).build();

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(ipAddressQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new I2RSUnderlayTopologyListener(dataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.TerminationPoint);
        listener.setOperator(mockOperator);
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>(1);
        pathIdentifiers.put(0, pathIdentifier);
        listener.setPathIdentifier(pathIdentifiers);
        UnderlayItem physicalNode = new UnderlayItem(nodeValueWithIp, null,
                TOPOLOGY_ID, nodeName, CorrelationItemEnum.TerminationPoint);
        NodeIdentifierWithPredicates nodePathArgument =
                new NodeIdentifierWithPredicates(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName);
        nodeYiid = nodeYiid.node(nodePathArgument);
        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();

        // create
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.WRITE);
        Optional<NormalizedNode<?, ?>> dataAfter = Optional.<NormalizedNode<?, ?>>fromNullable(nodeValueWithIp);
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
        Mockito.verify(mockOperator).processUpdatedChanges(Matchers.eq(nodeYiid), Matchers.refEq(physicalNode),
                Matchers.eq(TOPOLOGY_ID));

        // delete
        resetMocks();
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.DELETE);
        rootNode.setIdentifier(nodePathArgument);
        YangInstanceIdentifier nodeIdYiid = YangInstanceIdentifier.builder()
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName).build();
        listener.onDataTreeChanged(mockCollection);
        ArrayList<YangInstanceIdentifier> identifiers = new ArrayList<>();
        identifiers.add(nodeIdYiid);
        Mockito.verify(mockOperator).processRemovedChanges(Matchers.eq(nodeYiid), Matchers.eq(TOPOLOGY_ID));
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

    @Test
    public void testFiltrationRequest() {
        String nodeName = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER)
                .nodeWithKey(Network.QNAME,TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPOLOGY_ID)
                .node(Node.QNAME).build();
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName);

        TopologyFiltrator mockOperator = Mockito.mock(TopologyFiltrator.class);
        UnderlayTopologyListener listener = new I2RSUnderlayTopologyListener(dataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
        UnderlayItem physicalNode = new UnderlayItem(nodeValue, null, TOPOLOGY_ID, nodeName, CorrelationItemEnum.Node);

        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();
        NodeIdentifierWithPredicates nodePathArgument =
                new NodeIdentifierWithPredicates(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName);
        nodeYiid = nodeYiid.node(nodePathArgument);

        // create
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.WRITE);
        Optional<NormalizedNode<?, ?>> dataAfter = Optional.<NormalizedNode<?, ?>>fromNullable(nodeValue);
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
        Mockito.verify(mockOperator).processUpdatedChanges(Matchers.eq(nodeYiid), Matchers.refEq(physicalNode),
                Matchers.eq(TOPOLOGY_ID));

        // delete
        resetMocks();
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.DELETE);
        listener.onDataTreeChanged(mockCollection);
        Mockito.verify(mockOperator).processRemovedChanges(Matchers.eq(nodeYiid), Matchers.eq(TOPOLOGY_ID));
    }

    @Test(expected = IllegalStateException.class)
    public void testIncorrectNode() {
        String topoId = "node:1";
        String nodeName = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.of(Node.QNAME);
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, topoId);

        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new I2RSUnderlayTopologyListener(dataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>(1);
        pathIdentifiers.put(0, nodeYiid);
        listener.setPathIdentifier(pathIdentifiers);
        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();
        NodeIdentifierWithPredicates nodePathArgument =
                new NodeIdentifierWithPredicates(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName);

        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.WRITE);
        Optional<NormalizedNode<?, ?>> dataAfter = Optional.<NormalizedNode<?, ?>>fromNullable(nodeValue);
        rootNode.setDataAfter(dataAfter);
        rootNode.setIdentifier(nodePathArgument);
        listener.onDataTreeChanged(mockCollection);
    }
}
