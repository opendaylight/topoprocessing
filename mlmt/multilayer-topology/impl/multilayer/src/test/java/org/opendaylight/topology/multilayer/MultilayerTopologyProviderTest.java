/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multilayer;

import com.google.common.base.Optional;

import java.util.concurrent.Future;
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

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyType;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.multilayer.MultilayerTopologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.NativeL3IgpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.end.point.StitchingPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.AnnouncementContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.unidirectional.UnidirectionalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.announce.output.result.FaId;

import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class MultilayerTopologyProviderTest extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(MultilayerTopologyProviderTest.class);
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
        final Topology top = tbuilder.setServerProvided(Boolean.FALSE)
                .setTopologyTypes(topologyTypesBuilder.build())
                .setUnderlayTopology(lUnderlayTopology).build();

        return top;
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
        parser.init(LOG);
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();
        mlmtTopologyIid = buildTopologyIid(MLMT);
        this.provider = new MultilayerTopologyProvider();
        provider.init(LOG, processor, mlmtTopologyIid, parser, null);
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
        rwTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);

        mlmtTopology = buildMlmtTopology(MLMT);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, mlmtTopology, true);
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

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

        MlTopologyType mlTopologyTypes = mtTopologyType.getMultitechnologyTopology().getAugmentation(MlTopologyType.class);
        assertNotNull(mlTopologyTypes);
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

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

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

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
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

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        Topology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testOnFaAnnounce() throws Exception {
        InstanceIdentifier<Topology> topologyIid = buildTopologyIid(MLMT);
        final Topology wrTopology = buildMlmtTopology(MLMT);
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, topologyIid, wrTopology, true);
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        String nodeName1 = "node:1";
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId1 = new NodeId(nodeName1);
        NodeKey nodeKey = new NodeKey(nodeId1);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId1);
        Node wrNode = nodeBuilder.build();
        InstanceIdentifier<Node> nodeIid = topologyIid.child(Node.class, nodeKey);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, nodeIid, wrNode);
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        String tpName1 = "1:1";
        TpId tpId1 = new TpId(tpName1);
        TerminationPointKey tpKey = new TerminationPointKey(tpId1);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId1);
        TerminationPoint tp = tpBuilder.build();
        InstanceIdentifier<TerminationPoint> tpIid = nodeIid.child(TerminationPoint.class, tpKey);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, tpIid, tp);
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        String nodeName2 = "node:2";
        nodeBuilder = new NodeBuilder();
        NodeId nodeId2 = new NodeId(nodeName2);
        nodeKey = new NodeKey(nodeId2);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.setNodeId(nodeId2);
        wrNode = nodeBuilder.build();
        nodeIid = topologyIid.child(Node.class, nodeKey);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, nodeIid, wrNode);
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        tpBuilder = new TerminationPointBuilder();
        String tpName2 = "2:1";
        TpId tpId2 = new TpId(tpName2);
        tpKey = new TerminationPointKey(tpId2);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId2);
        tp = tpBuilder.build();
        tpIid = nodeIid.child(TerminationPoint.class, tpKey);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, tpIid, tp);
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
             waitObject.wait(1500);
        }

        ForwardingAdjAnnounceInputBuilder forwardingAdjAnnounceInputBuilder = new ForwardingAdjAnnounceInputBuilder();
        AnnouncementContextBuilder announcementContextBuilder = new AnnouncementContextBuilder();
        announcementContextBuilder.setId(new Uri("test"));
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setNode(nodeId1);
        headEndBuilder.setTpId(tpId1);
        List<TpId> lTpId = new ArrayList<TpId>();
        TpId supportingTpId = new TpId("supporting:1");
        lTpId.add(supportingTpId);
        headEndBuilder.setSupportingTp(lTpId);

        TailEndBuilder tailEndBuilder = new TailEndBuilder();
        tailEndBuilder.setNode(nodeId2);
        tailEndBuilder.setTpId(tpId2);
        lTpId = new ArrayList<TpId>();
        supportingTpId = new TpId("supporting:2");
        lTpId.add(supportingTpId);
        tailEndBuilder.setSupportingTp(lTpId);

        Long metric = 10L;
        final MtLinkMetricAttributeValueBuilder mtLinkMetricAVBuilder = new MtLinkMetricAttributeValueBuilder();
        mtLinkMetricAVBuilder.setMetric(metric);
        final ValueBuilder valueBuilder = new ValueBuilder();
        valueBuilder.addAugmentation(MtLinkMetricAttributeValue.class, mtLinkMetricAVBuilder.build());
        final AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setAttributeType(NativeL3IgpMetric.class);
        attributeBuilder.setValue(valueBuilder.build());
        final Uri uri = new Uri("test:1");
        attributeBuilder.setId(uri);
        final AttributeKey attributeKey = new AttributeKey(uri);
        attributeBuilder.setKey(attributeKey);

        List<Attribute> lAttribute = new ArrayList<Attribute>();
        lAttribute.add(attributeBuilder.build());

        NetworkTopologyRef networkTopologyRef = new NetworkTopologyRef(topologyIid);
        forwardingAdjAnnounceInputBuilder.setAnnouncementContext(announcementContextBuilder.build());
        forwardingAdjAnnounceInputBuilder.setAttribute(lAttribute);
        UnidirectionalBuilder unidirectionalBuilder = new UnidirectionalBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.UnidirectionalBuilder
                unidirBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.UnidirectionalBuilder();
        unidirBuilder.setUnidirectional(unidirectionalBuilder.build());
        forwardingAdjAnnounceInputBuilder.setDirectionalityInfo(unidirBuilder.build());
        forwardingAdjAnnounceInputBuilder.setHeadEnd(headEndBuilder.build());
        forwardingAdjAnnounceInputBuilder.setTailEnd(tailEndBuilder.build());
        forwardingAdjAnnounceInputBuilder.setNetworkTopologyRef(networkTopologyRef);

        Future<RpcResult<ForwardingAdjAnnounceOutput>> futureAnnounce =
                provider.forwardingAdjAnnounce(forwardingAdjAnnounceInputBuilder.build());
        RpcResult<ForwardingAdjAnnounceOutput> output = futureAnnounce.get();
        assertNotNull(output);
        ForwardingAdjAnnounceOutput resultAnnounceOutput = output.getResult();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.announce.output.Result
                resultOutput = resultAnnounceOutput.getResult();
        boolean b = resultOutput instanceof FaId;
        assertTrue(b);

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId faId = ((FaId)resultOutput).getFaId();

        ForwardingAdjWithdrawInputBuilder forwardingAdjWithdrawInputBuilder = new ForwardingAdjWithdrawInputBuilder();
        forwardingAdjWithdrawInputBuilder.setFaId(faId);
        forwardingAdjWithdrawInputBuilder.setNetworkTopologyRef(networkTopologyRef);

        Future<RpcResult<ForwardingAdjWithdrawOutput>> futureWithdraw =
               provider.forwardingAdjWithdraw(forwardingAdjWithdrawInputBuilder.build());
        RpcResult<ForwardingAdjWithdrawOutput> withdraw = futureWithdraw.get();
        assertNotNull(withdraw);
        ForwardingAdjWithdrawOutput resultAdjWithdraw = withdraw.getResult();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.Result
                resultWithdraw = resultAdjWithdraw.getResult();
        b = resultWithdraw instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.Ok;
        assertTrue(b);
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
