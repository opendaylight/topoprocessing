/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeAndTPAggregatorTest {

    private TopologyAggregator nodeAggregatorMock;
    private TerminationPointAggregator tpAggregatorMock;
    private TerminationPointPreAggregationFiltrator tpPreAggFiltratorMock;
    private TopologyManager topologyManagerMock;
    private Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTopology;
    private NodeAndTPAggregator testedAggregator;
    private Manager nodeManager;
    private Manager tpManager;

    @Before
    public void setUp() throws Exception {
        nodeAggregatorMock = Mockito.mock(TopologyAggregator.class);
        tpAggregatorMock = Mockito.mock(TerminationPointAggregator.class);
        tpPreAggFiltratorMock = Mockito.mock(TerminationPointPreAggregationFiltrator.class);
        topologyManagerMock = Mockito.mock(TopologyManager.class);

        // mock setter for manager
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                nodeManager = (Manager) invocation.getArguments()[0];
                return null;
            }
        }).when(nodeAggregatorMock).setTopologyManager(any(Manager.class));

        // mock setter for manager
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                tpManager = (Manager) invocation.getArguments()[0];
                return null;
            }
        }).when(tpAggregatorMock).setTopologyManager(any(Manager.class));

        targetFieldsPerTopology = new HashMap<String, Map<Integer, YangInstanceIdentifier>>();

        testedAggregator = new NodeAndTPAggregator(nodeAggregatorMock, tpAggregatorMock, tpPreAggFiltratorMock,
                        NetworkTopologyModel.class, targetFieldsPerTopology);
        testedAggregator.setTopologyManager(topologyManagerMock);
    }

    @Test
    public void testNodeAndTPAggregator() {
        Mockito.verify(nodeAggregatorMock, Mockito.times(1)).setTopologyManager(Matchers.any(Manager.class));
        Mockito.verify(tpAggregatorMock, Mockito.times(1)).setTopologyManager(Matchers.any(Manager.class));
        Mockito.verify(tpAggregatorMock, Mockito.times(1)).setAgregationInsideAggregatedNodes(true);
    }

    @Test
    public void testProcessCreatedChanges() {
        testedAggregator.processCreatedChanges(null, null, null);
        Mockito.verify(nodeAggregatorMock, Mockito.times(1)).processCreatedChanges(null, null, null);
    }

    @Test
    public void testProcessUpdatedChanges() {
        testedAggregator.processUpdatedChanges(null, null, null);
        Mockito.verify(nodeAggregatorMock, Mockito.times(1)).processUpdatedChanges(null, null, null);
    }

    @Test
    public void testProcessRemovedChanges() {
        testedAggregator.processRemovedChanges(null, null);
        Mockito.verify(nodeAggregatorMock, Mockito.times(1)).processRemovedChanges(null, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetTopologyManagerFailure() {
        testedAggregator.setTopologyManager(nodeManager);
    }

    @Test
    public void testAddOverlayItemNodeManager() {

    }

    @Test
    public void testUpdateOverlayItemNodeManager() {

    }

    @Test
    public void testUpdateOverlayItemNodeManagerNotFoundWrapper() {
        Mockito.when(topologyManagerMock.findWrapper(any(OverlayItem.class))).thenReturn(null);
        nodeManager.updateOverlayItem(null);
        Mockito.verify(topologyManagerMock, Mockito.times(1)).updateOverlayItem(null);
    }

    @Test
    public void testRemoveOverlayItemNodeManager() {

    }

    @Test
    public void testRemoveOverlayItemNodeManagerNotFoundWrapper() {
        Mockito.when(topologyManagerMock.findWrapper(any(OverlayItem.class))).thenReturn(null);
        nodeManager.removeOverlayItem(null);
        Mockito.verify(topologyManagerMock, Mockito.times(1)).removeOverlayItem(null);
    }
}
