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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

public class OverlayItemWrapperTest {
    private OverlayItem overlayItem;
    private String itemId = "id:1";
    private OverlayItemWrapper wrapper;

    @Test
    public void testSetup() {
        UnderlayItem item = new UnderlayItem(null, null, "topo:1", "node:1", CorrelationItemEnum.Node);
        List<UnderlayItem> underlayItems = new ArrayList<>();
        underlayItems.add(item);
        overlayItem = new OverlayItem(underlayItems, CorrelationItemEnum.Node);
        wrapper = new OverlayItemWrapper(itemId, overlayItem);
        Assert.assertEquals(itemId, wrapper.getId());
        String newId = "new-id:11";
        wrapper.setId(newId);
        Assert.assertEquals(newId, wrapper.getId());
        Queue<OverlayItem> overlayItems = new ConcurrentLinkedQueue<>();
        overlayItems.add(overlayItem);
        wrapper.setLogicalNodes(overlayItems);
        Assert.assertEquals(1, wrapper.getOverlayItems().size());

    }
}
