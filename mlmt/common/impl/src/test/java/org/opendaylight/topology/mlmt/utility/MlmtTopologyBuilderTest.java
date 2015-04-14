/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class MlmtTopologyBuilderTest extends AbstractDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtTopologyBuilderTest.class);
    private static final String MLMT1 = "mlmt:1";
    private final Object waitObject = new Object();
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;
    private MlmtTopologyBuilder builder;
    private Thread thread;
    private InstanceIdentifier<Topology> mlmtTopologyInstanceId;
    private TopologyId mlmtTopologyId;

    public class ChangeListener implements DataChangeListener {
        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }

    @Override
    protected void setupWithDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() throws Exception {
        /*
         * It is necessary to create first the network-topology data node
         * in order to populate with topology instances at a later moment
         */
        assertNotNull(dataBroker);

        NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
        NetworkTopology networkTopology = nb.build();

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        mlmtTopologyId = new TopologyId(MLMT1);
        TopologyKey mlmtTopologyKey = new TopologyKey(Preconditions.checkNotNull(mlmtTopologyId));
        mlmtTopologyInstanceId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, mlmtTopologyKey);
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setKey(mlmtTopologyKey);

        dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                mlmtTopologyInstanceId, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);

        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyInstanceId, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);

        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtTopologyBuilderTest");
        thread.start();
        builder = new MlmtTopologyBuilder();
        builder.init(dataBroker, LOG, processor);
    }

    private void createTopology(LogicalDatastoreType type) throws Exception {
        builder.createTopology(type, mlmtTopologyInstanceId);

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, mlmtTopologyInstanceId).get();
        assertNotNull(optional);
        assertTrue("mlmt:1 topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());
    }

    private void deleteTopology(LogicalDatastoreType type) throws Exception {
        builder.deleteTopology(type, mlmtTopologyInstanceId);

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, mlmtTopologyInstanceId).get();
        assertNotNull(optional);
        assertFalse("mlmt:1 topology is still present", optional.isPresent());
    }

    private void createUnderlayTopology(LogicalDatastoreType type, String underlayTopologyName) throws Exception {
        TopologyId underlayTopologyId = new TopologyId(underlayTopologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(underlayTopologyId));
        InstanceIdentifier<Topology> topologyInstanceId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
        builder.createUnderlayTopology(type, topologyInstanceId, underlayTopologyId);
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

    private void createNode(LogicalDatastoreType type, String nodeTopologyName, String nodeName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(nodeTopologyName);
        final NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        builder.createNode(type, topologyInstanceId, nodeTopologyId, nodeBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue("mlmt:1 topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());
        List<Node> lNode = rxTopology.getNode();
        assertFalse(lNode.isEmpty());

        /*
         * building array of node names to check against input one
         */
        List<String> lNodeNames = new ArrayList();
        for (Node cNode : lNode) {
            lNodeNames.add(cNode.getNodeId().getValue().toString());
        }

        assertTrue(lNodeNames.contains(nodeName));
    }

    private void deleteNode(LogicalDatastoreType type, String nodeTopologyName,
            String nodeName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);
        final NodeKey nodeKey = new NodeKey(new NodeId(nodeName));

        builder.deleteNode(type, topologyInstanceId, nodeKey);

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue(MLMT1 + " topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());

        List<Node> lNode = rxTopology.getNode();
        assertFalse(lNode.isEmpty());

        List<String> lNodeNames = new ArrayList();
        for (Node cNode : lNode) {
            String sNodeName = cNode.getNodeId().getValue().toString();
            lNodeNames.add(sNodeName);
        }

        assertFalse(lNodeNames.contains(nodeName));
    }

    private void deleteLastNode(LogicalDatastoreType type, String nodeTopologyName,
            String nodeName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);
        final NodeKey nodeKey = new NodeKey(new NodeId(nodeName));

        builder.deleteNode(type, topologyInstanceId, nodeKey);

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue(MLMT1 + " topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());

        List<Node> lNode = rxTopology.getNode();
        assertTrue(lNode.isEmpty());
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

    private void createTp(LogicalDatastoreType type, String nodeTopologyName,
            String nodeName, String tpName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);
        final TerminationPointBuilder tpBuilder = createTerminationPointBuilder(nodeName, tpName);
        final NodeKey nodeKey = new NodeKey(new NodeId(nodeName));

        builder.createTp(type, topologyInstanceId, nodeKey, tpBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue(MLMT1 + " topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());

        List<Node> lNode = rxTopology.getNode();
        assertFalse(lNode.isEmpty());
        /*
         * building array of node names
         */
        List<String> lNodeNames = new ArrayList();
        List<String> lTpNames = new ArrayList();
        for (Node cNode : lNode) {
            String sNodeName = cNode.getNodeId().getValue().toString();
            lNodeNames.add(sNodeName);
            if(nodeName.equals(sNodeName)) {
                List<TerminationPoint> lTp = cNode.getTerminationPoint();
                assertNotNull(lTp);
                for (TerminationPoint tp : lTp) {
                    String cTpName = tp.getKey().getTpId().getValue().toString();
                    assertNotNull(cTpName);
                    lTpNames.add(cTpName);
                }
                assertTrue(lTpNames.contains(tpName));
            }
        }

        assertTrue(lNodeNames.contains(nodeName));
    }

    private void deleteTp(LogicalDatastoreType type, String nodeTopologyName,
            String nodeName, String tpName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);
        final NodeKey nodeKey = new NodeKey(new NodeId(nodeName));
        TpId tpId = new TpId(tpName);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);

        builder.deleteTp(type, topologyInstanceId, nodeKey, tpKey);

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue(MLMT1 + " topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());

        List<Node> lNode = rxTopology.getNode();
        assertFalse(lNode.isEmpty());

        for (Node cNode : lNode) {
            String sNodeName = cNode.getNodeId().getValue().toString();
            if (nodeName.equals(sNodeName)) {
                assertTrue(cNode.getTerminationPoint().isEmpty());
            }
        }
    }

    private String buildLinkName(String sourceNodeName, String sourceTpName, String destNodeName, String destTpName) {
        return sourceNodeName + "&" + sourceTpName + "&" + destNodeName + "&" + destTpName;
    }

    private void createLink(LogicalDatastoreType type, String nodeTopologyName,
            String sourceNodeName, String sourceTpName, String destNodeName, String destTpName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);

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

        builder.createLink(type, topologyInstanceId, linkBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue(MLMT1 + " topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());

        List<Link> lLink = rxTopology.getLink();
        assertFalse(lLink.isEmpty());

        List<String> lLinkNames = new ArrayList();
        for (Link cLink : lLink) {
            String cLinkName = cLink.getLinkId().getValue().toString();
            lLinkNames.add(cLinkName);
        }

        assertTrue(lLinkNames.contains(sLinkName));
    }

    private void deleteLink(LogicalDatastoreType type, String nodeTopologyName,
            String sourceNodeName, String sourceTpName, String destNodeName, String destTpName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);

        String sLinkName = buildLinkName(sourceNodeName, sourceTpName, destNodeName, destTpName);
        LinkId linkId = new LinkId(sLinkName);
        LinkKey linkKey = new LinkKey(linkId);

        builder.deleteLink(type, topologyInstanceId, linkKey);

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue(MLMT1 + " topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());

        List<Link> lLink = rxTopology.getLink();
        assertFalse(lLink.isEmpty());

        List<String> lLinkNames = new ArrayList();
        for (Link cLink : lLink) {
            String cLinkName = cLink.getLinkId().getValue().toString();
            lLinkNames.add(cLinkName);
        }

        assertFalse(lLinkNames.contains(sLinkName));
    }

    private void deleteLastLink(LogicalDatastoreType type, String nodeTopologyName,
            String sourceNodeName, String sourceTpName, String destNodeName, String destTpName) throws Exception {
        final InstanceIdentifier<Topology> topologyInstanceId = mlmtTopologyInstanceId;
        final TopologyId nodeTopologyId = new TopologyId(MLMT1);

        String sLinkName = buildLinkName(sourceNodeName, sourceTpName, destNodeName, destTpName);
        LinkId linkId = new LinkId(sLinkName);
        LinkKey linkKey = new LinkKey(linkId);

        builder.deleteLink(type, topologyInstanceId, linkKey);

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(type, topologyInstanceId).get();
        assertNotNull(optional);
        assertTrue(MLMT1 + " topology not present", optional.isPresent());
        Topology rxTopology = optional.get();
        TopologyId checkTopologyId = rxTopology.getTopologyId();

        assertEquals(checkTopologyId.getValue().toString(), mlmtTopologyId.getValue().toString());

        List<Link> lLink = rxTopology.getLink();
        assertTrue(lLink.isEmpty());
    }

    @Test(timeout = 10000)
    public void testCreateTopologyConfiguration() throws Exception {
        createTopology(LogicalDatastoreType.CONFIGURATION);
    }

    @Test(timeout = 10000)
    public void testCreateTopologyOperational() throws Exception {
        createTopology(LogicalDatastoreType.OPERATIONAL);
    }

    @Test(timeout = 10000)
    public void testCreateUnderlayTopologyConfiguration() throws Exception {
        createUnderlayTopology(LogicalDatastoreType.CONFIGURATION, "underlay:1");
    }

    @Test(timeout = 10000)
    public void testCreateUnderlayTopologyOperational() throws Exception {
        createUnderlayTopology(LogicalDatastoreType.OPERATIONAL, "underlay:1");
    }

    @Test(timeout = 10000)
    public void testDeleteTopologyConfiguration() throws Exception {
        createTopology(LogicalDatastoreType.CONFIGURATION);
        deleteTopology(LogicalDatastoreType.CONFIGURATION);
    }

    @Test(timeout = 10000)
    public void testDeleteTopologyOperational() throws Exception {
        createTopology(LogicalDatastoreType.OPERATIONAL);
        deleteTopology(LogicalDatastoreType.OPERATIONAL);
    }

    @Test(timeout = 10000)
    public void testCreateNodeTpLinkConfiguration() throws Exception {
        createTopology(LogicalDatastoreType.CONFIGURATION);
        createNode(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:1");
        createNode(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:2");
        createTp(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:1", "tp:1");
        createTp(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:2", "tp:2");
        createLink(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:1", "tp:1", "node:2", "tp:2");
        createLink(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:2", "tp:2", "node:1", "tp:1");
        deleteLink(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:2", "tp:2", "node:1", "tp:1");
        deleteLastLink(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:1", "tp:1", "node:2", "tp:2");
        deleteTp(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:2", "tp:2");
        deleteTp(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:1", "tp:1");
        deleteNode(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:2");
        deleteLastNode(LogicalDatastoreType.CONFIGURATION, "example-linkstate", "node:1");
        deleteTopology(LogicalDatastoreType.CONFIGURATION);
    }

    @Test(timeout = 10000)
    public void testCreateNodeTpLinkOperational() throws Exception {
        createTopology(LogicalDatastoreType.OPERATIONAL);
        createNode(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:1");
        createNode(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:2");
        createTp(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:1", "tp:1");
        createTp(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:2", "tp:2");
        createLink(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:1", "tp:1", "node:2", "tp:2");
        createLink(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:2", "tp:2", "node:1", "tp:1");
        deleteLink(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:2", "tp:2", "node:1", "tp:1");
        deleteLastLink(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:1", "tp:1", "node:2", "tp:2");
        deleteTp(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:2", "tp:2");
        deleteTp(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:1", "tp:1");
        deleteNode(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:2");
        deleteLastNode(LogicalDatastoreType.OPERATIONAL, "example-linkstate", "node:1");
        deleteTopology(LogicalDatastoreType.OPERATIONAL);
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

        }
    }

    @AfterClass
    public static void allMethodsClear() {
    }
}
