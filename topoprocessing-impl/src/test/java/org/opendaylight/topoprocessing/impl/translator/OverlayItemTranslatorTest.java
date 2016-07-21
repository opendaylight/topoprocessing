/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Queue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

/**
 * @author marek.korenciak
 */
@RunWith(MockitoJUnitRunner.class)
public class OverlayItemTranslatorTest {

    OverlayItemTranslator overlayTranslator;
    OverlayItemWrapper overlayItemWrapper;

    @Mock
    OverlayItemWrapper overlayWrapperMock;
    @Mock
    Queue<OverlayItem> overlayItemsMock;
    @Mock
    NodeTranslator nodeTranslatorMock;
    @Mock
    LinkTranslator linkTranslatorMock;

    @Before
    public void testInit() {
        overlayTranslator = new OverlayItemTranslator(nodeTranslatorMock, linkTranslatorMock);
    }

    /**
     * Null input test
     */
    @Test
    public void testTranslateNullInput() {
        Assert.assertNull(overlayTranslator.translate(null));
    }

    /**
     * Test of input without overlay item
     */
    @Test
    public void testTranslateOverlayItemIsEmpty() {
        when(overlayWrapperMock.getOverlayItems()).thenReturn(overlayItemsMock);
        when(overlayItemsMock.isEmpty()).thenReturn(true);
        Assert.assertNull(overlayTranslator.translate(overlayWrapperMock));
        verify(nodeTranslatorMock, times(0)).translate(overlayWrapperMock);
        verify(linkTranslatorMock, times(0)).translate(overlayWrapperMock);
    }

    /**
     * Test of overlay item with correlation item value "Node"
     */
    @Test
    public void testTranslateCorrelationItemNode() {
        OverlayItem overlayItem = new OverlayItem(new ArrayList<>(), CorrelationItemEnum.Node);
        overlayItemWrapper = new OverlayItemWrapper("ID", overlayItem);
        when(nodeTranslatorMock.translate(overlayItemWrapper)).thenReturn(null);
        Assert.assertNull(overlayTranslator.translate(overlayItemWrapper));
        verify(nodeTranslatorMock, times(1)).translate(overlayItemWrapper);
        verify(linkTranslatorMock, times(0)).translate(overlayItemWrapper);
    }

    /**
     * Test of overlay item with correlation item value "TerminationPoint"
     */
    @Test
    public void testTranslateCorrelationItemTerminationPoint() {
        OverlayItem overlayItem = new OverlayItem(new ArrayList<>(), CorrelationItemEnum.TerminationPoint);
        overlayItemWrapper = new OverlayItemWrapper("ID", overlayItem);
        when(nodeTranslatorMock.translate(overlayItemWrapper)).thenReturn(null);
        Assert.assertNull(overlayTranslator.translate(overlayItemWrapper));
        verify(nodeTranslatorMock, times(1)).translate(overlayItemWrapper);
        verify(linkTranslatorMock, times(0)).translate(overlayItemWrapper);
    }

    /**
     * Test of overlay item with correlation item value "Link"
     */
    @Test
    public void testTranslateCorrelationItemLink() {
        OverlayItem overlayItem = new OverlayItem(new ArrayList<>(), CorrelationItemEnum.Link);
        overlayItemWrapper = new OverlayItemWrapper("ID", overlayItem);
        when(linkTranslatorMock.translate(overlayItemWrapper)).thenReturn(null);
        Assert.assertNull(overlayTranslator.translate(overlayItemWrapper));
        verify(linkTranslatorMock, times(1)).translate(overlayItemWrapper);
        verify(nodeTranslatorMock, times(0)).translate(overlayItemWrapper);
    }
}
