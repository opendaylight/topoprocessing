/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Preconditions;

import java.util.Collections;

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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MlmtTopologyNotifyTest {

    private static final String MLMT1 = "mlmt:1";
    private Thread thread;
    private InstanceIdentifier<Topology> mlmtTopologyInstanceId;
    private MlmtTopologyNotify mlmtTopologyNotify;
    private UpdateListener listener;
    private final Object waitObject = new Object();

    public class UpdateListener implements MlmtTopologyUpdateListener {
        public boolean nodeUpdated = false;
        public boolean tpUpdated = false;
        public boolean linkUpdated = false;
        MlmtTopologyUpdate rxEntry = null;

        @Override
        public void update(MlmtTopologyUpdate entry)  {
            final MlmtTopologyUpdateType topoUpdateType = entry.getType();
            if (topoUpdateType == MlmtTopologyUpdateType.NODE) {
                rxEntry = entry;
                nodeUpdated = true;
                synchronized (waitObject) {
                    waitObject.notify();
                }
            } else if (topoUpdateType == MlmtTopologyUpdateType.TP) {
                rxEntry = entry;
                tpUpdated = true;
                synchronized (waitObject) {
                    waitObject.notify();
                }
            } else if (topoUpdateType == MlmtTopologyUpdateType.LINK) {
                rxEntry = entry;
                linkUpdated = true;
                synchronized (waitObject) {
                    waitObject.notify();
                }
            }
        }
    }

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() throws Exception {
        TopologyId mlmtTopologyId = new TopologyId(MLMT1);
        TopologyKey mlmtTopologyKey = new TopologyKey(Preconditions.checkNotNull(mlmtTopologyId));
        mlmtTopologyInstanceId =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, mlmtTopologyKey);

        listener = new UpdateListener();
        mlmtTopologyNotify = new MlmtTopologyNotify(listener);
        thread = new Thread(mlmtTopologyNotify);
        thread.setDaemon(true);
        thread.setName("MlmtTopologyNotifyTest");
        thread.start();
    }

    private NodeBuilder createNodeBuilder(String nodeName) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId(nodeName);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        nodeBuilder.setSupportingNode(Collections.<SupportingNode>emptyList());

        return nodeBuilder;
    }

    private TerminationPointBuilder createTerminationPointBuilder(String nodeName,
            String tpName) {
        TpId tpId = new TpId(tpName);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        tpBuilder.setTpRef(Collections.<TpId>emptyList());

        return tpBuilder;
    }

    private String buildLinkName(String sourceNodeName, String sourceTpName,
            String destNodeName, String destTpName) {
        return sourceNodeName + "&" + sourceTpName + "&" + destNodeName + "&" + destTpName;
    }

    @Test(timeout = 10000)
    public void testUpdateOnNode() throws Exception {
       NodeBuilder nodeBuilder = createNodeBuilder("node:1");
       Node node = nodeBuilder.build();
       MlmtTopologyUpdateOnNode update = new MlmtTopologyUpdateOnNode(LogicalDatastoreType.OPERATIONAL,
               mlmtTopologyInstanceId, node);
       mlmtTopologyNotify.add(update);

       synchronized (waitObject) {
          waitObject.wait(5000);
        }

       assertTrue(listener.nodeUpdated);
       assertEquals(listener.rxEntry.getStoreType(), LogicalDatastoreType.OPERATIONAL);
       assertEquals(listener.rxEntry.getTopologyInstanceId(), mlmtTopologyInstanceId);
       assertEquals(listener.rxEntry.getNode(), node);
    }

    @Test(timeout = 10000)
    public void testUpdateOnTp() throws Exception {
       String nodeName = "node:1";
       String tpName = "tp:1";
       NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
       TerminationPointBuilder tpBuilder = createTerminationPointBuilder(nodeName, tpName);
       TerminationPoint tp = tpBuilder.build();
       MlmtTopologyUpdateOnTp update = new MlmtTopologyUpdateOnTp(LogicalDatastoreType.OPERATIONAL,
               mlmtTopologyInstanceId, nodeBuilder.getKey(), tp);
       mlmtTopologyNotify.add(update);

       synchronized (waitObject) {
          waitObject.wait(5000);
        }

       assertTrue(listener.tpUpdated);
       assertEquals(listener.rxEntry.getStoreType(), LogicalDatastoreType.OPERATIONAL);
       assertEquals(listener.rxEntry.getTopologyInstanceId(), mlmtTopologyInstanceId);
       assertEquals(listener.rxEntry.getNodeKey(), nodeBuilder.getKey());
       assertEquals(listener.rxEntry.getTerminationPoint(), tp);
    }

    @Test(timeout = 10000)
    public void testUpdateOnLink() throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);
        LinkBuilder linkBuilder = new LinkBuilder();
        String sourceNodeName = "node:1";
        String sourceTpName = "tp:1";
        String destNodeName = "node:2";
        String destTpName = "tp:2";
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
        Link link = linkBuilder.build();

        MlmtTopologyUpdateOnLink update = new MlmtTopologyUpdateOnLink(LogicalDatastoreType.OPERATIONAL,
               topologyInstanceId, link);
        mlmtTopologyNotify.add(update);

       synchronized (waitObject) {
          waitObject.wait(5000);
        }

       assertTrue(listener.linkUpdated);
       assertEquals(listener.rxEntry.getStoreType(), LogicalDatastoreType.OPERATIONAL);
       assertEquals(listener.rxEntry.getTopologyInstanceId(), mlmtTopologyInstanceId);
       assertEquals(listener.rxEntry.getLink(), link);
    }

    @After
    public void clear() {
        try {
            if (thread != null) {
                thread.interrupt();
                thread.join(2000);
                thread = null;
            }
        } catch (final InterruptedException e) {
            // NOOP
        }
    }

    @AfterClass
    public static void allMethodsClear() {
    }
}
