/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

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

    // these will be extracted with Mockito Answer
    private ITopologyManager nodeManager;
    private ITopologyManager tpManager;
    private UnderlayItem nodeWithAllTPs;

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
                nodeManager = (ITopologyManager) invocation.getArguments()[0];
                return null;
            }
        }).when(nodeAggregatorMock).setTopologyManager(any(ITopologyManager.class));

        // mock setter for manager
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                tpManager = (ITopologyManager) invocation.getArguments()[0];
                return null;
            }
        }).when(tpAggregatorMock).setTopologyManager(any(ITopologyManager.class));

        // mock process created changes in tp aggregators to get node with all termination points from wrapper
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                nodeWithAllTPs = (UnderlayItem) invocation.getArguments()[1];
                return null;
            }
        }).when(tpPreAggFiltratorMock).processCreatedChanges(any(YangInstanceIdentifier.class),
                any(UnderlayItem.class), any(String.class));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                nodeWithAllTPs = (UnderlayItem) invocation.getArguments()[1];
                return null;
            }
        }).when(tpAggregatorMock).processCreatedChanges(any(YangInstanceIdentifier.class),
                any(UnderlayItem.class), any(String.class));

        targetFieldsPerTopology = new HashMap<String, Map<Integer, YangInstanceIdentifier>>();
    }

    private void prepareTestedAggregator(Class<? extends Model> model) {
        testedAggregator = new NodeAndTPAggregator(nodeAggregatorMock, tpAggregatorMock, tpPreAggFiltratorMock,
                        model, targetFieldsPerTopology);
        testedAggregator.setTopologyManager(topologyManagerMock);
    }

    @Test
    public void testNodeAndTPAggregator() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        Mockito.verify(nodeAggregatorMock, times(1)).setTopologyManager(any(ITopologyManager.class));
        Mockito.verify(tpAggregatorMock, times(1)).setTopologyManager(any(ITopologyManager.class));
        Mockito.verify(tpAggregatorMock, times(1)).setAgregationInsideAggregatedNodes(true);
    }

    @Test
    public void testProcessCreatedChanges() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        testedAggregator.processCreatedChanges(null, null, null);
        Mockito.verify(nodeAggregatorMock, times(1)).processCreatedChanges(null, null, null);
    }

    @Test
    public void testProcessUpdatedChanges() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        testedAggregator.processUpdatedChanges(null, null, null);
        Mockito.verify(nodeAggregatorMock, times(1)).processUpdatedChanges(null, null, null);
    }

    @Test
    public void testProcessRemovedChanges() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        testedAggregator.processRemovedChanges(null, null);
        Mockito.verify(nodeAggregatorMock, times(1)).processRemovedChanges(null, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetTopologyManagerFailure() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        testedAggregator.setTopologyManager(nodeManager);
    }

    private void testAddOverlayItemNodeManager(Class<? extends Model> model) {
        prepareTestedAggregator(model);

        OverlayItemWrapper wrapper = createWrapperWithThreeTPs(model);

        Mockito.when(topologyManagerMock.findOrCreateWrapper(any(OverlayItem.class))).thenReturn(wrapper);

        nodeManager.addOverlayItem(new OverlayItem(new LinkedList<UnderlayItem>(), CorrelationItemEnum.Node));

        Mockito.verify(topologyManagerMock, times(1)).findOrCreateWrapper(any(OverlayItem.class));
        Mockito.verify(topologyManagerMock, times(1)).writeWrapper(wrapper, CorrelationItemEnum.Node);
        Mockito.verify(tpPreAggFiltratorMock, times(1)).processCreatedChanges(any(YangInstanceIdentifier.class),
                any(UnderlayItem.class), any(String.class));

        assertAmountOfTPsInNode(model, nodeWithAllTPs.getItem(), 3);
    }

    @Test
    public void testAddOverlayItemNodeManagerNT() {
        testAddOverlayItemNodeManager(NetworkTopologyModel.class);
    }

    @Test
    public void testAddOverlayItemNodeManagerI2RS() {
        testAddOverlayItemNodeManager(I2rsModel.class);
    }

    private void testUpdateOverlayItemNodeManager(Class<? extends Model> model) {
        prepareTestedAggregator(model);

        OverlayItemWrapper wrapper = createWrapperWithThreeTPs(model);

        Mockito.when(topologyManagerMock.findWrapper(any(OverlayItem.class))).thenReturn(wrapper);

        nodeManager.updateOverlayItem(new OverlayItem(new LinkedList<UnderlayItem>(), CorrelationItemEnum.Node));

        Mockito.verify(topologyManagerMock, times(1)).findWrapper(any(OverlayItem.class));
        Mockito.verify(topologyManagerMock, times(1)).updateOverlayItem(any(OverlayItem.class));
        Mockito.verify(tpPreAggFiltratorMock, times(1)).processCreatedChanges(any(YangInstanceIdentifier.class),
                any(UnderlayItem.class), any(String.class));

        assertAmountOfTPsInNode(model, nodeWithAllTPs.getItem(), 3);
    }

    @Test
    public void testUpdateOverlayItemNodeManagerNT() {
        testUpdateOverlayItemNodeManager(NetworkTopologyModel.class);
    }

    @Test
    public void testUpdateOverlayItemNodeManagerI2RS() {
        testUpdateOverlayItemNodeManager(I2rsModel.class);
    }

    @Test
    public void testUpdateOverlayItemNodeManagerNotFoundWrapper() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        Mockito.when(topologyManagerMock.findWrapper(any(OverlayItem.class))).thenReturn(null);
        nodeManager.updateOverlayItem(null);
        Mockito.verify(topologyManagerMock, times(1)).updateOverlayItem(null);
    }

    private void testRemoveOverlayItemNodeManager(Class<? extends Model> model) {
        prepareTestedAggregator(model);

        OverlayItemWrapper wrapper = createWrapperWithThreeTPs(model);

        Mockito.when(topologyManagerMock.findWrapper(any(OverlayItem.class))).thenReturn(wrapper);

        nodeManager.removeOverlayItem(new OverlayItem(new LinkedList<UnderlayItem>(), CorrelationItemEnum.Node));

        Mockito.verify(topologyManagerMock, times(1)).findWrapper(any(OverlayItem.class));
        Mockito.verify(topologyManagerMock, times(1)).removeOverlayItem(any(OverlayItem.class));
        Mockito.verify(tpPreAggFiltratorMock, times(1)).processCreatedChanges(any(YangInstanceIdentifier.class),
                any(UnderlayItem.class), any(String.class));

        assertAmountOfTPsInNode(model, nodeWithAllTPs.getItem(), 3);
    }

    @Test
    public void testRemoveOverlayItemNodeManagerNT() {
        testRemoveOverlayItemNodeManager(NetworkTopologyModel.class);
    }

    @Test
    public void testRemoveOverlayItemNodeManagerI2RS() {
        testRemoveOverlayItemNodeManager(I2rsModel.class);
    }

    @Test
    public void testRemoveOverlayItemNodeManagerNotFoundWrapper() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        Mockito.when(topologyManagerMock.findWrapper(any(OverlayItem.class))).thenReturn(null);
        nodeManager.removeOverlayItem(null);
        Mockito.verify(topologyManagerMock, times(1)).removeOverlayItem(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddOverlayItemTPManagerMoreUnderlayItems() {
        prepareTestedAggregator(NetworkTopologyModel.class);
        tpManager.addOverlayItem(createWrapperWithThreeTPs(NetworkTopologyModel.class).getOverlayItems().peek());
    }

    private void testAddOverlayItemTPManager(Class<? extends Model> model) {
        prepareTestedAggregator(model);
        int numberOfTPs = 3;
        MapNode tps = createTerminationPoints(model, numberOfTPs);
        OverlayItem overlayItem = createOverlayItemWithOneUnderlayWithTPs(model, "0", tps);
        OverlayItemWrapper wrapper = createWrapperWithThreeTPs(model);
        wrapper.addOverlayItem(overlayItem);
        Mockito.when(topologyManagerMock.findOrCreateWrapper(any(OverlayItem.class))).thenReturn(wrapper);

        nodeManager.addOverlayItem(new OverlayItem(new LinkedList<UnderlayItem>(), CorrelationItemEnum.Node));

        tpManager.addOverlayItem(overlayItem);

        assertEquals(wrapper.getAggregatedTerminationPoints(), tps);
    }

    @Test
    public void testAddOverlayItemTPManagerNT() {
        testAddOverlayItemTPManager(NetworkTopologyModel.class);
    }

    @Test
    public void testAddOverlayItemTPManagerI2RS() {
        testAddOverlayItemTPManager(I2rsModel.class);
    }

    private void assertAmountOfTPsInNode(Class<? extends Model> model, NormalizedNode<?, ?> normalizedNode,
            int amount) {
        Optional<NormalizedNode<?, ?>> tpMapNodeWithAllTPsOpt = Optional.absent();
        if (model.equals(NetworkTopologyModel.class)) {
            tpMapNodeWithAllTPsOpt = NormalizedNodes.findNode(normalizedNode,
                    InstanceIdentifiers.NT_TP_IDENTIFIER);
        } else if (model.equals(I2rsModel.class)) {
            tpMapNodeWithAllTPsOpt = NormalizedNodes.findNode(normalizedNode,
                    InstanceIdentifiers.I2RS_TERMINATION_POINT);
        }
        if (tpMapNodeWithAllTPsOpt.isPresent()) {
            MapNode tpMapNodeWithAllTPs = (MapNode) tpMapNodeWithAllTPsOpt.get();
            assertEquals("Node does not have correct amount of termination points",
                    tpMapNodeWithAllTPs.getValue().size(), amount);
        } else {
            fail("Termination point map node must be present");
        }
    }

    private OverlayItemWrapper createWrapperWithThreeTPs(Class<? extends Model> model) {
        String nodeId1 = "node-id-1";
        String nodeId2 = "node-id-2";

        MapNode tpMapNode1 = TopologyBuilder.createTerminationPointMapNode(model,
                TopologyBuilder.createTerminationPointMapEntry(model, "tp-id-1-1"),
                TopologyBuilder.createTerminationPointMapEntry(model, "tp-id-1-2"));
        MapNode tpMapNode2 = TopologyBuilder.createTerminationPointMapNode(model,
                TopologyBuilder.createTerminationPointMapEntry(model, "tp-id-2-1"));

        MapEntryNode node1 = TopologyBuilder.createNodeWithTerminationPoints(model, nodeId1, tpMapNode1);
        MapEntryNode node2 = TopologyBuilder.createNodeWithTerminationPoints(model, nodeId2, tpMapNode2);

        UnderlayItem underlayItem1 = new UnderlayItem(node1, null, "topology-id-1", nodeId1, CorrelationItemEnum.Node);
        UnderlayItem underlayItem2 = new UnderlayItem(node2, null, "topology-id-2", nodeId2, CorrelationItemEnum.Node);

        List<UnderlayItem> underlayItems = Arrays.asList(underlayItem1, underlayItem2);

        OverlayItem overlayItem = new OverlayItem(underlayItems, CorrelationItemEnum.Node);
        return new OverlayItemWrapper("wrapper-id", overlayItem);
    }

    private OverlayItem createOverlayItemWithOneUnderlayWithTPs(Class<? extends Model> model, String nodeId,
            MapNode terminationPoints) {
        MapEntryNode node = TopologyBuilder.createNodeWithTerminationPoints(model, nodeId, terminationPoints);
        UnderlayItem underlayItem = new UnderlayItem(node, null, "topology-id", nodeId, CorrelationItemEnum.Node);
        return new OverlayItem(Arrays.asList(underlayItem), CorrelationItemEnum.Node);
    }

    private MapNode createTerminationPoints(Class<? extends Model> model, int numberOfTPs) {
        LinkedList<MapEntryNode> terminationPoints = new LinkedList<>();
        for (int i = 0; i < numberOfTPs; i++) {
            terminationPoints.add(TopologyBuilder.createTerminationPointMapEntry(model, "tp-id-" + i));
        }
        return TopologyBuilder.createTerminationPointMapNode(model, terminationPoints);
    }
}
