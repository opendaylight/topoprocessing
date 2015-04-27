/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
public class PhysicalNode {

    private NormalizedNode<?, ?> node;
    private NormalizedNode<?, ?> leafNode;
    private String topologyRef;
    private LogicalNode logicalNodeIdentifier = null;
    private String nodeId;

    /**
     * Constructor
     * @param node
     * @param leafNode
     * @param topologyRef 
     */
    public PhysicalNode(NormalizedNode<?, ?> node, NormalizedNode<?, ?> leafNode,
            String topologyRef) {
        this.node = node;
        this.leafNode = leafNode;
        this.topologyRef = topologyRef;
    }

    public NormalizedNode<?, ?> getNode() {
        return node;
    }

    public void setNode(NormalizedNode<?, ?> node) {
        this.node = node;
    }

    public NormalizedNode<?, ?> getLeafNode() {
        return leafNode;
    }

    public void setLeafNode(NormalizedNode<?, ?> leafNode) {
        this.leafNode = leafNode;
    }

    public LogicalNode getLogicalNodeIdentifier() {
        return logicalNodeIdentifier;
    }

    public void setLogicalNodeIdentifier(LogicalNode logicalNodeIdentifier) {
        this.logicalNodeIdentifier = logicalNodeIdentifier;
    }

    /**
     * @return {@link YangInstanceIdentifier} representing {@link Topology}
     * which the node is stored in
     */
    public YangInstanceIdentifier getTopologyRef() {
        return topologyRef;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
