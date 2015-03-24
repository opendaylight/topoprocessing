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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class TopologyAggregator implements TopologyOperator {

    private Map<YangInstanceIdentifier,LogicalNode> aggregationMap = new HashMap<>();
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private CorrelationItemEnum correlationItem;
    private List<TopologyStore> topologyStores;

    /**
     * Constructor
     * @param correlationItem
     */
    public TopologyAggregator(CorrelationItemEnum correlationItem, List<TopologyStore> topologyStores) {
        this.correlationItem = correlationItem;
        this.topologyStores = topologyStores;
    }

    /**
     * Process newly created changes
     * @param createdEntries
     * @param topologyId
     */
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, 
            final String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
                    ts.getPhysicalNodes().put(createdEntry.getKey(), createdEntry.getValue());
                }
            } else {
                for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
                    for (Entry<YangInstanceIdentifier, PhysicalNode> entry : ts.getPhysicalNodes().entrySet()) {
                        if (createdEntry.getValue().getLeafNode().equals(entry.getValue().getLeafNode())) {
                            YangInstanceIdentifier logicalNodeIdentifier =
                                    idGenerator.getNextIdentifier(topologyId, correlationItem);
                            createdEntry.getValue().setLogicalIdentifier(logicalNodeIdentifier);
                            entry.getValue().setLogicalIdentifier(logicalNodeIdentifier);
                            List<PhysicalNode> physicalNodes = new ArrayList<>();
                            physicalNodes.add(createdEntry.getValue());
                            physicalNodes.add(entry.getValue());
                            LogicalNode logicalNode = createLogicalNode(physicalNodes);
                            addLogicalNode(logicalNodeIdentifier, logicalNode);
                        }
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
    public void processRemovedChanges(ArrayList<YangInstanceIdentifier> identifiers, final String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, PhysicalNode> physicalNodes = ts.getPhysicalNodes();
                for (YangInstanceIdentifier identifier : identifiers) {
                    PhysicalNode physicalNode = physicalNodes.remove(identifier);
                    // if identifier exists in topology store
                    if (null != physicalNode) {
                        YangInstanceIdentifier logicalIdentifier = physicalNode.getLogicalIdentifier();
                        // if physical node is part of some logical node
                        if (null != logicalIdentifier) {
                            LogicalNode logicalNode = this.aggregationMap.get(logicalIdentifier);
                            ArrayList<PhysicalNode> aggregatedNodes = logicalNode.getPhysicalNodes();
                            // if logical node consists only of 2 physical nodes
                            if (2 == aggregatedNodes.size()) {
                                aggregatedNodes.remove(physicalNode);
                                PhysicalNode restNode = aggregatedNodes.iterator().next();
                                restNode.setLogicalIdentifier(null);
                                aggregationMap.remove(logicalIdentifier);
                            } else {
                                aggregatedNodes.remove(physicalNode);
                            }
                        }
                    }
                }
            }
        }
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
