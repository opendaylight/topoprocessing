/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.adapter;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
/**
 * @author matej.perina
 */
public interface ModelAdapter {

    /**
     * Create model specific UnderlayTopologyListener
     * @param domDataBroker         DOM Data Broker
     * @param underlayTopologyId    underlay topology identifier
     * @param correlationItem       can be either Node or Link or TerminationPoint
     * @param datastoreType         type of data store
     * @param operator              topology operator to use
     * @param listeners             list of registered change listeners
     * @param pathIdentifier        identifier of the node on which the listener is registrated
     * @return new instance of model specific UnderlayTopologyListener
     */
    UnderlayTopologyListener registerUnderlayTopologyListener(DOMDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem, DatastoreType datastoreType, TopologyOperator operator
            ,List<ListenerRegistration<DOMDataChangeListener>> listeners, YangInstanceIdentifier pathIdentifier);

    /**
     * Create model specific TopologyRequestListener
     * @param dataBroker        access to Datastore
     * @param nodeSerializer    translates Topology into BindingAware object - for easier handling in TopologyRequestHandler
     * @param schemaHolder      access to SchemaContext and SchemaListener
     * @param rpcServices       rpcServices for rpc republishing
     * @param modelAdapters     registered ModelAdapters
     * @return new instance of model specific TopologyRequestListener
     */
    TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder, RpcServices rpcServices,
            Map<Class<? extends Model>, ModelAdapter> modelAdapters);

    /**
     * Create model specific OverlayItemTranslator
     * @return new instance of model specific TopologyRequestListener
     */
    OverlayItemTranslator createOverlayItemTranslator();
}
