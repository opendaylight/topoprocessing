/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.inventory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.topology.mlmt.inventory.InventoryTopologyProviderTest.ChangeListener;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class InventoryTopologyProviderTest extends AbstractDataBrokerTest {
    private final Object waitObject = new Object();
    private static final String MLMT = "mlmt:1";
    private static final String EXAMPLE = "example-linkstate-topology";
    private InventoryTopologyProvider provider;
    private MlmtOperationProcessor processor;
    private DataBroker dataBroker;
    private Thread thread;
    private Topology mlmtTopology;
    private Topology exampleTopology;
    InstanceIdentifier<Topology> mlmtTopologyIid;
    InstanceIdentifier<Topology> exampleIid;
    TopologyKey mlmtTopologyKey;

    public class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }

    private InstanceIdentifier<Topology> buildTopologyIid(final String topologyName) {
        TopologyId tid = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
    }

    private Topology buildMlmtTopology(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        mlmtTopologyKey = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
        final TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
        TopologyId underlayTopologyRef = new TopologyId(EXAMPLE);
        underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
        UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
        underlayTopologyBuilder.setKey(underlayKey);
        UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
        List<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
        lUnderlayTopology.add(underlayTopology);
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(mlmtTopologyKey);
        tbuilder.setTopologyId(topologyId);
        final Topology top = tbuilder.setTopologyTypes(topologyTypesBuilder.build())
                .setUnderlayTopology(lUnderlayTopology).build();

        return top;
    }

    private Topology buildUnderlayTopology(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);
        final Topology top = tbuilder.build();

        return top;
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
        InventoryAttributesParserTest parser = new InventoryAttributesParserTest();
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();
        mlmtTopologyIid = buildTopologyIid(MLMT);
        this.provider = new InventoryTopologyProvider();
        provider.init(processor, mlmtTopologyIid, parser);
        provider.setDataProvider(dataBroker);
        /*
         * It is necessary to create the network-topology containers in
         * both configuration and operational data storage
         */
        NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
        NetworkTopology networkTopology = nb.build();
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);

        dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                mlmtTopologyIid, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);

        mlmtTopology = buildMlmtTopology(MLMT);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, mlmtTopology, true);
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid, mlmtTopology, true);
        assertCommit(rwTx.submit());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();

        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Configuration mlmt:1 topology ", optional.isPresent());
        rxTopology = optional.get();
        assertNotNull(rxTopology);

        exampleIid = buildTopologyIid(EXAMPLE);
        exampleTopology = buildUnderlayTopology(EXAMPLE);

        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, nodeIid, nodeBuilder.build());
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, nodeIid, nodeBuilder.build());
        assertCommit(rwTx.submit());

        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        final InstanceIdentifier<TerminationPoint> instanceId = mlmtTopologyIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, instanceId, tpBuilder.build());
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, instanceId, tpBuilder.build());
        assertCommit(rwTx.submit());

        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        InstanceIdentifier<Link> linkIid = mlmtTopologyIid.child(Link.class, linkKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, linkIid, linkBuilder.build());
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, linkIid, linkBuilder.build());
        assertCommit(rwTx.submit());
    }

    @After
    public void clear() {
        // NOOP
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }

    @Test(timeout = 10000)
    public void onTopologyTest() throws Exception {
        mlmtTopology = buildMlmtTopology(MLMT);

        provider.onTopologyCreated(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, mlmtTopology);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Configuration mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);

        provider.onTopologyUpdated(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, mlmtTopology);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Configuration mlmt:1 topology ", optional.isPresent());
        rxTopology = optional.get();
        assertNotNull(rxTopology);

        provider.onTopologyDeleted(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Configuration mlmt:1 topology ", optional.isPresent());
        rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void onNodeTest() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId invNodeId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeId);
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey invNodeKey =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(invNodeId);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
                inventoryIid = InstanceIdentifier.create(Nodes.class)
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                                invNodeKey);

        NodeRef nodeRef = new NodeRef(inventoryIid);
        InventoryNodeBuilder inventoryNodeBuilder = new InventoryNodeBuilder();
        inventoryNodeBuilder.setInventoryNodeRef(nodeRef);

        provider.onNodeCreated(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, nodeBuilder.build());

        nodeBuilder.addAugmentation(InventoryNode.class, inventoryNodeBuilder.build());

        provider.onNodeCreated(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, nodeBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Node> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Node rxNode = optional.get();
        assertNotNull(rxNode);
        InventoryNode rxInventoryNode = rxNode.getAugmentation(InventoryNode.class);
        assertNotNull(rxInventoryNode);

        provider.onNodeUpdated(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, nodeBuilder.build());

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();
        assertNotNull(optional);

        provider.onNodeDeleted(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, nodeKey);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();
        assertNotNull(optional);
    }

    @Test(timeout = 10000)
    public void onNodeConnectorTest() throws Exception {
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        NodeKey nodeKey = new NodeKey(nodeId);

        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        final InstanceIdentifier<TerminationPoint> instanceId = mlmtTopologyIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        final InstanceIdentifier<InventoryNodeConnector> invNodeConnectorIid =
                mlmtTopologyIid.child(Node.class, nodeKey).child(TerminationPoint.class, tpKey)
                        .augmentation(InventoryNodeConnector.class);

        NodeConnectorRef nodeConnectorRef = new NodeConnectorRef(invNodeConnectorIid);
        InventoryNodeConnectorBuilder inventoryNodeConnectorBuilder = new InventoryNodeConnectorBuilder();
        inventoryNodeConnectorBuilder.setInventoryNodeConnectorRef(nodeConnectorRef);

        tpBuilder.addAugmentation(InventoryNodeConnector.class, inventoryNodeConnectorBuilder.build());

        provider.onTpCreated(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, nodeKey, tpBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        InstanceIdentifier<TerminationPoint> tpIid = mlmtTopologyIid.child(Node.class, nodeKey)
                .child(TerminationPoint.class, tpKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<TerminationPoint> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, tpIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        TerminationPoint rxTp = optional.get();
        assertNotNull(rxTp);
        InventoryNodeConnector rxInventoryNodeConnector = rxTp.getAugmentation(InventoryNodeConnector.class);
        assertNotNull(rxInventoryNodeConnector);

        provider.onTpUpdated(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, nodeKey, tpBuilder.build());

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, tpIid).get();
        assertNotNull(optional);

        provider.onTpDeleted(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, nodeKey, tpKey);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, tpIid).get();
        assertNotNull(optional);
    }

    @Test(timeout = 10000)
    public void onLinkTest() throws Exception {
        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        InstanceIdentifier<Link> linkIid = mlmtTopologyIid.child(Link.class, linkKey);

        provider.onLinkCreated(LogicalDatastoreType.OPERATIONAL,
		            mlmtTopologyIid, linkBuilder.build());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Link> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, linkIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Link link = optional.get();
        assertNotNull(link);

        provider.onLinkUpdated(LogicalDatastoreType.OPERATIONAL,
		            mlmtTopologyIid, linkBuilder.build());

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, linkIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        link = optional.get();
        assertNotNull(link);

        provider.onLinkDeleted(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, linkKey);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, linkIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        link = optional.get();
        assertNotNull(link);
    }

    @Test(timeout = 10000)
    public void onCloseTest() throws Exception {
        provider.close();
        assertNotNull(provider);
    }
}
