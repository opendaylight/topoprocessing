/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.api.structure;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
public class UnderlayItem {

    private NormalizedNode<?, ?> node;
    private NormalizedNode<?, ?> leafNode;
    private OverlayItem overlayItem = null;
    private String topologyId;
    private String itemId;
    private CorrelationItemEnum correlationItem;

    /**
     * Constructor
     * @param node underlay topology {@link Node} or {@link Link} or {@link TerminationPoint}
     * @param leafNode specified in target-field (in mapping)
     * @param topologyId identifier of {@link Topology}
     * @param itemId identifier of {@link Node} or {@link Link} or {@link TerminationPoint}
     * @param correlationItem can be either Node or Link or TerminationPoint
     */
    public UnderlayItem(NormalizedNode<?, ?> node, NormalizedNode<?, ?> leafNode,
            String topologyId, String itemId, CorrelationItemEnum correlationItem) {
        this.node = node;
        this.leafNode = leafNode;
        this.topologyId = topologyId;
        this.itemId = itemId;
        this.correlationItem = correlationItem;
    }

    /**
     * @return underlay topology {@link Node}
     */
    public NormalizedNode<?, ?> getNode() {
        return node;
    }

    /**
     * @param node underlay topology {@link Node}
     */
    public void setNode(NormalizedNode<?, ?> node) {
        this.node = node;
    }

    /**
     * @return node specified in target-field (in mapping)
     */
    public NormalizedNode<?, ?> getLeafNode() {
        return leafNode;
    }

    /**
     * @param leafNode node specified in target-field (in mapping)
     */
    public void setLeafNode(NormalizedNode<?, ?> leafNode) {
        this.leafNode = leafNode;
    }

    /**
     * 
     * @return {@link OverlayItem} that wraps this {@link UnderlayItem}
     */
    public OverlayItem getOverlayItem() {
        return overlayItem;
    }

    /**
     * @param overlayItem {@link OverlayItem} that wraps this {@link UnderlayItem}
     */
    public void setOverlayItem(OverlayItem overlayItem) {
        this.overlayItem = overlayItem;
    }

    /**
     * @return item's Id
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @return topology's Id
     */
    public String getTopologyId() {
        return topologyId;
    }

    /**
     * @return correlation item: Node or Link or TerminationPoint
     */
    public CorrelationItemEnum getCorrelationItem() {
        return correlationItem;
    }
}
