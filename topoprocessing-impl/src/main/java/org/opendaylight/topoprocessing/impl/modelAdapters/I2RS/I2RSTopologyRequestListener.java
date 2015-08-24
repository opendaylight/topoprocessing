/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.I2RS;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.modelAdapters.ModelAdapter;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/** *
 * @author matej.perina
 */

public class I2RSTopologyRequestListener extends TopologyRequestListener {

    public I2RSTopologyRequestListener(DOMDataBroker dataBroker, BindingNormalizedNodeSerializer nodeSerializer,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices, Map<Model, ModelAdapter> modelAdapters) {
        super(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected boolean isTopology(NormalizedNode<?, ?> normalizeNode) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean isTopologyRequest(NormalizedNode<?, ?> normalizedNode) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected TopologyRequestHandler createTopologyRequestHandler(DOMDataBroker dataBroker,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        // TODO Auto-generated method stub
        return null;
    }

}
