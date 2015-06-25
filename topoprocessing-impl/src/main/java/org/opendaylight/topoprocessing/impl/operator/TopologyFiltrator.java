/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.operator.filtrator.Filtrator;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TopologyFiltrator extends TopoStoreProvider implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyFiltrator.class);

    private List<Filtrator> filtrators = new ArrayList<>();
    private TopologyManager manager;

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries, String topologyId) {
        LOG.trace("Processing createdChanges");
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> itemEntry : createdEntries.entrySet()) {
            UnderlayItem newItemValue = itemEntry.getValue();
            if (passedFiltration(newItemValue)) {
                getTopologyStore(topologyId).getUnderlayItems().put(itemEntry.getKey(), newItemValue);
                OverlayItem overlayItem = wrapUnderlayItem(newItemValue);
                manager.addOverlayItem(overlayItem);
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId) {
        LOG.trace("Processing updatedChanges");
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> mapEntry : updatedEntries.entrySet()) {
            UnderlayItem updatedItem = mapEntry.getValue();
            UnderlayItem oldItem = getTopologyStore(topologyId).getUnderlayItems().get(mapEntry.getKey());
            if (null == oldItem) {
                // updatedItem is not present yet
                if (passedFiltration(updatedItem)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getUnderlayItems().put(mapEntry.getKey(), updatedItem);
                    manager.addOverlayItem(wrapUnderlayItem(updatedItem));
                }
                // else do nothing
            } else {
                // updatedItem exists already
                if (passedFiltration(updatedItem)) {
                    // passed through filtrator
                    getTopologyStore(topologyId).getUnderlayItems().put(mapEntry.getKey(), updatedItem);
                    OverlayItem overlayItem = oldItem.getOverlayItem();
                    updatedItem.setOverlayItem(overlayItem);
                    overlayItem.setUnderlayItems(Collections.singletonList(updatedItem));
                    manager.updateOverlayItem(overlayItem);
                } else {
                    // filtered out
                    OverlayItem oldOverlayItem = oldItem.getOverlayItem();
                    getTopologyStore(topologyId).getUnderlayItems().remove(mapEntry.getKey());
                    manager.removeOverlayItem(oldOverlayItem);
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOG.trace("Processing removedChanges");
        for (YangInstanceIdentifier itemIdentifier : identifiers) {
            UnderlayItem underlayItem = getTopologyStore(topologyId).getUnderlayItems().remove(itemIdentifier);
            if (null != underlayItem) {
                manager.removeOverlayItem(underlayItem.getOverlayItem());
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

    private boolean passedFiltration(UnderlayItem underlayItem) {
        for (Filtrator filtrator : filtrators) {
            if (filtrator.isFiltered(underlayItem)) {
                return false;
            }
        }
        return true;
    }

    private OverlayItem wrapUnderlayItem(UnderlayItem underlayItem) {
        List<UnderlayItem> underlayItems = Collections.singletonList(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems);
        underlayItem.setOverlayItem(overlayItem);
        return overlayItem;
    }

}
