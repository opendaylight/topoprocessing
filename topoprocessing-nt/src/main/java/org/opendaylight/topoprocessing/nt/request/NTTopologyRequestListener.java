/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.nt.request;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * @author matej.perina
 *
 */
public class NTTopologyRequestListener extends TopologyRequestListener {


    private Set<QName> correlationHashSet;
    private Set<QName> linkComputationHashSet;

    public NTTopologyRequestListener(DOMDataBroker dataBroker, DOMDataTreeChangeService domDataTreeChangeService,
            BindingNormalizedNodeSerializer nodeSerializer,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Map<Class<? extends Model>, ModelAdapter> modelAdapters) {
        super(dataBroker, domDataTreeChangeService, nodeSerializer, schemaHolder, rpcServices, modelAdapters);
        correlationHashSet = new HashSet<>();
        correlationHashSet.add(TopologyQNames.TOPOLOGY_CORRELATION_AUGMENT);
        linkComputationHashSet = new HashSet<>();
        linkComputationHashSet.add(TopologyQNames.LINK_COMPUTATION_AUGMENT);
    }

    @Override
    protected boolean isTopology(NormalizedNode<?, ?> normalizedNode) {

        return normalizedNode.getNodeType().equals(Topology.QNAME);
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
            DOMDataTreeChangeService domDataTreeChangeService, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, Map.Entry<InstanceIdentifier<?>,DataObject> fromNormalizedNode) {

        return new NTTopologyRequestHandler(dataBroker, domDataTreeChangeService, schemaHolder, rpcServices,
                fromNormalizedNode);
    }

}
