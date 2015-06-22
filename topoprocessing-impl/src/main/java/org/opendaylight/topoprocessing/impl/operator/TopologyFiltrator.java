/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matus.marko
 */
public class TopologyFiltrator extends TopoStoreProvider implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyFiltrator.class);

    private List<Filtrator> filtrators = new ArrayList<>();
    private TopologyManager manager;

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, String topologyId) {
        LOG.trace("Processing createdChanges");
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> nodeEntry : createdEntries.entrySet()) {
            PhysicalNode newNodeValue = nodeEntry.getValue();
            if (passedFiltration(newNodeValue)) {
                getTopologyStore(topologyId).getPhysicalNodes().put(nodeEntry.getKey(), newNodeValue);
                LogicalNode logicalNode = wrapPhysicalNode(newNodeValue);
                manager.addLogicalNode(logicalNode);
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries, String topologyId) {
        LOG.trace("Processing updatedChanges");
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> mapEntry : updatedEntries.entrySet()) {
            PhysicalNode updatedNode = mapEntry.getValue();
            PhysicalNode oldNode = getTopologyStore(topologyId).getPhysicalNodes().get(mapEntry.getKey());
            if (null == oldNode) {
                // updatedNode is not present yet
                if (passedFiltration(updatedNode)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getPhysicalNodes().put(mapEntry.getKey(), updatedNode);
                    manager.addLogicalNode(wrapPhysicalNode(updatedNode));
                }
                // else do nothing
            } else {
                // updatedNode exists already
                if (passedFiltration(updatedNode)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getPhysicalNodes().put(mapEntry.getKey(), updatedNode);
                    LogicalNode logicalNode = oldNode.getLogicalNode();
                    updatedNode.setLogicalNode(logicalNode);
                    logicalNode.setPhysicalNodes(Collections.singletonList(updatedNode));
                    manager.updateLogicalNode(logicalNode);
                } else {
                    // filtered out
                    LogicalNode oldLogicalNode = oldNode.getLogicalNode();
                    getTopologyStore(topologyId).getPhysicalNodes().remove(mapEntry.getKey());
                    manager.removeLogicalNode(oldLogicalNode);
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOG.trace("Processing removedChanges");
        for (YangInstanceIdentifier nodeIdentifier : identifiers) {
            PhysicalNode physicalNode = getTopologyStore(topologyId).getPhysicalNodes().remove(nodeIdentifier);
            if (null != physicalNode) {
                manager.removeLogicalNode(physicalNode.getLogicalNode());
            }
        }
    }

    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        this.manager = topologyManager;
    }

    /**
     * Add new filtrator
     * @param filter Node Ip Filtrator
     */
    public void addFilter(Filtrator filter) {
        filtrators.add(filter);
    }

    private boolean passedFiltration(PhysicalNode physicalNode) {
        for (Filtrator filtrator : filtrators) {
            if (filtrator.isFiltered(physicalNode)) {
                return false;
            }
        }
        return true;
    }

    private LogicalNode wrapPhysicalNode(PhysicalNode physicalNode) {
        List<PhysicalNode> physicalNodes = Collections.singletonList(physicalNode);
        LogicalNode logicalNode = new LogicalNode(physicalNodes);
        physicalNode.setLogicalNode(logicalNode);
        return logicalNode;
    }

}
