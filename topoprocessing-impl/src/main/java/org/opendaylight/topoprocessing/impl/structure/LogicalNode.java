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

/**
 * @author matus.marko
 */
public class LogicalNode {

    private ArrayList<PhysicalNode> physicalNodes = new ArrayList<>();

    public LogicalNode(ArrayList<PhysicalNode> physicalNodes) {
        this.physicalNodes = physicalNodes;
    }

    /**
     * Creates logical node
     * @param physicalNodes 
     */
    public LogicalNode(List<PhysicalNode> physicalNodes) {
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
        if (physicalNodes.contains(nodeToRemove)) {
            physicalNodes.remove(nodeToRemove);
        } else {
            throw new IllegalArgumentException("Node to remove not found in the list.");
        }
    }

    /**
     * Update physical node. Old node will be replaced by the new one.
     * @param oldNode 
     * @param newNode
     */
    public void updatePhysicalNode(PhysicalNode oldNode, PhysicalNode newNode) {
        if (physicalNodes.contains(oldNode)) {
            physicalNodes.remove(oldNode);
            physicalNodes.add(newNode);
        } else {
            throw new IllegalArgumentException("Node to replace not found in the list.");
        }
    }
    
    public ArrayList<PhysicalNode> getPhysicalNodes() {
        return physicalNodes;
    }

    public void setPhysicalNodes(ArrayList<PhysicalNode> physicalNodes) {
        this.physicalNodes = physicalNodes;
    }
}
