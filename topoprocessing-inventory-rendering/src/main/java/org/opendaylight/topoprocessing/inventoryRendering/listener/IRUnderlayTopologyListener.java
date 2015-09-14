/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.listener;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.listener.InventoryListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.NotificationInterConnector;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventoryRendering.operator.IRRenderingOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author matej.perina
 *
 */
public class IRUnderlayTopologyListener extends UnderlayTopologyListener{

    private static final Logger LOGGER = LoggerFactory.getLogger(IRUnderlayTopologyListener.class);
    public IRUnderlayTopologyListener(DOMDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        super(domDataBroker, underlayTopologyId, correlationItem);
    }

    @Override
    protected Map<YangInstanceIdentifier, NormalizedNode<?, ?>> listToMap(Iterator<NormalizedNode<?, ?>> nodes,
            final String underlayTopologyId) {
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = Maps.uniqueIndex(nodes,
                new Function<NormalizedNode<?, ?>, YangInstanceIdentifier>() {
            @Nullable
            @Override
            public YangInstanceIdentifier apply(NormalizedNode<?, ?> node) {
                return YangInstanceIdentifier
                        .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                        .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId)
                        .node(Node.QNAME)
                        .node(node.getIdentifier())
                        .build();
            }
        });
        return map;
    }

    public void registerUnderlayTopologyListener(DatastoreType datastoreType,
            List<ListenerRegistration<DOMDataChangeListener>> listeners){
        if (correlationItem.equals(CorrelationItemEnum.Node)) {
            TopoStoreProvider connTopoStoreProvider = new TopoStoreProvider();
            connTopoStoreProvider.initializeStore(underlayTopologyId, false);
            TopoStoreProvider renderingTopoProvider = new TopoStoreProvider();
            renderingTopoProvider.initializeStore(underlayTopologyId, false);
            IRRenderingOperator operator = new IRRenderingOperator();
            operator.setTopoStoreProvider(renderingTopoProvider);
            NotificationInterConnector connector = new NotificationInterConnector(underlayTopologyId,
                    correlationItem,connTopoStoreProvider);
            connector.setOperator(operator);
            this.setOperator(connector);
            InventoryListener invListener = new InventoryListener(underlayTopologyId);
            invListener.setOperator(connector);
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
        } else {
            throw new IllegalStateException("Rendering has to have CorrelationItem set to Node");
        }
    };
}
