/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TopologyFiltrator extends TopoStoreProvider implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyFiltrator.class);

    private List<NodeIpFiltrator> filters = new ArrayList<>();

    private TopologyManager manager;

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, String topologyId) {
        LOG.debug("Processing createdChanges");
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> nodeEntry : createdEntries.entrySet()) {
            PhysicalNode newNodeValue = nodeEntry.getValue();
            if (filter(newNodeValue)) {
                getTopologyStore(topologyId).getPhysicalNodes().put(nodeEntry.getKey(), newNodeValue);
                LogicalNode logicalNode = createLogicalNode(newNodeValue);
                manager.addLogicalNode(logicalNode);
            }
        }
        LOG.debug("CreatedChanges processed");
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries, String topologyId) {
        LOG.debug("Processing updatedChanges");
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> nodeEntry : updatedEntries.entrySet()) {
            PhysicalNode nodeValue = nodeEntry.getValue();
            if (filter(nodeEntry.getValue())) {
                getTopologyStore(topologyId).getPhysicalNodes().put(nodeEntry.getKey(), nodeValue);
                LogicalNode logicalNode = createLogicalNode(nodeValue);
                manager.addLogicalNode(logicalNode);
            } else  {
                PhysicalNode physicalNode = getTopologyStore(topologyId).getPhysicalNodes().remove(nodeValue);
                if (null != physicalNode) {
                    LogicalNode logicalNode = createLogicalNode(physicalNode);
                    manager.removeLogicalNode(logicalNode);
                }
            }
        }
        LOG.debug("UpdatedChanges processed");
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOG.debug("Processing removedChanges");
        for (YangInstanceIdentifier nodeIdentifier : identifiers) {
            PhysicalNode physicalNode = getTopologyStore(topologyId).getPhysicalNodes().remove(nodeIdentifier);
            if (null != physicalNode) {
                LogicalNode logicalNode = createLogicalNode(physicalNode);
                manager.removeLogicalNode(logicalNode);
            }
        }
        LOG.debug("RemovedChanges processed");
    }

    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        this.manager = manager;
    }

    /**
     * Add new filter
     * @param filter
     */
    public void addFilter(NodeIpFiltrator filter) {
        filters.add(filter);
    }

    private boolean filter(PhysicalNode physicalNode) {
        for (NodeIpFiltrator filtrator : filters) {
            if (false == filtrator.filter(physicalNode)) {
                return false;
            }
        }
        return true;
    }

    private LogicalNode createLogicalNode(PhysicalNode physicalNode) {
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        physicalNodes.add(physicalNode);
        LogicalNode logicalNode = new LogicalNode(physicalNodes);
        physicalNode.setLogicalIdentifier(logicalNode);
        return logicalNode;
    }

    public void initializeStore(String underlayTopologyId) {
        for (TopologyStore topologyStore : topologyStores) {
            if (underlayTopologyId == topologyStore.getId()) {
                return;
            }
        }
        topologyStores.add(new TopologyStore(underlayTopologyId,
                new HashMap<YangInstanceIdentifier, PhysicalNode>()));
    }
}
