/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.api.structure;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

/**
 * @author martin.dindoffer
 */
public class OverlayItemTest {

    private OverlayItem overlayItem;

    @Before
    public void setUp() throws Exception {
        List<UnderlayItem> underlayItems = new LinkedList<>();
        underlayItems.add(new UnderlayItem(null, null, null, "id1", CorrelationItemEnum.Node));
        overlayItem = new OverlayItem(underlayItems, CorrelationItemEnum.Node);
    }

    @Test(expected = NullPointerException.class)
    public void testContructorWithNullUnderlayItems() {
        OverlayItem oi = new OverlayItem(null, CorrelationItemEnum.Node);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemovalOfUnderlayItem() {
        overlayItem.removeUnderlayItem(new UnderlayItem(null, null, null, "id1", CorrelationItemEnum.Node));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateOfUnderlayItem() {
        overlayItem.updateUnderlayItem(new UnderlayItem(null, null, null, "id1", CorrelationItemEnum.Node),
                new UnderlayItem(null, null, null, "id2", CorrelationItemEnum.Node));
    }

    @Test(expected = NullPointerException.class)
    public void testSetUnderlayItems() {
        overlayItem.setUnderlayItems(null);
    }

}
