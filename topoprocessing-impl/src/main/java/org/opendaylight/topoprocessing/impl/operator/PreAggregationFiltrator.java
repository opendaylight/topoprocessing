/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.polkorab
 *
 */
public class PreAggregationFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreAggregationFiltrator.class);
    private TopologyAggregator aggregator;

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> nodeEntry : createdEntries.entrySet()) {
            PhysicalNode newNodeValue = nodeEntry.getValue();
            if (passedFiltration(newNodeValue)) {
                getTopologyStore(topologyId).getPhysicalNodes().put(nodeEntry.getKey(), newNodeValue);
                aggregator.processCreatedChanges(Collections.singletonMap(nodeEntry.getKey(), newNodeValue),
                        topologyId);
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> mapEntry : updatedEntries.entrySet()) {
            PhysicalNode updatedNode = mapEntry.getValue();
            PhysicalNode oldNode = getTopologyStore(topologyId).getPhysicalNodes().get(mapEntry.getKey());
            if (null == oldNode) {
                // updatedNode is not present yet
                if (passedFiltration(updatedNode)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getPhysicalNodes().put(mapEntry.getKey(), updatedNode);
                    aggregator.processCreatedChanges(Collections.singletonMap(mapEntry.getKey(), updatedNode),
                            topologyId);
                }
                // else do nothing
            } else {
                // updatedNode exists already
                if (passedFiltration(updatedNode)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getPhysicalNodes().put(mapEntry.getKey(), updatedNode);
                    aggregator.processUpdatedChanges(Collections.singletonMap(mapEntry.getKey(), updatedNode),
                            topologyId);
                } else {
                    // filtered out
                    getTopologyStore(topologyId).getPhysicalNodes().remove(mapEntry.getKey());
                    aggregator.processRemovedChanges(Collections.singletonList(mapEntry.getKey()), topologyId);
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOGGER.trace("Processing removedChanges");
        for (YangInstanceIdentifier nodeIdentifier : identifiers) {
            PhysicalNode physicalNode = getTopologyStore(topologyId).getPhysicalNodes().remove(nodeIdentifier);
            if (null != physicalNode) {
                aggregator.processRemovedChanges(Collections.singletonList(nodeIdentifier), topologyId);
            }
        }
    }

    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        throw new UnsupportedOperationException("PreAggregationFiltrator doesn't use TopologyManager");
    }

    /**
     * @param aggregator performs aggregation after filtering is done
     */
    public void setTopologyAggregator(TopologyAggregator aggregator) {
        this.aggregator = aggregator;
    }
}
