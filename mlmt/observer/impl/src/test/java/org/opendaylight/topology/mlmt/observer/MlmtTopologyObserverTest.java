/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.observer;

import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.junit.runner.RunWith;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;

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
        public <T extends RpcService> BindingAwareBroker.RpcRegistration<T> addRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(L listener) {
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

    private Topology buildUnderlayTopology(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);
        final Topology top = tbuilder.setServerProvided(Boolean.FALSE).build();

        return top;
    }

    private Topology buildMlmtTopology(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
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
        tbuilder.setKey(key);
        tbuilder.setTopologyId(topologyId);
        final Topology top = tbuilder.setServerProvided(Boolean.FALSE)
                .setTopologyTypes(topologyTypesBuilder.build())
                .setUnderlayTopology(lUnderlayTopology).build();

        return top;
    }

    private Topology buildTopology2(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);

        return tbuilder.setServerProvided(Boolean.FALSE).build();
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
        this.observer.init(dataBroker, rpcRegistry);

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
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, topologyIid, wrTopology, true);
        assertCommit(rwTx.submit());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, topologyIid).get();
        assertNotNull(optional);
        assertTrue("Configuration mlmt:1 topology ", optional.isPresent());

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        rTx = dataBroker.newReadOnlyTransaction();
        optional = rTx.read(LogicalDatastoreType.OPERATIONAL, topologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();

        assertEquals(rxTopology, wrTopology);
    }

    @Test(timeout = 10000)
    public void testOnMlmtTopologyCreated() throws Exception {
        InstanceIdentifier<Topology> topologyIid = buildTopologyIid(MLMT);
        final Topology wrTopology = buildMlmtTopology(MLMT);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, topologyIid, wrTopology, true);
        assertCommit(rwTx.submit());

        this.observer.onTopologyCreated(LogicalDatastoreType.OPERATIONAL, topologyIid, wrTopology);
        synchronized (waitObject) {
             waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, topologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();

        assertEquals(rxTopology, wrTopology);
    }

    @Test(timeout = 10000)
    public void testOnUnderlayTopologyCreated() throws Exception {
        InstanceIdentifier<Topology> mlmtTopologyIid = buildTopologyIid(MLMT);
        Topology wrTopology = buildMlmtTopology(MLMT);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid, wrTopology, true);
        assertCommit(rwTx.submit());

        InstanceIdentifier<Topology> exampleIid = buildTopologyIid(EXAMPLE);
        wrTopology = buildUnderlayTopology(EXAMPLE);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, exampleIid, wrTopology, true);
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        String nodeName1 = "node:1";
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId(nodeName1);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        Node wrNode = nodeBuilder.build();
        observer.onNodeCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, wrNode);

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        String tpName1 = "1:1";
        TpId tpId = new TpId(tpName1);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        TerminationPoint tp = tpBuilder.build();
        observer.onTpCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey, tp);

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        String nodeName2 = "node:2";
        nodeBuilder = new NodeBuilder();
        nodeId = new NodeId(nodeName2);
        nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        wrNode = nodeBuilder.build();
        observer.onNodeCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, wrNode);

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        tpBuilder = new TerminationPointBuilder();
        String tpName2 = "2:1";
        tpId = new TpId(tpName2);
        tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        tp = tpBuilder.build();
        observer.onTpCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey, tp);

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

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

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology node ", optional.isPresent());
        Topology rxTopology = optional.get();

        assertNotNull(rxTopology);

        List<Node> rxListNode = rxTopology.getNode();
        /* two nodes present */
        assertEquals(rxListNode.size(), 2);

        Node rxNode1 = rxListNode.get(0);
        assertNotNull(rxNode1);

        Node rxNode2 = rxListNode.get(1);
        assertNotNull(rxNode2);

        List<SupportingNode> rxlSuppNode1 = rxNode1.getSupportingNode();
        assertNotNull(rxlSuppNode1);

        String supportingNodeIdStr1 = rxlSuppNode1.get(0).getNodeRef().getValue().toString();
        List<SupportingNode> rxlSuppNode2 = rxNode2.getSupportingNode();
        assertNotNull(rxlSuppNode2);

        String supportingNodeIdStr2 = rxlSuppNode2.get(0).getNodeRef().getValue().toString();

        boolean b11 = supportingNodeIdStr1.equals(nodeName1);
        boolean b12 = supportingNodeIdStr1.equals(nodeName2);
        boolean b21 = supportingNodeIdStr2.equals(nodeName1);
        boolean b22 = supportingNodeIdStr2.equals(nodeName2);
        boolean c1 = b11 & b22;
        boolean c2 = b12 & b21;
        boolean c3 = (c1 & !c2) | (!c1 & c2);
        assertTrue(c3);

        List<TerminationPoint> rxlTp1 = rxNode1.getTerminationPoint();
        assertNotNull(rxlTp1);
        String tpRefStr1 = rxlTp1.get(0).getTpRef().get(0).getValue().toString();

        List<TerminationPoint> rxlTp2 = rxNode2.getTerminationPoint();
        assertNotNull(rxlTp2);
        String tpRefStr2 = rxlTp2.get(0).getTpRef().get(0).getValue().toString();

        b11 = tpRefStr1.equals(tpName1);
        b12 = tpRefStr1.equals(tpName2);
        b21 = tpRefStr2.equals(tpName1);
        b22 = tpRefStr2.equals(tpName2);
        c1 = b11 & b22;
        c2 = b12 & b21;
        c3 = (c1 & !c2) | (!c1 & c2);
        assertTrue(c3);

        List<Link> rxlLink = rxTopology.getLink();
        assertNotNull(rxlLink);
        String supportingLinkStr = rxlLink.get(0).getSupportingLink().get(0).getLinkRef().getValue().toString();

        assertEquals(supportingLinkStr, linkName1);
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