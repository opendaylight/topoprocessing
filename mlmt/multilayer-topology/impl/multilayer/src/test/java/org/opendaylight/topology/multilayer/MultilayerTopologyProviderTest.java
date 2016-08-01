/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multilayer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

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
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.end.point.StitchingPointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.bidirectional.BidirectionalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.unidirectional.UnidirectionalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.announce.output.result.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.AnnouncementContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.NativeL3IgpMetric;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class MultilayerTopologyProviderTest extends AbstractDataBrokerTest {
    private final Object waitObject = new Object();
    private static final String MLMT = "mlmt:1";
    private static final String EXAMPLE = "example-linkstate-topology";
    private MultilayerTopologyProvider provider;
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

    public class MlmtRpcProviderRegistryMock implements RpcProviderRegistry {

        public MlmtRpcProviderRegistryMock() { }

        @Override
        public <T extends RpcService> BindingAwareBroker.RpcRegistration<T>
                addRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T>
                addRoutedRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
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
        MultilayerAttributesParserTest parser = new MultilayerAttributesParserTest();
        parser.init();
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();
        mlmtTopologyIid = buildTopologyIid(MLMT);
        this.provider = new MultilayerTopologyProvider();

        ForwardingAdjacencyTopologyTest forwardingAdjacencyTopologyTest = new ForwardingAdjacencyTopologyTest();
        forwardingAdjacencyTopologyTest.init(processor, mlmtTopologyIid);
        forwardingAdjacencyTopologyTest.setDataProvider(dataBroker);

        provider.init(processor, mlmtTopologyIid, parser, forwardingAdjacencyTopologyTest);
        provider.setDataProvider(dataBroker);
        MlmtRpcProviderRegistryMock rpcRegistry = new MlmtRpcProviderRegistryMock();
        provider.registerRpcImpl(rpcRegistry, mlmtTopologyIid);
        /*
         * It is necessary to create the network-topology containers in
         * both configuration and operational data storage
         */
        NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
        NetworkTopology networkTopology = nb.build();
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class),
                networkTopology);
        assertCommit(rwTx.submit());

        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);

        mlmtTopology = buildMlmtTopology(MLMT);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, mlmtTopology, true);
        assertCommit(rwTx.submit());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);

        exampleIid = buildTopologyIid(EXAMPLE);
        exampleTopology = buildUnderlayTopology(EXAMPLE);
    }

    @Test(timeout = 10000)
    public void testOnTopologyCreated() throws Exception {
        provider.onTopologyCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, exampleTopology);

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);

        TopologyTypes topologyTypes = rxTopology.getTopologyTypes();
        MtTopologyType mtTopologyType = topologyTypes.getAugmentation(MtTopologyType.class);
        assertNotNull(mtTopologyType);

        MlTopologyType mlTopologyTypes = mtTopologyType.getMultitechnologyTopology().getAugmentation(
                MlTopologyType.class);
        assertNotNull(mlTopologyTypes);
    }

    @Test(timeout = 10000)
    public void testOnTopologyUpdated() throws Exception {
        provider.onTopologyUpdated(LogicalDatastoreType.OPERATIONAL, exampleIid, exampleTopology);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnTopologyDeleted() throws Exception {
        provider.onTopologyDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnNodeCreated() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        InstanceIdentifier<Node> nodeIid = exampleIid.child(Node.class, nodeKey);

        provider.onNodeCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeBuilder.build());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnNodeUpdated() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        InstanceIdentifier<Node> nodeIid = exampleIid.child(Node.class, nodeKey);

        provider.onNodeUpdated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeBuilder.build());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnNodeDeleted() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> nodeIid = exampleIid.child(Node.class, nodeKey);

        provider.onNodeDeleted(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnTpCreated() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        final InstanceIdentifier<TerminationPoint> instanceId = exampleIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        provider.onTpCreated(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid,
                nodeKey, tpBuilder.build());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnTpUpdated() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        final InstanceIdentifier<TerminationPoint> instanceId = exampleIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        provider.onTpUpdated(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid,
                nodeKey, tpBuilder.build());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnTpDeleted() throws Exception {
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        NodeKey nodeKey = new NodeKey(nodeId);
        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        final InstanceIdentifier<TerminationPoint> instanceId = exampleIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        provider.onTpDeleted(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid,
                nodeKey, tpKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    private void createMlmtNodeAndTp(String nodeName, String tpName1, String tpName2) throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId(nodeName);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId);
        Node wrNode = nodeBuilder.build();
        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, nodeIid, wrNode);
        assertCommit(rwTx.submit());

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        TpId tpId = new TpId(tpName1);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        TerminationPoint tp = tpBuilder.build();
        InstanceIdentifier<TerminationPoint> tpIid = nodeIid.child(TerminationPoint.class, tpKey);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, tpIid, tp);
        assertCommit(rwTx.submit());

        tpId = new TpId(tpName2);
        tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        tp = tpBuilder.build();
        tpIid = nodeIid.child(TerminationPoint.class, tpKey);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, tpIid, tp);
        assertCommit(rwTx.submit());
    }

    private void createMlmtLink(String linkName, String node1, String tp1,
            String node2, String tp2) throws Exception {
        LinkBuilder linkBuilder = new LinkBuilder();
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(new NodeId(node1));
        sourceBuilder.setSourceTp(new TpId(tp1));
        DestinationBuilder destinationBuiler = new DestinationBuilder();
        destinationBuiler.setDestNode(new NodeId(node2));
        destinationBuiler.setDestTp(new TpId(tp2));
        linkBuilder.setSource(sourceBuilder.build());
        linkBuilder.setDestination(destinationBuiler.build());
        InstanceIdentifier<Link> linkIid = mlmtTopologyIid.child(Link.class, linkKey);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, linkIid, linkBuilder.build());
        assertCommit(rwTx.submit());
    }

    private void prepareTopology() throws Exception {
        InstanceIdentifier<Topology> topologyIid = buildTopologyIid(MLMT);
        final Topology wrTopology = buildMlmtTopology(MLMT);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, topologyIid, wrTopology, true);
        assertCommit(rwTx.submit());

        createMlmtNodeAndTp("node:1", "1:1", "1:2");
        createMlmtNodeAndTp("node:2", "2:1", "2:2");
        createMlmtNodeAndTp("node:3", "3:1", "3:2");
        createMlmtNodeAndTp("node:4", "4:1", "4:2");
        createMlmtLink("link:12", "node:1", "1:2", "node:3", "3:2");
        createMlmtLink("link:24", "node:2", "2:2", "node:4", "4:2");
    }

    @Test(timeout = 10000)
    public void testOnLinkCreated() throws Exception {
        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        InstanceIdentifier<Link> linkIid = exampleIid.child(Link.class, linkKey);

        provider.onLinkCreated(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, linkBuilder.build());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnLinkUpdated() throws Exception {
        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        InstanceIdentifier<Link> linkIid = exampleIid.child(Link.class, linkKey);

        provider.onLinkUpdated(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, linkBuilder.build());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnLinkDeleted() throws Exception {
        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        LinkKey linkKey = new LinkKey(linkId);
        InstanceIdentifier<Link> linkIid = exampleIid.child(Link.class, linkKey);

        provider.onLinkDeleted(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, linkKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    private void executeTestOnFa(boolean bidirectional, boolean stitch) throws Exception {
        InstanceIdentifier<Topology> topologyIid = buildTopologyIid(MLMT);
        NodeId nodeId1 = new NodeId("node:1");
        TpId tpId1 = new TpId("1:1");
        NodeId nodeId2 = new NodeId("node:2");
        TpId tpId2 = new TpId("2:1");

        /*
         * Forwarding Adjacency announce
         */
        ForwardingAdjAnnounceInputBuilder forwardingAdjAnnounceInputBuilder =
                new ForwardingAdjAnnounceInputBuilder();
        AnnouncementContextBuilder announcementContextBuilder = new AnnouncementContextBuilder();
        announcementContextBuilder.setId(new Uri("test"));
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setNode(nodeId1);
        headEndBuilder.setTpId(tpId1);

        StitchingPointBuilder stitchingPointBuilder = new StitchingPointBuilder();
        if (stitch) {
            stitchingPointBuilder.setTpId(new TpId("1:2"));
            headEndBuilder.setStitchingPoint(stitchingPointBuilder.build());
        } else {
            headEndBuilder.setStitchingPoint(null);
        }

        List<TpId> lTpId = new ArrayList<TpId>();
        TpId supportingTpId = new TpId("supporting:1");
        lTpId.add(supportingTpId);
        headEndBuilder.setSupportingTp(lTpId);

        TailEndBuilder tailEndBuilder = new TailEndBuilder();
        tailEndBuilder.setNode(nodeId2);
        tailEndBuilder.setTpId(tpId2);

        if (stitch) {
            stitchingPointBuilder.setTpId(new TpId("2:2"));
            tailEndBuilder.setStitchingPoint(stitchingPointBuilder.build());
        } else {
            tailEndBuilder.setStitchingPoint(null);
        }

        lTpId = new ArrayList<TpId>();
        supportingTpId = new TpId("supporting:2");
        lTpId.add(supportingTpId);
        tailEndBuilder.setSupportingTp(lTpId);

        Long metric = 10L;
        MtLinkMetricAttributeValueBuilder mtLinkMetricAVBuilder = new MtLinkMetricAttributeValueBuilder();
        mtLinkMetricAVBuilder.setMetric(metric);
        ValueBuilder valueBuilder = new ValueBuilder();
        valueBuilder.addAugmentation(MtLinkMetricAttributeValue.class, mtLinkMetricAVBuilder.build());
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setAttributeType(NativeL3IgpMetric.class);
        attributeBuilder.setValue(valueBuilder.build());
        final Uri uri = new Uri("test:1");
        attributeBuilder.setId(uri);
        AttributeKey attributeKey = new AttributeKey(uri);
        attributeBuilder.setKey(attributeKey);

        List<Attribute> lAttribute = new ArrayList<Attribute>();
        lAttribute.add(attributeBuilder.build());

        NetworkTopologyRef networkTopologyRef = new NetworkTopologyRef(topologyIid);
        forwardingAdjAnnounceInputBuilder.setAnnouncementContext(announcementContextBuilder.build());
        forwardingAdjAnnounceInputBuilder.setAttribute(lAttribute);

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.UnidirectionalBuilder
                unidirBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.UnidirectionalBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.BidirectionalBuilder
                bidirBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.BidirectionalBuilder();

        if (bidirectional) {
            BidirectionalBuilder bidirectionalBuilder = new BidirectionalBuilder();
            bidirBuilder.setBidirectional(bidirectionalBuilder.build());
            forwardingAdjAnnounceInputBuilder.setDirectionalityInfo(bidirBuilder.build());
        } else {
            UnidirectionalBuilder unidirectionalBuilder = new UnidirectionalBuilder();
            unidirBuilder.setUnidirectional(unidirectionalBuilder.build());
            forwardingAdjAnnounceInputBuilder.setDirectionalityInfo(unidirBuilder.build());
        }

        forwardingAdjAnnounceInputBuilder.setHeadEnd(headEndBuilder.build());
        forwardingAdjAnnounceInputBuilder.setTailEnd(tailEndBuilder.build());
        forwardingAdjAnnounceInputBuilder.setNetworkTopologyRef(networkTopologyRef);
        forwardingAdjAnnounceInputBuilder.setOperStatus(FaOperStatus.Up);

        Future<RpcResult<ForwardingAdjAnnounceOutput>> futureAnnounce =
                provider.forwardingAdjAnnounce(forwardingAdjAnnounceInputBuilder.build());
        RpcResult<ForwardingAdjAnnounceOutput> output = futureAnnounce.get();
        assertNotNull(output);
        ForwardingAdjAnnounceOutput resultAnnounceOutput = output.getResult();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123
                .forwarding.adj.announce.output.Result resultOutput = resultAnnounceOutput.getResult();
        assertTrue(resultOutput instanceof FaId);

        forwardingAdjAnnounceInputBuilder.setOperStatus(FaOperStatus.Up);
        futureAnnounce = provider.forwardingAdjAnnounce(forwardingAdjAnnounceInputBuilder.build());
        output = futureAnnounce.get();
        assertNotNull(output);
        resultAnnounceOutput = output.getResult();
        resultOutput = resultAnnounceOutput.getResult();
        assertTrue(resultOutput instanceof FaId);

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId faId =
                ((FaId)resultOutput).getFaId();

        /*
         * Forwarding Adjacency update
         */
        ForwardingAdjUpdateInputBuilder forwardingAdjUpdateInputBuilder = new ForwardingAdjUpdateInputBuilder();
        forwardingAdjUpdateInputBuilder.setFaId(faId);
        forwardingAdjUpdateInputBuilder.setNetworkTopologyRef(networkTopologyRef);
        forwardingAdjUpdateInputBuilder.setAnnouncementContext(announcementContextBuilder.build());
        forwardingAdjUpdateInputBuilder.setHeadEnd(headEndBuilder.build());
        forwardingAdjUpdateInputBuilder.setTailEnd(tailEndBuilder.build());

        if (bidirectional) {
            forwardingAdjUpdateInputBuilder.setDirectionalityInfo(bidirBuilder.build());
        } else {
            forwardingAdjUpdateInputBuilder.setDirectionalityInfo(unidirBuilder.build());
        }

        metric = 20L;
        mtLinkMetricAVBuilder = new MtLinkMetricAttributeValueBuilder();
        mtLinkMetricAVBuilder.setMetric(metric);
        valueBuilder = new ValueBuilder();
        valueBuilder.addAugmentation(MtLinkMetricAttributeValue.class, mtLinkMetricAVBuilder.build());
        attributeBuilder = new AttributeBuilder();
        attributeBuilder.setAttributeType(NativeL3IgpMetric.class);
        attributeBuilder.setValue(valueBuilder.build());
        attributeBuilder.setId(uri);
        attributeKey = new AttributeKey(uri);
        attributeBuilder.setKey(attributeKey);
        forwardingAdjUpdateInputBuilder.setAttribute(lAttribute);
        forwardingAdjUpdateInputBuilder.setOperStatus(FaOperStatus.Up);

        Future<RpcResult<ForwardingAdjUpdateOutput>> futureUpdate =
                provider.forwardingAdjUpdate(forwardingAdjUpdateInputBuilder.build());
        RpcResult<ForwardingAdjUpdateOutput> update = futureUpdate.get();
        assertNotNull(update);
        ForwardingAdjUpdateOutput resultUpdateOutput = update.getResult();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update
                .output.Result resultUpdate = resultUpdateOutput.getResult();
        assertNotNull(resultUpdate);
        assertTrue(resultUpdate instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer
                .rev150123.forwarding.adj.update.output.result.Ok);

         /*
          * Forwarding Adjacency withdraw
         */
        ForwardingAdjWithdrawInputBuilder forwardingAdjWithdrawInputBuilder =
                new ForwardingAdjWithdrawInputBuilder();
        forwardingAdjWithdrawInputBuilder.setFaId(faId);
        forwardingAdjWithdrawInputBuilder.setNetworkTopologyRef(networkTopologyRef);

        Future<RpcResult<ForwardingAdjWithdrawOutput>> futureWithdraw =
               provider.forwardingAdjWithdraw(forwardingAdjWithdrawInputBuilder.build());
        RpcResult<ForwardingAdjWithdrawOutput> withdraw = futureWithdraw.get();
        assertNotNull(withdraw);
        ForwardingAdjWithdrawOutput resultAdjWithdraw = withdraw.getResult();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw
                .output.Result resultWithdraw = resultAdjWithdraw.getResult();
        assertTrue(resultWithdraw instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer
                .rev150123.forwarding.adj.withdraw.output.result.Ok);
    }

    @Test(timeout = 10000)
    public void testOnForwardingAdjacency() throws Exception {
        prepareTopology();
        executeTestOnFa(false, false);
        executeTestOnFa(true, false);
        executeTestOnFa(false, true);
        executeTestOnFa(true, true);
    }

    @Test
    public void testOnClose()  throws Exception {
        provider.close();
        assertNotNull(provider);
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
