/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.base.Preconditions;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MlmtTopologyUpdateTest{
    private InstanceIdentifier<Topology> mlmtTopologyInstanceId;

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        TopologyId mlmtTopologyId = new TopologyId("mlmt:1");
        TopologyKey mlmtTopologyKey = new TopologyKey(Preconditions.checkNotNull(mlmtTopologyId));
        mlmtTopologyInstanceId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, mlmtTopologyKey);
    }

    @Test
    public void testNodeUpdate() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId("node1");
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);

        MlmtTopologyUpdateOnNode update = new MlmtTopologyUpdateOnNode(LogicalDatastoreType.CONFIGURATION,
                mlmtTopologyInstanceId, nodeBuilder.build());
        assertNotNull(update);

        MlmtTopologyUpdateType rxType = update.getType();
        assertEquals(rxType, MlmtTopologyUpdateType.NODE);

        LogicalDatastoreType rxStoreType = update.getStoreType();
        assertEquals(rxStoreType, LogicalDatastoreType.CONFIGURATION);

        InstanceIdentifier<Topology> rxTopologyId = update.getTopologyInstanceId();
        assertEquals(rxTopologyId, mlmtTopologyInstanceId);

        Node rxNode = update.getNode();
        assertNotNull(rxNode);

        NodeKey rxNodeKey = update.getNodeKey();
        assertEquals(rxNodeKey, nodeKey);

        TerminationPoint rxTerminationPoint = update.getTerminationPoint();
        assertNull(rxTerminationPoint);

        Link rxLink = update.getLink();
        assertNull(rxLink);

        update = new MlmtTopologyUpdateOnNode(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyInstanceId, nodeBuilder.build());
        assertNotNull(update);

        rxType = update.getType();
        assertEquals(rxType, MlmtTopologyUpdateType.NODE);

        rxStoreType = update.getStoreType();
        assertEquals(rxStoreType, LogicalDatastoreType.OPERATIONAL);

        rxTopologyId = update.getTopologyInstanceId();
        assertEquals(rxTopologyId, mlmtTopologyInstanceId);

        rxNode = update.getNode();
        assertNotNull(rxNode);

        rxNodeKey = update.getNodeKey();
        assertEquals(rxNodeKey, nodeKey);

        rxTerminationPoint = update.getTerminationPoint();
        assertNull(rxTerminationPoint);

        rxLink = update.getLink();
        assertNull(rxLink);
    }

    @Test
    public void testTpUpdate() throws Exception {
        TpId tpId = new TpId("node1:2");
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);

        NodeId nodeId = new NodeId("node1");
        NodeKey nodeKey = new NodeKey(nodeId);

        MlmtTopologyUpdateOnTp update = new MlmtTopologyUpdateOnTp(LogicalDatastoreType.CONFIGURATION,
                mlmtTopologyInstanceId, nodeKey, tpBuilder.build());
        assertNotNull(update);

        MlmtTopologyUpdateType rxType = update.getType();
        assertEquals(rxType, MlmtTopologyUpdateType.TP);

        LogicalDatastoreType rxStoreType = update.getStoreType();
        assertEquals(rxStoreType, LogicalDatastoreType.CONFIGURATION);

        InstanceIdentifier<Topology> rxTopologyId = update.getTopologyInstanceId();
        assertEquals(rxTopologyId, mlmtTopologyInstanceId);

        Node rxNode = update.getNode();
        assertNull(rxNode);

        NodeKey rxNodeKey = update.getNodeKey();
        assertEquals(rxNodeKey, nodeKey);

        TerminationPoint rxTerminationPoint = update.getTerminationPoint();
        assertNotNull(rxTerminationPoint);

        Link rxLink = update.getLink();
        assertNull(rxLink);

        update = new MlmtTopologyUpdateOnTp(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyInstanceId, nodeKey, tpBuilder.build());
        assertNotNull(update);

        rxType = update.getType();
        assertEquals(rxType, MlmtTopologyUpdateType.TP);

        rxStoreType = update.getStoreType();
        assertEquals(rxStoreType, LogicalDatastoreType.OPERATIONAL);

        rxTopologyId = update.getTopologyInstanceId();
        assertEquals(rxTopologyId, mlmtTopologyInstanceId);

        rxNode = update.getNode();
        assertNull(rxNode);

        rxNodeKey = update.getNodeKey();
        assertEquals(rxNodeKey, nodeKey);

        rxTerminationPoint = update.getTerminationPoint();
        assertNotNull(rxTerminationPoint);

        rxLink = update.getLink();
        assertNull(rxLink);
    }

    private String buildLinkName(String sourceNodeName, String sourceTpName, String destNodeName, String destTpName) {
        return sourceNodeName + "&" + sourceTpName + "&" + destNodeName + "&" + destTpName;
    }

    @Test
    public void testLinkUpdate() throws Exception {
        String sourceNodeName = "node:1";
        String sourceTpName = "tp:1";
        String destNodeName = "node:2";
        String destTpName = "tp:2";
        LinkBuilder linkBuilder = new LinkBuilder();
        String sLinkName = buildLinkName(sourceNodeName, sourceTpName, destNodeName, destTpName);
        LinkId linkId = new LinkId(sLinkName);
        LinkKey linkKey = new LinkKey(linkId);
        SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(new NodeId(sourceNodeName));
        sourceBuilder.setSourceTp(new TpId(sourceTpName));
        DestinationBuilder destBuilder = new DestinationBuilder();
        destBuilder.setDestNode(new NodeId(destNodeName));
        destBuilder.setDestTp(new TpId(destTpName));

        linkBuilder.setKey(linkKey);
        linkBuilder.setLinkId(linkId);
        linkBuilder.setSource(sourceBuilder.build());
        linkBuilder.setDestination(destBuilder.build());

        MlmtTopologyUpdateOnLink update = new MlmtTopologyUpdateOnLink(LogicalDatastoreType.CONFIGURATION,
                mlmtTopologyInstanceId, linkBuilder.build());
        assertNotNull(update);

        MlmtTopologyUpdateType rxType = update.getType();
        assertEquals(rxType, MlmtTopologyUpdateType.LINK);

        LogicalDatastoreType rxStoreType = update.getStoreType();
        assertEquals(rxStoreType, LogicalDatastoreType.CONFIGURATION);

        InstanceIdentifier<Topology> rxTopologyId = update.getTopologyInstanceId();
        assertEquals(rxTopologyId, mlmtTopologyInstanceId);

        Node rxNode = update.getNode();
        assertNull(rxNode);

        NodeKey rxNodeKey = update.getNodeKey();
        assertNull(rxNodeKey);

        TerminationPoint rxTerminationPoint = update.getTerminationPoint();
        assertNull(rxTerminationPoint);

        Link rxLink = update.getLink();
        assertNotNull(rxLink);

        update = new MlmtTopologyUpdateOnLink(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyInstanceId, linkBuilder.build());
        assertNotNull(update);

        rxType = update.getType();
        assertEquals(rxType, MlmtTopologyUpdateType.LINK);

        rxStoreType = update.getStoreType();
        assertEquals(rxStoreType, LogicalDatastoreType.OPERATIONAL);

        rxTopologyId = update.getTopologyInstanceId();
        assertEquals(rxTopologyId, mlmtTopologyInstanceId);

        rxNode = update.getNode();
        assertNull(rxNode);

        rxNodeKey = update.getNodeKey();
        assertNull(rxNodeKey);

        rxTerminationPoint = update.getTerminationPoint();
        assertNull(rxTerminationPoint);

        rxLink = update.getLink();
        assertNotNull(rxLink);
    }

    @After
    public void clear() {
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }

}
