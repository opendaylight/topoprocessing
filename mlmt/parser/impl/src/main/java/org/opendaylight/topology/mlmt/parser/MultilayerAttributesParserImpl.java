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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjacencyAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.Unidirectional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.Bidirectional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.DirectionalityInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoTerminationPointBuilder;
import org.slf4j.Logger;
import org.opendaylight.topology.multilayer.MultilayerAttributesParser;

public class MultilayerAttributesParserImpl implements MultilayerAttributesParser {

    private static Logger log;
    private static final String FA_ID_PREFIX = "fa/";
    private static final String FA_ID_UNIDIR = "unidir/";
    private static final String FA_ID_BIDIR = "bidir/";
    private static final String FA_SUBID_FIRSTLEG = "0";
    private static final String FA_SUBID_SECONDLEG = "1";
    private static final String FA_ID_ITEM_SEP = "/";
    private static final String FA_REG_UNI_MATCH = "^unidir\\" + "/(.*)\\" + "/(.*)";
    private static final String FA_REG_BID_MATCH = "^bidir\\" + "/(.*)\\" + "/(.*)";
    private static final String LINK_REG_UNI_MATCH = "(.*)\\" + "/(.*)";
    private static final String LINK_REG_BID_MATCH = "(.*)\\" + "/(.*)";
    private Bidirectional bidirectional;
    private Unidirectional unidirectional;

    public void init(final Logger logger) {
        log = logger;
        log.debug("FA_REG_BID_MATCH " + FA_REG_UNI_MATCH);
        log.debug("FA_REG_BID_MATCH " + FA_REG_BID_MATCH);
        log.debug("LINK_REG_UNI_MATCH " + LINK_REG_UNI_MATCH);
        log.debug("LINK_REG_BID_MATCH " + LINK_REG_BID_MATCH);

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.bidirectional.BidirectionalBuilder
            bidirectionalBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.bidirectional.BidirectionalBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.BidirectionalBuilder
            bidirectionalBuilder2 = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.BidirectionalBuilder();
        bidirectionalBuilder2.setBidirectional(bidirectionalBuilder.build());
        bidirectional = bidirectionalBuilder2.build();

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.unidirectional.UnidirectionalBuilder
            unidirectionalBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.unidirectional.UnidirectionalBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.UnidirectionalBuilder
            unidirectionalBuilder2 = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.UnidirectionalBuilder();
        unidirectionalBuilder2.setUnidirectional(unidirectionalBuilder.build());
        unidirectional = unidirectionalBuilder2.build();
    }

    @Override
    public FaEndPoint parseHeadEnd(ForwardingAdjacencyAttributes input) {
        return input.getHeadEnd();
    }

    @Override
    public FaEndPoint parseTailEnd(ForwardingAdjacencyAttributes input) {
        return input.getTailEnd();
    }

    @Override
    public NodeId parseNodeId(FaEndPoint faEndPoint) {
        return faEndPoint.getNode();
    }

    @Override
    public TpId parseTpId(FaEndPoint faEndPoint) {
        return faEndPoint.getTpId();
    }

    @Override
    public List<Attribute> parseMtInfoAttribute(MtInfo mtInfo) {
        return mtInfo.getAttribute();
    }

    @Override
    public List<TpId> parseSupportingTp(FaEndPoint faEndPoint) {
        return faEndPoint.getSupportingTp();
    }

    private String allocFaId() {
        return UUID.randomUUID().toString();
    }

    private String getLinkSubId(boolean secondLeg) {
        if (secondLeg) {
           return FA_SUBID_SECONDLEG;
        }

        return FA_SUBID_FIRSTLEG;
    }

    private String buildFaId(String strFaId, boolean bidirFlag, String networkTopologyRef) {
        String faId;
        if (bidirFlag) {
           faId = FA_ID_BIDIR;
        }
        else {
           faId = FA_ID_UNIDIR;
        }
        if (networkTopologyRef != null) {
            faId = faId + networkTopologyRef + FA_ID_ITEM_SEP;
        }
        faId = faId + strFaId;

        return faId;
    }

    private String buildFaId(int faIdValue, boolean bidirFlag, String networkTopologyRef) {
        return buildFaId(Integer.toString(faIdValue), bidirFlag, networkTopologyRef);
    }

    @Override
    public String parseFaId(boolean bidirFlag, String networkTopologyRef) {
        return buildFaId(allocFaId(), bidirFlag, networkTopologyRef);
    }

    private LinkId buildLinkId(String uuId, boolean secondLeg) {
		String strLinkId = uuId + FA_ID_ITEM_SEP + getLinkSubId(secondLeg);
        return new LinkId(strLinkId);
    }

