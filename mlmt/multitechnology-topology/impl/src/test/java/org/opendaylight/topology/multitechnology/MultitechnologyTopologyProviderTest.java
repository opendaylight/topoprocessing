/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multitechnology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.math.BigDecimal;
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
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.multitechnology.MultitechnologyTopologyProviderTest.ChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.Value;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedLinkAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedNodeAttributeValue;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.PccCapabilities;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.Srlg;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.SrlgBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.node.attributes.Ipv4LocalAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.node.attributes.Ipv4LocalAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.node.attributes.Ipv4LocalAddressKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.node.attributes.Ipv6LocalAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.node.attributes.Ipv6LocalAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.node.attributes.Ipv6LocalAddressKey;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class MultitechnologyTopologyProviderTest extends AbstractDataBrokerTest {
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
        MultitechnologyAttributesParserTest parser = new MultitechnologyAttributesParserTest();
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();
        mlmtTopologyIid = buildTopologyIid(MLMT);
        this.provider = new MultitechnologyTopologyProvider();
        provider.init(processor, mlmtTopologyIid, parser);
        provider.setDataProvider(dataBroker);
        /*
         * It is necessary to create the network-topology containers in
         * both configuration and operational data storage
         */
        NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
        NetworkTopology networkTopology = nb.build();
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class), networkTopology);
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

    private void handleTopologyTest(LogicalDatastoreType storageType, boolean update) throws Exception {
        if (update) {
            provider.onTopologyCreated(storageType, exampleIid, exampleTopology);
            provider.onTopologyUpdated(storageType, exampleIid, exampleTopology);
        } else {
            provider.onTopologyCreated(storageType, exampleIid, exampleTopology);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(storageType, mlmtTopologyIid).get();
        if (optional != null && optional.isPresent() && optional.get() != null) {
            Topology rxTopology = optional.get();
            assertNotNull(rxTopology);

            TopologyTypes topologyTypes = rxTopology.getTopologyTypes();
            if (topologyTypes != null) {
                MtTopologyType mtTopologyType = topologyTypes.getAugmentation(MtTopologyType.class);
                if (mtTopologyType != null) {
                    assertNotNull(mtTopologyType.getMultitechnologyTopology());
                }
            }
        }
    }

    @Test(timeout = 10000)
    public void onTopologyCreatedTest() throws Exception {
        handleTopologyTest(LogicalDatastoreType.OPERATIONAL, false);
        handleTopologyTest(LogicalDatastoreType.CONFIGURATION, false);
    }

    @Test(timeout = 10000)
    public void onTopologyUpdatedTest() throws Exception {
        handleTopologyTest(LogicalDatastoreType.OPERATIONAL, true);
        handleTopologyTest(LogicalDatastoreType.CONFIGURATION, true);
    }

    private void handleNodeTest(LogicalDatastoreType storageType, boolean update) throws Exception {
        TedBuilder tedBuilder = new TedBuilder();

        Ipv4Address ipv4Address = new Ipv4Address("10.0.0.1");
        tedBuilder.setTeRouterIdIpv4(ipv4Address);

        Ipv6Address ipv6Address = new Ipv6Address("2041:0000:130F:0000:0000:07C0:853A:140B");
        tedBuilder.setTeRouterIdIpv6(ipv6Address);

        Ipv4Prefix ipv4Prefix = new Ipv4Prefix("10.2.1.0/24");
        Ipv4LocalAddressBuilder ipv4LocalAddressBuilder = new Ipv4LocalAddressBuilder();
        Ipv4LocalAddressKey ipv4LocalAddressKey = new Ipv4LocalAddressKey(ipv4Prefix);
        ipv4LocalAddressBuilder.setKey(ipv4LocalAddressKey);
        ipv4LocalAddressBuilder.setIpv4Prefix(ipv4Prefix);
        List<Ipv4LocalAddress> lIpv4LocalAddress = new ArrayList<Ipv4LocalAddress>();
        lIpv4LocalAddress.add(ipv4LocalAddressBuilder.build());
        tedBuilder.setIpv4LocalAddress(lIpv4LocalAddress);

        Ipv6Prefix ipv6Prefix = new Ipv6Prefix("2001:db8::/32");
        Ipv6LocalAddressBuilder ipv6LocalAddressBuilder = new Ipv6LocalAddressBuilder();
        Ipv6LocalAddressKey ipv6LocalAddressKey = new Ipv6LocalAddressKey(ipv6Prefix);
        ipv6LocalAddressBuilder.setKey(ipv6LocalAddressKey);
        ipv6LocalAddressBuilder.setIpv6Prefix(ipv6Prefix);
        List<Ipv6LocalAddress> lIpv6LocalAddress = new ArrayList<Ipv6LocalAddress>();
        lIpv6LocalAddress.add(ipv6LocalAddressBuilder.build());
        tedBuilder.setIpv6LocalAddress(lIpv6LocalAddress);

        PccCapabilities pccCapabilities = new PccCapabilities(true, true, true, true, true, true, true, true, true);
        tedBuilder.setPccCapabilities(pccCapabilities);

        IsisNodeAttributesBuilder isisNodeAttributesBuilder = new IsisNodeAttributesBuilder();
        isisNodeAttributesBuilder.setTed(tedBuilder.build());
        IgpNodeAttributes1Builder igpNodeAttributes1Builder = new IgpNodeAttributes1Builder();
        igpNodeAttributes1Builder.setIsisNodeAttributes(isisNodeAttributesBuilder.build());
        IgpNodeAttributesBuilder igpNodeAttributesBuilder = new IgpNodeAttributesBuilder();
        igpNodeAttributesBuilder.addAugmentation(IgpNodeAttributes1.class, igpNodeAttributes1Builder.build());
        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setIgpNodeAttributes(igpNodeAttributesBuilder.build());

        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());

        if (update) {
            provider.onNodeCreated(storageType, exampleIid, nodeBuilder.build());
        } else {
            provider.onNodeUpdated(storageType, exampleIid, nodeBuilder.build());
        }

        final String path = "native-ted:1";
        final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
        final Uri uri = new Uri(path);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final InstanceIdentifier<Attribute> instanceAttributeId = mlmtTopologyIid.child(Node.class, nodeKey)
                .augmentation(MtInfoNode.class).child(Attribute.class, attributeKey);
        final Optional<Attribute> sourceAttributeObject = rx.read(storageType, instanceAttributeId).get();

        if (storageType == LogicalDatastoreType.OPERATIONAL) {
            if (sourceAttributeObject != null && sourceAttributeObject.isPresent()) {
                Attribute attribute = sourceAttributeObject.get();
                assertNotNull(attribute);
                Value value = attribute.getValue();
                assertNotNull(value);
                MtTedNodeAttributeValue mtTedNodeAttributeValue =
                        value.getAugmentation(MtTedNodeAttributeValue.class);
                assertNotNull(mtTedNodeAttributeValue);
                assertEquals(mtTedNodeAttributeValue.getTeRouterIdIpv4().getValue(), ipv4Address.getValue());
                assertEquals(mtTedNodeAttributeValue.getTeRouterIdIpv6().getValue(), ipv6Address.getValue());

                List<Ipv4LocalAddress> rxIpv4LocalAddress = mtTedNodeAttributeValue.getIpv4LocalAddress();
                assertNotNull(rxIpv4LocalAddress);
                assertFalse(rxIpv4LocalAddress.isEmpty());
                assertEquals(rxIpv4LocalAddress.get(0).getIpv4Prefix(), ipv4Prefix);

                List<Ipv6LocalAddress> rxIpv6LocalAddress = mtTedNodeAttributeValue.getIpv6LocalAddress();
                assertNotNull(rxIpv6LocalAddress);
                assertFalse(rxIpv6LocalAddress.isEmpty());
                assertEquals(rxIpv6LocalAddress.get(0).getIpv6Prefix(), ipv6Prefix);

                assertEquals(mtTedNodeAttributeValue.getPccCapabilities(), pccCapabilities);
            }
        } else {
            if (sourceAttributeObject != null) {
                assertFalse(sourceAttributeObject.isPresent());
            }
        }
    }

    @Test(timeout = 10000)
    public void onNodeCreatedTest() throws Exception {
        handleNodeTest(LogicalDatastoreType.OPERATIONAL, false);
        handleNodeTest(LogicalDatastoreType.CONFIGURATION, false);
    }

    @Test(timeout = 10000)
    public void onNodeUpdatedTest() throws Exception {
        handleNodeTest(LogicalDatastoreType.OPERATIONAL, true);
        handleNodeTest(LogicalDatastoreType.CONFIGURATION, true);
    }

    private void handleTpTest(LogicalDatastoreType storageType, boolean update) throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);

        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);

        if (update) {
            provider.onTpUpdated(storageType, mlmtTopologyIid,
                    nodeKey, tpBuilder.build());
        } else {
            provider.onTpCreated(storageType, mlmtTopologyIid,
                    nodeKey, tpBuilder.build());
        }

        final InstanceIdentifier<TerminationPoint> instanceId = mlmtTopologyIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
        final Optional<TerminationPoint> sourceAttributeObject =
                rx.read(storageType, instanceId).get();
        assertNotNull(sourceAttributeObject);
        assertTrue(sourceAttributeObject.isPresent());
    }

    @Test(timeout = 10000)
    public void onTpCreatedTest() throws Exception {
        handleTpTest(LogicalDatastoreType.OPERATIONAL, false);
    }

    @Test(timeout = 10000)
    public void onTpUpdatedTest() throws Exception {
        handleTpTest(LogicalDatastoreType.OPERATIONAL, true);
    }

    private void handleLinkTest(LogicalDatastoreType storageType, boolean update) throws Exception {
        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        InstanceIdentifier<Link> linkIid = mlmtTopologyIid.child(Link.class, linkKey);

        java.lang.Long teDefaultMetric = 20L;
        java.lang.Long metric = 100L;
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link
                .attributes.isis.link.attributes.TedBuilder tedBuilder = new org.opendaylight.yang.gen.v1.urn
                .tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes
                .TedBuilder();
        tedBuilder.setTeDefaultMetric(teDefaultMetric);
        java.lang.Long color = 2L;
        tedBuilder.setColor(color);
        BigDecimal maxLinkBandwidth = new BigDecimal(1250000);
        tedBuilder.setMaxLinkBandwidth(maxLinkBandwidth);
        BigDecimal maxResvLinkBandwidth = new BigDecimal(1000000);
        tedBuilder.setMaxResvLinkBandwidth(maxResvLinkBandwidth);

        UnreservedBandwidthBuilder unreservedBandwidthBuilder = new UnreservedBandwidthBuilder();
        int unreservedBandwidthValue = 31250;
        List<UnreservedBandwidth> lUnreservedBandwidth = new ArrayList<UnreservedBandwidth>();
        for (java.lang.Short priority=0; priority <=7; priority++) {
            UnreservedBandwidthKey unreservedBandwidthKey = new UnreservedBandwidthKey(priority);
            unreservedBandwidthBuilder.setBandwidth(new BigDecimal(unreservedBandwidthValue));
            lUnreservedBandwidth.add(unreservedBandwidthBuilder.build());
        }
        tedBuilder.setUnreservedBandwidth(lUnreservedBandwidth);

        Integer protectionType = 1;
        SrlgBuilder srlgBuilder = new SrlgBuilder();
        srlgBuilder.setLinkProtectionType(protectionType);
        tedBuilder.setSrlg(srlgBuilder.build());

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

        if (update) {
            provider.onLinkUpdated(storageType, exampleIid, linkBuilder.build());
        } else {
            provider.onLinkCreated(storageType, exampleIid, linkBuilder.build());
        }

        String path = "native-ted:1";
        ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
        Uri uri = new Uri(path);
        AttributeKey attributeKey = new AttributeKey(uri);
        InstanceIdentifier<Attribute> instanceAttributeId = mlmtTopologyIid.child(Link.class, linkKey)
                .augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);
        Optional<Attribute> sourceAttributeObject =
                rx.read(storageType, instanceAttributeId).get();

        if (storageType == LogicalDatastoreType.OPERATIONAL) {
            if (sourceAttributeObject != null && sourceAttributeObject.isPresent()) {
                Attribute attribute = sourceAttributeObject.get();
                assertNotNull(attribute);
                Value value = attribute.getValue();
                assertNotNull(value);
                MtTedLinkAttributeValue mtTedLinkAttributeValue =
                        value.getAugmentation(MtTedLinkAttributeValue.class);
                assertNotNull(mtTedLinkAttributeValue);
                assertEquals(mtTedLinkAttributeValue.getTeDefaultMetric(), teDefaultMetric);
                assertEquals(mtTedLinkAttributeValue.getColor(), color);
                assertEquals(mtTedLinkAttributeValue.getMaxLinkBandwidth(), maxLinkBandwidth);
                assertEquals(mtTedLinkAttributeValue.getMaxResvLinkBandwidth(), maxResvLinkBandwidth);
                List<UnreservedBandwidth> rxList = mtTedLinkAttributeValue.getUnreservedBandwidth();
                assertNotNull(rxList);
                assertFalse(rxList.isEmpty());
                Srlg rxSrlg = mtTedLinkAttributeValue.getSrlg();
                assertNotNull(rxSrlg);
                assertEquals(rxSrlg.getLinkProtectionType(),protectionType);

                path = "native-l3-igp-metric:1";
                rx = dataBroker.newReadOnlyTransaction();
                uri = new Uri(path);
                attributeKey = new AttributeKey(uri);
                instanceAttributeId = mlmtTopologyIid.child(Link.class, linkKey)
                        .augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);
                sourceAttributeObject = rx.read(storageType, instanceAttributeId).get();

                assertNotNull(sourceAttributeObject);
                assertTrue(sourceAttributeObject.isPresent());
                attribute = sourceAttributeObject.get();
                assertNotNull(attribute);
                value = attribute.getValue();
                assertNotNull(value);
                MtLinkMetricAttributeValue mtLinkMetricAttributeValue =
                        value.getAugmentation(MtLinkMetricAttributeValue.class);
                assertNotNull(mtLinkMetricAttributeValue);
                assertEquals(mtLinkMetricAttributeValue.getMetric(), metric);
            }
        } else {
            if (sourceAttributeObject != null) {
                assertFalse(sourceAttributeObject.isPresent());
            }
        }
    }

    @Test(timeout = 10000)
    public void onLinkCreatedTest() throws Exception {
        handleLinkTest(LogicalDatastoreType.OPERATIONAL, false);
        handleLinkTest(LogicalDatastoreType.CONFIGURATION, false);
    }

    @Test(timeout = 10000)
    public void onLinkUpdatedTest() throws Exception {
        handleLinkTest(LogicalDatastoreType.OPERATIONAL, true);
        handleLinkTest(LogicalDatastoreType.CONFIGURATION, true);
    }

    private void handleTopologyDeleted(LogicalDatastoreType storageType) throws Exception {
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.merge(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, mlmtTopology, true);
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.delete(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid);
        assertCommit(rwTx.submit());

        provider.onTopologyDeleted(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertFalse("Operational mlmt:1 topology ", optional.isPresent());
    }

    @Test(timeout = 10000)
    public void onTopologyDeleted() throws Exception {
        handleTopologyDeleted(LogicalDatastoreType.OPERATIONAL);
        handleTopologyDeleted(LogicalDatastoreType.CONFIGURATION);
    }

    private void handleNodeDeleted(LogicalDatastoreType storageType) throws Exception {
        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);

        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(storageType, nodeIid, nodeBuilder.build());
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.delete(storageType, nodeIid);
        assertCommit(rwTx.submit());

        provider.onNodeDeleted(storageType, mlmtTopologyIid, nodeKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Node> optional = rTx.read(storageType, nodeIid).get();
        assertNotNull(optional);
        assertFalse("Operational mlmt:1 topology ", optional.isPresent());
    }

    @Test(timeout = 10000)
    public void onNodeDeleted() throws Exception {
        handleNodeDeleted(LogicalDatastoreType.OPERATIONAL);
        handleNodeDeleted(LogicalDatastoreType.CONFIGURATION);
    }

    private void handleTpDeleted(LogicalDatastoreType storageType) throws Exception {
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

        TpId tpId = new TpId("tp:1");
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        final InstanceIdentifier<TerminationPoint> tpIid = mlmtTopologyIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, tpIid, tpBuilder.build());
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.delete(LogicalDatastoreType.OPERATIONAL, tpIid);
        assertCommit(rwTx.submit());

        provider.onTpDeleted(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, nodeKey, tpKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<TerminationPoint> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, tpIid).get();
        assertNotNull(optional);
        assertFalse("Operational mlmt:1 topology ", optional.isPresent());
    }

    @Test(timeout = 10000)
    public void onTpDeleted() throws Exception {
        handleTpDeleted(LogicalDatastoreType.OPERATIONAL);
        handleTpDeleted(LogicalDatastoreType.CONFIGURATION);
    }

    private void handleLinkDeleted(LogicalDatastoreType storageType) throws Exception {
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

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.delete(LogicalDatastoreType.OPERATIONAL, linkIid);
        assertCommit(rwTx.submit());

        provider.onLinkDeleted(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, linkKey);

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Link> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, linkIid).get();
        assertNotNull(optional);
        assertFalse("Operational mlmt:1 topology ", optional.isPresent());
    }

    @Test(timeout = 10000)
    public void onLinkDeleted() throws Exception {
        handleLinkDeleted(LogicalDatastoreType.OPERATIONAL);
        handleLinkDeleted(LogicalDatastoreType.CONFIGURATION);
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
