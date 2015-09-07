/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

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
    protected TopologyAggregator aggregator;
    
    public PreAggregationFiltrator(TopoStoreProvider topoStoreProvider) {
            super(topoStoreProvider);
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        if (passedFiltration(createdEntry.getItem())) {
            aggregator.processCreatedChanges(identifier, createdEntry, topologyId);
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem olditem = getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().get(identifier);
        if (null == olditem) {
            // updateditem is not present yet
            if (passedFiltration(updatedEntry.getItem())) {
                // passed through filtrator
                aggregator.processCreatedChanges(identifier, updatedEntry, topologyId);
            }
            // else do nothing
        } else {
            // updateditem exists already
            if (passedFiltration(updatedEntry.getItem())) {
                // passed through filtrator
                aggregator.processUpdatedChanges(identifier, updatedEntry, topologyId);
            } else {
                // filtered out
                aggregator.processRemovedChanges(identifier, topologyId);
            }
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier identifier, String topologyId) {
        LOGGER.trace("Processing removedChanges");
        aggregator.processRemovedChanges(identifier, topologyId);
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
