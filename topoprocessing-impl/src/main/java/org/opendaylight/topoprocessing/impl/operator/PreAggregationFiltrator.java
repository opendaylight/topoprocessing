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
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
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
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> itemEntry : createdEntries.entrySet()) {
            UnderlayItem newItem = itemEntry.getValue();
            if (passedFiltration(newItem)) {
                getTopologyStore(topologyId).getUnderlayItems().put(itemEntry.getKey(), newItem);
                aggregator.processCreatedChanges(Collections.singletonMap(itemEntry.getKey(), newItem),
                        topologyId);
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> mapEntry : updatedEntries.entrySet()) {
            UnderlayItem updatedItem = mapEntry.getValue();
            UnderlayItem olditem = getTopologyStore(topologyId).getUnderlayItems().get(mapEntry.getKey());
            if (null == olditem) {
                // updateditem is not present yet
                if (passedFiltration(updatedItem)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getUnderlayItems().put(mapEntry.getKey(), updatedItem);
                    aggregator.processCreatedChanges(Collections.singletonMap(mapEntry.getKey(), updatedItem),
                            topologyId);
                }
                // else do nothing
            } else {
                // updateditem exists already
                if (passedFiltration(updatedItem)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getUnderlayItems().put(mapEntry.getKey(), updatedItem);
                    aggregator.processUpdatedChanges(Collections.singletonMap(mapEntry.getKey(), updatedItem),
                            topologyId);
                } else {
                    // filtered out
                    getTopologyStore(topologyId).getUnderlayItems().remove(mapEntry.getKey());
                    aggregator.processRemovedChanges(Collections.singletonList(mapEntry.getKey()), topologyId);
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOGGER.trace("Processing removedChanges");
        for (YangInstanceIdentifier itemIdentifier : identifiers) {
            UnderlayItem item = getTopologyStore(topologyId).getUnderlayItems().remove(itemIdentifier);
            if (null != item) {
                aggregator.processRemovedChanges(Collections.singletonList(itemIdentifier), topologyId);
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
