/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.parser;

import java.util.List;
import java.util.ArrayList;

import org.junit.runner.RunWith;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjacencyAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.Unidirectional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.Bidirectional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.DirectionalityInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.NativeL3IgpMetric;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class MultilayerAttributesParserImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(MultilayerAttributesParserImplTest.class);
    private MultilayerAttributesParserImpl parser;

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        this.parser = new MultilayerAttributesParserImpl();
        this.parser.init(LOG);
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
    public void parseTpId() {
        String tpName = "node:1";
        TpId tpId = new TpId(tpName);
        HeadEndBuilder headEndBuilder = new HeadEndBuilder();
        headEndBuilder.setTpId(tpId);
        tpId = parser.parseTpId(headEndBuilder.build());
        assertEquals(tpId.getValue().toString(), tpName);
    }

    @Test
    public void parseMtInfoAttribute() {
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

    @After
    public void clear() {
        // NOOP
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }
}
