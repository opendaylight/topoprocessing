/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class IRRenderingOperatorTest {

    private IRRenderingOperator operator;
    private TopologyManager managerMock;
    private TopoStoreProvider topoStoreProviderMock;
    private TopologyStore topologyStoreMock;
    private Map<YangInstanceIdentifier, UnderlayItem> items;
    private YangInstanceIdentifier yangInstanceIdentifierMock;
    private UnderlayItem underlayItemMock;
    private String topologyId;

    @Before
    public void before() {
        managerMock = mock(TopologyManager.class);
        topoStoreProviderMock = mock(TopoStoreProvider.class);
        topologyStoreMock = mock(TopologyStore.class);
        items = new HashMap<>();
        yangInstanceIdentifierMock = mock(YangInstanceIdentifier.class);
        underlayItemMock = mock(UnderlayItem.class);
        topologyId = "some id";

        operator = new IRRenderingOperator();
        operator.setTopologyManager(managerMock);
        operator.setTopoStoreProvider(topoStoreProviderMock);

        when(topoStoreProviderMock.getTopologyStore(topologyId)).thenReturn(topologyStoreMock);
        when(topologyStoreMock.getUnderlayItems()).thenReturn(items);
    }

    @Test
    public void testProcessCreatedChangesCreatedEntryIsNull() {
        operator.processCreatedChanges(yangInstanceIdentifierMock, null, topologyId);
        verify(topoStoreProviderMock, never()).getTopologyStore(topologyId);
        verify(managerMock, never()).addOverlayItem(any(OverlayItem.class));
    }

    @Test
    public void testProcessCreatedChanges() {
        operator.processCreatedChanges(yangInstanceIdentifierMock, underlayItemMock, topologyId);
        assertEquals(1, items.size());
        assertEquals(underlayItemMock, items.get(yangInstanceIdentifierMock));
        verify(underlayItemMock, times(1)).setOverlayItem(any(OverlayItem.class));
        verify(managerMock, times(1)).addOverlayItem(any(OverlayItem.class));
    }

    @Test
    public void testProcessUpdatedChangesUpdatedEntryIsNull() {
        operator.processUpdatedChanges(yangInstanceIdentifierMock, null, topologyId);
        verify(topoStoreProviderMock, never()).getTopologyStore(topologyId);
        verify(managerMock, never()).addOverlayItem(any(OverlayItem.class));
    }

    @Test
    public void testProcessUpdatedChanges() {
        UnderlayItem underlayItemMock2 = mock(UnderlayItem.class);
        items.put(yangInstanceIdentifierMock, underlayItemMock2);
        OverlayItem overlayItemMock = mock(OverlayItem.class);
        when(underlayItemMock2.getOverlayItem()).thenReturn(overlayItemMock);

        operator.processUpdatedChanges(yangInstanceIdentifierMock, underlayItemMock, topologyId);
        verify(overlayItemMock, times(1)).setUnderlayItems(any(List.class));
        verify(underlayItemMock, times(1)).setOverlayItem(overlayItemMock);
        assertEquals(1, items.size());
        assertEquals(underlayItemMock, items.get(yangInstanceIdentifierMock));
        verify(managerMock, times(1)).updateOverlayItem(overlayItemMock);
    }

    @Test
    public void testProcessRemovedChangesIdentifierIsNull() {
        operator.processRemovedChanges(null, topologyId);
        verify(topoStoreProviderMock, never()).getTopologyStore(topologyId);
        verify(managerMock, never()).removeOverlayItem(any(OverlayItem.class));
    }

    @Test
    public void testProcessRemovedChangesUnderlayItemIsNull() {
        operator.processRemovedChanges(yangInstanceIdentifierMock, topologyId);
        verify(topoStoreProviderMock, times(1)).getTopologyStore(topologyId);
        verify(managerMock, never()).removeOverlayItem(any(OverlayItem.class));
    }

    @Test
    public void testProcessRemovedChanges() {
        items.put(yangInstanceIdentifierMock, underlayItemMock);

        operator.processRemovedChanges(yangInstanceIdentifierMock, topologyId);
        verify(topoStoreProviderMock, times(1)).getTopologyStore(topologyId);
        verify(underlayItemMock, times(1)).getOverlayItem();
        verify(managerMock, times(1)).removeOverlayItem(any(OverlayItem.class));
    }
}
