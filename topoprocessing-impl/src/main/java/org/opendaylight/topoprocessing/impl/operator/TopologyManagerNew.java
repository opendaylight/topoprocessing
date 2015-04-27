/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.LogicalNodeWrapper;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;

/**
 * @author martin.uhlir
 *
 */
public class TopologyManagerNew {
    
    private IdentifierGenerator idGenerator;

    List<LogicalNodeWrapper> wrappers = new ArrayList<>();

    private void updateInWrapper(PhysicalNode updatedPhysicalNode) {
        for (LogicalNodeWrapper wrapper : wrappers) {
            for (LogicalNode logicalNode : wrapper.getLogicalNodes()) {
                for (PhysicalNode physicalNode : logicalNode.getPhysicalNodes()) {
                    if (updatedPhysicalNode.getTopologyRef().equals(physicalNode.getTopologyRef()) &&
                            updatedPhysicalNode.getNodeId().equals(physicalNode.getNodeId())) {
                        physicalNode = updatedPhysicalNode;
                    }
                }
            }
        }
    }
    

    /**
     * Adds new Logical node into
     * - existing Logical node wrapper
     * - new Logical node wrapper 
     * @param newLogicalNode - logical node which shall be put into wrapper
     */
    public void addLogicalNodeToWrapper(LogicalNode newLogicalNode) {
        if (newLogicalNode != null && newLogicalNode.getPhysicalNodes() != null) {
            for (PhysicalNode newPhysicalNode : newLogicalNode.getPhysicalNodes()) {
                for (LogicalNodeWrapper wrapper : wrappers) {
                    for (LogicalNode logicalNodeFromWrapper : wrapper.getLogicalNodes()) {
                        for (PhysicalNode physicalNode : logicalNodeFromWrapper.getPhysicalNodes()) {
                            if (physicalNode.getTopologyRef().equals(newPhysicalNode.getTopologyRef()) &&
                                    physicalNode.getNodeId().equals(newPhysicalNode.getNodeId())) {
                                wrapper.addLogicalNode(newLogicalNode);
                                return;
                            }
                        }
                    }
                }
            }
            //generate wrapper id
            String wrapperId = idGenerator.getNextIdentifier();
            //create new Logical node wrapper and add the logical node into it
            LogicalNodeWrapper newWrapper = new LogicalNodeWrapper(wrapperId, newLogicalNode);
            wrappers.add(newWrapper);
        }
    }

    /**
     * @param physicalNode
     */
    public void processUpdatedLogicalNode(PhysicalNode physicalNode) {
        updateInWrapper(physicalNode);
    }
    
}
