/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.listener;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author andrej.zan
 */
@RunWith(MockitoJUnitRunner.class)
public class I2RSUnderlayTopologyListenerTest {

    private static final String TOPOLOGY_ID = "mytopo:1";

    @Mock private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockChange;
    @Mock private DOMDataBroker domDataBroker;
    @Mock private NormalizedNode<PathArgument, MapNode> mockNormalizedNode;
    @Mock private DOMDataReadOnlyTransaction readTransaction;
    @Mock private CheckedFuture<Optional<NormalizedNode<?,?>>, ReadFailedException> readFuture;

    @Before
    public void setUp() {
        Mockito.when(domDataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        Mockito.when(readTransaction.read((LogicalDatastoreType) Matchers.any(),
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
                Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName).addChild(nodeIpValue).build();
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, nodeValueWithIp);

        YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.of(ipAddressQname);
        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new I2RSUnderlayTopologyListener(domDataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
        listener.setPathIdentifier(pathIdentifier);
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
        YangInstanceIdentifier nodeIdYiid = YangInstanceIdentifier.builder()
                .node(Network.QNAME).nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPOLOGY_ID)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName).build();
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
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName);
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, nodeValue);

        TopologyFiltrator mockOperator = Mockito.mock(TopologyFiltrator.class);
        UnderlayTopologyListener listener = new I2RSUnderlayTopologyListener(domDataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
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
        YangInstanceIdentifier nodeIdYiid = YangInstanceIdentifier.builder()
                .node(Network.QNAME).nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPOLOGY_ID)
                .node(Node.QNAME).nodeWithKey(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeName).build();
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
        MapEntryNode nodeValue = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, topoId);
        HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> mapCreated = new HashMap<>();
        mapCreated.put(nodeYiid, nodeValue);

        TopologyAggregator mockOperator = Mockito.mock(TopologyAggregator.class);
        UnderlayTopologyListener listener = new I2RSUnderlayTopologyListener(domDataBroker, TOPOLOGY_ID,
                CorrelationItemEnum.Node);
        listener.setOperator(mockOperator);
        listener.setPathIdentifier(nodeYiid);
        listener.readExistingData(YangInstanceIdentifier.builder().build(), DatastoreType.OPERATIONAL);

        Mockito.when(mockChange.getCreatedData()).thenReturn(mapCreated);
        listener.onDataChanged(mockChange);
    }

    @Test
    public void testReadExistingData() {
        DOMDataBroker domDataBrokerLocal = Mockito.mock(DOMDataBroker.class);
        UnderlayTopologyListener listener = new I2RSUnderlayTopologyListener(domDataBrokerLocal,
                TOPOLOGY_ID, CorrelationItemEnum.Node);
        listener.setOperator(Mockito.mock(TopologyOperator.class));
        listener.setPathIdentifier(YangInstanceIdentifier.builder().build());
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().build();
        DOMDataReadOnlyTransaction readTransaction = Mockito.mock(DOMDataReadOnlyTransaction.class);
        Mockito.when(domDataBrokerLocal.newReadOnlyTransaction()).thenReturn(readTransaction);
        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> checkedFuture
                = Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>>absent());
        Mockito.when(readTransaction.read(LogicalDatastoreType.OPERATIONAL, path)).thenReturn(checkedFuture);

        listener.readExistingData(path, DatastoreType.OPERATIONAL);
    }
}
