/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.api.structure;

import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
public class UnderlayItem {

    private NormalizedNode<?, ?> item;
    private Map<Integer, NormalizedNode<?, ?>> leafNodes;
    private OverlayItem overlayItem = null;
    private String topologyId;
    private String itemId;
    private CorrelationItemEnum correlationItem;

    /**
     * Constructor
     * @param item underlay topology {@link Node} or {@link Link} or {@link TerminationPoint}
     * @param leafNodes specified in target-field (in mapping)
     * @param topologyId identifier of {@link Topology}
     * @param itemId identifier of {@link Node} or {@link Link} or {@link TerminationPoint}
     * @param correlationItem can be either Node or Link or TerminationPoint
     */
    public UnderlayItem(NormalizedNode<?, ?> item, Map<Integer, NormalizedNode<?, ?>> leafNodes,
            String topologyId, String itemId, CorrelationItemEnum correlationItem) {
        this.item = item;
        this.leafNodes = leafNodes;
        this.topologyId = topologyId;
        this.itemId = itemId;
        this.correlationItem = correlationItem;
    }

    /**
     * @return underlay topology {@link Node}, {@link Link} or {@link TerminationPoint}
     */
    public NormalizedNode<?, ?> getItem() {
        return item;
    }

    /**
     * @param item - {@link Node}, {@link Link} or {@link TerminationPoint}
     */
    public void setItem(NormalizedNode<?, ?> item) {
        this.item = item;
    }

    /**
     * @param itemId
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    /**
     * @return node specified in target-field (in mapping)
     */
    public Map<Integer, NormalizedNode<?, ?>> getLeafNodes() {
        return leafNodes;
    }

    /**
     * @param leafNodes node specified in target-field (in mapping)
     */
    public void setLeafNodes(Map<Integer, NormalizedNode<?, ?>> leafNodes) {
        this.leafNodes = leafNodes;
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
