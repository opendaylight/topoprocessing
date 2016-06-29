/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author michal.vrsansky
 */
public class LinkFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkFiltrator.class);

    public LinkFiltrator(TopoStoreProvider topoStoreProvider) {
        super(topoStoreProvider);
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdItem, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        if (CorrelationItemEnum.Node.equals(createdItem.getCorrelationItem()) ||
                        passedFiltration(createdItem.getLeafNodes().values())) {
            topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, createdItem);
            manager.addOverlayItem(wrapUnderlayItem(createdItem));
            LOGGER.trace("Link passed filtration/node getting through: {}",createdItem.getItemId());
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedItem, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem oldItem = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().get(identifier);
        if (oldItem == null) {
            // updatedItem is not present yet
            if (updatedItem.getCorrelationItem().equals(CorrelationItemEnum.Node) ||
                    passedFiltration(updatedItem.getLeafNodes().values())) {
                // link passed through filtrator ot its node
                topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems().put(identifier, updatedItem);
                manager.addOverlayItem(wrapUnderlayItem(updatedItem));
                LOGGER.trace("Link passed filtration/node getting through: {}",updatedItem.getItemId());
            }
        } else {
            // updatedItem exists already
            if (updatedItem.getCorrelationItem().equals(CorrelationItemEnum.Node) ||
                    passedFiltration(updatedItem.getLeafNodes().values())) {
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
}
