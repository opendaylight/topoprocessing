/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.Inventory;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.modelAdapters.ModelAdapter;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/** *
 * @author matej.perina
 *
 */
public class InvModelAdapter implements ModelAdapter {

    @Override
    public UnderlayTopologyListener registerUnderlayTopologyListener(DOMDataBroker domDataBroker,
            String underlayTopologyId, CorrelationItemEnum correlationItem, DatastoreType datastoreType, TopologyOperator operator
            ,List<ListenerRegistration<DOMDataChangeListener>> listeners) {

        InvUnderlayTopologyListener listener = new InvUnderlayTopologyListener(domDataBroker, underlayTopologyId, correlationItem);
        listener.registerUnderlayTopologyListener(datastoreType,operator,listeners);
        return listener;
    }

    @Override
    public TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, Map<Model, ModelAdapter> modelAdapters) {
        return new InvTopologyRequestListener(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
    }

    @Override
    public OverlayItemTranslator createOverlayItemTranslator() {
        return new OverlayItemTranslator(new InvNodeTranslator(), new InvLinkTranslator());
    }

}
