/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventory.adapter;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventory.listener.InvUnderlayTopologyListener;
import org.opendaylight.topoprocessing.inventory.request.InvTopologyRequestListener;
import org.opendaylight.topoprocessing.inventory.translator.InvLinkTranslator;
import org.opendaylight.topoprocessing.inventory.translator.InvNodeTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;

/** *
 * @author matej.perina
 *
 */
public class InvModelAdapter implements ModelAdapter {

    @Override
    public UnderlayTopologyListener registerUnderlayTopologyListener(PingPongDataBroker dataBroker,
            String underlayTopologyId, CorrelationItemEnum correlationItem, DatastoreType datastoreType,
            TopologyOperator operator, List<ListenerRegistration<DOMDataTreeChangeListener>> listeners,
            Map<Integer, YangInstanceIdentifier> pathIdentifiers) {

        InvUnderlayTopologyListener listener = new InvUnderlayTopologyListener(dataBroker, underlayTopologyId, correlationItem);
        listener.registerUnderlayTopologyListener(datastoreType,operator,listeners,pathIdentifiers);
        return listener;
    }

    @Override
    public TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, Map<Class<? extends Model>, ModelAdapter> modelAdapters) {
        return new InvTopologyRequestListener(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
    }

    @Override
    public OverlayItemTranslator createOverlayItemTranslator() {
        return new OverlayItemTranslator(new InvNodeTranslator(), new InvLinkTranslator());
    }

    @Override
    public YangInstanceIdentifier buildItemIdentifier(InstanceIdentifierBuilder builder,
            CorrelationItemEnum correlationItemEnum) {
        switch (correlationItemEnum) {
        case Node:
        case TerminationPoint:
            builder.node(Node.QNAME);
            break;
        case Link:
            builder.node(Link.QNAME);
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: "
                    + correlationItemEnum);
        }
        return builder.build();
    }

    @Override
    public InstanceIdentifierBuilder createTopologyIdentifier(String underlayTopologyId) {
        InstanceIdentifierBuilder identifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId);
        return identifier;
    }
}
