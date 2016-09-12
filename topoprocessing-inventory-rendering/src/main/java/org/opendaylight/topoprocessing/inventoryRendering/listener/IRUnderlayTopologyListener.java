/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.listener;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.NotificationInterConnector;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventoryRendering.operator.IRRenderingOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.InventoryRenderingModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matej.perina
 *
 */
public class IRUnderlayTopologyListener extends UnderlayTopologyListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRUnderlayTopologyListener.class);

    public IRUnderlayTopologyListener(DOMDataTreeChangeService domDataTreeChangeService, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        super(domDataTreeChangeService, underlayTopologyId, correlationItem);
     // this needs to be done because for processing TerminationPoints we need to filter Node instead of TP
        if (CorrelationItemEnum.TerminationPoint.equals(correlationItem)) {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node,
                    NetworkTopologyModel.class);
            this.itemQName = TopologyQNames.buildItemQName(CorrelationItemEnum.Node, InventoryRenderingModel.class);
        } else {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(correlationItem,
                    InventoryRenderingModel.class);
            this.itemQName = TopologyQNames.buildItemQName(correlationItem, InventoryRenderingModel.class);
        }
        this.itemIdentifier = YangInstanceIdentifier.of(itemQName);
    }

    public void registerUnderlayTopologyListener(LogicalDatastoreType datastoreType,
            List<ListenerRegistration<DOMDataTreeChangeListener>> listeners) {
        if (correlationItem.equals(CorrelationItemEnum.Node)) {
            TopoStoreProvider connTopoStoreProvider = new TopoStoreProvider();
            connTopoStoreProvider.initializeStore(underlayTopologyId, false);
            TopoStoreProvider renderingTopoProvider = new TopoStoreProvider();
            renderingTopoProvider.initializeStore(underlayTopologyId, false);
            IRRenderingOperator operator = new IRRenderingOperator();
            operator.setTopoStoreProvider(renderingTopoProvider);
            NotificationInterConnector connector =
                    new NotificationInterConnector(correlationItem,connTopoStoreProvider);
            connector.setOperator(operator);
            this.setOperator(connector);
            IRInventoryListener invListener = new IRInventoryListener(underlayTopologyId);
            invListener.setOperator(connector);
            YangInstanceIdentifier invId = YangInstanceIdentifier.of(Nodes.QNAME)
                    .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME);
            DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(datastoreType, invId);
            ListenerRegistration<DOMDataTreeChangeListener> invListenerRegistration =
                    domDataTreeChangeService.registerDataTreeChangeListener(treeId,
                            (DOMDataTreeChangeListener) invListener);
            listeners.add(invListenerRegistration);
        } else {
            throw new IllegalStateException("Rendering has to have CorrelationItem set to Node");
        }
    }
}
