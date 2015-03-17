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

    TopologyAggregator aggregator = new TopologyAggregator();
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
                            YangInstanceIdentifier logicalNodeIdentifier = getLogicalNodeYiidStub();
                            newEntry.getValue().setLogicalIdentifier(logicalNodeIdentifier);
                            entry.getValue().setLogicalIdentifier(logicalNodeIdentifier);
                            List<PhysicalNode> physicalNodes = new ArrayList<>();
                            physicalNodes.add(newEntry.getValue());
                            physicalNodes.add(entry.getValue());
                            LogicalNode logicalNode = aggregator.createLogicalNode(physicalNodes);
                            aggregator.addLogicalNode(logicalNodeIdentifier, logicalNode);
                        }
                    }
                }
            }
        }
    /**
     * Delete node from Aggregation map
     * @param identifier Yang Instance Identifier
     * @param topologyId Topology Identification
     */
    public void processDeletedChanges(ArrayList<YangInstanceIdentifier> identifiers, final String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, PhysicalNode> physicalNodes = ts.getPhysicalNodes();
                for (YangInstanceIdentifier identifier : identifiers) {
                    PhysicalNode physicalNode = physicalNodes.remove(identifier);
                    if (null != physicalNode) {
                        // TODO remove from aggregator map
                    }
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

    private YangInstanceIdentifier getLogicalNodeYiidStub() {
        // TODO temporary method stub - this has to be properly implemented or totally changed 
        throw new RuntimeException();
    }

    public void update(Map<YangInstanceIdentifier, PhysicalNode> resultEntries) {

    }

    public void delete(YangInstanceIdentifier identifier) {

    }

}
