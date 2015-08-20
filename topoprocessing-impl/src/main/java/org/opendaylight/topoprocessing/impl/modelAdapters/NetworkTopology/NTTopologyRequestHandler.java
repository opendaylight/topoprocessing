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
import org.opendaylight.topoprocessing.impl.operator.PreAggregationFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TerminationPointAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.filtration.Filter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matej.perina
 */
public class NTTopologyRequestHandler extends TopologyRequestHandler{
    
    public NTTopologyRequestHandler(DOMDataBroker domDataBroker, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, ModelAdapter modelAdapter) {
        super(domDataBroker, schemaHolder, rpcServices, modelAdapter);
    }

    @Override
    protected void modelInitFiltration(TopologyFiltrator filtrator, UnderlayTopologyListener listener,
            YangInstanceIdentifier pathIdentifier) {
        listener.setOperator(filtrator);
    }

    @Override
    protected void modelInitAggregation(TopologyAggregator aggregator, UnderlayTopologyListener listener,
            Mapping mapping, YangInstanceIdentifier pathIdentifier, PreAggregationFiltrator filtrator) {
        
        if ( filtrator == null) {
            listener.setOperator(aggregator);
        } else {
            listener.setOperator(filtrator);
        }
        listener.setPathIdentifier(pathIdentifier);
    }
}
