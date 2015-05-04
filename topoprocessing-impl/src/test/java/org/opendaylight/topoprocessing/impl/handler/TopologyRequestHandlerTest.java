/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.CorrelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.equality._case.EqualityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestHandlerTest {

    private static final String TOPOLOGY_1 = "openflow:1";
    private static final String TOPOLOGY_2 = "bgp-node:4";

    @Mock
    DOMDataBroker mockDataBroker;
    @Mock
    PathTranslator mockTranslator;
    @Mock
    SchemaContext mockContext;

    private Topology topology;

    @Before
    public void setUp() {
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology(TOPOLOGY_1);
        mappingBuilder1.setTargetField(new LeafPath("openflow:opf/desc:desc/ip:ip"));
        MappingBuilder mappingBuilder2 = new MappingBuilder();
        mappingBuilder2.setUnderlayTopology(TOPOLOGY_2);
        mappingBuilder2.setTargetField(new LeafPath("bgpnode:bgp/desc:desc/ip:ip"));
        ArrayList<Mapping> mappings = new ArrayList<>();
        mappings.add(mappingBuilder1.build());
        mappings.add(mappingBuilder2.build());
        EqualityBuilder equalityBuilder = new EqualityBuilder();
        equalityBuilder.setMapping(mappings);
        EqualityCaseBuilder caseBuilder = new EqualityCaseBuilder();
        caseBuilder.setEquality(equalityBuilder.build());
        CorrelationBuilder cBuilder = new CorrelationBuilder();
        cBuilder.setName(Equality.class);
        cBuilder.setCorrelationItem(CorrelationItemEnum.Node);
        cBuilder.setCorrelationType(caseBuilder.build());
        ArrayList<Correlation> correlations = new ArrayList<>();
        correlations.add(cBuilder.build());
        CorrelationsBuilder corrsBuilder = new CorrelationsBuilder();
        corrsBuilder.setCorrelation(correlations);
        CorrelationAugmentBuilder augmentBuilder = new CorrelationAugmentBuilder();
        augmentBuilder.setCorrelations(corrsBuilder.build());
        TopologyBuilder topoBuilder = new TopologyBuilder();
        topoBuilder.setTopologyId(new TopologyId("mytopo:1"));
        topoBuilder.addAugmentation(CorrelationAugment.class, augmentBuilder.build());
        topology = topoBuilder.build();
    }

    @Test
    public void test() {
//        TopologyRequestHandler handler = new TopologyRequestHandler(mockDataBroker);
//        handler.setTranslator(mockTranslator);
//
//        YangInstanceIdentifier identifier = YangInstanceIdentifier.builder().node(Node.QNAME).build();
//        Mockito.when(mockTranslator.translate(Matchers.anyString(), Matchers.eq(CorrelationItemEnum.Node),
//                Matchers.any()))
//                    .thenReturn(identifier);
//
//        GlobalSchemaContextHolder contextHolder = new GlobalSchemaContextHolder(mockContext);
//
//        YangInstanceIdentifier nodeIdentifier1 = YangInstanceIdentifier.builder()
//                .node(NetworkTopology.QNAME)
//                .node(Topology.QNAME)
//                .nodeWithKey(Topology.QNAME,
//                        QName.create("(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology-id"),
//                        TOPOLOGY_1)
//                .node(Node.QNAME)
//                .build();
//
//        YangInstanceIdentifier nodeIdentifier2 = YangInstanceIdentifier.builder()
//                .node(NetworkTopology.QNAME)
//                .node(Topology.QNAME)
//                .nodeWithKey(Topology.QNAME,
//                        QName.create("(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology-id"),
//                        TOPOLOGY_2)
//                .node(Node.QNAME)
//                .build();
//
//        handler.processNewRequest(topology);
//
//        Mockito.verify(mockDataBroker).registerDataChangeListener((LogicalDatastoreType) Matchers.any(),
//                Matchers.eq(nodeIdentifier1), (DOMDataChangeListener) Matchers.any(), (DataChangeScope) Matchers.any());
//
//        Mockito.verify(mockDataBroker).registerDataChangeListener((LogicalDatastoreType) Matchers.any(),
//                Matchers.eq(nodeIdentifier2), (DOMDataChangeListener) Matchers.any(), (DataChangeScope) Matchers.any());
//
//        Assert.assertTrue("Size of listeners is wrong", handler.getListeners().size() == 2);
//
//        Assert.assertNull(handler.getListeners().get(0));
    }
}
