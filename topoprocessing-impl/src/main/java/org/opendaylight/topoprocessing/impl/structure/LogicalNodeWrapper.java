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
 * @author martin.uhlir
 *
 */
public class LogicalNodeWrapper {
    private List<LogicalNode> logicalNodes = new ArrayList<>();
    private String nodeId;
    
    /**
     * Constructor
     * @param nodeId
     */
    public LogicalNodeWrapper(String nodeId, LogicalNode logicalNode) {
        this.nodeId = nodeId;
        addLogicalNode(logicalNode);
    }
    
    public void addLogicalNode(LogicalNode logicalNode) {
        logicalNodes.add(logicalNode);
    }
    
    public List<LogicalNode> getLogicalNodes() {
        return logicalNodes;
    }

    public void setLogicalNodes(List<LogicalNode> logicalNodes) {
        this.logicalNodes = logicalNodes;
    }
}
