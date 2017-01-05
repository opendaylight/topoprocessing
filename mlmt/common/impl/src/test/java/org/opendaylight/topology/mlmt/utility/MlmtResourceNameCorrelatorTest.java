/*
 * Copyright (c) 2017 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

public class MlmtResourceNameCorrelatorTest {

    private static MlmtResourceNameCorrelator mlmtResourceNameCorrelator;

    @BeforeClass
    public static void allMethodsSetUp() {
        mlmtResourceNameCorrelator = new MlmtResourceNameCorrelator();
        mlmtResourceNameCorrelator.init();
    }

    @Before
    public void setUp() throws Exception {
        // NOOP
    }

    @Test(timeout = 3000)
    public void testPutMlmtNodeId() throws Exception {
        final String nodeName = "10.0.0.1";
        final String nodeIdString = "bgpls://IsisLevel1:0/type=node&as=10&domain=3&area=0&router=1000.0000.0001";
        final NodeId nodeId = new NodeId(nodeIdString);
        mlmtResourceNameCorrelator.putMlmtNodeId(nodeName, nodeId);
        String rxNodeName = mlmtResourceNameCorrelator.getMlmtNodeName(nodeId);
        assertEquals(rxNodeName, nodeName);
        NodeId rxNodeId = mlmtResourceNameCorrelator.getMlmtNodeId(nodeName);
        assertEquals(rxNodeId, nodeId);
        mlmtResourceNameCorrelator.removeMlmtNodeId(nodeName);
        rxNodeName = mlmtResourceNameCorrelator.getMlmtNodeName(nodeId);
        assertNull(rxNodeName);
        rxNodeId = mlmtResourceNameCorrelator.getMlmtNodeId(nodeName);
        assertNull(rxNodeId);
    }

    @Test(timeout = 3000)
    public void testPutNodeId2NodeName() throws Exception {
        final String nodeName = "10.0.0.1";
        final String nodeIdString = "bgpls://IsisLevel1:0/type=node&as=10&domain=3&area=0&router=1000.0000.0001";
        final NodeId nodeId = new NodeId(nodeIdString);
        mlmtResourceNameCorrelator.putNodeId2NodeName(nodeId, nodeName);
        String rxNodeName = mlmtResourceNameCorrelator.getNodeName(nodeId);
        assertEquals(rxNodeName, nodeName);
    }

    @Test(timeout = 3000)
    public void testPutMlmtTerminationPointId() throws Exception {
        final String nodeName = "10.0.0.1";
        final String nodeIdString = "bgpls://IsisLevel1:0/type=node&as=10&domain=3&area=0&router=1000.0000.0001";
        final NodeId nodeId = new NodeId(nodeIdString);
        final String tpName = "1.5.2.2";
        final String tpIdString = "bgpls://IsisLevel1:0/type=tp&ipv4=1.5.2.2&id=0";
        final TpId tpId = new TpId(tpIdString);
        mlmtResourceNameCorrelator.putMlmtNodeId(nodeName, nodeId);
        mlmtResourceNameCorrelator.putMlmtTerminationPointId(nodeName, tpName, tpId);
        TpId rxTpId = mlmtResourceNameCorrelator.getMlmtTerminationPointId(nodeName, tpName);
        assertEquals(rxTpId, tpId);
        String rxTpName = mlmtResourceNameCorrelator.getMlmtTerminationPointName(nodeId, tpId);
        assertEquals(rxTpName, tpName);
        mlmtResourceNameCorrelator.removeMlmtTerminationPointId(nodeName, tpName);
        rxTpName = mlmtResourceNameCorrelator.getMlmtTerminationPointName(nodeId, tpId);
        assertNull(rxTpName);
        rxTpId = mlmtResourceNameCorrelator.getMlmtTerminationPointId(nodeName, tpName);
        assertNull(rxTpId);
    }

    @Test(timeout = 3000)
    public void testPutMlmtLinkId() throws Exception {
        final String linkName = "10.0.0.1:1.5.2.2";
        final String linkIdString = "bgpls://IsisLevel1:0/router=1000.0000.0001&ipv4=1.5.2.2&id=0";
        final LinkId linkId = new LinkId(linkIdString);
        mlmtResourceNameCorrelator.putMlmtLinkId(linkName, linkId);
        LinkId rxLinkId = mlmtResourceNameCorrelator.getMlmtLinkId(linkName);
        assertEquals(rxLinkId, linkId);
        String rxLinkName = mlmtResourceNameCorrelator.getMlmtLinkName(linkId);
        assertEquals(rxLinkName, linkName);
        mlmtResourceNameCorrelator.removeMlmtLinkId(linkName);
        rxLinkId = mlmtResourceNameCorrelator.getMlmtLinkId(linkName);
        assertNull(rxLinkId);
        rxLinkName = mlmtResourceNameCorrelator.getMlmtLinkName(linkId);
        assertNull(rxLinkName);
    }

    @Test(timeout = 3000)
    public void testPutUnderNodeId() throws Exception {
        final NodeId underNodeId = new NodeId("underNodeId:10.0.0.1");
        final NodeId mlmtNodeId = new NodeId("mlmtNodeId:10.0.0.1");;
        mlmtResourceNameCorrelator.putUnderNodeId(underNodeId, mlmtNodeId);
        NodeId rxMlmtNodeId = mlmtResourceNameCorrelator.getUnderNodeId2Mlmt(underNodeId);
        assertEquals(rxMlmtNodeId, mlmtNodeId);
        NodeId rxUnderNodeId = mlmtResourceNameCorrelator.getMlmtNodeId2Under(mlmtNodeId);
        assertEquals(rxUnderNodeId, underNodeId);
        mlmtResourceNameCorrelator.removeUnderNodeId(underNodeId);
        rxMlmtNodeId = mlmtResourceNameCorrelator.getUnderNodeId2Mlmt(underNodeId);
        assertNull(rxMlmtNodeId);
        rxUnderNodeId = mlmtResourceNameCorrelator.getMlmtNodeId2Under(mlmtNodeId);
        assertNull(rxUnderNodeId);
    }

    @Test(timeout = 3000)
    public void testPutUnderTerminationPointId() throws Exception {
        final NodeId underNodeId = new NodeId("underNodeId:10.0.0.1");
        final TpId underTpId = new TpId("underTpId:1.5.4.2");
        final TpId tpId = new TpId("mlmtTpId:1.5.4.2");
        mlmtResourceNameCorrelator.putUnderTerminationPointId2Mlmt(underNodeId, underTpId, tpId);
        TpId rxTpId = mlmtResourceNameCorrelator.getUnderTerminationPointId2Mlmt(underNodeId, underTpId);
        assertEquals(rxTpId, tpId);
        mlmtResourceNameCorrelator.removeUnderTerminationPointId2Mlmt(underNodeId, underTpId);
        rxTpId = mlmtResourceNameCorrelator.getUnderTerminationPointId2Mlmt(underNodeId, underTpId);
        assertNull(rxTpId);
    }

    @Test(timeout = 3000)
    public void testPutIsoSystemId() throws Exception {
        final String nodeName = "10.0.0.1";
        final String nodeIdString = "bgpls://IsisLevel1:0/type=node&as=10&domain=3&area=0&router=1000.0000.0001";
        final NodeId nodeId = new NodeId(nodeIdString);
        final String isoSystemIdString = "1000.0000.0001";
        IsoSystemId isoSystemId = IsoSystemId.getDefaultInstance(isoSystemIdString);
        mlmtResourceNameCorrelator.putIsoSystemId2NodeName(isoSystemId, nodeName);
        String rxNodeName = mlmtResourceNameCorrelator.getNodeName(isoSystemId);
        assertEquals(rxNodeName, nodeName);
        mlmtResourceNameCorrelator.putIsoSystemId2NodeId(isoSystemId, nodeId);
        NodeId rxNodeId = mlmtResourceNameCorrelator.getNodeId(isoSystemId);
        assertEquals(rxNodeId, nodeId);
        mlmtResourceNameCorrelator.removeIsoSystemId2NodeName(isoSystemId);
        rxNodeName = mlmtResourceNameCorrelator.getNodeName(isoSystemId);
        assertNull(rxNodeName);
        mlmtResourceNameCorrelator.removeIsoSystemId2NodeId(isoSystemId);
        rxNodeId = mlmtResourceNameCorrelator.getNodeId(isoSystemId);
        assertNull(rxNodeId);
        mlmtResourceNameCorrelator.putIsoSystemId2NodeId(isoSystemId, nodeId);
        rxNodeId = mlmtResourceNameCorrelator.getNodeId(isoSystemId);
        assertEquals(rxNodeId, nodeId);
        mlmtResourceNameCorrelator.removeNodeId2IsoSystemId(nodeId);
        rxNodeId = mlmtResourceNameCorrelator.getNodeId(isoSystemId);
        assertNull(rxNodeId);
    }

    @Test(timeout = 3000)
    public void testPutIpv4Address() throws Exception {
        final String nodeName = "10.0.0.1";
        final String nodeIdString = "bgpls://IsisLevel1:0/type=node&as=10&domain=3&area=0&router=1000.0000.0001";
        final NodeId nodeId = new NodeId(nodeIdString);
        String ipv4AddressString = "172.56.100.14";
        Ipv4Address ipv4Address = Ipv4Address.getDefaultInstance(ipv4AddressString);
        mlmtResourceNameCorrelator.putIpv4Address2NodeName(ipv4Address, nodeName);
        String rxNodeName = mlmtResourceNameCorrelator.getNodeName(ipv4Address);
        assertEquals(rxNodeName, nodeName);
        mlmtResourceNameCorrelator.removeIpv4Address2NodeName(ipv4Address);
        rxNodeName = mlmtResourceNameCorrelator.getNodeName(ipv4Address);
        assertNull(rxNodeName);
        mlmtResourceNameCorrelator.putIpv4Address2NodeId(ipv4Address, nodeId);
        NodeId rxNodeId = mlmtResourceNameCorrelator.getNodeId(ipv4Address);
        assertEquals(rxNodeId, nodeId);
        Ipv4Address rxIpv4Address = mlmtResourceNameCorrelator.getIpv4Address(nodeId);
        assertEquals(rxIpv4Address, ipv4Address);
        mlmtResourceNameCorrelator.removeIpv4Address2NodeId(ipv4Address);
        rxIpv4Address = mlmtResourceNameCorrelator.getIpv4Address(nodeId);
        assertNull(rxIpv4Address);
        rxNodeId = mlmtResourceNameCorrelator.getNodeId(ipv4Address);
        assertNull(rxNodeId);
        mlmtResourceNameCorrelator.putIpv4Address2NodeId(ipv4Address, nodeId);
        rxNodeId = mlmtResourceNameCorrelator.getNodeId(ipv4Address);
        assertEquals(rxNodeId, nodeId);
        rxIpv4Address = mlmtResourceNameCorrelator.getIpv4Address(nodeId);
        mlmtResourceNameCorrelator.removeNodeId2Ipv4Address(nodeId);
        rxNodeId = mlmtResourceNameCorrelator.getNodeId(ipv4Address);
        assertNull(rxNodeId);
        rxIpv4Address = mlmtResourceNameCorrelator.getIpv4Address(nodeId);
        assertNull(rxIpv4Address);
    }

    @Test(timeout = 3000)
    public void testIpv4Address2TpId() throws Exception {
        final String nodeName = "10.0.0.1";
        final TpId tpId = new TpId("mlmtTpId:1.5.4.2");
        String ipv4AddressString = "172.56.100.14";
        Ipv4Address ipv4Address = Ipv4Address.getDefaultInstance(ipv4AddressString);
        mlmtResourceNameCorrelator.putIpv4Address2TpId(ipv4Address, nodeName, tpId);
        TpId rxTpId = mlmtResourceNameCorrelator.getTpId(ipv4Address, nodeName);
        assertEquals(rxTpId, tpId);
        mlmtResourceNameCorrelator.removeTpId2Ipv4Address(ipv4Address, nodeName);
        rxTpId = mlmtResourceNameCorrelator.getTpId(ipv4Address, nodeName);
        assertNull(rxTpId);
    }

    @Test(timeout = 3000)
    public void testInvNodeKey2NodeId() throws Exception {
        final String nodeIdString = "bgpls://IsisLevel1:0/type=node&as=10&domain=3&area=0&router=1000.0000.0001";
        final NodeId nodeId = new NodeId(nodeIdString);
        final String invNodeString = "invNode:1";
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId invNodeId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(invNodeString);
        final NodeKey invNodeKey = new NodeKey(invNodeId);
        mlmtResourceNameCorrelator.putInvNodeKey2NodeId(invNodeKey, nodeId);
        final NodeId rxNodeId = mlmtResourceNameCorrelator.getNodeId(invNodeKey);
        assertEquals(rxNodeId, nodeId);
    }

    @Test(timeout = 3000)
    public void testInvNodeConnectorKey2TpId() throws Exception {
        final String invNodeString = "invNode:1";
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId invNodeId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(invNodeString);
        final NodeKey invNodeKey = new NodeKey(invNodeId);
        final String nodeConnectorIdString = "invNodeConnector:1";
        final NodeConnectorId nodeConnectorId = new NodeConnectorId(nodeConnectorIdString);
        final NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(nodeConnectorId);
        final TpId tpId = new TpId("mlmtTpId:1.5.4.2");
        mlmtResourceNameCorrelator.putInvNodeConnectorKey2TpId(invNodeKey, nodeConnectorKey, tpId);
        TpId rxTpId = mlmtResourceNameCorrelator.getTpId(invNodeKey, nodeConnectorKey);
        assertEquals(tpId, rxTpId);
        mlmtResourceNameCorrelator.removeInvNodeConnectorKey2TpId(invNodeKey, nodeConnectorKey);
        rxTpId = mlmtResourceNameCorrelator.getTpId(invNodeKey, nodeConnectorKey);
        assertNull(rxTpId);
    }

    @Test(timeout = 3000)
    public void testUnderLinkId2Mlmt() throws Exception {
         final String linkString = "10.0.0.1:172.16.54.12";
         final LinkId linkId = new LinkId(linkString);
         final String underlinkString = "bgpls://IsisLevel1:0/router=1000.0000.0001&ipv4=1.5.2.2&id=0";
         final LinkId underLinkId = new LinkId(underlinkString);
         mlmtResourceNameCorrelator.putUnderLinkId2Mlmt(underLinkId, linkId);
         LinkId rxLinkId = mlmtResourceNameCorrelator.getUnderLinkId2Mlmt(underLinkId);
         assertEquals(rxLinkId, linkId);
         mlmtResourceNameCorrelator.removeUnderLinkId2Mlmt(underLinkId);
         rxLinkId = mlmtResourceNameCorrelator.getUnderLinkId2Mlmt(underLinkId);
         assertNull(rxLinkId);
    }
}
