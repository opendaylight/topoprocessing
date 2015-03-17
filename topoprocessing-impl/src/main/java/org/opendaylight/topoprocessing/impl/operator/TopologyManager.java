/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import com.google.common.base.Predicates;

/**
 * @author matus.marko
 */
public class TopologyManager {

    private Map<YangInstanceIdentifier, LogicalNode> map = new HashMap<>();
    private List<TopologyStore> topologyStores = new ArrayList<>();

    /**
     * Process newly created changes
     * @param newEntries
     * @param topologyId
     */
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> newEntries, final String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> newEntry : newEntries.entrySet()) {
                    ts.getPhysicalNodes().put(newEntry.getKey(), newEntry.getValue());
                }
            } else {
                for (Entry<YangInstanceIdentifier, PhysicalNode> newEntry : newEntries.entrySet()) {
                    for (Entry<YangInstanceIdentifier, PhysicalNode> entry : ts.getPhysicalNodes().entrySet()) {
                        if (newEntry.getValue().getLeafNode().equals(entry.getValue().getLeafNode())) {
                            // TODO do aggregation
                        }
                    }
                }
            }
        }
    }

    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> resultEntries) {

    }

    /**
     * Delete node from Aggregation map
     * @param identifier Yang Instance Identifier
     * @param topologyId Topology Identification
     */
    public void processDeletedChanges(YangInstanceIdentifier identifier, final String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, PhysicalNode> physicalNodes = ts.getPhysicalNodes();
                PhysicalNode physicalNode = physicalNodes.remove(identifier);
                if (null != physicalNode) {
                    // TODO remove from aggregator map
                }
            }
        }
    }

    /**
     * Add new empty record to topology store list
     * @param underlayTopology
     */
    public void initializeTopologyStore(String underlayTopology) {
        topologyStores.add(new TopologyStore(underlayTopology,
                new HashMap<YangInstanceIdentifier, PhysicalNode>()));
    }

}
