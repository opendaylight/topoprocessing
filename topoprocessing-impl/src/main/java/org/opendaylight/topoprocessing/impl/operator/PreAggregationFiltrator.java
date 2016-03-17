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
    protected TopologyOperator operator;

    public PreAggregationFiltrator(TopoStoreProvider topoStoreProvider) {
            super(topoStoreProvider);
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        if (passedFiltration(createdEntry.getLeafNodes().values())) {
            operator.processCreatedChanges(identifier, createdEntry, topologyId);
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem olditem = getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().get(identifier);
        if (null == olditem) {
            // updateditem is not present yet
            if (passedFiltration(updatedEntry.getLeafNodes().values())) {
                // passed through filtrator
                operator.processCreatedChanges(identifier, updatedEntry, topologyId);
            }
            // else do nothing
        } else {
            // updateditem exists already
            if (passedFiltration(updatedEntry.getLeafNodes().values())) {
                // passed through filtrator
                operator.processUpdatedChanges(identifier, updatedEntry, topologyId);
            } else {
                // filtered out
                operator.processRemovedChanges(identifier, topologyId);
            }
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier identifier, String topologyId) {
        LOGGER.trace("Processing removedChanges");
        operator.processRemovedChanges(identifier, topologyId);
    }

    @Override
    public void setTopologyManager(Manager manager) {
        throw new UnsupportedOperationException("PreAggregationFiltrator doesn't use TopologyManager");
    }

    /**
     * @param operator performs operation after filtering is done
     */
    public void setTopologyAggregator(TopologyOperator operator) {
        this.operator = operator;
    }
}
