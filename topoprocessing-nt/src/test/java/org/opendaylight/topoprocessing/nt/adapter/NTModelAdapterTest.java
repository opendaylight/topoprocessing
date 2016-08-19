/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.nt.adapter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.testUtilities.TestingDOMDataBroker;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;

public class NTModelAdapterTest {

    private NTModelAdapter adapter = new NTModelAdapter();

    @Test
    public void testRegisterUnderlayTopologyListener() {
        TestingDOMDataBroker testingDOMDataBroker = new TestingDOMDataBroker();
        PingPongDataBroker dataBroker = new PingPongDataBroker(testingDOMDataBroker);
        String underlayTopologyId = "topo:1";
        CorrelationItemEnum correlationItem = CorrelationItemEnum.Node;
        LogicalDatastoreType datastoreType = LogicalDatastoreType.OPERATIONAL;
        TopologyOperator operator = mock(TopologyOperator.class);
        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>();
        UnderlayTopologyListener listener = adapter.registerUnderlayTopologyListener(dataBroker, underlayTopologyId,
                correlationItem, datastoreType, operator, null, pathIdentifiers);
        assertEquals(correlationItem, listener.getCorrelationItem());
        assertEquals(operator, listener.getOperator());
        assertEquals(underlayTopologyId, listener.getUnderlayTopologyId());
    }

    @Test
    public void testCreateTopologyRequestListener() {
        TestingDOMDataBroker dataBroker = new TestingDOMDataBroker();
        BindingNormalizedNodeSerializer nodeSerializer = mock(BindingNormalizedNodeSerializer.class);
        GlobalSchemaContextHolder schemaHolder = mock(GlobalSchemaContextHolder.class);
        RpcServices rpcServices = mock(RpcServices.class);
        Map<Class<? extends Model>, ModelAdapter> modelAdapters = new HashMap<>();
        TopologyRequestListener listener = adapter.createTopologyRequestListener(dataBroker, nodeSerializer,
                schemaHolder, rpcServices, modelAdapters);
        assertEquals(0, listener.getTopoRequestHandlers().size());
        // 7 default filtrators are added
        assertEquals(7, listener.getFiltrators().size());
    }

    @Test
    public void testCreateOverlayItemTranslator() {
        OverlayItemTranslator translator = adapter.createOverlayItemTranslator();
        OverlayItem overlayItem = new OverlayItem(new ArrayList<>(), CorrelationItemEnum.Node);
        OverlayItemWrapper overlayItemWrapper = new OverlayItemWrapper("ID", overlayItem);
        Assert.assertNotNull(translator.translate(overlayItemWrapper));
    }

    @Test
    public void testCreateItemIdentifier() {
        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        YangInstanceIdentifier nodeIdentifier = adapter.buildItemIdentifier(builder, CorrelationItemEnum.Node);
        builder = YangInstanceIdentifier.builder();
        YangInstanceIdentifier tpIdentifier =
                adapter.buildItemIdentifier(builder, CorrelationItemEnum.TerminationPoint);
        assertEquals(nodeIdentifier, tpIdentifier);
        builder = YangInstanceIdentifier.builder();
        YangInstanceIdentifier linkIdentifier = adapter.buildItemIdentifier(builder, CorrelationItemEnum.Link);
        builder = YangInstanceIdentifier.builder();
        assertEquals(builder.node(Link.QNAME).build(), linkIdentifier);
    }

    @Test
    public void testCreateTopologyIdentifier() {
        String underlayTopologyId = "topo:1";
        YangInstanceIdentifier identifier = adapter.createTopologyIdentifier(underlayTopologyId).build();
        assertEquals(YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId).build(), identifier);
    }
}
