/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.parser;

import org.opendaylight.topology.multitechnology.MultitechnologyAttributesParser;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.TedLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.TedNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultitechnologyAttributesParserImpl implements MultitechnologyAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(MultitechnologyAttributesParserImpl.class);

    private IgpNodeAttributes getIgpNodeAttributes(final Node node) {

        final Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 == null) {
            return null;
        }

        return node1.getIgpNodeAttributes();
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributes
            getIsIsNodeAttributes(IgpNodeAttributes igpNodeAttributes) {

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1
                igpNodeAttributes1 = igpNodeAttributes.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1.class);
        if (igpNodeAttributes1 == null) {
            return null;
        }

        return igpNodeAttributes1.getIsisNodeAttributes();
    }

    @Override
    public TedNodeAttributes parseTedNodeAttributes(final Node node) {

        IgpNodeAttributes igpNodeAttributes = getIgpNodeAttributes(node);
        if (igpNodeAttributes == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributes
                isisNodeAttributes = getIsIsNodeAttributes(igpNodeAttributes);
        if (isisNodeAttributes == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.Ted
        ted = isisNodeAttributes.getTed();

        return ted;
    }

    @Override
    public TedLinkAttributes parseTedLinkAttributes(final Link link) {

        final Link1 link1 = link.getAugmentation(Link1.class);
        if (link1 == null) {
            LOG.debug("MultitechnologyAttributesParserImpl.parseTedLinkAttributes link1 is null");
            return null;
        }
        final IgpLinkAttributes igpLinkAttributes = link1.getIgpLinkAttributes();
        if (igpLinkAttributes == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1
                 igpLinkAttributes1 = igpLinkAttributes.getAugmentation(
                         org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1.class);
        if (igpLinkAttributes1 == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributes
                isisLinkAttributes = igpLinkAttributes1.getIsisLinkAttributes();
        if (isisLinkAttributes == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.Ted
        ted = isisLinkAttributes.getTed();

        return ted;
    }

    @Override
    public Long parseLinkMetric (final Link link) {

        final Link1 link1 = link.getAugmentation(Link1.class);
        if (link1 == null) {
            LOG.debug("MultitechnologyAttributesParserImpl.parseLinkMetric link1 is null");
            return null;
        }
        final IgpLinkAttributes igpLinkAttributes = link1.getIgpLinkAttributes();
        if (igpLinkAttributes == null) {
            return null;
        }

        return igpLinkAttributes.getMetric();
    }
}
