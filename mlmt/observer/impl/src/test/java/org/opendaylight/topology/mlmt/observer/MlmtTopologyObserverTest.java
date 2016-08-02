/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.topology.mlmt.utility.MlmtConsequentAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

@RunWith(MockitoJUnitRunner.class)
public class MlmtTopologyObserverTest extends AbstractDataBrokerTest {

    private final Object waitObject = new Object();
    private static final String MLMT = "mlmt:1";
    private static final String EXAMPLE = "example-linkstate-topology";
    private MlmtTopologyObserver observer;
    private DataBroker dataBroker;

    public class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }

     public class MlmtRpcProviderRegistryMock implements RpcProviderRegistry {

        public MlmtRpcProviderRegistryMock() { }

        @Override
        public <T extends RpcService> BindingAwareBroker.RpcRegistration<T> addRpcImplementation(
                Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T> addRoutedRpcImplementation(
                Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L>
                registerRouteChangeListener(L listener) {
            return Mockito.mock(ListenerRegistration.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
            return null;
        }
    }

    private InstanceIdentifier<Topology> buildTopologyIid(final String topologyName) {
        TopologyId tid = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
    }

    private Topology buildUnderlayTopology(final String topologyName, boolean multitechFlag) {
        TopologyId topologyId = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);
        final TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
        if (multitechFlag) {
            final MultitechnologyTopologyBuilder multitechnologyTopologyBuilder = new MultitechnologyTopologyBuilder();
            final MtTopologyTypeBuilder mtTopologyTypeBuilder = new MtTopologyTypeBuilder();
            mtTopologyTypeBuilder.setMultitechnologyTopology(multitechnologyTopologyBuilder.build());
            topologyTypesBuilder.addAugmentation(MtTopologyType.class, mtTopologyTypeBuilder.build());
        }
        final Topology top = tbuilder.setTopologyTypes(topologyTypesBuilder.build()).build();

        return top;
    }

    private Topology buildMlmtTopology(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
        final TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
        topologyTypesBuilder.addAugmentation(MtTopologyType.class, new MtTopologyTypeBuilder().build());
        TopologyId underlayTopologyRef = new TopologyId(EXAMPLE);
        underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
        UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
        underlayTopologyBuilder.setKey(underlayKey);
        UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
        List<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
        lUnderlayTopology.add(underlayTopology);
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);
        final Topology top = tbuilder.setTopologyTypes(topologyTypesBuilder.build())
                .setUnderlayTopology(lUnderlayTopology)
                .setLink(Collections.<Link>emptyList())
                .setNode(Collections.<Node>emptyList())
                .build();

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
        MlmtRpcProviderRegistryMock rpcRegistry = new MlmtRpcProviderRegistryMock();
        this.observer = new MlmtTopologyObserver();
        this.observer.init(dataBroker, rpcRegistry, null, null);

        /*
         * It is necessary to create the network-topology containers in
         * both configuration and operational data storage
         */
        NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
        NetworkTopology networkTopology = nb.build();
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class),
                networkTopology);
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        InstanceIdentifier<Topology> topologyIid = buildTopologyIid(MLMT);
        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                topologyIid, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);
    }

    @Test(timeout = 10000)
    public void testMlmtConfigured() throws Exception {
        InstanceIdentifier<Topology> topologyIid = buildTopologyIid(MLMT);
        final Topology wrTopology = buildMlmtTopology(MLMT);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, topologyIid, wrTopology);
        assertCommit(rwTx.submit());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, topologyIid).get();
        assertNotNull(optional);
        assertTrue("Configuration mlmt:1 topology ", optional.isPresent());

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, topologyIid).get();
        if (optional != null && optional.isPresent() == true) {
            Topology rxTopology = optional.get();
            assertEquals(rxTopology, wrTopology);
        }
    }

    @Test(timeout = 10000)
    public void testOnTopologyCreated() throws Exception {
        InstanceIdentifier<Topology> topologyIid = buildTopologyIid(MLMT);
        final Topology wrTopology = buildMlmtTopology(MLMT);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, topologyIid, wrTopology, true);
        assertCommit(rwTx.submit());

        observer.onTopologyCreated(LogicalDatastoreType.OPERATIONAL, topologyIid, wrTopology);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, topologyIid).get();
        if (optional != null && optional.isPresent() == true && optional.get() != null) {
            Topology rxTopology = optional.get();
            assertEquals(rxTopology, wrTopology);
        }
    }

    @Test(timeout = 10000)
    public void testOnUnderlayTopologyCreated() throws Exception {
        handleTestOnUnderlayTopologyCreated(false);
        handleTestOnUnderlayTopologyCreated(true);
   }

    private void handleTestOnUnderlayTopologyCreated(boolean multitechFlag) throws Exception {
        InstanceIdentifier<Topology> mlmtTopologyIid = buildTopologyIid(MLMT);
        Topology wrTopology = buildMlmtTopology(MLMT);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid, wrTopology);
        assertCommit(rwTx.submit());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        if (optional != null && optional.isPresent() == true) {
            Topology rxTopology = optional.get();
            assertEquals(rxTopology, wrTopology);
        }

        InstanceIdentifier<Topology> exampleIid = buildTopologyIid(EXAMPLE);
        wrTopology = buildUnderlayTopology(EXAMPLE, multitechFlag);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, exampleIid, wrTopology, true);
        assertCommit(rwTx.submit());
        MlmtConsequentAction mlmtConsequentAction = observer.getMlmtConsequentAction(exampleIid);
        if (multitechFlag) {
            assertEquals(mlmtConsequentAction, MlmtConsequentAction.COPY);
        } else {
            assertEquals(mlmtConsequentAction, MlmtConsequentAction.BUILD);
        }

        String nodeName1 = "node:1";
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId(nodeName1);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        nodeBuilder.setTerminationPoint(Collections.<TerminationPoint>emptyList());
        Node wrNode = nodeBuilder.build();
        observer.onNodeCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, wrNode);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        String tpName1 = "1:1";
        TpId tpId = new TpId(tpName1);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        TerminationPoint tp = tpBuilder.build();
        observer.onTpCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey, tp);

        String nodeName2 = "node:2";
        nodeBuilder = new NodeBuilder();
        nodeId = new NodeId(nodeName2);
        nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        wrNode = nodeBuilder.build();
        observer.onNodeCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, wrNode);

        tpBuilder = new TerminationPointBuilder();
        String tpName2 = "2:1";
        tpId = new TpId(tpName2);
        tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        tp = tpBuilder.build();
        observer.onTpCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey, tp);

        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName1 = "link1";
        LinkId linkId = new LinkId(linkName1);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        linkBuilder.setLinkId(linkId);

        SourceBuilder sourceBuilder = new SourceBuilder();
        nodeId = new NodeId(nodeName1);
        sourceBuilder.setSourceNode(nodeId);
        tpId = new TpId(tpName1);
        sourceBuilder.setSourceTp(tpId);
        linkBuilder.setSource(sourceBuilder.build());

        DestinationBuilder destinationBuilder = new DestinationBuilder();
        nodeId = new NodeId(nodeName2);
        destinationBuilder.setDestNode(nodeId);
        tpId = new TpId(tpName2);
        destinationBuilder.setDestTp(tpId);
        linkBuilder.setDestination(destinationBuilder.build());
        Link link = linkBuilder.build();

        observer.onLinkCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, link);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);

        /*
         * Update observer methods section
         */
        observer.onTopologyUpdated(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, wrTopology);

        rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optionalTopology = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optionalTopology);
        assertTrue("Operational mlmt:1 topology node ", optionalTopology.isPresent());

        nodeName1 = "node:1";
        nodeBuilder = new NodeBuilder();
        nodeId = new NodeId(nodeName1);
        nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        wrNode = nodeBuilder.build();
        observer.onNodeUpdated(LogicalDatastoreType.OPERATIONAL, exampleIid, wrNode);

        tpBuilder = new TerminationPointBuilder();
        tpName1 = "1:1";
        tpId = new TpId(tpName1);
        tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        tp = tpBuilder.build();
        observer.onTpUpdated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey, tp);

        linkBuilder = new LinkBuilder();
        linkName1 = "link1";
        linkId = new LinkId(linkName1);
        linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        linkBuilder.setLinkId(linkId);
        observer.onLinkUpdated(LogicalDatastoreType.OPERATIONAL, exampleIid, link);

        /*
         * Delete observer methods section
         */
        observer.onLinkDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid, linkKey);

        rTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Link> linkIid = mlmtTopologyIid.child(Link.class, linkKey);
        Optional<Link> rxOptionalLink = rTx.read(LogicalDatastoreType.OPERATIONAL, linkIid).get();
        assertNotNull(rxOptionalLink);
        assertFalse("Operational mlmt:1 topology link ", rxOptionalLink.isPresent());

        nodeName1 = "node:1";
        nodeId = new NodeId(nodeName1);
        nodeKey = new NodeKey(nodeId);
        tpName1 = "1:1";
        tpId = new TpId(tpName1);
        tpKey = new TerminationPointKey(tpId);
        observer.onTpDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey, tpKey);

        nodeName1 = "node:2";
        nodeId = new NodeId(nodeName1);
        nodeKey = new NodeKey(nodeId);
        tpName1 = "2:1";
        tpId = new TpId(tpName1);
        tpKey = new TerminationPointKey(tpId);
        observer.onTpDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey, tpKey);

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology node ", optional.isPresent());
        rxTopology = optional.get();
        assertNotNull(rxTopology);

        nodeName1 = "node:1";
        nodeId = new NodeId(nodeName1);
        nodeKey = new NodeKey(nodeId);
        observer.onNodeDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey);

        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);
        rTx = dataBroker.newReadOnlyTransaction();
        Optional<Node> optionalNode = rTx.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();
        if (optionalNode != null && !optionalNode.isPresent()) {
            assertFalse("Operational mlmt:1 topology node ", optionalNode.isPresent());
        }

        nodeName1 = "node:2";
        nodeId = new NodeId(nodeName1);
        nodeKey = new NodeKey(nodeId);
        observer.onNodeDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey);

        nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);
        rTx = dataBroker.newReadOnlyTransaction();
        optionalNode = rTx.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();
        if (optionalNode != null && !optionalNode.isPresent()) {
            assertFalse("Operational mlmt:1 topology node ", optionalNode.isPresent());
        }

        observer.onTopologyDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid);

        optionalTopology = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optionalTopology);
        assertTrue("Operational mlmt:1 topology node ", optionalTopology.isPresent());
    }

    @Test(timeout = 10000)
    public void testOnObservedTopologyCreated() throws Exception {
        InstanceIdentifier<Topology> exampleIid = buildTopologyIid(EXAMPLE);
        TopologyId topologyId = new TopologyId(EXAMPLE);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);

        String nodeName1 = "node:1";
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId(nodeName1);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        List<Node> lNode = new ArrayList<Node>();
        lNode.add(nodeBuilder.build());

        tbuilder.setNode(lNode);
        final Topology wrTopology = tbuilder.build();

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, exampleIid, wrTopology, true);
        assertCommit(rwTx.submit());

        observer.onObservedTopologyCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, wrTopology);

        InstanceIdentifier<Topology> mlmtTopologyIid = buildTopologyIid(MLMT);
        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Node> optionalNode = rTx.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();

        if (optionalNode != null) {
            assertFalse("Operational mlmt:1 topology node ", optionalNode.isPresent());
        }
    }

    @Test(timeout = 10000)
    public void testOnMlmtTopologyCreated() throws Exception {
        InstanceIdentifier<Topology> mlmtTopologyIid = buildTopologyIid(MLMT);
        TopologyId topologyId = new TopologyId(MLMT);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);

        String nodeName1 = "node:1";
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId(nodeName1);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        List<Node> lNode = new ArrayList<Node>();
        lNode.add(nodeBuilder.build());

        tbuilder.setNode(lNode);
        final Topology wrTopology = tbuilder.build();

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid, wrTopology, true);
        assertCommit(rwTx.submit());

        observer.onMlmtTopologyCreated(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid, wrTopology);

        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Node> optionalNode = rTx.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();
        if (optionalNode != null && optionalNode.isPresent()) {
            assertTrue("Operational mlmt:1 topology node ", optionalNode.isPresent());
        }
    }

    @Test(timeout = 10000)
    public void testOnConsequentAction() throws Exception {
        InstanceIdentifier<Topology> exampleIid = buildTopologyIid(EXAMPLE);
        TopologyId topologyId = new TopologyId(EXAMPLE);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);
        final Topology wrTopology = tbuilder.build();

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, exampleIid, wrTopology, true);
        assertCommit(rwTx.submit());

        MlmtConsequentAction consequentAction = observer.getMlmtConsequentAction(exampleIid);
        assertEquals(consequentAction, MlmtConsequentAction.BUILD);
    }

    @After
    public void clear() {
        // NOOP
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }
}
