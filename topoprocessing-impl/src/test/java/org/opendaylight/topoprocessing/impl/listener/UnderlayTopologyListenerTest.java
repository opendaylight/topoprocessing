package org.opendaylight.topoprocessing.impl.listener;

import static org.mockito.Mockito.mock;
import org.junit.Assert;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.testUtilities.TestDataTreeCandidateNode;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class UnderlayTopologyListenerTest {
    private static final String TOPOLOGY_ID = "test:1";

    @Mock
    private Collection<DataTreeCandidate> mockCollection;
    @Mock
    private Iterator<DataTreeCandidate> mockIteratorCandidate;
    @Mock
    private DataTreeCandidate mockDataTreeCandidate;
    @Mock
    private DataTreeCandidateNode mockDataTreeCandidateNode;
    @Mock
    private Collection<DataTreeCandidateNode> mockDataTreeCandidateNodeCollection;
    @Mock
    private Iterator<DataTreeCandidateNode> mockDataTreeCandidateNodeIterator;
    private PingPongDataBroker dataBrokerMock = new PingPongDataBroker(mock(DOMDataBroker.class));
    private UnderlayTopologyListener listener = new TestUnderlayTopologyListener(dataBrokerMock, TOPOLOGY_ID,
            CorrelationItemEnum.Node);

    @Test
    public void test() {
        Assert.assertEquals(TOPOLOGY_ID, listener.getUnderlayTopologyId());
        Assert.assertEquals(CorrelationItemEnum.Node, listener.getCorrelationItem());
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
        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, nodeIpValue);

        UnderlayItem physicalNode = new UnderlayItem(testNode, targetFields, TOPOLOGY_ID, nodeName,
                CorrelationItemEnum.Node);
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

    @Test
    public void testAggregationTP() {
        Assert.assertEquals(TOPOLOGY_ID, listener.getUnderlayTopologyId());
        listener = new TestUnderlayTopologyListener(dataBrokerMock, TOPOLOGY_ID, CorrelationItemEnum.TerminationPoint);
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

    @Test
    public void testFiltration() {
        Assert.assertEquals(TOPOLOGY_ID, listener.getUnderlayTopologyId());
        Assert.assertEquals(CorrelationItemEnum.Node, listener.getCorrelationItem());
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

        TopologyFiltrator mockOperator = Mockito.mock(TopologyFiltrator.class);
        listener.setOperator(mockOperator);
        Assert.assertEquals(mockOperator, listener.getOperator());
        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, nodeIpValue);

        UnderlayItem physicalNode = new UnderlayItem(testNode, null, TOPOLOGY_ID, nodeName,
                CorrelationItemEnum.Node);
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

    private void resetMocks() {
        Mockito.reset(mockCollection, mockIteratorCandidate, mockDataTreeCandidate, mockDataTreeCandidateNode,
                mockDataTreeCandidateNodeCollection, mockDataTreeCandidateNodeIterator);
    }

    private class TestUnderlayTopologyListener extends UnderlayTopologyListener {

        public TestUnderlayTopologyListener(PingPongDataBroker dataBroker, String underlayTopologyId,
                CorrelationItemEnum correlationItem) {
            super(dataBroker, underlayTopologyId, correlationItem);
            if (CorrelationItemEnum.TerminationPoint.equals(correlationItem)) {
                this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node,
                        NetworkTopologyModel.class);
                this.itemQName = TopologyQNames.buildItemQName(CorrelationItemEnum.Node, NetworkTopologyModel.class);
            } else {
                this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(correlationItem,
                        NetworkTopologyModel.class);
                this.itemQName = TopologyQNames.buildItemQName(correlationItem, NetworkTopologyModel.class);
            }
            this.itemIdentifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                    .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId).node(itemQName)
                    .build();
        }

    }
}
