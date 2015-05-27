package org.opendaylight.topoprocessing.impl.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import java.util.*;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class UnderlayTopologyListenerTest {

    private static final String TOPOLOGY_ID = "mytopo:1";

    @Mock private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockChange;

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
        UnderlayTopologyListener listener = new UnderlayTopologyListener(mockOperator, TOPOLOGY_ID, pathIdentifier);

        Map<YangInstanceIdentifier, PhysicalNode> createdEntries = new HashMap<>();
        PhysicalNode physicalNode = new PhysicalNode(nodeValueWithIp, nodeIpValue, TOPOLOGY_ID, nodeName);
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
        UnderlayTopologyListener listener = new UnderlayTopologyListener(mockOperator, TOPOLOGY_ID, nodeYiid);

        Map<YangInstanceIdentifier, PhysicalNode> createdEntries = new HashMap<>();
        PhysicalNode physicalNode = new PhysicalNode(nodeValue, null, TOPOLOGY_ID, nodeName);
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
}
