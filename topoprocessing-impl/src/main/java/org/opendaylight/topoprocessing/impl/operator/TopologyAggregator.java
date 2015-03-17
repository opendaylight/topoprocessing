/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.HashMap;
import java.util.List;
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
     * Creates logical node
     * @param physicalNodes 
     * @return new logical node
     */
    public LogicalNode createLogicalNode(List<PhysicalNode> physicalNodes) {
        return new LogicalNode(physicalNodes);
    }

    /**
     * Adds logical node to the map
     * @param logicalNodeIdentifier
     * @param logicalNode
     */
    public void addLogicalNode(YangInstanceIdentifier logicalNodeIdentifier,
            LogicalNode logicalNode) {
        if (logicalNodeIdentifier != null) {
            aggregationMap.put(logicalNodeIdentifier, logicalNode);
        } else {
            throw new IllegalArgumentException("Logical node identifier cannto be null.");
        }
    }
}
