/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.structure;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;

/**
 * @author martin.uhlir
 *
 */
public class OverlayItemWrapper {
    private List<OverlayItem> overlayItems = new ArrayList<>();
    private String itemId;
    
    /**
     * Constructor
     * @param itemId
     * @param overlayItem 
     */
    public OverlayItemWrapper(String itemId, OverlayItem overlayItem) {
        this.itemId = itemId;
        addOverlayItem(overlayItem);
    }
    
    /**
     * @param overlayItem
     */
    public void addOverlayItem(OverlayItem overlayItem) {
        overlayItems.add(overlayItem);
    }
    
    /**
     * @return
     */
    public List<OverlayItem> getOverlayItems() {
        return overlayItems;
    }

    /**
     * @param overlayItems
     */
    public void setLogicalNodes(List<OverlayItem> overlayItems) {
        this.overlayItems = overlayItems;
    }

    /**
     * @return the nodeId
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @param itemId the itemId to set
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
