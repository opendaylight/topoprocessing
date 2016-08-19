/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventory.listener;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.topoprocessing.impl.listener.InventoryListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.NotificationInterConnector;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matej.perina
 *
 */
public class InvUnderlayTopologyListener extends UnderlayTopologyListener {

    public InvUnderlayTopologyListener(DOMDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        super(domDataBroker, underlayTopologyId, correlationItem);
        // this needs to be done because for processing TerminationPoints we need to filter Node instead of TP
        if (CorrelationItemEnum.TerminationPoint.equals(correlationItem)) {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node,
                    OpendaylightInventoryModel.class);
            this.itemQName = TopologyQNames.buildItemQName(CorrelationItemEnum.Node, OpendaylightInventoryModel.class);
        } else {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(correlationItem,
                    OpendaylightInventoryModel.class);
            this.itemQName = TopologyQNames.buildItemQName(correlationItem, OpendaylightInventoryModel.class);
        }
        this.itemIdentifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME,TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId)
                .node(itemQName).build();
    }

    public void registerUnderlayTopologyListener(LogicalDatastoreType datastoreType, TopologyOperator operator,
            List<ListenerRegistration<DOMDataTreeChangeListener>> listeners,
            Map<Integer, YangInstanceIdentifier> invPathIdentifiers) {
        if (correlationItem.equals(CorrelationItemEnum.Node)
                || correlationItem.equals(CorrelationItemEnum.TerminationPoint)) {
            TopoStoreProvider connTopoStoreProvider = new TopoStoreProvider();
            NotificationInterConnector connector =
                    new NotificationInterConnector(correlationItem,connTopoStoreProvider);
            connTopoStoreProvider.initializeStore(underlayTopologyId, false);
            this.setOperator(connector);
            InventoryListener invListener = new InventoryListener(underlayTopologyId, correlationItem);
            invListener.setOperator(connector);
            invListener.setPathIdentifier(invPathIdentifiers);
            YangInstanceIdentifier invId = YangInstanceIdentifier.of(Nodes.QNAME)
                    .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME);
            DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(datastoreType, invId);
            ListenerRegistration<DOMDataTreeChangeListener> invListenerRegistration =
                    domDataTreeChangeService.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) invListener);
            connector.setOperator(operator);
            listeners.add(invListenerRegistration);
        } else {
            this.setOperator(operator);
        }
    }
}
