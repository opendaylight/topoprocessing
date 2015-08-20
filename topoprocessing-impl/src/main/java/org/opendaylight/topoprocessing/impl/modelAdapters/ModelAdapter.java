/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.modelAdapters;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
/**
 * @author matej.perina
 */
public interface ModelAdapter {

    /**
     * Create model specific UnderlayTopologyListener
     * @param domDataBroker         DOM Data Broker
     * @param underlayTopologyId    underlay topology identifier
     * @param correlationItem       can be either Node or Link or TerminationPoint
     * @return new instance of model specific UnderlayTopologyListener
     */
    UnderlayTopologyListener createUnderlayTopologyListener(DOMDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem);

    /**
     * Create model specific TopologyRequestListener
     * @param dataBroker        access to Datastore
     * @param nodeSerializer    translates Topology into BindingAware object - for easier handling in TopologyRequestHandler
     * @param schemaHolder      access to SchemaContext and SchemaListener
     * @param rpcServices       rpcServices for rpc republishing
     *  @return new instance of model specific TopologyRequestListener
     */
    TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices);

    /**
     * Create model specific TopologyRequestListener
     * @return new instance of model specific TopologyRequestListener
     */
    OverlayItemTranslator createOverlayItemTranslator();
}
