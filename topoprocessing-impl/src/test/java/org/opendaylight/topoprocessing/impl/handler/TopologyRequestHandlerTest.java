/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.CorrelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.equality._case.EqualityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author michal.polkorab
 *
 */
public class TopologyRequestHandlerTest {

    @Mock
    DOMDataBroker mockDataBroker;

    @Mock
    PathTranslator mockTranslator;

    @Mock
    SchemaContext mockContext;
    private Topology topology;

    @Before
    public void startup() {
        MockitoAnnotations.initMocks(this);
        TopologyBuilder topoBuilder = new TopologyBuilder();
        topoBuilder.setTopologyId(new TopologyId("mytopo:1"));
        CorrelationAugmentBuilder augmentBuilder = new CorrelationAugmentBuilder();
        CorrelationsBuilder corrsBuilder = new CorrelationsBuilder();
        ArrayList<Correlation> correlations = new ArrayList<>();
        CorrelationBuilder cBuilder = new CorrelationBuilder();
        cBuilder.setName(Equality.class);
        cBuilder.setCorrelationItem(CorrelationItemEnum.Node);
        EqualityCaseBuilder caseBuilder = new EqualityCaseBuilder();
        EqualityBuilder equalityBuilder = new EqualityBuilder();
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.setUnderlayTopology("openflow:1");
        mappingBuilder.setTargetField(new LeafPath("bgpnode:bgp/desc:desc/ip:ip"));
        mappings.add(mappingBuilder.build());
        equalityBuilder.setMapping(mappings);
        caseBuilder.setEquality(equalityBuilder.build());
        cBuilder.setCorrelationType(caseBuilder.build());
        correlations.add(cBuilder.build());
        corrsBuilder.setCorrelation(correlations);
        augmentBuilder.setCorrelations(corrsBuilder.build());
        topoBuilder.addAugmentation(CorrelationAugment.class, augmentBuilder.build());
        topology = topoBuilder.build();
    }

    @Test(expected=NullPointerException.class)
    public void test() {
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDataBroker);
        handler.setTranslator(mockTranslator);
        YangInstanceIdentifier identifier = YangInstanceIdentifier.builder().node(Node.QNAME).build();
        YangInstanceIdentifier identifier2 = YangInstanceIdentifier.builder().node(Node.QNAME).build();
        YangInstanceIdentifier identifier3 = YangInstanceIdentifier.builder().node(Node.QNAME).build();
        
        GlobalSchemaContextHolder.setSchemaContext(mockContext);
        
        try {
            nejakyKod.hodException();
            Assert.fail();
        } catch (Exception e) {
            // success - expected behavior
        }
        Mockito.when(mockTranslator.translate(Matchers.anyString())).thenReturn(null, identifier2, identifier3);
        
        
        Mockito.verify(mockDataBroker.registerDataChangeListener((LogicalDatastoreType) Matchers.any(),
                (YangInstanceIdentifier) Matchers.any(), (DOMDataChangeListener) Matchers.any(), (DataChangeScope)Matchers.any())
                , Mockito.times(5));
        
        handler.processNewRequest(topology);
        
    }

    @Test
    public void testa() {
        GlobalSchemaContextHolder.setSchemaContext(mockContext);
        System.out.println("Equals: " + mockContext.equals(GlobalSchemaContextHolder.getSchemaContext()));
        
    }
}
