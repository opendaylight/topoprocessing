/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;

import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.List;

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
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongTransactionChain;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv4AddressAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv4AddressAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.CorrelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Aggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.AggregationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Filtration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.FiltrationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.FilterBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.util.concurrent.CheckedFuture;

@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestHandlerTest {

    private static final String TOPO1 = "TOPO1";

    @Mock private DOMDataBroker mockDomDataBroker;
    @Mock private PathTranslator mockTranslator;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private RpcServices mockRpcServices;
    @Mock private ListenerRegistration<DOMRpcAvailabilityListener> mockDOMRpcAvailabilityListener;
    @Mock private DOMRpcService mockDOMRpcService;
    @Mock private DOMTransactionChain mockDomTransactionChain;
    @Mock private DOMDataWriteTransaction mockDomDataWriteTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> domCheckedFuture;
    @Mock private SchemaContext mockGlobalSchemaContext;
    @Mock private ListenerRegistration<DOMDataChangeListener> mockDOMDataChangeListener;
    @Mock private TransactionChainListener listener;

    private YangInstanceIdentifier pathIdentifier;
    private Topology topology;
    private TopologyRequestHandler handler;

    private PingPongDataBroker pingPongDataBroker;

    private abstract class UnknownCorrelationBase extends CorrelationBase {
        //
    }

    @Before
    public void setUp() {
      // initialize mockito RPC service call
      Mockito.when(mockRpcServices.getRpcService()).thenReturn(mockDOMRpcService);
      Mockito.when(mockRpcServices.getRpcService().registerRpcListener((DOMRpcAvailabilityListener) any()))
          .thenReturn(mockDOMRpcAvailabilityListener);
      pingPongDataBroker = new PingPongDataBroker(mockDomDataBroker);
      
      Mockito.when(mockDomDataBroker.createTransactionChain((TransactionChainListener) any()))
          .thenReturn(mockDomTransactionChain);
      Mockito.when(mockDomTransactionChain.newWriteOnlyTransaction()).thenReturn(mockDomDataWriteTransaction);
      Mockito.when(mockDomDataWriteTransaction.submit()).thenReturn(domCheckedFuture);
      // initialize read-only transaction for pre-loading datastore
      DOMDataReadOnlyTransaction mockTransaction = Mockito.mock(DOMDataReadOnlyTransaction.class);
      Mockito.when(pingPongDataBroker.newReadOnlyTransaction()).thenReturn(mockTransaction);
      CheckedFuture mockReadFuture = Mockito.mock(CheckedFuture.class);
      Mockito.when(mockTransaction.read((LogicalDatastoreType) Matchers.any(),
              (YangInstanceIdentifier) Matchers.any())).thenReturn(mockReadFuture);
    }

    @Test (expected=NullPointerException.class)
    public void testProcessNewRequestWithNullParameter() {
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(null);
    }

    @Test (expected=IllegalStateException.class)
    public void testTopologyWithoutAugmentation() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);

        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    private static TopologyBuilder createTopologyBuilder(String topologyName) {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(topologyName);
        topoBuilder.setTopologyId(topologyId);
        return topoBuilder;
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationMissingInAugment() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = new CorrelationAugmentBuilder();
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    private static CorrelationAugmentBuilder createCorrelation(Class<? extends CorrelationBase> correlationBase,
            Filtration filtration, Aggregation aggregation, CorrelationItemEnum correlationItem) {
        CorrelationBuilder cBuilder = new CorrelationBuilder();
        cBuilder.setType(correlationBase);
        cBuilder.setAggregation(aggregation);
        cBuilder.setFiltration(filtration);
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
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeUnificationCase() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeNodeIpFiltrationCase() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(FiltrationOnly.class, null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithUnknownCorrelationType() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(UnknownCorrelationBase.class, null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithMappingsNull() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationItemNull() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology("pcep-topology:1");
        mappingBuilder1.setTargetField(new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"));
        mappingBuilder1.setInputModel(Model.NetworkTopology);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), null);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);

        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Model) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.processNewRequest(topoBuilder.build());
    }

    @Test
    public void testConfigurationAndOperationListenerRegistration() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology("pcep-topology:1");
        mappingBuilder1.setTargetField(new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"));
        mappingBuilder1.setInputModel(Model.NetworkTopology);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        // CONFIGURATION listener registration
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.setDatastoreType(DatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Model) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest(topoBuilder.build());
        Assert.assertEquals(1, listeners.size());
        // OPERATIONAL listener registration
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);
        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Model) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest(topoBuilder.build());
        Assert.assertEquals(1, listeners.size());
    }


    @Test
    public void testUnificationCase() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Unification.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology("pcep-topology:1");
        mappingBuilder1.setTargetField(new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"));
        mappingBuilder1.setInputModel(Model.NetworkTopology);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null, aggBuilder.build(),
                CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker,
                mockSchemaHolder, mockRpcServices);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Model) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest(topoBuilder.build());
        Assert.assertEquals(1, listeners.size());
    }

    @Test
    public void testIpv4AddressFiltration() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        FiltrationBuilder fBuilder = new FiltrationBuilder();
        fBuilder.setUnderlayTopology("pcep-topology:1");
        ArrayList<Filter> filters = new ArrayList<>();
        FilterBuilder filterBuilder1 = new FilterBuilder();
        filterBuilder1.setTargetField(new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"));
        filterBuilder1.setInputModel(Model.NetworkTopology);
        Ipv4Prefix ipv4prefix = new Ipv4Prefix("192.168.0.1/24");
        IpPrefix ipPrefix = new IpPrefix(ipv4prefix);
        filterBuilder1.setFilterType(Ipv4Address.class);
        Ipv4AddressAugmentBuilder augmentBuilder = new Ipv4AddressAugmentBuilder();
        augmentBuilder.setIpv4Address(ipPrefix);

        filterBuilder1.addAugmentation(Ipv4AddressAugment.class, augmentBuilder.build());
        filters.add(filterBuilder1.build());
        fBuilder.setFilter(filters);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(FiltrationOnly.class,
                fBuilder.build(), null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);
        handler.setFiltrators(DefaultFiltrators.getDefaultFiltrators());
        pathIdentifier = InstanceIdentifiers.NODE_IDENTIFIER;
        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Model) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest(topoBuilder.build());
        Assert.assertEquals(1, listeners.size());
        // test registration CONFIGURATION listener registration
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.setDatastoreType(DatastoreType.CONFIGURATION);
        handler.setFiltrators(DefaultFiltrators.getDefaultFiltrators());
        pathIdentifier = InstanceIdentifiers.NODE_IDENTIFIER;
        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Model) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest(topoBuilder.build());
        Assert.assertEquals(2, listeners.size());
    }

    @Test
    public void testCloseListeners() {
        testUnificationCase();

        handler.processDeletionRequest();
        Mockito.verify(mockDOMDataChangeListener, Mockito.times(1)).close();
        Assert.assertEquals(0, handler.getListeners().size());
    }

    @Test
    public void testDeletionWithEmptyListener() {
        // pre-testing setup
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        handler = new TopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices);
        handler.setDatastoreType(DatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Model) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest(topoBuilder.build());
        Assert.assertEquals(0, listeners.size());
        // process deletion request
        handler.processDeletionRequest();
    }

    @Test
    public void testDeletionWithTransactionChainNull() {
        Mockito.when(mockDomDataBroker.createTransactionChain((TransactionChainListener) any()))
            .thenReturn(null);
        testDeletionWithEmptyListener();
    }

    @Test
    public void testDeletionWithTransactionChainClosed() {
        Mockito.doThrow(new TransactionChainClosedException("The chain has been closed"))
            .when(mockDomTransactionChain).close();
        testDeletionWithEmptyListener();
    }
}
