/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.nt.request;

import static org.mockito.Matchers.any;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.testUtilities.TestingDOMDataBroker;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.nt.adapter.NTModelAdapter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.FilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.Ipv4AddressFilterTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.ipv4.address.filter.type.Ipv4AddressFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.LinkComputationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.LinkComputation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@RunWith(MockitoJUnitRunner.class)
public class NTTopologyRequestHandlerTest {

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
    @Mock private Map.Entry<InstanceIdentifier<?>,DataObject> mockFromNormalizedNode;

    private YangInstanceIdentifier pathIdentifier;
    private Topology topology;
    private TopologyRequestHandler handler;
    private InstanceIdentifier<?> identifier = InstanceIdentifier.create(Topology.class);

    private PingPongDataBroker pingPongDataBroker;
    private TestingDOMDataBroker testingBroker;

    private abstract class UnknownCorrelationBase extends CorrelationBase {
        //
    }

    @Before
    public void setUp() {
        // initialize mockito RPC service call
        Mockito.when(mockRpcServices.getRpcService()).thenReturn(mockDOMRpcService);
        Mockito.when(mockRpcServices.getRpcService().registerRpcListener((DOMRpcAvailabilityListener) any()))
            .thenReturn(mockDOMRpcAvailabilityListener);
        testingBroker = new TestingDOMDataBroker();
        pingPongDataBroker = new PingPongDataBroker(testingBroker);
        // initialize mockito transaction chain and writer
        Mockito.when(mockDomDataBroker.createTransactionChain((TransactionChainListener) any()))
            .thenReturn(mockDomTransactionChain);
        Mockito.when(mockDomTransactionChain.newWriteOnlyTransaction()).thenReturn(mockDomDataWriteTransaction);
        Mockito.when(mockDomDataWriteTransaction.submit()).thenReturn(domCheckedFuture);
        // initialize read-only transaction for pre-loading datastore
        DOMDataReadOnlyTransaction mockTransaction = Mockito.mock(DOMDataReadOnlyTransaction.class);
        Mockito.when(mockDomDataBroker.newReadOnlyTransaction()).thenReturn(mockTransaction);
        CheckedFuture mockReadFuture = Mockito.mock(CheckedFuture.class);
        Mockito.when(mockTransaction.read((LogicalDatastoreType) Matchers.any(),
                (YangInstanceIdentifier) Matchers.any())).thenReturn(mockReadFuture);
    }

