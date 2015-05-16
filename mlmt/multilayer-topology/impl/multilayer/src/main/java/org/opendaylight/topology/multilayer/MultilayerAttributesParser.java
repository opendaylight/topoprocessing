/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.multilayer;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjacencyAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.DirectionalityInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;

public interface MultilayerAttributesParser {

    String parseFaId(boolean bidirFlag, String topologyName);

    LinkId parseLinkId(FaId faId, boolean secondLeg);

    LinkId parseLinkId(String strFaId, boolean secondLeg);

    FaEndPoint parseHeadEnd(ForwardingAdjacencyAttributes input);

    FaEndPoint parseTailEnd(ForwardingAdjacencyAttributes input);

    NodeId parseNodeId(FaEndPoint faEndPoint);

    TpId parseTpId(FaEndPoint faEndPoint);

    List<Attribute> parseMtInfoAttribute(MtInfo mtInfo);

    List<TpId> parseSupportingTp(FaEndPoint faEndPoint);

    LinkBuilder parseLinkBuilder(ForwardingAdjacencyAttributes input, String faId);

    TerminationPointBuilder parseTerminationPointBuilder(FaEndPoint faEndPoint);

    LinkBuilder swapSourceDestination(LinkBuilder linkBuilder);

    DirectionalityInfo parseDirection(FaId faId);
}

