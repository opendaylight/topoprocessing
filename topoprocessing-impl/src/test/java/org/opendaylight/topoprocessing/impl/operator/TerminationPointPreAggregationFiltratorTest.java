/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.filtrator.AbstractFiltrator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TerminationPointPreAggregationFiltratorTest {

    private static final String TOPOLOGY_ID = "topo:1";
    private static final String NODE_ID = "node:1";
    private static final QName UNNUMBERED_QNAME =
        QName.create(TerminationPoint.QNAME, "unnumbered").intern();

    private TerminationPointPreAggregationFiltrator filtrator;
    private YangInstanceIdentifier nodeYiid = YangInstanceIdentifier.builder(InstanceIdentifiers.NODE_IDENTIFIER)
            .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID).build();

    @Mock private AbstractFiltrator filter;
    private TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
    private TestAggregator aggregator = new TestAggregator(topoStoreProvider);
    private UnderlayItem underlayItemInput;
    private UnderlayItem underlayItemOutput;
    private MapEntryNode nodeValueInput;
    private MapEntryNode nodeValueOutput;
    private YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.builder().node(UNNUMBERED_QNAME).build();

    class TestAggregator extends TerminationPointAggregator {

        private MapEntryNode output;

        public TestAggregator(TopoStoreProvider topoStoreProvider) {
            super(topoStoreProvider, NetworkTopologyModel.class);
        }

        public void setOutput(MapEntryNode output) {
            this.output = output;
        }

        @Override
        public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry,
                    String topologyId) {
            MapEntryNode inputNode = (MapEntryNode) createdEntry.getItem();
            Assert.assertEquals("Nodes should be equal", output, inputNode);
        }

        @Override
        public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry,
                    String topologyId) {
            MapEntryNode inputNode = (MapEntryNode) updatedEntry.getItem();
            Assert.assertEquals("Nodes should be equal", output, inputNode);
        }
    }

    @Before
    public void setUp() {
        String tpId1 = "tp1";
        String value1 = "15";
        filtrator = new TerminationPointPreAggregationFiltrator(topoStoreProvider, NetworkTopologyModel.class);
        topoStoreProvider.initializeStore(TOPOLOGY_ID, false);
        filtrator.addFilter(filter);
        filtrator.setTopologyAggregator(aggregator);

        MapEntryNode tp1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                tpId1).withChild(ImmutableNodes.leafNode(UNNUMBERED_QNAME, value1)).build();
        // input
        nodeValueInput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID)
                .withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withChild(tp1).build()).build();
        underlayItemInput = new UnderlayItem(nodeValueInput, null, TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);

        // output
        nodeValueOutput = ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME,
                NODE_ID).withChild(ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).build()).build();
        underlayItemOutput = new UnderlayItem(nodeValueOutput, null, TOPOLOGY_ID, NODE_ID,
                CorrelationItemEnum.TerminationPoint);
        Mockito.when(filter.getPathIdentifier()).thenReturn(pathIdentifier);
    }

    @Test
    public void testProcessCreatedChangesValid() {
        Mockito.when(filter.isFiltered(Matchers.<NormalizedNode<?, ?>>any())).thenReturn(false);
        aggregator.setOutput(nodeValueInput);
        filtrator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_ID);
    }

    @Test
    public void testProcessCreatedChangesInvalid() {
        Mockito.when(filter.isFiltered(Matchers.<NormalizedNode<?, ?>>any())).thenReturn(true);
        aggregator.setOutput(nodeValueOutput);
        filtrator.processCreatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_ID);
    }

    @Test
    public void testProcessUpdatedChangesValid() {
        Mockito.when(filter.isFiltered(Matchers.<NormalizedNode<?, ?>>any())).thenReturn(false);
        aggregator.setOutput(nodeValueInput);
        filtrator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_ID);
    }

    @Test
    public void testProcessUpdatedChangesInvalid() {
        Mockito.when(filter.isFiltered(Matchers.<NormalizedNode<?, ?>>any())).thenReturn(true);
        aggregator.setOutput(nodeValueOutput);
        filtrator.processUpdatedChanges(nodeYiid, underlayItemInput, TOPOLOGY_ID);
    }
}
