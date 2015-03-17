/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class TopologyAggregator implements TopologyOperator {

    Map<YangInstanceIdentifier,LogicalNode> aggregationMap = new HashMap<>();
    
    public void remove(YangInstanceIdentifier identifier) {

    }

    /**
     * Create Logical node from two Physical nodes
     * @param physicalNode1 - first input Physical node
     * @param physicalNode2 - second input Physical node 
     * @return Logical node
     */
    public LogicalNode createLogicalNode(PhysicalNode physicalNode1, PhysicalNode physicalNode2) {
        return new LogicalNode(physicalNode1, physicalNode2);
    }

    /**
     * @param logicalNodeIdentifier
     * @param logicalNode
     */
    public void createAggregationdMap(YangInstanceIdentifier logicalNodeIdentifier,
            LogicalNode logicalNode) {
        aggregationMap.put(logicalNodeIdentifier, logicalNode);
    }
}
