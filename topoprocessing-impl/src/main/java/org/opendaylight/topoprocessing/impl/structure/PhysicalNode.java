/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.topoprocessing.api.filtration.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
public class PhysicalNode implements UnderlayItem {

    private NormalizedNode<?, ?> node;
    private NormalizedNode<?, ?> leafNode;
    private LogicalNode logicalNode = null;
    private String topologyId;
    private String nodeId;

    /**
     * Constructor
     * @param node underlay topology {@link Node}
     * @param leafNode specified in target-field (in mapping)
     * @param topologyId identifier of {@link Topology}
     * @param nodeId identifier of {@link Node}
     */
    public PhysicalNode(NormalizedNode<?, ?> node, NormalizedNode<?, ?> leafNode,
            String topologyId, String nodeId) {
        this.node = node;
        this.leafNode = leafNode;
        this.topologyId = topologyId;
        this.nodeId = nodeId;
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
     * @return {@link LogicalNode} that wraps this {@link PhysicalNode}
     */
    public LogicalNode getLogicalNode() {
        return logicalNode;
    }

    /**
     * @param logicalNode {@link LogicalNode} that wraps this {@link PhysicalNode}
     */
    public void setLogicalNode(LogicalNode logicalNode) {
        this.logicalNode = logicalNode;
    }

    /**
     * @return node's Id
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return topology's Id
     */
    public String getTopologyId() {
        return topologyId;
    }
}
