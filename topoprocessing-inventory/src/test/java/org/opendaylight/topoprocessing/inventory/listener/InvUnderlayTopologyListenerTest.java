/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventory.listener;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Assert;
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
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class InvUnderlayTopologyListenerTest {

    private static final String TOPOLOGY_ID = "mytopo:1";

    @Mock private NormalizedNode<PathArgument, MapNode> mockNormalizedNode;

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
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME).nodeWithKey(Topology.QNAME,TopologyQNames.TOPOLOGY_ID_QNAME,TOPOLOGY_ID)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).build();
        LeafNode<String> nodeIpValue = ImmutableNodes.leafNode(ipAddressQname, ipAddress);
        MapEntryNode nodeValueWithIp = ImmutableNodes.mapEntryBuilder(
                Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).addChild(nodeIpValue).build();

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(ipAddressQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new InvUnderlayTopologyListener(dataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>(1);
        pathIdentifiers.put(0, pathIdentifier);
        listener.setPathIdentifier(pathIdentifiers);

        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, nodeIpValue);
        UnderlayItem physicalNode = new UnderlayItem(nodeValueWithIp, targetFields, TOPOLOGY_ID, nodeName,
                CorrelationItemEnum.Node);
        NodeIdentifierWithPredicates nodePathArgument = new NodeIdentifierWithPredicates(Node.QNAME,
                TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName);
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
        listener.onDataTreeChanged(mockCollection);
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
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME).nodeWithKey(Topology.QNAME,TopologyQNames.TOPOLOGY_ID_QNAME,TOPOLOGY_ID)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).build();
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName);

        TopologyFiltrator mockOperator = Mockito.mock(TopologyFiltrator.class);
        UnderlayTopologyListener listener = new InvUnderlayTopologyListener(dataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
        UnderlayItem physicalNode = new UnderlayItem(nodeValue, null, TOPOLOGY_ID, nodeName, CorrelationItemEnum.Node);
        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();
        NodeIdentifierWithPredicates nodePathArgument = new NodeIdentifierWithPredicates(Node.QNAME,
                TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName);

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
        String nodeName = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.of(Node.QNAME);
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, nodeName);
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, nodeValue);

        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new InvUnderlayTopologyListener(dataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>(1);
        pathIdentifiers.put(0, nodeYiid);
        listener.setPathIdentifier(pathIdentifiers);
        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();
        NodeIdentifierWithPredicates nodePathArgument = new NodeIdentifierWithPredicates(Node.QNAME,
                TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName);

        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.WRITE);
        Optional<NormalizedNode<?, ?>> dataAfter = Optional.<NormalizedNode<?, ?>>fromNullable(nodeValue);
        rootNode.setDataAfter(dataAfter);
        rootNode.setIdentifier(nodePathArgument);
        listener.onDataTreeChanged(mockCollection);
    }

    @Test
    public void testAggregationTP() {
        UnderlayTopologyListener listener = new InvUnderlayTopologyListener(dataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.TerminationPoint);
        Assert.assertEquals(CorrelationItemEnum.TerminationPoint, listener.getCorrelationItem());
        String nodeName = "node:1";
        String ipAddress = "10.0.0.1";
        QName ipAddressQname = QName.create(Node.QNAME, "ip-address");
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME).nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).build();

        LeafNode<String> nodeIpValue = ImmutableNodes.leafNode(ipAddressQname, ipAddress);
        MapEntryNode testNode = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).addChild(nodeIpValue)
                .build();

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(ipAddressQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        listener.setOperator(mockOperator);
        Assert.assertEquals(mockOperator, listener.getOperator());
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>(1);
        pathIdentifiers.put(0, pathIdentifier);
        listener.setPathIdentifier(pathIdentifiers);

        UnderlayItem physicalNode = new UnderlayItem(testNode, null, TOPOLOGY_ID, nodeName,
                CorrelationItemEnum.TerminationPoint);
        NodeIdentifierWithPredicates nodePathArgument = new NodeIdentifierWithPredicates(Node.QNAME,
                TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName);
        TestDataTreeCandidateNode rootNode = new TestDataTreeCandidateNode();

        // create
        setUpMocks(rootNode);
        rootNode.setModificationType(ModificationType.WRITE);
        Optional<NormalizedNode<?, ?>> dataAfter = Optional.<NormalizedNode<?, ?>> fromNullable(testNode);
        rootNode.setDataAfter(dataAfter);
        rootNode.setIdentifier(nodePathArgument);
        listener.onDataTreeChanged(mockCollection);
        Mockito.verify(mockOperator).processCreatedChanges(Matchers.eq(nodeYiid), Matchers.refEq(physicalNode),
                Matchers.eq(TOPOLOGY_ID));
    }

}
