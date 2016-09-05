/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventoryRendering.request;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.testUtilities.TestingDOMDataBroker;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.inventoryRendering.adapter.IRModelAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.InventoryRenderingModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.RenderingOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.CorrelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Rendering;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.RenderingBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;

/**
 *
 * @author matej.perina
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class IRTopologyRequestHandlerTest {

    private static final String TOPO1 = "TOPO1";

    @Mock private DOMDataBroker mockDomDataBroker;
    @Mock private DOMDataTreeChangeService mockDomDataTreeChangeService;
    @Mock private PathTranslator mockTranslator;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private RpcServices mockRpcServices;
    @Mock private ListenerRegistration<DOMRpcAvailabilityListener> mockDOMRpcAvailabilityListener;
    @Mock private DOMRpcService mockDOMRpcService;
    @Mock private DOMTransactionChain mockTransactionChain;
    @Mock private DOMDataWriteTransaction mockDomDataWriteTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> domCheckedFuture;
    @Mock private SchemaContext mockGlobalSchemaContext;
    @Mock private ListenerRegistration<DOMDataTreeChangeListener> mockDOMDataTreeChangeListenerRegistrations;
    @Mock private Map.Entry<InstanceIdentifier<?>,DataObject> mockFromNormalizedNode;

    private YangInstanceIdentifier pathIdentifier;
    private Topology topology;
    private TopologyRequestHandler handler;
    private final InstanceIdentifier<?> identifier = InstanceIdentifier.create(Topology.class);
    private static Class<? extends Model> IRModel = InventoryRenderingModel.class;
    private PingPongDataBroker pingPongDataBroker;
    private TestingDOMDataBroker testingBroker;

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
            .thenReturn(mockTransactionChain);
        Mockito.when(mockTransactionChain.newWriteOnlyTransaction()).thenReturn(mockDomDataWriteTransaction);
        Mockito.when(mockDomDataWriteTransaction.submit()).thenReturn(domCheckedFuture);
        // initialize read-only transaction for pre-loading datastore
        DOMDataReadOnlyTransaction mockTransaction = Mockito.mock(DOMDataReadOnlyTransaction.class);
        Mockito.when(mockDomDataBroker.newReadOnlyTransaction()).thenReturn(mockTransaction);
        CheckedFuture mockReadFuture = Mockito.mock(CheckedFuture.class);
        Mockito.when(mockTransaction.read((LogicalDatastoreType) any(),
                (YangInstanceIdentifier) any())).thenReturn(mockReadFuture);
        Mockito.when(mockDomDataTreeChangeService.registerDataTreeChangeListener(any(), any()))
            .thenReturn(mockDOMDataTreeChangeListenerRegistrations);
    }

    @Test (expected = NullPointerException.class)
    public void testTopologyWithoutAugmentation() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    private static TopologyBuilder createTopologyBuilder(String topologyName) {
        TopologyBuilder topoBuilder = new TopologyBuilder();
        TopologyId topologyId = new TopologyId(topologyName);
        topoBuilder.setTopologyId(topologyId);
        return topoBuilder;
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationMissingInAugment() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = new CorrelationAugmentBuilder();
        CorrelationsBuilder cBuilder = new CorrelationsBuilder();
        cBuilder.setOutputModel(IRModel);
        correlationAugmentBuilder.setCorrelations(cBuilder.build());
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    private static CorrelationAugmentBuilder createCorrelation(Class<? extends CorrelationBase> correlationBase,
            Rendering rendering, CorrelationItemEnum correlationItem) {
        CorrelationBuilder cBuilder = new CorrelationBuilder();
        cBuilder.setType(correlationBase);
        cBuilder.setRendering(rendering);
        cBuilder.setCorrelationItem(correlationItem);

        List<Correlation> correlations = new ArrayList<>();
        correlations.add(cBuilder.build());
        CorrelationsBuilder correlationsBuilder = new CorrelationsBuilder();
        correlationsBuilder.setCorrelation(correlations);
        correlationsBuilder.setOutputModel(IRModel);
        CorrelationAugmentBuilder correlationAugmentBuilder = new CorrelationAugmentBuilder();
        correlationAugmentBuilder.setCorrelations(correlationsBuilder.build());
        return correlationAugmentBuilder;
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithoutCorrelationType() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(null,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithoutCorrelationDataRenderingCase() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(RenderingOnly.class,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationWithUnknownCorrelationType() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(CorrelationBase.class,
                null, CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.processNewRequest();
    }

    @Test (expected = IllegalStateException.class)
    public void testCorrelationItemNull() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        RenderingBuilder renderingBuilder = new RenderingBuilder();
        renderingBuilder.setUnderlayTopology("flow:1");
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(RenderingOnly.class,
                renderingBuilder.build(), null);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);

        Mockito.when(mockTranslator.translate((String) any(), (CorrelationItemEnum) any(),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.processNewRequest();
    }

    @Test
    public void testRenderingCase() {
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        RenderingBuilder renderingBuilder = new RenderingBuilder();
        renderingBuilder.setUnderlayTopology("flow:1");
        renderingBuilder.setInputModel(IRModel);
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(RenderingOnly.class,
                renderingBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(IRModel, new IRModelAdapter());
        // CONFIGURATION listener registration
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataTreeChangeService.registerDataTreeChangeListener(any(), any()))
            .thenReturn(mockDOMDataTreeChangeListenerRegistrations);
        handler.processNewRequest();
        Assert.assertEquals(2, listeners.size());
        // OPERATIONAL listener registration
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.OPERATIONAL);
        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        handler.setListeners(listeners);
        Mockito.when(mockDomDataTreeChangeService.registerDataTreeChangeListener(any(), any()))
            .thenReturn(mockDOMDataTreeChangeListenerRegistrations);
        handler.processNewRequest();
        Assert.assertEquals(4, listeners.size());
    }

    @Test
    public void testCloseListeners() {
        testRenderingCase();

        handler.processDeletionRequest(0);
        Mockito.verify(mockDOMDataTreeChangeListenerRegistrations, Mockito.times(4)).close();
        Assert.assertEquals(0, handler.getListeners().size());
    }

    @Test (expected = IllegalStateException.class)
    public void testRenderingWithoutRenderingData() {
        // pre-testing setup
        TopologyBuilder topoBuilder = createTopologyBuilder(TOPO1);
        RenderingBuilder renderingBuilder = new RenderingBuilder();
        CorrelationAugmentBuilder correlationAugmentBuilder = createCorrelation(RenderingOnly.class,
                renderingBuilder.build(), CorrelationItemEnum.Node);
        topoBuilder.addAugmentation(CorrelationAugment.class, correlationAugmentBuilder.build());
        Entry<?, DataObject> entry = Maps.immutableEntry(identifier, (DataObject) topoBuilder.build());
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        modelAdapters.put(IRModel, new IRModelAdapter());
        handler = new IRTopologyRequestHandler(pingPongDataBroker, mockDomDataTreeChangeService, mockSchemaHolder,
                mockRpcServices, (Entry<InstanceIdentifier<?>, DataObject>) entry);
        handler.setModelAdapters(modelAdapters);
        handler.setDatastoreType(LogicalDatastoreType.CONFIGURATION);

        Mockito.when(mockTranslator.translate((String) any(), Mockito.eq(CorrelationItemEnum.Node),
                (GlobalSchemaContextHolder) any(), (Class<? extends Model>) any())).thenReturn(pathIdentifier);
        handler.setTranslator(mockTranslator);
        List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
        handler.setListeners(listeners);
        Mockito.when(mockDomDataTreeChangeService.registerDataTreeChangeListener(any(), any()))
            .thenReturn(mockDOMDataTreeChangeListenerRegistrations);
        handler.processNewRequest();
    }
}
