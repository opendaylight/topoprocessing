/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.request;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsCorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.I2rsLinkComputationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.LinkComputation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class I2RSTopologyRequestHandler extends TopologyRequestHandler {

    public I2RSTopologyRequestHandler(DOMDataBroker domDataBroker, DOMDataTreeChangeService domDataTreeChangeService,
            GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Map.Entry<InstanceIdentifier<?>,DataObject> fromNormalizedNode) {
        super(domDataBroker, domDataTreeChangeService, schemaHolder, rpcServices, fromNormalizedNode);
    }

    @Override
    protected Class<? extends Model> getModel(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        return ((Network) fromNormalizedNode.getValue()).getAugmentation(I2rsCorrelationAugment.class)
                .getCorrelations().getOutputModel();
    }

    @Override
    protected String getTopologyId(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        return ((Network) fromNormalizedNode.getValue()).getNetworkId().getValue();
    }

    @Override
    protected Correlations getCorrelations(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        return ((Network) fromNormalizedNode.getValue()).getAugmentation(I2rsCorrelationAugment.class)
                .getCorrelations();
    }

    @Override
    protected LinkComputation getLinkComputation(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
        I2rsLinkComputationAugment augment = ((Network) fromNormalizedNode.getValue())
                .getAugmentation(I2rsLinkComputationAugment.class);
        if (augment == null) {
            return null;
        }
        return augment.getLinkComputation();
    }
}
