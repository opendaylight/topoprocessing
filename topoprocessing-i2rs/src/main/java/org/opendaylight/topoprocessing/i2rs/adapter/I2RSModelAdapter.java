/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.adapter;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.i2rs.listener.I2RSUnderlayTopologyListener;
import org.opendaylight.topoprocessing.i2rs.request.I2RSTopologyRequestListener;
import org.opendaylight.topoprocessing.i2rs.translator.I2RSLinkTranslator;
import org.opendaylight.topoprocessing.i2rs.translator.I2RSNodeTranslator;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;

/**
 * @author matej.perina
 */

public class I2RSModelAdapter implements ModelAdapter {

    @Override
    public UnderlayTopologyListener registerUnderlayTopologyListener(DOMDataBroker dataBroker,
            String underlayTopologyId, CorrelationItemEnum correlationItem, DatastoreType datastoreType,
            TopologyOperator operator, List<ListenerRegistration<DOMDataTreeChangeListener>> listeners,
            Map<Integer, YangInstanceIdentifier> pathIdentifiers) {

        I2RSUnderlayTopologyListener listener =
                new I2RSUnderlayTopologyListener(dataBroker, underlayTopologyId, correlationItem);
        listener.setOperator(operator);
        return listener;
    }

    @Override
    public TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, Map<Class<? extends Model>, ModelAdapter> modelAdapters) {

        return new I2RSTopologyRequestListener(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
    }

    @Override
    public OverlayItemTranslator createOverlayItemTranslator() {
        return new OverlayItemTranslator(new I2RSNodeTranslator(),new I2RSLinkTranslator());
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
        InstanceIdentifierBuilder identifier = YangInstanceIdentifier
                .builder(InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER)
                .nodeWithKey(Network.QNAME, TopologyQNames.I2RS_NETWORK_ID_QNAME, underlayTopologyId);
        return identifier;
    }
}
