/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.translator;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.uhlir
 *
 */
public class LinkTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(LinkTranslator.class);

    /**
     * Converts OverlayItemWrapper object containing links to datastore link object
     * @param wrapper OverlayItemWrapper object containing link OverlayItems
     * @return {@link Link} in datastore format
     */
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

        return ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, wrapper.getId())
                .withChild(supportingLinks.build())
                .build();
    }

}
