/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author michal.vrsansky
 */
public class LinkFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkFiltrator.class);

    private List<Filtrator> filtrators = new ArrayList<>();
    protected Manager manager;
    private TopoStoreProvider topoStoreProvider;

    public LinkFiltrator(TopoStoreProvider topoStoreProvider) {
        super(topoStoreProvider);
        this.topoStoreProvider = topoStoreProvider;
    }

    protected TopoStoreProvider getTopoStoreProvider() {
            return topoStoreProvider;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdItem, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        if (CorrelationItemEnum.Node.equals(createdItem.getCorrelationItem()) ||
                        passedFiltration(createdItem.getLeafNodes().values().iterator().next())) {
            topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, createdItem);
            manager.addOverlayItem(wrapUnderlayItem(createdItem));
            LOGGER.trace("Link passed filtration/node getting through: {}",createdItem.getItemId());
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedItem, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem oldItem = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().get(identifier);
        if (null == oldItem) {
            // updatedItem is not present yet
            if (updatedItem.getCorrelationItem().equals(CorrelationItemEnum.Node) ||
                    passedFiltration(updatedItem.getLeafNodes().values().iterator().next())) {
                // link passed through filtrator ot its node
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, updatedItem);
                manager.addOverlayItem(wrapUnderlayItem(updatedItem));
                LOGGER.trace("Link passed filtration/node getting through: {}",updatedItem.getItemId());
            }
        } else {
            // updatedItem exists already
            if (updatedItem.getCorrelationItem().equals(CorrelationItemEnum.Node) ||
                    passedFiltration(updatedItem.getLeafNodes().values().iterator().next())) {
                // link passed through filtrator ot its node
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, updatedItem);
                OverlayItem overlayItem = oldItem.getOverlayItem();
                updatedItem.setOverlayItem(overlayItem);
                Queue<UnderlayItem> underlayItems = new ConcurrentLinkedQueue<>();
                underlayItems.add(updatedItem);
                overlayItem.setUnderlayItems(underlayItems);
                manager.updateOverlayItem(overlayItem);
                LOGGER.trace("Updated link passed filtration/node getting through: {}",updatedItem.getItemId());
            } else {
                // filtered out
                OverlayItem oldOverlayItem = oldItem.getOverlayItem();
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().remove(identifier);
                manager.removeOverlayItem(oldOverlayItem);
                LOGGER.trace("Removed link/node: {}",updatedItem.getItemId());
            }
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, String topologyId) {
        LOGGER.trace("Processing removedChanges");
        UnderlayItem underlayItem = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems()
                .remove(itemIdentifier);
        if (null != underlayItem) {
            manager.removeOverlayItem(underlayItem.getOverlayItem());
        }
    }

    @Override
    public void setTopologyManager(Manager manager) {
        this.manager = manager;
    }

    /**
     * Add new filtrator
     * @param filter Node Ip Filtrator
     */
    public void addFilter(Filtrator filter) {
        filtrators.add(filter);
    }

    @Override
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
