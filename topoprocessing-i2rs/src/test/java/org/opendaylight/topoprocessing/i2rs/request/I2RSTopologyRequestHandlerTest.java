/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.request;

import static org.mockito.Matchers.any;

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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.i2rs.adapter.I2RSModelAdapter;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.testUtilities.TestingDOMDataBroker;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsCorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsCorrelationAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv4Address;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.FilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.Ipv4AddressFilterTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.ipv4.address.filter.type.Ipv4AddressFilterBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author andrej.zan
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class I2RSTopologyRequestHandlerTest {

    private static final String TOPO1 = "TOPO1";

    @Mock private PathTranslator mockTranslator;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private RpcServices mockRpcServices;
    @Mock private ListenerRegistration<DOMRpcAvailabilityListener> mockDOMRpcAvailabilityListener;
    @Mock private DOMRpcService mockDOMRpcService;
    @Mock private DOMTransactionChain mockDomTransactionChain;
    @Mock private DOMDataWriteTransaction mockDomDataWriteTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> domCheckedFuture;
    @Mock private SchemaContext mockGlobalSchemaContext;
    @Mock private ListenerRegistration<DOMDataTreeChangeListener> mockDOMDataTreeChangeListener;
    @Mock private DOMDataTreeChangeService mockDomDataTreeChangeService;
    @Mock private Map.Entry<InstanceIdentifier<?>,DataObject> mockFromNormalizedNode;

    private YangInstanceIdentifier pathIdentifier;
    private TopologyRequestHandler handler;
    private InstanceIdentifier<?> identifier = InstanceIdentifier.create(Network.class);

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
        Mockito.when(mockDomTransactionChain.newWriteOnlyTransaction()).thenReturn(mockDomDataWriteTransaction);
        Mockito.when(mockDomDataWriteTransaction.submit()).thenReturn(domCheckedFuture);
        // initialize read-only transaction for pre-loading datastore
        DOMDataReadOnlyTransaction mockTransaction = Mockito.mock(DOMDataReadOnlyTransaction.class);
        CheckedFuture mockReadFuture = Mockito.mock(CheckedFuture.class);
        Mockito.when(mockTransaction.read((LogicalDatastoreType) Matchers.any(),
                (YangInstanceIdentifier) Matchers.any())).thenReturn(mockReadFuture);
    }

    @Test (expected = NullPointerException.class)
    public void testTopologyWithoutAugmentation() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    private static NetworkBuilder createNetworkBuilder(String topologyName) {
        NetworkBuilder networkBuilder = new NetworkBuilder();
        NetworkId networkId = new NetworkId(topologyName);
        networkBuilder.setNetworkId(networkId);
        return networkBuilder;
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationMissingInAugment() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = new I2rsCorrelationAugmentBuilder();
        CorrelationsBuilder cBuilder = new CorrelationsBuilder();
        cBuilder.setOutputModel(I2rsModel.class);
        correlationAugmentBuilder.setCorrelations(cBuilder.build());
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    private static I2rsCorrelationAugmentBuilder createCorrelation(Class<? extends CorrelationBase> correlationBase,
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
        correlationsBuilder.setOutputModel(I2rsModel.class);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = new I2rsCorrelationAugmentBuilder();
        correlationAugmentBuilder.setCorrelations(correlationsBuilder.build());
        return correlationAugmentBuilder;
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeEqualityCase() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                null, CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeUnificationCase() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                null, CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithoutCorrelationTypeNodeIpFiltrationCase() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(FiltrationOnly.class, null,
                null, CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithUnknownCorrelationType() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(UnknownCorrelationBase.class, null,
                null, CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithMappingsNull() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationItemNull() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
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
        mappingBuilder1.setInputModel(I2rsModel.class);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), null);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);

        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.processNewRequest();
    }

    @Test
    public void testConfigurationAndOperationListenerRegistration() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
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
        mappingBuilder1.setInputModel(I2rsModel.class);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(I2rsModel.class, new I2RSModelAdapter());
        // CONFIGURATION listener registration
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        handler.processNewRequest();
        Assert.assertEquals(1, listeners.size());
        // OPERATIONAL listener registration
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.OPERATIONAL);
        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        listeners = new ArrayList<>();
        handler.setListeners(listeners);
        handler.processNewRequest();
        Assert.assertEquals(1, listeners.size());
    }


    @Test
    public void testUnificationCase() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
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
        mappingBuilder1.setInputModel(I2rsModel.class);
        mappingBuilder1.setAggregateInside(false);
        mappings.add(mappingBuilder1.build());
        aggBuilder.setMapping(mappings);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(I2rsModel.class, new I2RSModelAdapter());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.OPERATIONAL);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        handler.processNewRequest();
        Assert.assertEquals(1, listeners.size());
    }

    @Test
    public void testIpv4AddressFiltration() {
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        FiltrationBuilder fBuilder = new FiltrationBuilder();
        fBuilder.setUnderlayTopology("pcep-topology:1");
        ArrayList<Filter> filters = new ArrayList<>();
        FilterBuilder filterBuilder1 = new FilterBuilder();
        filterBuilder1.setTargetField(
                new LeafPath("network-topology-pcep:path-computation-client/network-topology-pcep:ip-address"));
        filterBuilder1.setInputModel(I2rsModel.class);
        Ipv4Prefix ipv4prefix = new Ipv4Prefix("192.168.0.1/24");
        IpPrefix ipPrefix = new IpPrefix(ipv4prefix);
        filterBuilder1.setFilterType(Ipv4Address.class);
        Ipv4AddressFilterBuilder filterBuilder = new Ipv4AddressFilterBuilder();
        filterBuilder.setIpv4Address(ipPrefix);

        filterBuilder1.setFilterTypeBody(new Ipv4AddressFilterTypeBuilder().setIpv4AddressFilter(filterBuilder.build())
                .build());
        filters.add(filterBuilder1.build());
        fBuilder.setFilter(filters);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(FiltrationOnly.class,
                fBuilder.build(), null, CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(I2rsModel.class, new I2RSModelAdapter());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.OPERATIONAL);
        handler.setFiltrators(DefaultFiltrators.getDefaultFiltrators());
        pathIdentifier = InstanceIdentifiers.NODE_IDENTIFIER;
        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        handler.processNewRequest();
        Assert.assertEquals(1, listeners.size());
        // test registration CONFIGURATION listener registration
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.CONFIGURATION);
        handler.setFiltrators(DefaultFiltrators.getDefaultFiltrators());
        pathIdentifier = InstanceIdentifiers.NODE_IDENTIFIER;
        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.setListeners(listeners);
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
        NetworkBuilder networkBuilder = createNetworkBuilder(TOPO1);
        AggregationBuilder aggBuilder = new AggregationBuilder();
        aggBuilder.setAggregationType(Equality.class);
        ArrayList<Mapping> mappings = new ArrayList<>();
        aggBuilder.setMapping(mappings);
        I2rsCorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(AggregationOnly.class, null,
                aggBuilder.build(), CorrelationItemEnum.Node);
        networkBuilder.addAugmentation(I2rsCorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) networkBuilder.build());
        handler = new I2RSTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setDatastoreType(LogicalDatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        handler.processNewRequest();
        Assert.assertEquals(0, listeners.size());
        // process deletion request
        handler.processDeletionRequest(0);
    }

    @Test
    public void testDeletionWithTransactionChainNull() {
        testDeletionWithEmptyListener();
    }

    @Test
    public void testDeletionWithTransactionChainClosed() {
        Mockito.doThrow(new TransactionChainClosedException("The chain has been closed"))
            .when(mockDomTransactionChain).close();
        testDeletionWithEmptyListener();
    }
}
