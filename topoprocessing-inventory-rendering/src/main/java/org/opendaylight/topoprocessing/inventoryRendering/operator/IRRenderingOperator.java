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

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
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
    private TopologyManager manager;
    private TopoStoreProvider topoStoreProvider;

    private OverlayItem wrapUnderlayItem(UnderlayItem underlayItem) {
        List<UnderlayItem> underlayItems = Collections.singletonList(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems, underlayItem.getCorrelationItem());
        underlayItem.setOverlayItem(overlayItem);
        return overlayItem;
    }

    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        this.manager = topologyManager;
    }

    public void setTopoStoreProvider(TopoStoreProvider topoStoreProvider) {
        this.topoStoreProvider = topoStoreProvider;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem createdItem,
            String topologyId) {
        LOGGER.trace("Processing createdChnages");
        Map<YangInstanceIdentifier, UnderlayItem> items =
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
        UnderlayItem newItem = createdItem;
        items.put(itemIdentifier, newItem);
        OverlayItem item = wrapUnderlayItem(newItem);
        newItem.setOverlayItem(item);
        manager.addOverlayItem(item);

    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem updatedItem,
            String topologyId) {
        LOGGER.trace("Processing updateChanges");
        Map<YangInstanceIdentifier, UnderlayItem> items =
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
        UnderlayItem updatedUndItem = updatedItem;
        UnderlayItem oldItem = items.get(itemIdentifier);
        OverlayItem item = oldItem.getOverlayItem();
        item.setUnderlayItems(Collections.singletonList(updatedUndItem));
        updatedItem.setOverlayItem(item);
        items.put(itemIdentifier, updatedUndItem);
        manager.updateOverlayItem(item);

    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, String topologyId) {
        LOGGER.trace("Processing removeChanges");
        Map<YangInstanceIdentifier, UnderlayItem> items =
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
        UnderlayItem underlayItem = items.remove(itemIdentifier);
        if (null != underlayItem) {
            manager.removeOverlayItem(underlayItem.getOverlayItem());
        }

    }

}
