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
    private String id;

    /**
     * Constructor
     * @param itemId aggregated item id
     * @param overlayItem initial overlay item to be wrapped
     */
    public OverlayItemWrapper(String itemId, OverlayItem overlayItem) {
        this.id = itemId;
        addOverlayItem(overlayItem);
    }

    /**
     * @param overlayItem adds overlay item into this wrapper
     */
    public void addOverlayItem(OverlayItem overlayItem) {
        overlayItems.add(overlayItem);
    }

    /**
     * @return all overlay items wrapped by this wrapper
     */
    public List<OverlayItem> getOverlayItems() {
        return overlayItems;
    }

    /**
     * @param overlayItems sets overlay items wrapped by this wrapper
     */
    public void setLogicalNodes(List<OverlayItem> overlayItems) {
        this.overlayItems = overlayItems;
    }

    /**
     * @return aggregated item id
     */
    public String getId() {
        return id;
    }

    /**
     * @param wrapperId aggregated item item id to set
     */
    public void setId(String wrapperId) {
        this.id = wrapperId;
    }
}
