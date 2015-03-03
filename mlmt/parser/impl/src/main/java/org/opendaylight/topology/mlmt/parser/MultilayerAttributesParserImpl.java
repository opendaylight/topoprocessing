/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.parser;

import java.util.Collections;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInput;

import org.slf4j.Logger;
import org.opendaylight.topology.multilayer.MultilayerAttributesParser;

public class MultilayerAttributesParserImpl implements MultilayerAttributesParser {

    private static Logger log;

    public void init(final Logger logger) {
        log = logger;
    }

    @Override
    public NodeId parseNodeId(ForwardingAdjAnnounceInput input) {

        return input.getHeadEnd().getNode();
    }

    @Override
    public LinkBuilder parseLinkBuilder(ForwardingAdjAnnounceInput input) {
        final HeadEnd headEnd = input.getHeadEnd();
        final NodeId headNodeId = headEnd.getNode();
        final TpId headTpId = headEnd.getTpId();
        final TailEnd tailEnd = input.getTailEnd();
        final NodeId tailNodeId = tailEnd.getNode();
        final TpId tailTpId = tailEnd.getTpId();
        final SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(headNodeId).setSourceTp(headTpId);
        final DestinationBuilder destinationBuilder = new DestinationBuilder();
        destinationBuilder.setDestNode(headNodeId).setDestTp(tailTpId);
        final LinkBuilder linkBuilder = new LinkBuilder();
        final LinkId linkId = new LinkId(headNodeId.toString());
        final LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setSource(sourceBuilder.build()).setDestination(destinationBuilder.build())
                .setKey(linkKey).setLinkId(linkId)
                .setSupportingLink(Collections.<SupportingLink>emptyList());

        return linkBuilder;
     }

    @Override
    public TerminationPointBuilder parseTerminationPointBuilder(TpId tpId, List<TpId> lSupportingTp) {

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setTpId(tpId);
        tpBuilder.setTpRef(lSupportingTp);
        tpBuilder.setKey(new TerminationPointKey(tpId));

        return tpBuilder;
    }
}
