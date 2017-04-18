/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjacencyAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.DirectionalityInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.Unidirectional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.NativeL3IgpMetric;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MultilayerAttributesParserImplTest {

    private MultilayerAttributesParserImpl parser;

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        this.parser = new MultilayerAttributesParserImpl();
        this.parser.init();
    }

    @Test
    public void parseHeadEndTest() {
        NodeId nodeId = new NodeId("node:1");
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setNode(nodeId);
        ForwardingAdjAnnounceInputBuilder builder = new ForwardingAdjAnnounceInputBuilder();
        builder.setHeadEnd(headEndBuilder.build());
        FaEndPoint faEndPoint = parser.parseHeadEnd(builder.build());
        assertNotNull(faEndPoint);
    }

    @Test
    public void parseTailEndTest() {
        NodeId nodeId = new NodeId("node:1");
        TailEndBuilder tailEndBuilder = new TailEndBuilder();
        tailEndBuilder.setNode(nodeId);
        ForwardingAdjAnnounceInputBuilder builder = new ForwardingAdjAnnounceInputBuilder();
        builder.setTailEnd(tailEndBuilder.build());
        ForwardingAdjacencyAttributes input;
        FaEndPoint faEndPoint = parser.parseTailEnd(builder.build());
        assertNotNull(faEndPoint);
    }

    @Test
    public void parseNodeIdTest() {
        String nodeName = "node:1";
        NodeId nodeId = new NodeId(nodeName);
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setNode(nodeId);
        nodeId = parser.parseNodeId(headEndBuilder.build());
        assertEquals(nodeId.getValue().toString(), nodeName);
        TailEndBuilder tailEndBuilder = new TailEndBuilder();
        tailEndBuilder.setNode(nodeId);
        nodeId = parser.parseNodeId(headEndBuilder.build());
        assertEquals(nodeId.getValue().toString(), nodeName);
    }


    @Test
    public void parseTpIdTest() {
        String tpName = "tp:1";
        TpId tpId = new TpId(tpName);
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setTpId(tpId);
        tpId = parser.parseTpId(headEndBuilder.build());
        assertEquals(tpId.getValue().toString(), tpName);
    }

    @Test
    public void parseMtInfoAttributeTest() {
        Long metric = 100L;
        String path = "native-l3-igp-metric:1";
        Uri uri = new Uri(path);
        AttributeKey attributeKey = new AttributeKey(uri);
        MtLinkMetricAttributeValueBuilder mtLinkMetricAVBuilder = new MtLinkMetricAttributeValueBuilder();
        mtLinkMetricAVBuilder.setMetric(metric);
        ValueBuilder valueBuilder = new ValueBuilder();
        valueBuilder.addAugmentation(MtLinkMetricAttributeValue.class, mtLinkMetricAVBuilder.build());
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setAttributeType(NativeL3IgpMetric.class);
        attributeBuilder.setValue(valueBuilder.build());
        attributeBuilder.setId(uri);
        attributeBuilder.setKey(attributeKey);
        MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
        final List<Attribute> la = new ArrayList<Attribute>();
        la.add(attributeBuilder.build());
        mtInfoLinkBuilder.setAttribute(la);
        List<Attribute> lMtInfo = parser.parseMtInfoAttribute(mtInfoLinkBuilder.build());
        assertNotNull(lMtInfo);
    }

    @Test
    public void parseSupportingTpTest() {
        List<TpId> lTpId = new ArrayList<>();
        TpId tpId = new TpId("tp:1");
        lTpId.add(tpId);
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setSupportingTp(lTpId);
        List<TpId> rxlTpId = parser.parseSupportingTp(headEndBuilder.build());
        assertNotNull(rxlTpId);
        TpId rxTpId = rxlTpId.get(0);
        assertNotNull(rxTpId);
        assertEquals(rxTpId.getValue().toString(), tpId.getValue().toString());
    }

    private String buildTopologyName(String topologyName) {
        TopologyId tid = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        InstanceIdentifier<Topology> instanceId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, key);
        NetworkTopologyRef ref = new NetworkTopologyRef(instanceId);
        InstanceIdentifier<?> iid = ref.getValue();
        TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
        return topologyKey.getTopologyId().getValue();
    }

    @Test
    public void parseFaIdTest() {
        String topologyName = buildTopologyName("mlmt:1");
        String parsedFaId = parser.parseFaId(false, topologyName);
        assertNotNull(parsedFaId);
        parsedFaId = parser.parseFaId(true, topologyName);
        assertNotNull(parsedFaId);
    }

    @Test
    public void parseDirectionTest() {
        String topologyName = buildTopologyName("mlmt:1");
        String parsedFaId = parser.parseFaId(false, topologyName);
        FaId faId = new FaId(parsedFaId);
        DirectionalityInfo directionalityInfo = parser.parseDirection(faId);
        boolean b = (directionalityInfo instanceof Unidirectional);
        assertTrue(b);
    }

    private LinkBuilder buildLinkBuilder(String node1, String tp1, String node2, String tp2) {
        NodeId nodeId = new NodeId(node1);
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setNode(nodeId);
        TpId tpId = new TpId(tp1);
        headEndBuilder.setTpId(tpId);
        TailEndBuilder tailEndBuilder = new TailEndBuilder();
        nodeId = new NodeId(node2);
        tailEndBuilder.setNode(nodeId);
        tpId = new TpId(tp2);
        tailEndBuilder.setTpId(tpId);
        ForwardingAdjAnnounceInputBuilder builder = new ForwardingAdjAnnounceInputBuilder();
        builder.setHeadEnd(headEndBuilder.build());
        builder.setTailEnd(tailEndBuilder.build());
        return parser.parseLinkBuilder(builder.build(), "fa");
    }

    @Test
    public void parseLinkBuilderTest() {
        assertNotNull(buildLinkBuilder("node1", "tp1", "node2", "tp2"));
    }

    @Test
    public void parseTerminationPointBuilderTest() {
        NodeId nodeId = new NodeId("node1");
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setNode(nodeId);
        TpId tpId = new TpId("tp1");
        headEndBuilder.setTpId(tpId);
        TerminationPointBuilder tpBuilder = parser.parseTerminationPointBuilder(headEndBuilder.build());
        assertNotNull(tpBuilder);
    }

    @Test
    public void swapSourceDestinationTest() {
        String node1 = "node:1";
        String node2 = "node:2";
        String tp1 = "tp:1";
        String tp2 = "tp:2";
        LinkBuilder linkBuilder = buildLinkBuilder(node1, tp1, node2, tp2);
        LinkBuilder swappedLinkBuilder = parser.swapSourceDestination(linkBuilder, true);
        assertEquals(swappedLinkBuilder.getSource().getSourceNode().getValue().toString(), node2);
        assertEquals(swappedLinkBuilder.getSource().getSourceTp().getValue().toString(), tp2);
        assertEquals(swappedLinkBuilder.getDestination().getDestNode().getValue().toString(), node1);
        assertEquals(swappedLinkBuilder.getDestination().getDestTp().getValue().toString(), tp1);
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
