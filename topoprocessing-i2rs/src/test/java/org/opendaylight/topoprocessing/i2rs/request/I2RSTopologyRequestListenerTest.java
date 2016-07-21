/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.request;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractCheckedFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.i2rs.adapter.I2RSModelAdapter;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.DefaultFiltrators;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsCorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;

/**
 * @author andrej.zan
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class I2RSTopologyRequestListenerTest {

    private TopologyRequestListener listener;
    private static final String TOPO_NAME = "mytopo:1";

    @Mock private DOMDataBroker mockBroker;
    @Mock private BindingNormalizedNodeSerializer mockNodeSerializer;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private RpcServices mockRpcServices;
    @Mock private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> mockChange;
    @Mock private DOMTransactionChain mockTransactionChain;
    @Mock private DOMDataWriteTransaction mockTransaction;
    @Mock private UserDefinedFilter userDefinedFilter;
    @Mock private FiltratorFactory userDefinedFiltratorFactory;
    private Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
    private PingPongDataBroker pingPongBroker;

    private class UserDefinedFilter extends FilterBase
    {
        // testing class for testing purpose
    }

    @Before
    public void setUp() {
        pingPongBroker = new PingPongDataBroker(mockBroker);
        modelAdapters.put(I2rsModel.class, new I2RSModelAdapter());
        listener = new I2RSTopologyRequestListener(pingPongBroker, mockNodeSerializer, mockSchemaHolder,
                mockRpcServices, modelAdapters);
        listener.setDatastoreType(DatastoreType.OPERATIONAL);

        Mockito.when(mockRpcServices.getRpcService()).thenReturn(Mockito.mock(DOMRpcService.class));
        Mockito.when(mockBroker.createTransactionChain((TransactionChainListener) Matchers.any())).thenReturn(
                mockTransactionChain);
        Mockito.when(mockTransactionChain.newWriteOnlyTransaction()).thenReturn(mockTransaction);
        Mockito.when(mockTransaction.submit()).thenReturn(Mockito.mock(AbstractCheckedFuture.class));
    }

    @Test
    public void testCreateCorrectNode() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(Network.QNAME)
                .nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPO_NAME).build();
        MapEntryNode node = ImmutableNodes.mapEntryBuilder(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME,
                TOPO_NAME).addChild(createAugNode()).build();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = new HashMap<>();
        map.put(yiid, node);
        Mockito.when(mockChange.getCreatedData()).thenReturn(map);
        // augmentation
        I2rsCorrelationAugment mockCorrelationAugument = Mockito.mock(I2rsCorrelationAugment.class);
        Correlations mockCorrelations = Mockito.mock(Correlations.class);
        Mockito.when(mockCorrelationAugument.getCorrelations()).thenReturn(mockCorrelations);
        Mockito.when(mockCorrelations.getCorrelation()).thenReturn(new ArrayList<Correlation>());
        Answer<Class<? extends Model>> answer = new Answer<Class<? extends Model>>() {
            @Override
            public Class<? extends Model> answer(InvocationOnMock invocation)
                    throws Throwable {
                return I2rsModel.class;
            }
        };
        Mockito.when(mockCorrelations.getOutputModel()).then(answer);
        // topology
        NetworkBuilder networkBuilder = new NetworkBuilder();
        NetworkId networkId = NetworkId.getDefaultInstance(TOPO_NAME);
        Network topology = networkBuilder.setKey(new NetworkKey(networkId))
                .setNetworkId(networkId)
                .addAugmentation(I2rsCorrelationAugment.class, mockCorrelationAugument)
                .build();
        Map.Entry<? extends InstanceIdentifier<?>, DataObject> topoEntry = Maps.immutableEntry(
                (InstanceIdentifier<?>) Mockito.mock(InstanceIdentifier.class), (DataObject) topology);
        Mockito.when(mockNodeSerializer.fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any())).thenReturn(
                (Map.Entry<InstanceIdentifier<?>, DataObject>) topoEntry);

        listener.onDataChanged(mockChange);
        TopologyRequestHandler handler = listener.getTopoRequestHandlers().get(yiid);
        Assert.assertNotNull("RequestHandler should be created", handler);
    }

    private AugmentationNode createAugNode() {
        Set<QName> qnames = new HashSet<>();
        qnames.add(TopologyQNames.TOPOLOGY_CORRELATION_AUGMENT);
        AugmentationIdentifier augId = new AugmentationIdentifier(qnames);
        AugmentationNode augNode = ImmutableAugmentationNodeBuilder.create()
                .withNodeIdentifier(augId).withChild(ImmutableNodes.containerNode(Correlations.QNAME))
                .build();
        return augNode;
    }

    @Test
    public void testCreateWrongNode1() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(Network.QNAME).build();

        MapNode node = ImmutableNodes.mapNodeBuilder(Network.QNAME).withChild(
                ImmutableNodes.mapEntry(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPO_NAME)).build();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = new HashMap<>();
        map.put(yiid, node);
        Mockito.when(mockChange.getCreatedData()).thenReturn(map);

        listener.onDataChanged(mockChange);
        Mockito.verify(mockNodeSerializer, Mockito.times(0)).fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any());
    }

    @Test
    public void testCreateWrongNode2() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(Network.QNAME)
                .nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPO_NAME)
                .node(Node.QNAME)
                .build();
        MapNode node = ImmutableNodes.mapNodeBuilder(Node.QNAME).build();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = new HashMap<>();
        map.put(yiid, node);
        Mockito.when(mockChange.getCreatedData()).thenReturn(map);

        listener.onDataChanged(mockChange);
        Mockito.verify(mockNodeSerializer, Mockito.times(0)).fromNormalizedNode(
                (YangInstanceIdentifier) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any());
    }

    @Test
    public void testRemoveNode() {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.builder().node(Network.QNAME)
                .nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, TOPO_NAME).build();
        Map<YangInstanceIdentifier, TopologyRequestHandler> handlers = listener.getTopoRequestHandlers();
        // pre insert topology request handler
        TopologyRequestHandler mockRequestHandler = Mockito.mock(TopologyRequestHandler.class);
        handlers.put(yiid, mockRequestHandler);
        // process removal
        Set<YangInstanceIdentifier> removedPaths = new HashSet<>();
        removedPaths.add(yiid);
        Mockito.when(mockChange.getRemovedPaths()).thenReturn(removedPaths);
        listener.onDataChanged(mockChange);
        Mockito.verify(mockRequestHandler).processDeletionRequest(0);
        Assert.assertEquals("RequestHandlersMap should be empty", 0, handlers.size());
    }

    @Test
    public void testRegisterFiltrator() {
        listener.registerFiltrator(UserDefinedFilter.class, userDefinedFiltratorFactory);
        Assert.assertEquals("Listener's map should contain default filtrators plus one (the registrated)",
                DefaultFiltrators.getDefaultFiltrators().size() + 1, listener.getFiltrators().size());
    }

    @Test
    public void testUnregisterFiltrator() {
        testRegisterFiltrator();
        listener.unregisterFiltrator(UserDefinedFilter.class);
        Assert.assertEquals("The map should contain default filtrators again -"
                + "after the user filtrator was added and removed.",
                DefaultFiltrators.getDefaultFiltrators().size() , listener.getFiltrators().size());
    }

    /**
     * Unregistering of non-existing filtrator should not cause any problem.
     */
    @Test
    public void testUnregisterNonExistingFiltrator() {
        listener.unregisterFiltrator(UserDefinedFilter.class);
        Assert.assertEquals("Default filtrators should remain untouched",
                DefaultFiltrators.getDefaultFiltrators().size() , listener.getFiltrators().size());
    }
}
