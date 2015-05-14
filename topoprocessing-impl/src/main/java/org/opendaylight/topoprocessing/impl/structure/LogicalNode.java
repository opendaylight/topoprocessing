/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 * @author martin.uhlir
 */
public class LogicalNode {

    private List<PhysicalNode> physicalNodes = new ArrayList<>();

    /**
     * Creates logical node
     * @param physicalNodes
     */
    public LogicalNode(List<PhysicalNode> physicalNodes) {
        Preconditions.checkNotNull(physicalNodes, "physicalNodes cannot be null");
        this.physicalNodes.addAll(physicalNodes);
    }

    /**
     * Adds a physical node to the list of nodes
     * @param physicalNode physical node to be added
     */
    public void addPhysicalNode(PhysicalNode physicalNode) {
        physicalNodes.add(physicalNode);
    }

    /**
     * Removes physical node from the list of nodes
     * @param nodeToRemove physical node to be removed
     */
    public void removePhysicalNode(PhysicalNode nodeToRemove) {
        boolean removed = physicalNodes.remove(nodeToRemove); 
        if (!removed) {
            throw new IllegalArgumentException("Node to remove not found in the list.");
        }
    }

    /**
     * Update physical node. Old node will be replaced by the new one.
     * @param oldNode 
     * @param newNode
     */
    public void updatePhysicalNode(PhysicalNode oldNode, PhysicalNode newNode) {
        boolean removed = physicalNodes.remove(oldNode); 
        if (removed) {
            physicalNodes.add(newNode);
        } else {
            throw new IllegalArgumentException("Node to replace not found in the list.");
        }
    }

    /**
     * @return {@link PhysicalNode}s (underlay nodes)
     */
    public List<PhysicalNode> getPhysicalNodes() {
        return physicalNodes;
    }

    /**
     * Sets {@link PhysicalNode}s
     * @param physicalNodes underlay nodes
     */
    public void setPhysicalNodes(List<PhysicalNode> physicalNodes) {
        Preconditions.checkNotNull(physicalNodes, "physicalNodes parameter cannot be null");
        this.physicalNodes = physicalNodes;
    }
}
