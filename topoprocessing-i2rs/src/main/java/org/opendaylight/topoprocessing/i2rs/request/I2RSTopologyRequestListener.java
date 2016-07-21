/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.request;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * @author matej.perina
 */

public class I2RSTopologyRequestListener extends TopologyRequestListener {

    private HashSet<QName> correlationHashSet;
    private HashSet<QName> linkComputationHashSet;

    public I2RSTopologyRequestListener(DOMDataBroker dataBroker, BindingNormalizedNodeSerializer nodeSerializer,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Map<Class<? extends Model>, ModelAdapter> modelAdapters) {
        super(dataBroker, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
        correlationHashSet = new HashSet<>();
        correlationHashSet.add(TopologyQNames.TOPOLOGY_CORRELATION_AUGMENT);
        linkComputationHashSet = new HashSet<>();
        linkComputationHashSet.add(TopologyQNames.LINK_COMPUTATION_AUGMENT);
        super.identifier = InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER;
    }

    @Override
    protected boolean isTopology(NormalizedNode<?, ?> normalizedNode) {
        return normalizedNode.getNodeType().equals(Network.QNAME);
    }

    @Override
    protected boolean isTopologyRequest(NormalizedNode<?, ?> normalizedNode) {
        return NormalizedNodes.findNode(normalizedNode, new AugmentationIdentifier(correlationHashSet)).isPresent();
    }

    @Override
    protected boolean isLinkCalculation(NormalizedNode<?, ?> normalizedNode) {
        return NormalizedNodes.findNode(normalizedNode, new AugmentationIdentifier(linkComputationHashSet)).isPresent();
    }

    @Override
    protected TopologyRequestHandler createTopologyRequestHandler(DOMDataBroker dataBroker,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {

        return new I2RSTopologyRequestHandler(dataBroker, schemaHolder, rpcServices, fromNormalizedNode);
    }
}
