/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.nt.translator;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.topoprocessing.api.structure.ComputedLink;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.LinkTranslator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matej.perina
 *
 */
public class NTLinkTranslator implements LinkTranslator{

    private static final Logger LOG = LoggerFactory.getLogger(NTLinkTranslator.class);

    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        LOG.debug("Transforming OverlayItemWrapper containing Links to network-topology format");
        DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> linkNode =
                ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, wrapper.getId());
        MapNode supportingLinks = buildSupportingLinks(wrapper);
        linkNode.withChild(supportingLinks);
        UnderlayItem link = wrapper.getOverlayItems().peek().getUnderlayItems().peek();
        if(link instanceof ComputedLink) {
            setSrcDestNode(link, linkNode);
        }

        return linkNode.build();
    }

    private void setSrcDestNode(UnderlayItem link,
            DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> linkNode) {
        ComputedLink computedLink = (ComputedLink) link;
        ContainerNode sourceNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(Source.QNAME))
                .withChild(ImmutableNodes.leafNode(TopologyQNames.LINK_SOURCE_NODE_QNAME,
                        NormalizedNodes.findNode(computedLink.getSrcNode(),
                                YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME)).get().getValue()))
                .build();
        ContainerNode destNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(Destination.QNAME))
                .withChild(ImmutableNodes.leafNode(TopologyQNames.LINK_DEST_NODE_QNAME,
                        NormalizedNodes.findNode(computedLink.getDstNode(),
                                YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME)).get().getValue()))
                .build();
        linkNode.withChild(sourceNode).withChild(destNode);
    }

    private MapNode buildSupportingLinks(OverlayItemWrapper wrapper) {
        List<UnderlayItem> writtenLinks = new ArrayList<>();
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingLinks = ImmutableNodes.mapNodeBuilder(
                SupportingLink.QNAME);
        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            // iterate through underlay items
            for (UnderlayItem underlayItem : overlayItem.getUnderlayItems()) {
                if (! writtenLinks.contains(underlayItem)) {
                    writtenLinks.add(underlayItem);
                    // prepare supporting nodes
                    StringBuilder linkRef = new StringBuilder();
                    linkRef.append("/").append(NetworkTopology.QNAME.getLocalName()).append("/")
                            .append(Topology.QNAME.getLocalName()).append("/")
                            .append(underlayItem.getTopologyId()).append("/")
                            .append(Link.QNAME.getLocalName()).append("/")
                            .append(underlayItem.getItemId());
                    supportingLinks.withChild(ImmutableNodes.mapEntryBuilder(SupportingLink.QNAME,
                            TopologyQNames.LINK_REF, linkRef.toString()).build());
                }
            }
        }
        return supportingLinks.build();
    }

}
