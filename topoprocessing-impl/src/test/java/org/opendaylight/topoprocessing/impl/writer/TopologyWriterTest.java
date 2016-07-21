/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.writer;

import com.google.common.util.concurrent.CheckedFuture;

import java.util.ArrayList;

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
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
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

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyWriterTest {

    private static final String TOPOLOGY_ID = "mytopo:1";
    private TopologyWriter topologyWriterNTModel;
    private TopologyWriter topologyWriterI2rsModel;

    private static final YangInstanceIdentifier topologyIdentifierNTModel = YangInstanceIdentifier
            .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID).build();
    private static final YangInstanceIdentifier nodeIdentifierNTModel = YangInstanceIdentifier
            .builder(topologyIdentifierNTModel).node(Node.QNAME).build();
    private static final YangInstanceIdentifier linkIdentifierNTModel = YangInstanceIdentifier
            .builder(topologyIdentifierNTModel).node(Link.QNAME).build();

    private static final YangInstanceIdentifier topologyIdentifierI2rsModel = YangInstanceIdentifier
            .builder(InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER)
            .nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPOLOGY_ID).build();
    private static final YangInstanceIdentifier nodeIdentifierI2rsModel = YangInstanceIdentifier
            .builder(topologyIdentifierI2rsModel).node(org.opendaylight.yang.gen.v1.urn.ietf.params
            .xml.ns.yang.ietf.network.rev150608.network.Node.QNAME).build();
    private static final YangInstanceIdentifier linkIdentifierI2rsModel = YangInstanceIdentifier
            .builder(topologyIdentifierI2rsModel).node(org.opendaylight.yang.gen.v1.urn.ietf.params
            .xml.ns.yang.ietf.network.topology.rev150608.network.Link.QNAME).build();

    @Mock private DOMTransactionChain transactionChain;
    @Mock private OverlayItemTranslator translator;
    @Mock private DOMDataWriteTransaction transaction;
    @Mock private CheckedFuture<Void,TransactionCommitFailedException> submit;
    @Mock private DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> topologyTypes;

    /**
     * Initializes writer.
     */
    @Before
    public void setUp() {
        topologyWriterNTModel = new TopologyWriter(TOPOLOGY_ID, NetworkTopologyModel.class);
        topologyWriterNTModel.setTransactionChain(transactionChain);

        topologyWriterI2rsModel = new TopologyWriter(TOPOLOGY_ID, I2rsModel.class);
        topologyWriterI2rsModel.setTransactionChain(transactionChain);
    }

    /**
     * Tests if overlay topology was correctly initialized - this means Topology with topology-id
     * + Link and Node mapnodes are written (initialization without I2rsModel).
     */
    @Test
    public void testInitOverlayTopologyNTModel() {
        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);
        topologyWriterNTModel.initOverlayTopology();

        YangInstanceIdentifier networkId = YangInstanceIdentifier.of(NetworkTopology.QNAME);

        MapNode nodeMapNode = ImmutableNodes.mapNodeBuilder(Node.QNAME).build();
        MapNode linkMapNode = ImmutableNodes.mapNodeBuilder(Link.QNAME).build();
        ContainerNode networkNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(NetworkTopology.QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(Topology.QNAME)
                        .withChild(ImmutableNodes.mapEntryBuilder(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME,
                                TOPOLOGY_ID)
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
     * Tests if overlay topology was correctly initialized - this means Topology with topology-id
     * + Link and Node mapnodes are written (initialization with I2rsModel).
     */
    @Test
    public void testInitOverlayTopologyI2rsModel() {
        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);
        topologyWriterI2rsModel.initOverlayTopology();

        YangInstanceIdentifier networkId = YangInstanceIdentifier.of(Network.QNAME);

        MapNode nodeMapNode = ImmutableNodes.mapNodeBuilder(org.opendaylight.yang.gen.v1.urn.ietf.params
                .xml.ns.yang.ietf.network.rev150608.network.Node.QNAME).build();
        MapNode linkMapNode = ImmutableNodes.mapNodeBuilder(org.opendaylight.yang.gen.v1.urn.ietf.params
                .xml.ns.yang.ietf.network.topology.rev150608.network.Link.QNAME).build();
        MapNode networkNode = ImmutableNodes.mapNodeBuilder(Network.QNAME)
                        .withChild(ImmutableNodes.mapEntryBuilder(Network.QNAME,
                                TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPOLOGY_ID)
                                .withChild(nodeMapNode)
                                .withChild(linkMapNode).build())
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
     * Tests if topology-types node was written.
     */
    @Test
    public void testWriteTopologyTypes() {
        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);
        topologyWriterNTModel.writeTopologyTypes(topologyTypes);
        YangInstanceIdentifier topologyTypesYiid = topologyIdentifierNTModel.node(TopologyTypes.QNAME);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        Mockito.verify(transaction).put(LogicalDatastoreType.OPERATIONAL, topologyTypesYiid, topologyTypes);
        Mockito.verify(transaction).submit();
    }

    /**
     * Tests if writer release resources correctly.
     */
    @Test
    public void testTearDown() {
        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);
        topologyWriterNTModel.tearDown();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        Mockito.verify(transaction).delete(LogicalDatastoreType.OPERATIONAL, topologyIdentifierNTModel);
        Mockito.verify(transaction).submit();
        Mockito.verify(transactionChain, Mockito.times(1)).close();
    }

    /**
     * Test if delete without I2rsModel initialization is working correctly with all CorrelationItemEnum input
     * parameters.
     */
    @Test
    public void testDeleteItemNTModel() {
        OverlayItem overlayItem = new OverlayItem(new ArrayList<>(), CorrelationItemEnum.Node);
        OverlayItemWrapper wrapper = new OverlayItemWrapper("ID", overlayItem);

        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);

        YangInstanceIdentifier topologyTypesYiidNodeAndTerminationPoint = YangInstanceIdentifier
                .builder(nodeIdentifierNTModel).nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                        "ID").build();
        YangInstanceIdentifier topologyTypesYiidLink = YangInstanceIdentifier.builder(linkIdentifierNTModel)
                .nodeWithKey(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, "ID").build();
        //Node testing
        topologyWriterNTModel.deleteItem(wrapper, CorrelationItemEnum.Node);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        //TerminationPoint testing
        topologyWriterNTModel.deleteItem(wrapper, CorrelationItemEnum.TerminationPoint);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        //Link testing
        topologyWriterNTModel.deleteItem(wrapper, CorrelationItemEnum.Link);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }

        Mockito.verify(transaction, Mockito.times(2)).delete(LogicalDatastoreType.OPERATIONAL,
                topologyTypesYiidNodeAndTerminationPoint);
        Mockito.verify(transaction, Mockito.times(1)).delete(LogicalDatastoreType.OPERATIONAL, topologyTypesYiidLink);
        Mockito.verify(transaction, Mockito.times(3)).submit();
    }

    /**
     * Test if delete with I2rsModel initialization is working correctly with all CorrelationItemEnum input
     * parameters.
     */
    @Test
    public void testDeleteItemI2rsModel() {
        OverlayItem overlayItem = new OverlayItem(new ArrayList<>(), CorrelationItemEnum.Node);
        OverlayItemWrapper wrapper = new OverlayItemWrapper("ID", overlayItem);

        Mockito.when(transactionChain.newWriteOnlyTransaction()).thenReturn(transaction);
        Mockito.when(transaction.submit()).thenReturn(submit);

        YangInstanceIdentifier topologyTypesYiidNodeAndTerminationPoint = YangInstanceIdentifier
                .builder(nodeIdentifierI2rsModel).nodeWithKey(org.opendaylight.yang.gen.v1.urn.ietf.params
                        .xml.ns.yang.ietf.network.rev150608.network.Node.QNAME,
                        TopologyQNames.I2RS_NODE_ID_QNAME, "ID").build();
        YangInstanceIdentifier topologyTypesYiidLink = YangInstanceIdentifier.builder(linkIdentifierI2rsModel)
                .nodeWithKey(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                        .rev150608.network.Link.QNAME, TopologyQNames.I2RS_LINK_ID_QNAME, "ID").build();
        //Node testing
        topologyWriterI2rsModel.deleteItem(wrapper, CorrelationItemEnum.Node);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        //TerminationPoint testing
        topologyWriterI2rsModel.deleteItem(wrapper, CorrelationItemEnum.TerminationPoint);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        //Link testing
        topologyWriterI2rsModel.deleteItem(wrapper, CorrelationItemEnum.Link);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }

        Mockito.verify(transaction, Mockito.times(2)).delete(LogicalDatastoreType.OPERATIONAL,
                topologyTypesYiidNodeAndTerminationPoint);
        Mockito.verify(transaction, Mockito.times(1)).delete(LogicalDatastoreType.OPERATIONAL, topologyTypesYiidLink);
        Mockito.verify(transaction, Mockito.times(3)).submit();
    }

    /**
     * Test if NullPointerException is thrown, when there is null wrapper parameter in deleteItem method.
     */
    @Test (expected = NullPointerException.class)
    public void testDeleteItemWrapperNull() {
        topologyWriterNTModel.deleteItem(null, CorrelationItemEnum.Link);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Exception while waiting on thread pool to process transaction");
        }
        Mockito.verify(transaction, Mockito.times(0)).delete(LogicalDatastoreType.OPERATIONAL,
                (YangInstanceIdentifier)Mockito.any());
    }
}