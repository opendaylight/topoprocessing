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
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;

/**
 * @author martin.uhlir
 *
 */
public class TopologyManager {
    
    private IdentifierGenerator idGenerator = new IdentifierGenerator();

    List<LogicalNodeWrapper> wrappers = new ArrayList<>();
    TopologyWriter writer = new TopologyWriter();

    /** for testing purpose only */
    public List<LogicalNodeWrapper> getWrappers() {
        return wrappers;
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
                            if (physicalNode.getNodeIdentifier().equals(newPhysicalNode.getNodeIdentifier())
//                                    && physicalNode.getNodeId().equals(newPhysicalNode.getNodeId())
                                    ) {
                                wrapper.addLogicalNode(newLogicalNode);
                                writer.writeNode(wrapper);
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
            writer.writeNode(newWrapper);
        }
    }

//    /**
//     * @param physicalNode
//     */
//    public void processUpdatedLogicalNode2(PhysicalNode updatedPhysicalNode) {
//        for (LogicalNodeWrapper wrapper : wrappers) {
//            for (LogicalNode logicalNode : wrapper.getLogicalNodes()) {
//                for (PhysicalNode physicalNode : logicalNode.getPhysicalNodes()) {
//                    if (updatedPhysicalNode.getTopologyRef().equals(physicalNode.getTopologyRef())
////                            && updatedPhysicalNode.getNodeId().equals(physicalNode.getNodeId())
//                            ) {
//                        physicalNode = updatedPhysicalNode;
//                    }
//                }
//            }
//        }
//    }

    /**
     * @param logicalIdentifier
     */
    public void updateLogicalNode(LogicalNode logicalIdentifier) {
        for (LogicalNodeWrapper wrapper : wrappers) {
            for (LogicalNode logicalNode : wrapper.getLogicalNodes()) {
                if (logicalNode.equals(logicalIdentifier)) {
                    writer.writeNode(wrapper);
                }
            }
        }
    }

    /**
     * @param logicalIdentifier
     */
    public void removeLogicalNode(LogicalNode logicalIdentifier) {
        List<LogicalNodeWrapper> wrappersToRemove = new ArrayList<>();
        List<LogicalNode> logicalNodesToRemove = new ArrayList<LogicalNode>();
        for (LogicalNodeWrapper wrapper : wrappers) {
            for (LogicalNode logicalNode : wrapper.getLogicalNodes()) {
                if (logicalNode.equals(logicalIdentifier)) {
                    logicalNodesToRemove.add(logicalIdentifier);
                    //wrapper.getLogicalNodes().remove(logicalIdentifier);
                }
            }
            for (LogicalNode logicalNodeToRemove : logicalNodesToRemove) {
                wrapper.getLogicalNodes().remove(logicalNodeToRemove);
            }

            if (wrapper.getLogicalNodes().size() == 0) {
                // remove logical node wrapper as well
                wrappersToRemove.add(wrapper);
                //wrappers.remove(wrapper);
            }
        }
        for (LogicalNodeWrapper wrapperToRemove : wrappersToRemove) {
            wrappers.remove(wrapperToRemove);
        }
    }



}
