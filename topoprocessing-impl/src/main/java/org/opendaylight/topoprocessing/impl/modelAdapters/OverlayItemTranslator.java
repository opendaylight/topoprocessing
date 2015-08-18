/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters;

import java.util.List;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.LinkTranslator;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/** *
 * @author matej.perina
 */

public abstract class OverlayItemTranslator {

    /**
     * Convert LogicalNode to Node
     * @param wrapper LogicalNodeWrapper object
     * @return Node
     */
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        NormalizedNode<?, ?> result = null;
        if (wrapper != null) {
            List<OverlayItem> overlayItems = wrapper.getOverlayItems();
            if (!overlayItems.isEmpty()) {
                CorrelationItemEnum correlationItem = overlayItems.get(0).getCorrelationItem();
                switch (correlationItem) {
                case Node:
                case TerminationPoint:
                    result = translateNode(wrapper);
                    break;
                case Link:
                    result = translateLink(wrapper);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Correlation item set: "
                            + correlationItem);
                }
            }
        }
        return result;
    }

    protected abstract NormalizedNode<?,?> translateNode(OverlayItemWrapper wrapper);

    protected abstract NormalizedNode<?,?> translateLink(OverlayItemWrapper wrapper);
}
