/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventoryRendering.operator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.ITopologyManager;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author matej.perina
 *
 */

public class IRRenderingOperator implements TopologyOperator {


    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyOperator.class);
    private ITopologyManager manager;
    private TopoStoreProvider topoStoreProvider;

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        if (createdEntry != null) {
            LOGGER.trace("Processing createdChnages");
            Map<YangInstanceIdentifier, UnderlayItem> items =
                    topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            items.put(identifier, createdEntry);
            OverlayItem item = wrapUnderlayItem(createdEntry);
            createdEntry.setOverlayItem(item);
            manager.addOverlayItem(item);

        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        if (updatedEntry != null) {
            LOGGER.trace("Processing updateChanges");
            Map<YangInstanceIdentifier, UnderlayItem> items =
                    topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            UnderlayItem oldItem = items.get(identifier);
            OverlayItem item = oldItem.getOverlayItem();
            Queue<UnderlayItem> underlayItems = new ConcurrentLinkedQueue<>();
            underlayItems.add(updatedEntry);
            item.setUnderlayItems(underlayItems);
            updatedEntry.setOverlayItem(item);
            items.put(identifier, updatedEntry);
            manager.updateOverlayItem(item);
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier identifier, String topologyId) {
        if (identifier != null) {
            LOGGER.trace("Processing removeChanges");
            Map<YangInstanceIdentifier, UnderlayItem> items =
                    topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            UnderlayItem underlayItem = items.remove(identifier);
            if (null != underlayItem) {
                manager.removeOverlayItem(underlayItem.getOverlayItem());
            }
        }

    }

    private OverlayItem wrapUnderlayItem(UnderlayItem underlayItem) {
        List<UnderlayItem> underlayItems = Collections.singletonList(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems, underlayItem.getCorrelationItem());
        return overlayItem;
    }

    @Override
    public void setTopologyManager(ITopologyManager manager) {
        this.manager = manager;
    }

    public void setTopoStoreProvider(TopoStoreProvider topoStoreProvider) {
        this.topoStoreProvider = topoStoreProvider;
    }

}
