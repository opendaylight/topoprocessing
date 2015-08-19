/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.NetworkTopology;

import org.opendaylight.topoprocessing.impl.modelAdapters.ModelAdapter;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.Correlations;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matej.perina
 */
public class NTTopologyRequestListener extends TopologyRequestListener{

    public NTTopologyRequestListener(DOMDataBroker dataBroker, BindingNormalizedNodeSerializer nodeSerializer,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices, ModelAdapter modelAdapter) {
        super(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapter);
    }

    @Override
    protected boolean isTopology(NormalizedNode<?, ?> normalizedNode) {
        
        return normalizedNode.getNodeType().equals(Topology.QNAME);
    }

    @Override
    protected String getTopologyId(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        
        return ((Topology) fromNormalizedNode.getValue()).getTopologyId().getValue();
    }

    @Override
    protected Correlations getCorrelations(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        
        return ((Topology) fromNormalizedNode.getValue()).getAugmentation(CorrelationAugment.class).getCorrelations();
    }

    @Override
    protected TopologyRequestHandler createTopologyRequestHandler(DOMDataBroker dataBroker,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices, ModelAdapter modelAdapter) {

        return new NTTopologyRequestHandler(dataBroker, schemaHolder, rpcServices, modelAdapter);
    }

}
