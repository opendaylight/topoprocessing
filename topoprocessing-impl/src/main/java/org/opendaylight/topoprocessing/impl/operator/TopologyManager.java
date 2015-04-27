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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
public class TopologyManager {

    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private List<LogicalNodeWrapper> wrappers = new ArrayList<>();
    private TopologyWriter writer;
    private String overlayTopologyId;

    /**
     * @param overlayTopologyId
     */
    public TopologyManager(String overlayTopologyId) {
        this.overlayTopologyId = overlayTopologyId;
    }

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
    public void addLogicalNode(LogicalNode newLogicalNode) {
        if (newLogicalNode != null && newLogicalNode.getPhysicalNodes() != null) {
            for (PhysicalNode newPhysicalNode : newLogicalNode.getPhysicalNodes()) {
                for (LogicalNodeWrapper wrapper : wrappers) {
                    for (LogicalNode logicalNodeFromWrapper : wrapper.getLogicalNodes()) {
                        for (PhysicalNode physicalNode : logicalNodeFromWrapper.getPhysicalNodes()) {
                            if (physicalNode.getNodeIdentifier().equals(newPhysicalNode.getNodeIdentifier())) {
                                wrapper.addLogicalNode(newLogicalNode);
                                writer.writeNode(wrapper);
                                return;
                            }
                        }
                    }
                }
            }
            //generate wrapper id
            YangInstanceIdentifier wrapperId =
                    idGenerator.getNextIdentifier(overlayTopologyId, CorrelationItemEnum.Node);
            //create new Logical node wrapper and add the logical node into it
            LogicalNodeWrapper newWrapper = new LogicalNodeWrapper(wrapperId, newLogicalNode);
            wrappers.add(newWrapper);
            writer.writeNode(newWrapper);
        }
    }

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
        LogicalNodeWrapper foundWrapper = null;
        for (LogicalNodeWrapper wrapper : wrappers) {
            if (wrapper.getLogicalNodes().contains(logicalIdentifier)) {
                wrapper.getLogicalNodes().remove(logicalIdentifier);
                foundWrapper = wrapper;
                break;
            }
        }
        if (foundWrapper != null) {
            if (foundWrapper.getLogicalNodes().size() == 0) {
                // remove logical node wrapper as well
                writer.deleteNode(foundWrapper);
                wrappers.remove(foundWrapper);
            } else {
                writer.writeNode(foundWrapper);
            }
        }
    }

    /**
     * @param writer writes into operational datastore
     */
    public void setWriter(TopologyWriter writer) {
        this.writer = writer;
    }
}
