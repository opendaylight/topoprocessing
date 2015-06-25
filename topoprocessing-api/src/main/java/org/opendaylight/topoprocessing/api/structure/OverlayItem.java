/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.api.structure;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 * @author martin.uhlir
 */
public class OverlayItem {

    private List<UnderlayItem> underlayItems = new ArrayList<>();
    private CorrelationItemEnum correlationItem;

    /**
     * Creates overlay item
     * @param underlayItems list of underlayItems that form this OverlayItem
     * @param correlationItem type of underlayItems
     */
    public OverlayItem(List<UnderlayItem> underlayItems, CorrelationItemEnum correlationItem) {
        Preconditions.checkNotNull(underlayItems, "underlayItems cannot be null");
        this.underlayItems.addAll(underlayItems);
        this.correlationItem = correlationItem;
    }

    /**
     * Adds an underlay item to the list of items
     * @param underlayItem to be added
     */
    public void addUnderlayItem(UnderlayItem underlayItem) {
        underlayItems.add(underlayItem);
    }

    /**
     * Removes underlay item from the list of items
     * @param itemToRemove underlay item to be removed
     */
    public void removeUnderlayItem(UnderlayItem itemToRemove) {
        boolean removed = underlayItems.remove(itemToRemove);
        if (!removed) {
            throw new IllegalArgumentException("Item to remove not found in the list.");
        }
    }

    /**
     * Update underlay item. Old item will be replaced by the new one.
     * @param oldItem original item stored in topostore
     * @param newItem updated item
     */
    public void updateUnderlayItem(UnderlayItem oldItem, UnderlayItem newItem) {
        boolean removed = underlayItems.remove(oldItem);
        if (removed) {
            underlayItems.add(newItem);
        } else {
            throw new IllegalArgumentException("Item to replace not found in the list.");
        }
    }

    /**
     * @return {@link UnderlayItem}s (underlay items)
     */
    public List<UnderlayItem> getUnderlayItems() {
        return underlayItems;
    }

    /**
     * Sets {@link UnderlayItem}s
     * @param underlayItems underlay items
     */
    public void setUnderlayItems(List<UnderlayItem> underlayItems) {
        Preconditions.checkNotNull(underlayItems, "underlayItems parameter cannot be null");
        this.underlayItems = underlayItems;
    }

    /**
     * @return correlation item: Node or Link or TerminationPoint
     */
    public CorrelationItemEnum getCorrelationItem() {
        return correlationItem;
    }
}
