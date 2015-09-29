/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventory.listener;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.impl.listener.InventoryListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.NotificationInterConnector;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author matej.perina
 *
 */
public class InvUnderlayTopologyListener extends UnderlayTopologyListener{

    public InvUnderlayTopologyListener(PingPongDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        super(domDataBroker, underlayTopologyId, correlationItem);
    }

    public void registerUnderlayTopologyListener(DatastoreType datastoreType, TopologyOperator operator
            ,List<ListenerRegistration<DOMDataTreeChangeListener>> listeners, YangInstanceIdentifier invPathIdentifier){
        if (correlationItem.equals(CorrelationItemEnum.Node)) {
            TopoStoreProvider connTopoStoreProvider = new TopoStoreProvider();
            NotificationInterConnector connector = new NotificationInterConnector(underlayTopologyId,
                    correlationItem,connTopoStoreProvider);
            connTopoStoreProvider.initializeStore(underlayTopologyId, false);
            this.setOperator(connector);
            InventoryListener invListener = new InventoryListener(underlayTopologyId);
            invListener.setOperator(connector);
            invListener.setPathIdentifier(invPathIdentifier);
            YangInstanceIdentifier invId = YangInstanceIdentifier.of(Nodes.QNAME)
                    .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME);
            DOMDataTreeIdentifier treeId;
            if (datastoreType.equals(DatastoreType.OPERATIONAL)) {
                treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, invId);
            } else {
                treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, invId);
            }
            ListenerRegistration<DOMDataTreeChangeListener> invListenerRegistration =
                    dataBroker.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) invListener);
            connector.setOperator(operator);
            listeners.add(invListenerRegistration);
        } else {
            this.setOperator(operator);
        }
    };
}
