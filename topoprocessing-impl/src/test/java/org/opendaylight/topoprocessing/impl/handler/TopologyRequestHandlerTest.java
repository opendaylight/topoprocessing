/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NodeIpFiltration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.CorrelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.CorrelationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.equality._case.EqualityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.util.concurrent.CheckedFuture;

@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestHandlerTest {

    private static final String TOPOLOGY_1 = "openflow:1";
    private static final String TOPOLOGY_2 = "bgp-node:4";
    private static final String TOPO1 = "TOPO1";

    @Mock private DOMDataBroker mockDomDataBroker;
    @Mock private PathTranslator mockTranslator;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private RpcServices mockRpcServices;
    @Mock private ListenerRegistration<DOMRpcAvailabilityListener> mockListenerRegistration;
    @Mock private DOMRpcService mockDOMRpcService;
    @Mock private DOMTransactionChain mockDomTransactionChain;
    @Mock private DOMDataWriteTransaction mockDomDataWriteTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> domCheckedFuture;
    @Mock private SchemaContext mockGlobalSchemaContext;


    private YangInstanceIdentifier pathIdentifier;

    private Topology topology;

    private abstract class UnknownCorrelationBase extends CorrelationBase {
        //
    }

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

        // initialize mockito RPC service call
        Mockito.when(mockRpcServices.getRpcService()).thenReturn(mockDOMRpcService);
        Mockito.when(mockRpcServices.getRpcService().registerRpcListener((DOMRpcAvailabilityListener) Mockito.any()))
            .thenReturn(mockListenerRegistration);
        // initialize mockito transaction chain and writer
        Mockito.when(mockDomDataBroker.createTransactionChain((TransactionChainListener) Mockito.any()))
        .thenReturn(mockDomTransactionChain);
        Mockito.when(mockDomTransactionChain.newWriteOnlyTransaction()).thenReturn(mockDomDataWriteTransaction);
        Mockito.when(mockDomDataWriteTransaction.submit()).thenReturn(domCheckedFuture);
        
        //Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(mockGlobalSchemaContext);

    }



    @Test (expected=NullPointerException.class)
    public void testProcessNewRequestWithNullParameter() {
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(null);
    }

    @Test (expected=IllegalStateException.class)
    public void testTopologyWithoutAugmentation() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);

        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationMissingInAugment() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        CorrelationAugmentBuilder correlationAugmentBuilder = new CorrelationAugmentBuilder();
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    private static CorrelationAugmentBuilder createCorrelation(Class<? extends CorrelationBase> correlationBase,
            CorrelationType correlationType, CorrelationItemEnum correlationItem) {
        CorrelationBuilder cBuilder = new CorrelationBuilder();
        cBuilder.setName(correlationBase);
        cBuilder.setCorrelationType(correlationType);
        cBuilder.setCorrelationItem(correlationItem);

        List<Correlation> correlations = new ArrayList<>();
        correlations.add(cBuilder.build());
        CorrelationsBuilder correlationsBuilder = new CorrelationsBuilder();
        correlationsBuilder.setCorrelation(correlations);
        CorrelationAugmentBuilder correlationAugmentBuilder = new CorrelationAugmentBuilder();
        correlationAugmentBuilder.setCorrelations(correlationsBuilder.build());
        return correlationAugmentBuilder;
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeEqualityCase() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(Equality.class, null,
                CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeUnificationCase() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(Unification.class, null,
                CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeNodeIpFiltrationCase() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(NodeIpFiltration.class, null,
                CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithUnknownCorrelationType() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(UnknownCorrelationBase.class, null,
                CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithMappingsNull() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        EqualityCaseBuilder caseBuilder = new EqualityCaseBuilder();
        EqualityBuilder equalityBuilder = new EqualityBuilder();
        caseBuilder.setEquality(equalityBuilder.build());
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(Equality.class, caseBuilder.build(),
                CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test
    public void testCorrelationItemNotSet() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        EqualityCaseBuilder caseBuilder = new EqualityCaseBuilder();
        EqualityBuilder equalityBuilder = new EqualityBuilder();
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology("pcep-topology:1");
        mappingBuilder1.setTargetField(new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"));
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        equalityBuilder.setMapping(mappings);
        caseBuilder.setEquality(equalityBuilder.build());
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(Equality.class, caseBuilder.build(), null);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);


        Mockito.when(mockTranslator.translate((String) Mockito.any(), (CorrelationItemEnum) Mockito.any(),
                (GlobalSchemaContextHolder) Mockito.any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test
    public void test2() {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(TOPO1);
        topoBuilder.setTopologyId(topologyId);
        EqualityCaseBuilder caseBuilder = new EqualityCaseBuilder();
        EqualityBuilder equalityBuilder = new EqualityBuilder();
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology("pcep-topology:1");
        mappingBuilder1.setTargetField(new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"));
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        equalityBuilder.setMapping(mappings);
        caseBuilder.setEquality(equalityBuilder.build());
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(Equality.class, caseBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        TopologyRequestHandler handler = new TopologyRequestHandler(mockDomDataBroker,
                mockSchemaHolder, mockRpcServices);


        Mockito.when(mockTranslator.translate((String) Mockito.any(), (CorrelationItemEnum) Mockito.any(),
                (GlobalSchemaContextHolder) Mockito.any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.processNewRequest(topoBuilder.build());
    }

    

    @Test
    public void test3() {
        //datastoretype OPERATIONAL alebo CONFIGURATION
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
