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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matus.marko
 */
public class TopologyFiltrator implements TopologyOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyFiltrator.class);

    private List<Filtrator> filtrators = new ArrayList<>();
    protected TopologyManager manager;
    private TopoStoreProvider topoStoreProvider;

    public TopologyFiltrator(TopoStoreProvider topoStoreProvider) {
        this.topoStoreProvider = topoStoreProvider;
    }

    protected TopoStoreProvider getTopoStoreProvider() {
            return topoStoreProvider;
    }


    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdItem, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        if (passedFiltration(createdItem.getItem())) {
            topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, createdItem);
            OverlayItem overlayItem = wrapUnderlayItem(createdItem);
            manager.addOverlayItem(overlayItem);
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedItem, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem oldItem = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().get(identifier);
        if (null == oldItem) {
            // updatedItem is not present yet
            if (passedFiltration(updatedItem.getItem())) {
                // passed through filtrator
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, updatedItem);
                manager.addOverlayItem(wrapUnderlayItem(updatedItem));
            }
            // else do nothing
        } else {
            // updatedItem exists already
            if (passedFiltration(updatedItem.getItem())) {
                // passed through filtrator
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, updatedItem);
                OverlayItem overlayItem = oldItem.getOverlayItem();
                updatedItem.setOverlayItem(overlayItem);
                Queue<UnderlayItem> underlayItems = new ConcurrentLinkedQueue<>();
                underlayItems.add(updatedItem);
                overlayItem.setUnderlayItems(underlayItems);
                manager.updateOverlayItem(overlayItem);
            } else {
                // filtered out
                OverlayItem oldOverlayItem = oldItem.getOverlayItem();
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().remove(identifier);
                manager.removeOverlayItem(oldOverlayItem);
            }
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, String topologyId) {
        LOGGER.trace("Processing removedChanges");
        UnderlayItem underlayItem = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().remove(itemIdentifier);
        if (null != underlayItem) {
            manager.removeOverlayItem(underlayItem.getOverlayItem());
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

    protected boolean passedFiltration(NormalizedNode<?, ?> node) {
        for (Filtrator filtrator : filtrators) {
            if (filtrator.isFiltered(node)) {
                return false;
            }
        }
        return true;
    }

    protected OverlayItem wrapUnderlayItem(UnderlayItem underlayItem) {
        List<UnderlayItem> underlayItems = Collections.singletonList(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems, underlayItem.getCorrelationItem());
        underlayItem.setOverlayItem(overlayItem);
        return overlayItem;
    }
}
