package org.opendaylight.topoprocessing.impl.listener;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.*;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class UnderlayTopologyListenerTest {

    private static final String TOPOLOGY_ID = "mytopo:1";

    @Mock private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockChange;
    @Mock private DOMDataBroker domDataBroker;

    @Mock private NormalizedNode<PathArgument, MapNode> mockNormalizedNode;

    @Before
    public void setUp() {
        DOMDataReadOnlyTransaction readTrandaction = Mockito.mock(DOMDataReadOnlyTransaction.class);
        Mockito.when(domDataBroker.newReadOnlyTransaction()).thenReturn(readTrandaction);
        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture = Mockito.mock(CheckedFuture.class);
        Mockito.when(readTrandaction.read((LogicalDatastoreType) Matchers.any(),
                (YangInstanceIdentifier) Matchers.any())).thenReturn(readFuture);
    }

    @Test
    public void testAggregation() {
        QName ipAddressQname = QName.create(Node.QNAME, "ip-address");
        String nodeName = "node:1";
        String ipAddress = "192.168.1.1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.of(Node.QNAME);
        LeafNode<String> nodeIpValue = ImmutableNodes.leafNode(ipAddressQname, ipAddress);
        MapEntryNode nodeValueWithIp = ImmutableNodes.mapEntryBuilder(
                Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).addChild(nodeIpValue).build();
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, nodeValueWithIp);

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(ipAddressQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new UnderlayTopologyListener(domDataBroker, mockOperator, TOPOLOGY_ID, pathIdentifier);
        listener.readExistingData(YangInstanceIdentifier.builder().build(), DatastoreType.OPERATIONAL);

        Map<YangInstanceIdentifier, UnderlayItem> createdEntries = new HashMap<>();
        UnderlayItem physicalNode = new UnderlayItem(nodeValueWithIp, nodeIpValue, TOPOLOGY_ID, nodeName, CorrelationItemEnum.Node);
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
        YangInstanceIdentifier nodeIdYiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME).nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).build();
        Mockito.when(mockChange.getRemovedPaths()).thenReturn(Collections.singleton(nodeIdYiid));
        listener.onDataChanged(mockChange);
        ArrayList<YangInstanceIdentifier> identifiers = new ArrayList<>();
        identifiers.add(nodeIdYiid);
        Mockito.verify(mockOperator).processRemovedChanges(Matchers.refEq(identifiers), Matchers.eq(TOPOLOGY_ID));
    }

    @Test
    public void testFiltrationRequest() {
        String nodeName = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.of(Node.QNAME);
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName);
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, nodeValue);

        TopologyFiltrator mockOperator = Mockito.mock(TopologyFiltrator.class);
        UnderlayTopologyListener listener = new UnderlayTopologyListener(
                domDataBroker, mockOperator, TOPOLOGY_ID, nodeYiid);
        listener.readExistingData(YangInstanceIdentifier.builder().build(), DatastoreType.OPERATIONAL);
        Map<YangInstanceIdentifier, UnderlayItem> createdEntries = new HashMap<>();
        UnderlayItem physicalNode = new UnderlayItem(nodeValue, null, TOPOLOGY_ID, nodeName, CorrelationItemEnum.Node);
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
        YangInstanceIdentifier nodeIdYiid = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME).nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeName).build();
        Mockito.when(mockChange.getRemovedPaths()).thenReturn(Collections.singleton(nodeIdYiid));
        listener.onDataChanged(mockChange);
        ArrayList<YangInstanceIdentifier> identifiers = new ArrayList<>();
        identifiers.add(nodeIdYiid);
        Mockito.verify(mockOperator).processRemovedChanges(Matchers.refEq(identifiers), Matchers.eq(TOPOLOGY_ID));
    }

    @Test(expected = IllegalStateException.class)
    public void testIncorrectNode() {
        String topoId = "node:1";
        YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.of(Node.QNAME);
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, topoId);
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, nodeValue);

        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new UnderlayTopologyListener(domDataBroker, mockOperator, TOPOLOGY_ID, nodeYiid);
        listener.readExistingData(YangInstanceIdentifier.builder().build(), DatastoreType.OPERATIONAL);

        Mockito.when(mockChange.getCreatedData()).thenReturn(mapCreated);
        listener.onDataChanged(mockChange);
    }

    @Test
    public void testReadExistingData() {
        DOMDataBroker domDataBrokerLocal = Mockito.mock(DOMDataBroker.class);
        UnderlayTopologyListener listener = new UnderlayTopologyListener(domDataBrokerLocal,
                Mockito.mock(TopologyOperator.class), TOPOLOGY_ID, YangInstanceIdentifier.builder().build());
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().build();
        DOMDataReadOnlyTransaction readTransaction = Mockito.mock(DOMDataReadOnlyTransaction.class);
        Mockito.when(domDataBrokerLocal.newReadOnlyTransaction()).thenReturn(readTransaction);
        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> checkedFuture
                = Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>>absent());
        Mockito.when(readTransaction.read(LogicalDatastoreType.OPERATIONAL, path)).thenReturn(checkedFuture);

        listener.readExistingData(path, DatastoreType.OPERATIONAL);
    }
}
