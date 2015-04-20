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
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class handling aggregation correlation
 * @author matus.marko
 * @author martin.uhlir
 */
public class TopologyAggregator implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAggregator.class);

    private AggregationMap aggregationMap = new AggregationMap();
    private IdentifierGenerator idGenerator;
    private CorrelationItemEnum correlationItem;
    private List<TopologyStore> topologyStores;

    /**
     * Constructor
     * @param correlationItem Correlation Item Enumerator
     * @param topologyStores Topology Stores
     * @param idGenerator Identifier Generator
     */
    public TopologyAggregator(CorrelationItemEnum correlationItem, List<TopologyStore> topologyStores,
                              IdentifierGenerator idGenerator) {
        LOG.debug("Initializing aggregator");
        this.correlationItem = correlationItem;
        this.topologyStores = topologyStores;
        this.idGenerator = idGenerator;
    }

    @Override
    public AggregationMap processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries,
                                                final String topologyId) {
        LOG.debug("Processing createdChanges");
        for (Entry<YangInstanceIdentifier, PhysicalNode> createdEntry : createdEntries.entrySet()) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    ts.getPhysicalNodes().put(createdEntry.getKey(), createdEntry.getValue());
                }
            }
            createAggregatedNodes(createdEntry.getValue(), topologyId);
        }
        LOG.debug("CreatedChanges processed");
        return aggregationMap;
    }

    private void createAggregatedNodes(PhysicalNode newNode,String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (!ts.getId().equals(topologyId)) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> topoStoreEntry : ts.getPhysicalNodes().entrySet()) {
                    PhysicalNode topoStoreNode = topoStoreEntry.getValue();
                    if (topoStoreNode.getLeafNode().getValue().equals(newNode.getLeafNode().getValue())) {
                        // no previous aggregation on this node
                        if (topoStoreNode.getLogicalIdentifier() == null) {
                            LOG.debug("Creating new Logical Node");
                            YangInstanceIdentifier logicalNodeIdentifier =
                                    idGenerator.getNextIdentifier(topologyId, correlationItem);
                            topoStoreNode.setLogicalIdentifier(logicalNodeIdentifier);
                            newNode.setLogicalIdentifier(logicalNodeIdentifier);
                            // create new logical node
                            List<PhysicalNode> nodesToAggregate = new ArrayList<>();
                            nodesToAggregate.add(newNode);
                            nodesToAggregate.add(topoStoreNode);
                            LogicalNode logicalNode = new LogicalNode(nodesToAggregate);
                            aggregationMap.put(logicalNodeIdentifier, logicalNode);
                        } else {
                            LOG.debug("Adding physical node to the Logical Node");
                            // add new physical node into existing logical node
                            YangInstanceIdentifier logicalIdentifier = topoStoreNode.getLogicalIdentifier();
                            newNode.setLogicalIdentifier(logicalIdentifier);
                            LogicalNode logicalNode = aggregationMap.get(logicalIdentifier);
                            logicalNode.addPhysicalNode(newNode);
                            aggregationMap.put(logicalIdentifier, logicalNode);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public AggregationMap processRemovedChanges(List<YangInstanceIdentifier> identifiers, final String topologyId) {
        LOG.debug("Processing removedChanges");
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, PhysicalNode> physicalNodes = ts.getPhysicalNodes();
                for (YangInstanceIdentifier identifier : identifiers) {
                    PhysicalNode physicalNode = physicalNodes.remove(identifier);
                    // if identifier exists in topology store
                    if (null != physicalNode) {
                        YangInstanceIdentifier logicalIdentifier = physicalNode.getLogicalIdentifier();
                        // if physical node is part of some logical node
                        removePhysicalNodeFromLogicalNode(physicalNode, logicalIdentifier);
                    }
                }
            }
        }
        return aggregationMap;
    }

    private void removePhysicalNodeFromLogicalNode(PhysicalNode physicalNode,
            YangInstanceIdentifier logicalIdentifier) {
        if (null != logicalIdentifier) {
            LogicalNode logicalNode = aggregationMap.get(logicalIdentifier);
            List<PhysicalNode> aggregatedNodes = logicalNode.getPhysicalNodes();
            aggregatedNodes.remove(physicalNode);
            // if logical node consists only of 1 physical node
            if (1 == aggregatedNodes.size()) {
                LOG.debug("Removing logical node");
                PhysicalNode restNode = aggregatedNodes.iterator().next();
                restNode.setLogicalIdentifier(null);
                aggregationMap.remove(logicalIdentifier);
            } else {
                LOG.debug("Removing physical node from logical node");
                aggregationMap.put(logicalIdentifier, logicalNode);
            }
        }
    }

    @Override
    public AggregationMap processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
                                                String topologyId) {
        LOG.debug("Processing updatedChanges");
        for (Entry<YangInstanceIdentifier, PhysicalNode> updatedEntry : updatedEntries.entrySet()) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    LOG.debug("Updating logical node");
                    PhysicalNode oldPhysicalNode = ts.getPhysicalNodes().get(updatedEntry.getKey());
                    if (oldPhysicalNode == null) {
                        throw new IllegalStateException("Updated physical node not found in Topology Store");
                    }
                    YangInstanceIdentifier logicalIdentifier = oldPhysicalNode.getLogicalIdentifier();
                    if(oldPhysicalNode.getLeafNode().equals(updatedEntry.getValue().getLeafNode())) {
                        oldPhysicalNode.setNode(updatedEntry.getValue().getNode());
                        if (null != logicalIdentifier) {
                            aggregationMap.markUpdated(logicalIdentifier, aggregationMap.get(logicalIdentifier));
                        }
                    } else {
                        removePhysicalNodeFromLogicalNode(oldPhysicalNode, logicalIdentifier);
                        createAggregatedNodes(updatedEntry.getValue(), topologyId);
                    }
                }
            }
        }
        return aggregationMap;
    }
}
