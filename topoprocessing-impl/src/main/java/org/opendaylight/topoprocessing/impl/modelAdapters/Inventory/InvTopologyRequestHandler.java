/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.Inventory;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.listener.InventoryListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.modelAdapters.ModelAdapter;
import org.opendaylight.topoprocessing.impl.operator.NotificationInterConnector;
import org.opendaylight.topoprocessing.impl.operator.PreAggregationFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestHandler;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matej.perina
 */
public class InvTopologyRequestHandler extends TopologyRequestHandler{
    

    public InvTopologyRequestHandler(DOMDataBroker domDataBroker, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices, ModelAdapter modelAdapter) {
        super(domDataBroker, schemaHolder, rpcServices, modelAdapter);
    }

    @Override
    protected void modelInitFiltration(TopologyFiltrator filtrator, UnderlayTopologyListener listener,
            YangInstanceIdentifier pathIdentifier) {
        NotificationInterConnector connector = null;
        if (listener.getCorrelationItem().equals(CorrelationItemEnum.Node)) {
            TopoStoreProvider connTopoStoreProvider = new TopoStoreProvider();
            String underlayTopologyId = listener.getUnderlayTopologyId();
            CorrelationItemEnum correlationItem = listener.getCorrelationItem();
            DOMDataBroker domDataBroker = listener.getDomDataBroker();
            connector = new NotificationInterConnector(underlayTopologyId, correlationItem,connTopoStoreProvider);
            connTopoStoreProvider.initializeStore(underlayTopologyId, false);
            listener.setOperator(connector);
            InventoryListener invListener = new InventoryListener(underlayTopologyId);
            invListener.setOperator(connector);
            invListener.setPathIdentifier(pathIdentifier);
            YangInstanceIdentifier invId = YangInstanceIdentifier.of(Nodes.QNAME)
                    .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME);
            ListenerRegistration<DOMDataChangeListener> invListenerRegistration;
            if (datastoreType.equals(DatastoreType.OPERATIONAL)) {
                invListenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.OPERATIONAL, invId, invListener, DataChangeScope.SUBTREE);
            } else {
                invListenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.CONFIGURATION, invId, invListener, DataChangeScope.SUBTREE);
            }
            listeners.add(invListenerRegistration);
        }
        if (connector == null) {
            listener.setOperator(filtrator);
        } else {
            connector.setOperator(filtrator);
        }
    }

    @Override
    protected void modelInitAggregation(TopologyAggregator aggregator, UnderlayTopologyListener listener,
            Mapping mapping, YangInstanceIdentifier pathIdentifier, PreAggregationFiltrator filtrator) {
        
        NotificationInterConnector connector = null;
        CorrelationItemEnum correlationItem = listener.getCorrelationItem();
        String underlayTopologyId = listener.getUnderlayTopologyId();
        DOMDataBroker domDataBroker = listener.getDomDataBroker();
        if (correlationItem.equals(CorrelationItemEnum.Node)) {
            TopoStoreProvider connTopoStoreProvider = new TopoStoreProvider();
            connector = new NotificationInterConnector(underlayTopologyId, correlationItem,connTopoStoreProvider);
            connTopoStoreProvider.initializeStore(underlayTopologyId, false);
            listener.setOperator(connector);
            InventoryListener invListener = new InventoryListener(underlayTopologyId);
            invListener.setOperator(connector);
            invListener.setPathIdentifier(pathIdentifier);
            YangInstanceIdentifier invId = YangInstanceIdentifier.of(Nodes.QNAME)
                    .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME);
            ListenerRegistration<DOMDataChangeListener> invListenerRegistration;
            if (datastoreType.equals(DatastoreType.OPERATIONAL)) {
                invListenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.OPERATIONAL, invId, invListener, DataChangeScope.SUBTREE);
            } else {
                invListenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.CONFIGURATION, invId, invListener, DataChangeScope.SUBTREE);
            }
            listeners.add(invListenerRegistration);
        }
        if ( filtrator == null ) {
            if ( connector == null ) {
                listener.setOperator(aggregator);
            } else {
                connector.setOperator(aggregator);
            }
        } else {
            if ( connector == null ) {
                listener.setOperator(filtrator);
            } else {
                connector.setOperator(filtrator);
            }
        }
        if (connector == null) {
            listener.setPathIdentifier(pathIdentifier);
        }
    }
}
