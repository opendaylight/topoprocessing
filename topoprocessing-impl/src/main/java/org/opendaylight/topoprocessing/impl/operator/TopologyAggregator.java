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
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.UnificationCase;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Class handling aggregation correlation
 * @author matus.marko
 * @author martin.uhlir
 */
public class TopologyAggregator implements TopologyOperator {

    TopologyManager topologyManagerNew = new TopologyManager();

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAggregator.class);

    private CorrelationItemEnum correlationItem;
    private TopologyWriter topologyWriter;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private List<TopologyStore> topologyStores = new ArrayList<>();


    /** for testing purpose only */
    public TopologyManager getTopologyManagerNew() {
        return topologyManagerNew;
    }
    /** for testing purpose only */
    public List<TopologyStore> getTopologyStores() {
        return topologyStores;
    }

    /**
     * Constructor
     * @param correlationItem Correlation Item Enumerator
     * @param topologyStores Topology Stores
     * @param idGenerator Identifier Generator
     */
    public TopologyAggregator() {
        LOG.debug("Initializing aggregator");
    }

    /** for testing purpose only */
    public void setTopologyStores(List<TopologyStore> topologyStores) {
        this.topologyStores = topologyStores;
    }

    /**
     * @param correlationItem
     */
    public void setCorrelationItem(CorrelationItemEnum correlationItem) {
        this.correlationItem = correlationItem;
    }

    /**
     * Set correlation and initialize stores
     * @param correlation Correlation
     */
    public void initializeStructures(Correlation correlation) {
        if (correlation.getName().equals(Equality.class)) {
            EqualityCase typeCase = (EqualityCase) correlation.getCorrelationType();
            List<Mapping> mappings = typeCase.getEquality().getMapping();
            iterateMappings(mappings);
        } else if (correlation.getName().equals(Unification.class)) {
            UnificationCase typeCase = (UnificationCase) correlation.getCorrelationType();
            List<Mapping> mappings = typeCase.getUnification().getMapping();
            iterateMappings(mappings);
            //TODO Unification Operator
        }
    }

    private void iterateMappings(List<Mapping> mappings) {
        if (mappings != null) {
            for (Mapping mapping : mappings) {
                initializeStore(mapping.getUnderlayTopology());
            }
        }
    }

    private void initializeStore(String underlayTopologyId) {
        for (TopologyStore topologyStore : topologyStores) {
            if (underlayTopologyId == topologyStore.getId()) {
                return;
            }
        }
        topologyStores.add(new TopologyStore(underlayTopologyId,
                new HashMap<YangInstanceIdentifier, PhysicalNode>()));
    }

    /**
     * @param topologyWriter writes data into operational datastore
     */
    public void set(TopologyWriter topologyWriter) {
        this.topologyWriter = topologyWriter;
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries,
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
    }

    private void createAggregatedNodes(PhysicalNode newNode, String topologyId) {
        for (TopologyStore ts : topologyStores) {
            if (!ts.getId().equals(topologyId)) {
                for (Entry<YangInstanceIdentifier, PhysicalNode> topoStoreEntry : ts.getPhysicalNodes().entrySet()) {
                    PhysicalNode topoStoreNode = topoStoreEntry.getValue();
                    if (topoStoreNode.getLeafNode().getValue().equals(newNode.getLeafNode().getValue())) {
                        // no previous aggregation on this node
                        if (topoStoreNode.getLogicalIdentifier() == null) {
                            LOG.debug("Creating new Logical Node");
                            // create new logical node
                            List<PhysicalNode> nodesToAggregate = new ArrayList<>();
                            nodesToAggregate.add(newNode);
                            nodesToAggregate.add(topoStoreNode);
                            LogicalNode logicalNode = new LogicalNode(nodesToAggregate);
                            topoStoreNode.setLogicalIdentifier(logicalNode);
                            newNode.setLogicalIdentifier(logicalNode);
                            topologyManagerNew.addLogicalNodeToWrapper(logicalNode);
                        } else {
                            LOG.debug("Adding physical node to existing Logical Node");
                            // add new physical node into existing logical node
                            LogicalNode logicalNodeIdentifier = topoStoreNode.getLogicalIdentifier();
                            newNode.setLogicalIdentifier(logicalNodeIdentifier);
                            logicalNodeIdentifier.addPhysicalNode(newNode);
                            topologyManagerNew.updateLogicalNode(logicalNodeIdentifier);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, final String topologyId) {
        LOG.debug("Processing removedChanges");
        for (TopologyStore ts : topologyStores) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, PhysicalNode> physicalNodes = ts.getPhysicalNodes();
                for (YangInstanceIdentifier identifier : identifiers) {
                    PhysicalNode physicalNode = physicalNodes.remove(identifier);
                    // if identifier exists in topology store
                    if (physicalNode != null) {
                        // if physical node is part of some logical node
                        removePhysicalNodeFromLogicalNode(physicalNode);
                    }
                }
            }
        }
    }

    private void removePhysicalNodeFromLogicalNode(PhysicalNode nodeToRemove) {
        LogicalNode logicalIdentifier = nodeToRemove.getLogicalIdentifier();
        List<PhysicalNode> physicalNodes = logicalIdentifier.getPhysicalNodes();
        if (physicalNodes.size() == 2) {
            LOG.debug("Removing logical node");
            for (PhysicalNode physicalNode : physicalNodes) {
                physicalNode.getLogicalIdentifier().removePhysicalNode(physicalNode);
                physicalNode.setLogicalIdentifier(null);
            }
            topologyManagerNew.removeLogicalNode(logicalIdentifier);
        } else {
            LOG.debug("Removing physical node from logical node");
            nodeToRemove.getLogicalIdentifier().removePhysicalNode(nodeToRemove);
            nodeToRemove.setLogicalIdentifier(null);
            topologyManagerNew.updateLogicalNode(logicalIdentifier);
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
                                                String topologyId) {
        LOG.debug("Processing updatedChanges");
        for (Entry<YangInstanceIdentifier, PhysicalNode> updatedEntry : updatedEntries.entrySet()) {
            for (TopologyStore ts : topologyStores) {
                if (ts.getId().equals(topologyId)) {
                    LOG.debug("Updating logical node");
                    PhysicalNode physicalNode = ts.getPhysicalNodes().get(updatedEntry.getKey());
                    Preconditions.checkNotNull(physicalNode, "Updated physical node not found in Topology Store");
                    PhysicalNode updatedEntryValue = updatedEntry.getValue();
                    physicalNode.setNode(updatedEntryValue.getNode());
                    NormalizedNode<?, ?> leafNode = physicalNode.getLeafNode();
                    // if Leaf Node was changed
                    if (! leafNode.equals(updatedEntryValue.getLeafNode())) {
                        physicalNode.setLeafNode(updatedEntryValue.getLeafNode());
                        removePhysicalNodeFromLogicalNode(physicalNode);
                        createAggregatedNodes(physicalNode, topologyId);
                    } else if (physicalNode.getLogicalIdentifier() != null) {
                        // in case that only Node value was changed
                        topologyManagerNew.updateLogicalNode(physicalNode.getLogicalIdentifier());
                    }
                    break;
                }
            }
            LOG.error("Unable to update node because it was not found in topologyStore." + updatedEntry);
        }
    }
}
