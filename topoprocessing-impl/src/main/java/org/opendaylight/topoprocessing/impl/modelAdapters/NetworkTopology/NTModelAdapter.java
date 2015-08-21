/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.NetworkTopology;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.modelAdapters.ModelAdapter;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;

public class NTModelAdapter implements ModelAdapter {

    @Override
    public UnderlayTopologyListener createUnderlayTopologyListener(DOMDataBroker domDataBroker,
            String underlayTopologyId, CorrelationItemEnum correlationItem) {

        return new NTUnderlayTopologyListener(domDataBroker, underlayTopologyId, correlationItem);
    }

    @Override
    public TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices) {

        return new NTTopologyRequestListener(dataBroker, nodeSerializer, schemaHolder, rpcServices, this);
    }

    @Override
    public OverlayItemTranslator createOverlayItemTranslator() {

        return new OverlayItemTranslator(new NTNodeTranslator(),new NTLinkTranslator());
    }

}