    @Test (expected=NullPointerException.class)
    public void testTopologyWithoutAugmentation() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
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
        CorrelationsBuilder cBuilder = new CorrelationsBuilder();
        cBuilder.setOutputModel(NetworkTopologyModel.class);
        correlationAugmentBuilder.setCorrelations(cBuilder.build());
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
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
        correlationsBuilder.setOutputModel(NetworkTopologyModel.class);
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
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeUnificationCase() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeNodeIpFiltrationCase() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(FiltrationOnly.class, null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithUnknownCorrelationType() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(UnknownCorrelationBase.class, null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationWithMappingsNull() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected=IllegalStateException.class)
    public void testCorrelationItemNull() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology("pcep-topology:1");
        TargetFieldBuilder targetFieldBuider = new TargetFieldBuilder()
                .setTargetFieldPath(
                        new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"))
                .setMatchingKey(0);
        List<TargetField> targetFields = new ArrayList<>(1);
        targetFields.add(targetFieldBuider.build());
        mappingBuilder1.setTargetField(targetFields);
        mappingBuilder1.setInputModel(NetworkTopologyModel.class);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), null);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);

        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.processNewRequest();
    }

    @Test
    public void testConfigurationAndOperationListenerRegistration() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        MappingBuilder mappingBuilder1 = new MappingBuilder();
        mappingBuilder1.setUnderlayTopology("pcep-topology:1");
        TargetFieldBuilder targetFieldBuider = new TargetFieldBuilder()
                .setTargetFieldPath(
                        new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"))
                .setMatchingKey(0);
        List<TargetField> targetFields = new ArrayList<>(1);
        targetFields.add(targetFieldBuider.build());
        mappingBuilder1.setTargetField(targetFields);
        mappingBuilder1.setInputModel(NetworkTopologyModel.class);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(NetworkTopologyModel.class, new NTModelAdapter());
        // CONFIGURATION listener registration
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(DatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest();
        Assert.assertEquals(1, listeners.size());
        // OPERATIONAL listener registration
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);
        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest();
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
        TargetFieldBuilder targetFieldBuider = new TargetFieldBuilder()
                .setTargetFieldPath(
                        new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"))
                .setMatchingKey(0);
        List<TargetField> targetFields = new ArrayList<>(1);
        targetFields.add(targetFieldBuider.build());
        mappingBuilder1.setTargetField(targetFields);
        mappingBuilder1.setInputModel(NetworkTopologyModel.class);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null, aggBuilder.build(),
                CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(NetworkTopologyModel.class, new NTModelAdapter());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest();
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
        filterBuilder1.setInputModel(NetworkTopologyModel.class);
        Ipv4Prefix ipv4prefix = new Ipv4Prefix("192.168.0.1/24");
        IpPrefix ipPrefix = new IpPrefix(ipv4prefix);
        filterBuilder1.setFilterType(Ipv4Address.class);
        Ipv4AddressFilterBuilder filterBuilder = new Ipv4AddressFilterBuilder();
        filterBuilder.setIpv4Address(ipPrefix);

        filterBuilder1.setFilterTypeBody(new Ipv4AddressFilterTypeBuilder().setIpv4AddressFilter(filterBuilder.build())
                .build());
        filters.add(filterBuilder1.build());
        fBuilder.setFilter(filters);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(FiltrationOnly.class,
                fBuilder.build(), null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(NetworkTopologyModel.class, new NTModelAdapter());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);
        handler.setFiltrators(DefaultFiltrators.getDefaultFiltrators());
        pathIdentifier = InstanceIdentifiers.NODE_IDENTIFIER;
        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest();
        Assert.assertEquals(1, listeners.size());
        // test registration CONFIGURATION listener registration
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(DatastoreType.CONFIGURATION);
        handler.setFiltrators(DefaultFiltrators.getDefaultFiltrators());
        pathIdentifier = InstanceIdentifiers.NODE_IDENTIFIER;
        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest();
        Assert.assertEquals(2, listeners.size());
    }

    @Test
    public void testCloseListeners() {
        testUnificationCase();

        handler.processDeletionRequest(0);
        Assert.assertTrue("Listener wasn't closed", testingBroker.getListenerClosed());
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
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new NTTopologyRequestHandler(pingPongDataBroker, mockSchemaHolder, mockRpcServices,
                (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setDatastoreType(DatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataBroker.registerDataChangeListener(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                (YangInstanceIdentifier) any(), (DOMDataChangeListener) any(),
                Mockito.eq(DataChangeScope.SUBTREE)))
                .thenReturn(mockDOMDataChangeListener);
        handler.processNewRequest();
        Assert.assertEquals(0, listeners.size());
        // process deletion request
        handler.processDeletionRequest(0);
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

    @Test
    public void testGetLinkComputation() {
        Topology mockTopology = Mockito.mock(Topology.class);
        LinkComputationAugment mockLinkCompAug = Mockito.mock(LinkComputationAugment.class);
        LinkComputation mockLinkComputation = Mockito.mock(LinkComputation.class);

        Mockito.when(mockFromNormalizedNode.getValue()).thenReturn(mockTopology);
        Mockito.when(mockTopology.getAugmentation(any(Class.class))).thenReturn(mockLinkCompAug);
        Mockito.when(mockLinkCompAug.getLinkComputation()).thenReturn(mockLinkComputation);

        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        aggBuilder.setMapping(mappings);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class,
                null, aggBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        NTTopologyRequestHandler handler = new NTTopologyRequestHandler(pingPongDataBroker,
                mockSchemaHolder, mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);

        Assert.assertEquals(handler.getLinkComputation(mockFromNormalizedNode), mockLinkComputation);

        Mockito.when(mockTopology.getAugmentation(any(Class.class))).thenReturn(null);
        Assert.assertEquals(handler.getLinkComputation(mockFromNormalizedNode), null);
    }

}
