/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventory.request;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.LinkComputationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.LinkComputation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author matej.perina
 *
 */
public class InvTopologyRequestHandler extends TopologyRequestHandler {


    public InvTopologyRequestHandler(DOMDataBroker domDataBroker, DOMDataTreeChangeService domDataTreeChangeService,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices, Map.Entry<InstanceIdentifier<?>,
            DataObject> fromNormalizedNode) {
        super(domDataBroker, domDataTreeChangeService, schemaHolder, rpcServices, fromNormalizedNode);
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
    protected Class<? extends Model> getModel(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        return ((Topology) fromNormalizedNode.getValue()).getAugmentation(CorrelationAugment.class)
                .getCorrelations().getOutputModel();
    }

    @Override
    protected LinkComputation getLinkComputation(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        Topology topo = ((Topology) fromNormalizedNode.getValue());
        LinkComputationAugment linkCompAug = topo.getAugmentation(LinkComputationAugment.class);
        if (linkCompAug == null) {
            return null;
        }
        return linkCompAug.getLinkComputation();
    }
}