    @Override
    public LinkId parseLinkId(String uuId, boolean secondLeg) {
        return buildLinkId(uuId, secondLeg);
    }

    @Override
    public LinkId parseLinkId(FaId faId, boolean secondLeg) {
		String uuId = extractUuIdFromFaId(faId.getValue().toString());
        return parseLinkId(uuId, secondLeg);
    }

    @Override
    public DirectionalityInfo parseDirection(FaId faId) {
        final String strFaId = faId.getValue();
        Pattern r = Pattern.compile(FA_REG_UNI_MATCH);
        Matcher m = r.matcher(strFaId);
        if (m.find()) {
            return unidirectional;
        }
        r = Pattern.compile(FA_REG_BID_MATCH);
        m = r.matcher(strFaId);
        if (m.find()) {
            return bidirectional;
        }

        return null;
    }

    String extractUuIdFromFaId(String strFaId) {
        Pattern r = Pattern.compile(FA_REG_UNI_MATCH);
        Matcher m = r.matcher(strFaId);
        if (m.find()) {
            return m.group(2);
        }
        r = Pattern.compile(FA_REG_BID_MATCH);
        m = r.matcher(strFaId);
        if (m.find()) {
            return m.group(2);
        }

        return null;
    }

    String extractUuIdFromLink(String strLinkId) {
        Pattern r = Pattern.compile(LINK_REG_UNI_MATCH);
        Matcher m = r.matcher(strLinkId);
        if (m.find()) {
            return m.group(1);
        }
        r = Pattern.compile(LINK_REG_BID_MATCH);
        m = r.matcher(strLinkId);
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    @Override
    public LinkBuilder parseLinkBuilder(ForwardingAdjacencyAttributes input, String faId) {
        final HeadEnd headEnd = input.getHeadEnd();
        final NodeId headNodeId = headEnd.getNode();
        final TpId headTpId = headEnd.getTpId();
        final TailEnd tailEnd = input.getTailEnd();
        final NodeId tailNodeId = tailEnd.getNode();
        final TpId tailTpId = tailEnd.getTpId();
        final SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(headNodeId).setSourceTp(headTpId);
        final DestinationBuilder destinationBuilder = new DestinationBuilder();
        destinationBuilder.setDestNode(tailNodeId).setDestTp(tailTpId);
        final LinkBuilder linkBuilder = new LinkBuilder();
        String uuId = extractUuIdFromFaId(faId);
        final LinkId linkId = buildLinkId(uuId, false);
        final LinkKey linkKey = new LinkKey(linkId);
        final MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
        mtInfoLinkBuilder.setAttribute(parseMtInfoAttribute(input));
        linkBuilder.addAugmentation(MtInfoLink.class, mtInfoLinkBuilder.build());
        linkBuilder.setSource(sourceBuilder.build()).setDestination(destinationBuilder.build())
                .setKey(linkKey).setLinkId(linkId)
                .setSupportingLink(Collections.<SupportingLink>emptyList());

        return linkBuilder;
     }

    @Override
    public TerminationPointBuilder parseTerminationPointBuilder(FaEndPoint faEndPoint) {
        final TpId tpId = parseTpId(faEndPoint);
        final List<TpId> lSupportingTp = parseSupportingTp(faEndPoint);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);
        tpBuilder.setTpId(tpId).setTpRef(lSupportingTp).setKey(tpKey);
        final MtInfoTerminationPointBuilder mtInfoTpBuilder = new MtInfoTerminationPointBuilder();
        final List<Attribute> tailEndAttribute = parseMtInfoAttribute(faEndPoint);
        mtInfoTpBuilder.setAttribute(tailEndAttribute);
        tpBuilder.addAugmentation(MtInfoTerminationPoint.class, mtInfoTpBuilder.build());

        return tpBuilder;
    }

    @Override
    public LinkBuilder swapSourceDestination(LinkBuilder linkBuilder) {
        Source source = linkBuilder.getSource();
        Destination destination = linkBuilder.getDestination();
        final SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(destination.getDestNode()).setSourceTp(destination.getDestTp());
        final DestinationBuilder destinationBuilder = new DestinationBuilder();
        destinationBuilder.setDestNode(source.getSourceNode()).setDestTp(source.getSourceTp());
        linkBuilder.setDestination(destinationBuilder.build());
        linkBuilder.setSource(sourceBuilder.build());
        String uuId = extractUuIdFromLink(linkBuilder.getLinkId().getValue().toString());
        LinkId linkId = buildLinkId(uuId, true);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);

        return linkBuilder;
    }
}
