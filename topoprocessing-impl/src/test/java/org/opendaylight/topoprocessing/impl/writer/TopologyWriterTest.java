/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.writer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyWriterTest {

    private final static String TOPOLOGY_ID = "mytopo:1";
    private TopologyWriter topologyWriter;
    private final YangInstanceIdentifier topologyIdentifier = YangInstanceIdentifier
            .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID).build();

    @Mock private DOMTransactionChain transactionChain;
    @Mock private DOMDataWriteTransaction transaction;
    @Mock private CheckedFuture<Void,TransactionCommitFailedException> submit;
    @Mock private DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> topologyTypes;
;

    /**
     * Initializes writer
     */
    @Before
    public void setUp() {
        topologyWriter = new TopologyWriter(TOPOLOGY_ID, NetworkTopologyModel.class);
        topologyWriter.setTransactionChain(transactionChain);
    }

    /**
     * Tests if overlay topology was correctly initialized - this means Topology with topology-id
     * + Link and Node mapnodes are written
     */
    @Test
    public void testInitOverlayTopology() {
        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);
        topologyWriter.initOverlayTopology();

        YangInstanceIdentifier networkId = YangInstanceIdentifier.of(NetworkTopology.QNAME);

        MapNode nodeMapNode = ImmutableNodes.mapNodeBuilder(Node.QNAME).build();
        MapNode linkMapNode = ImmutableNodes.mapNodeBuilder(Link.QNAME).build();
        ContainerNode networkNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(NetworkTopology.QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(Topology.QNAME)
                        .withChild(ImmutableNodes.mapEntryBuilder(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID)
                                .withChild(nodeMapNode)
                                .withChild(linkMapNode).build())
                        .build())
                .build();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        Mockito.verify(transaction).merge(LogicalDatastoreType.OPERATIONAL, networkId, networkNode);
        Mockito.verify(transaction).submit();
    }

    /**
     * Tests if topology-types node was written
     */
    @Test
    public void testWriteTopologyTypes() {
        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);
        topologyWriter.writeTopologyTypes(topologyTypes);
        YangInstanceIdentifier topologyTypesYiid = topologyIdentifier.node(TopologyTypes.QNAME);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        Mockito.verify(transaction).put(LogicalDatastoreType.OPERATIONAL, topologyTypesYiid, topologyTypes);
        Mockito.verify(transaction).submit();
    }

    /**
     * Tests if writer release resources correctly
     */
    @Test
    public void testTearDown() {
        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);
        topologyWriter.tearDown();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        Mockito.verify(transaction).delete(LogicalDatastoreType.OPERATIONAL, topologyIdentifier);
        Mockito.verify(transaction).submit();
        Mockito.verify(transactionChain, Mockito.times(1)).close();
    }
}
