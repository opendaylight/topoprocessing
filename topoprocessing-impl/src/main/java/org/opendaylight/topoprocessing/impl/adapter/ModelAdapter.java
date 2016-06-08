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
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
/**
 * @author matej.perina
 */
public interface ModelAdapter {

    /**
     * Create model specific UnderlayTopologyListener.
     * @param dataBroker            PingPong Data Broker
     * @param underlayTopologyId    underlay topology identifier
     * @param correlationItem       can be either Node or Link or TerminationPoint
     * @param datastoreType         type of data store
     * @param operator              topology operator to use
     * @param listeners             list of registered change listeners
     * @param pathIdentifier        identifier of the node on which the listener is registrated
     * @return new instance of model specific UnderlayTopologyListener
     */
    UnderlayTopologyListener registerUnderlayTopologyListener(PingPongDataBroker dataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem, DatastoreType datastoreType, TopologyOperator operator
            ,List<ListenerRegistration<DOMDataTreeChangeListener>> listeners,
            Map<Integer, YangInstanceIdentifier> pathIdentifier);

    /**
     * Create model specific TopologyRequestListener.
     * @param dataBroker        access to Datastore
     * @param nodeSerializer    translates Topology into BindingAware object - for easier handling in
     *                          TopologyRequestHandler
     * @param schemaHolder      access to SchemaContext and SchemaListener
     * @param rpcServices       rpcServices for rpc republishing
     * @param modelAdapters     registered ModelAdapters
     * @return new instance of model specific TopologyRequestListener
     */
    TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, Map<Class<? extends Model>, ModelAdapter> modelAdapters);

    /**
     * Create model specific OverlayItemTranslator.
     * @return new instance of model specific TopologyRequestListener
     */
    OverlayItemTranslator createOverlayItemTranslator();

    /**
     * Builds item identifier (identifies item {@link MapNode}).
     * @param builder starting builder (set with specific topology) that will be appended
     * with corresponding item QName
     * @param correlationItemEnum item type
     * @return item identifier (identifies item {@link MapNode})
     */
    YangInstanceIdentifier buildItemIdentifier(YangInstanceIdentifier.InstanceIdentifierBuilder builder,
            CorrelationItemEnum correlationItemEnum);

    /**
     * Creates model specific (topology or network) identifier builder.
     * @param underlayTopologyId
     * @return new model specific topology builder
     */
    InstanceIdentifierBuilder createTopologyIdentifier(String underlayTopologyId);
}
