/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.nt.adapter;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.nt.listener.NTUnderlayTopologyListener;
import org.opendaylight.topoprocessing.nt.request.NTTopologyRequestListener;
import org.opendaylight.topoprocessing.nt.translator.NTLinkTranslator;
import org.opendaylight.topoprocessing.nt.translator.NTNodeTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matej.perina
 *
 */
public class NTModelAdapter implements ModelAdapter {

    @Override
    public UnderlayTopologyListener registerUnderlayTopologyListener(DOMDataBroker domDataBroker,
            String underlayTopologyId, CorrelationItemEnum correlationItem, DatastoreType datastoreType, TopologyOperator operator
            ,List<ListenerRegistration<DOMDataChangeListener>> listeners, YangInstanceIdentifier pathIdentifier) {

        NTUnderlayTopologyListener listener = new NTUnderlayTopologyListener(domDataBroker, underlayTopologyId, correlationItem);
        listener.setPathIdentifier(pathIdentifier);
        listener.registerTopologyOperator(operator);
        return listener;
    }

    @Override
    public TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, Map<Class<? extends Model>, ModelAdapter> modelAdapters) {

        return new NTTopologyRequestListener(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
    }

    @Override
    public OverlayItemTranslator createOverlayItemTranslator() {

        return new OverlayItemTranslator(new NTNodeTranslator(),new NTLinkTranslator());
    }

}
