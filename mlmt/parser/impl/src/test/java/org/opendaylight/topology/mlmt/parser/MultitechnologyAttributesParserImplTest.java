/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.parser;

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

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.TedNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.TedLinkAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class MultitechnologyAttributesParserImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(MultitechnologyAttributesParserImplTest.class);
    private MultitechnologyAttributesParserImpl parser;
    private static final String topologyName = "example:1";
    private static final String nodeName = "example-node:1";

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        this.parser = new MultitechnologyAttributesParserImpl();
        this.parser.init(LOG);
        NodeId nodeId = new NodeId(nodeName);
        NodeKey nodeKey = new NodeKey(nodeId);
    }

    @Test
    public void parseTedNodeAttributesTest() {
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
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
        TedNodeAttributes tedNodeAttributes = parser.parseTedNodeAttributes(nodeBuilder.build());
        assertEquals(tedNodeAttributes.getTeRouterIdIpv4().getValue(), ipv4Address.getValue());
    }

    @Test
    public void parseTedLinkAttributesTest() {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder
                tedBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder();
        Long color = (Long)10L;
        tedBuilder.setColor(color);
        IsisLinkAttributesBuilder isisLinkAttributedBuilder = new IsisLinkAttributesBuilder();
        isisLinkAttributedBuilder.setTed(tedBuilder.build());
        IgpLinkAttributes1Builder igpLinkAttributes1Builder = new IgpLinkAttributes1Builder();
        igpLinkAttributes1Builder.setIsisLinkAttributes(isisLinkAttributedBuilder.build());
        IgpLinkAttributesBuilder igpLinkAttributesBuilder = new IgpLinkAttributesBuilder();
        igpLinkAttributesBuilder.addAugmentation(IgpLinkAttributes1.class, igpLinkAttributes1Builder.build());
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setIgpLinkAttributes(igpLinkAttributesBuilder.build());
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.addAugmentation(Link1.class, link1Builder.build());
        TedLinkAttributes tedLinkAttributes = parser.parseTedLinkAttributes(linkBuilder.build());
        assertEquals(tedLinkAttributes.getColor(), color);
   }

    @Test
    public void parseLinkMetricTest() {
        Long metric = (Long)150L;
        IgpLinkAttributesBuilder igpLinkAttributesBuilder = new IgpLinkAttributesBuilder();
        igpLinkAttributesBuilder.setMetric(metric);
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setIgpLinkAttributes(igpLinkAttributesBuilder.build());
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.addAugmentation(Link1.class, link1Builder.build());
        Long parsedMetric = parser.parseLinkMetric(linkBuilder.build());
        assertEquals(parsedMetric, metric);
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
