/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import java.util.Queue;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
public class OverlayItemTranslator {

    NodeTranslator nodeTranslator;
    LinkTranslator linkTranslator;

    public OverlayItemTranslator(NodeTranslator nodeTranslator, LinkTranslator linkTranslator) {
        this.nodeTranslator = nodeTranslator;
        this.linkTranslator = linkTranslator;
    }
    /**
     * Convert LogicalNode to Node.
     * @param wrapper LogicalNodeWrapper object
     * @return Node
     */
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        NormalizedNode<?, ?> result = null;
        if (wrapper != null) {
            Queue<OverlayItem> overlayItems = wrapper.getOverlayItems();
            if (!overlayItems.isEmpty()) {
                CorrelationItemEnum correlationItem = overlayItems.peek().getCorrelationItem();
                switch (correlationItem) {
                    case Node:
                    case TerminationPoint:
                        result = nodeTranslator.translate(wrapper);
                        break;
                    case Link:
                        result = linkTranslator.translate(wrapper);
                        break;
                    default:
                        throw new IllegalArgumentException("Wrong Correlation item set: "
                                + correlationItem);
                }
            }
        }
        return result;
    }
}
