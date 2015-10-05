/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.linkComputation.translator;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.LinkTranslator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.linkComputation.api.ComputedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matej.perina
 *
 */

public class LCLinkTranslator implements LinkTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(LCLinkTranslator.class);

    public static final QName LINK_SOURCE_NODE_QNAME = QName.create(Source.QNAME, "source-node");

    public static final QName LINK_SOURCE_TP_QNAME = QName.create(Source.QNAME, "source-tp");

    public static final QName LINK_DEST_NODE_QNAME = QName.create(Source.QNAME, "dest-node");

    public static final QName LINK_DEST_TP_QNAME = QName.create(Source.QNAME, "dest-tp");

    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        LOG.debug("Transforming OverlayItemWrapper containing Links to network-topology format");
        List<UnderlayItem> writtenLinks = new ArrayList<>();
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingLinks = ImmutableNodes.mapNodeBuilder(
                SupportingLink.QNAME);
        // iterate through overlay items containing lists
        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            // TODO - add source and destination translation
            // iterate through underlay items
            for (UnderlayItem underlayItem : overlayItem.getUnderlayItems()) {
                if (! writtenLinks.contains(underlayItem)) {
                    writtenLinks.add(underlayItem);
                    // prepare supporting nodes
                    supportingLinks.withChild(ImmutableNodes.mapEntryBuilder(SupportingLink.QNAME,
                            TopologyQNames.LINK_REF, underlayItem.getItemId()).build());
                }
            }
        }

        ComputedLink computedLink = (ComputedLink) wrapper.getOverlayItems().get(0).getUnderlayItems().get(0);
        ContainerNode sourceNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(Source.QNAME))
                .withChild(ImmutableNodes.leafNode(LINK_SOURCE_NODE_QNAME, computedLink.getSrcNode().getIdentifier()))
                .build();
        ContainerNode destNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(Destination.QNAME))
                .withChild(ImmutableNodes.leafNode(LINK_DEST_NODE_QNAME, computedLink.getDstNode().getIdentifier()))
                .build();

        return ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, wrapper.getId())
                .withChild(sourceNode)
                .withChild(destNode)
                .withChild(supportingLinks.build())
                .build();
    }


}
