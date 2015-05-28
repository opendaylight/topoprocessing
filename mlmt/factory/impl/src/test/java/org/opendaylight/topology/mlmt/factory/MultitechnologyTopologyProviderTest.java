/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.multitechnology;

import com.google.common.base.Optional;

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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.Value;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedNodeAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedLinkAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.parser.MultitechnologyAttributesParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class MultitechnologyTopologyProviderTest extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(MultitechnologyTopologyProviderTest.class);
    private final Object waitObject = new Object();
    private static final String MLMT = "mlmt:1";
    private static final String EXAMPLE = "example-linkstate-topology";
    private MultitechnologyTopologyProvider provider;
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
        MultitechnologyAttributesParserImpl parser = new MultitechnologyAttributesParserImpl();
        parser.init(LOG);
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();
        mlmtTopologyIid = buildTopologyIid(MLMT);
        this.provider = new MultitechnologyTopologyProvider();
        provider.init(LOG, processor, mlmtTopologyIid, parser);
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
    public void onTopologyCreatedTest() throws Exception {
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
    }

    @Test(timeout = 10000)
    public void onNodeCreatedTest() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, nodeIid, nodeBuilder.build());
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        TedBuilder tedBuilder = new TedBuilder();
        Ipv4Address ipv4Address = new Ipv4Address("10.0.0.1");
        tedBuilder.setTeRouterIdIpv4(ipv4Address);
        IsisNodeAttributesBuilder isisNodeAttributesBuilder = new IsisNodeAttributesBuilder();
        isisNodeAttributesBuilder.setTed(tedBuilder.build());
        IgpNodeAttributes1Builder igpNodeAttributes1Builder = new IgpNodeAttributes1Builder();
        igpNodeAttributes1Builder.setIsisNodeAttributes(isisNodeAttributesBuilder.build());
        IgpNodeAttributesBuilder igpNodeAttributesBuilder = new IgpNodeAttributesBuilder();
        igpNodeAttributesBuilder.addAugmentation(IgpNodeAttributes1.class, igpNodeAttributes1Builder.build());
        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setIgpNodeAttributes(igpNodeAttributesBuilder.build());
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());

        provider.onNodeCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, nodeBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        final String path = "native-ted:1";
        final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
        final Uri uri = new Uri(path);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final InstanceIdentifier<Attribute> instanceAttributeId = mlmtTopologyIid.child(Node.class, nodeKey).
                augmentation(MtInfoNode.class).child(Attribute.class, attributeKey);
        final Optional<Attribute> sourceAttributeObject =
                rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

        assertNotNull(sourceAttributeObject);
        assertTrue(sourceAttributeObject.isPresent());
        Attribute attribute = sourceAttributeObject.get();
        assertNotNull(attribute);
        Value value = attribute.getValue();
        assertNotNull(value);
        MtTedNodeAttributeValue mtTedNodeAttributeValue = value.getAugmentation(MtTedNodeAttributeValue.class);
        assertNotNull(mtTedNodeAttributeValue);
        assertEquals(mtTedNodeAttributeValue.getTeRouterIdIpv4().getValue(), ipv4Address.getValue());
    }

    @Test(timeout = 10000)
    public void onTpCreatedTest() throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, nodeIid, nodeBuilder.build());
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        final InstanceIdentifier<TerminationPoint> instanceId = mlmtTopologyIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, instanceId, tpBuilder.build());
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        provider.onTpCreated(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid,
                nodeKey, tpBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
        final Optional<TerminationPoint> sourceAttributeObject =
                rx.read(LogicalDatastoreType.OPERATIONAL, instanceId).get();
        assertNotNull(sourceAttributeObject);
        assertTrue(sourceAttributeObject.isPresent());
    }

    @Test(timeout = 10000)
    public void onLinkCreatedTest() throws Exception {
        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        InstanceIdentifier<Link> linkIid = mlmtTopologyIid.child(Link.class, linkKey);

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, linkIid, linkBuilder.build());
        assertCommit(rwTx.submit());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        java.lang.Long teDefaultMetric = 20L;
        java.lang.Long metric = 100L;
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder
                tedBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder();
        tedBuilder.setTeDefaultMetric(teDefaultMetric);
        IsisLinkAttributesBuilder isisLinkAttributesBuilder = new IsisLinkAttributesBuilder();
        isisLinkAttributesBuilder.setTed(tedBuilder.build());
        IgpLinkAttributes1Builder igpLinkAttributes1Builder = new IgpLinkAttributes1Builder();
        igpLinkAttributes1Builder.setIsisLinkAttributes(isisLinkAttributesBuilder.build());
        IgpLinkAttributesBuilder igpLinkAttributesBuilder = new IgpLinkAttributesBuilder();
        igpLinkAttributesBuilder.addAugmentation(IgpLinkAttributes1.class, igpLinkAttributes1Builder.build());
        igpLinkAttributesBuilder.setMetric(metric);
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setIgpLinkAttributes(igpLinkAttributesBuilder.build());
        linkBuilder.addAugmentation(Link1.class, link1Builder.build());

        provider.onLinkCreated(LogicalDatastoreType.OPERATIONAL, exampleIid, linkBuilder.build());

        synchronized (waitObject) {
            waitObject.wait(1500);
        }

        String path = "native-ted:1";
        ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
        Uri uri = new Uri(path);
        AttributeKey attributeKey = new AttributeKey(uri);
        InstanceIdentifier<Attribute> instanceAttributeId = mlmtTopologyIid.child(Link.class, linkKey)
                .augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);
        Optional<Attribute> sourceAttributeObject =
                rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

        assertNotNull(sourceAttributeObject);
        assertTrue(sourceAttributeObject.isPresent());
        Attribute attribute = sourceAttributeObject.get();
        assertNotNull(attribute);
        Value value = attribute.getValue();
        assertNotNull(value);
        MtTedLinkAttributeValue mtTedLinkAttributeValue = value.getAugmentation(MtTedLinkAttributeValue.class);
        assertNotNull(mtTedLinkAttributeValue);
        assertEquals(mtTedLinkAttributeValue.getTeDefaultMetric(), teDefaultMetric);

        path = "native-l3-igp-metric:1";
        rx = dataBroker.newReadOnlyTransaction();
        uri = new Uri(path);
        attributeKey = new AttributeKey(uri);
        instanceAttributeId = mlmtTopologyIid.child(Link.class, linkKey)
                .augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);
        sourceAttributeObject = rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

        assertNotNull(sourceAttributeObject);
        assertTrue(sourceAttributeObject.isPresent());
        attribute = sourceAttributeObject.get();
        assertNotNull(attribute);
        value = attribute.getValue();
        assertNotNull(value);
        MtLinkMetricAttributeValue mtLinkMetricAttributeValue = value.getAugmentation(MtLinkMetricAttributeValue.class);
        assertNotNull(mtLinkMetricAttributeValue);
        assertEquals(mtLinkMetricAttributeValue.getMetric(), metric);
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