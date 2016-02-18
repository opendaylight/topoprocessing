/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author matus.marko
 */
public class TerminationPointFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationPointFiltrator.class);

    private YangInstanceIdentifier pathIdentifier;

    private Class<? extends Model> model;

    public TerminationPointFiltrator(TopoStoreProvider topoStoreProvider, Class<? extends Model> model) {
        super(topoStoreProvider);
        this.model = model;
    }

    public TerminationPointFiltrator(TopoStoreProvider topoStoreProvider) {
        super(topoStoreProvider);
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        NormalizedNode<?, ?> node = createdEntry.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = null;
        if (model.equals(NetworkTopologyModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(node,InstanceIdentifiers.NT_TERMINATION_POINT);
        } else if (model.equals(I2rsModel.class)){
            tpMapNodeOpt = NormalizedNodes.findNode(node,InstanceIdentifiers.I2RS_TERMINATION_POINT);
        } else if (model.equals(OpendaylightInventoryModel.class)){
            tpMapNodeOpt = NormalizedNodes
                    .findNode(createdEntry.getLeafNodes().get(0),YangInstanceIdentifier.of(NodeConnector.QNAME));
        }
        if (tpMapNodeOpt.isPresent()) {
            node = filterTerminationPoints(node, (MapNode) tpMapNodeOpt.get());
            createdEntry.setItem(node);
        }
        getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().put(identifier, createdEntry);
        OverlayItem overlayItem = wrapUnderlayItem(createdEntry);
        manager.addOverlayItem(overlayItem);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        Map<YangInstanceIdentifier, UnderlayItem> oldUnderlayItems =
                getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems();
        OverlayItem overlayItem;
        UnderlayItem newUnderlayItem = updatedEntry;
        NormalizedNode<?, ?> newNode = newUnderlayItem.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(newNode,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        if (tpMapNodeOpt.isPresent()) {
            newNode = filterTerminationPoints(newNode, (MapNode) tpMapNodeOpt.get());
            newUnderlayItem.setItem(newNode);
        }
        UnderlayItem oldUnderlayItem = oldUnderlayItems.get(identifier);
        overlayItem = oldUnderlayItem.getOverlayItem();
        Queue<UnderlayItem> underlayItems = new ConcurrentLinkedQueue<>();
        underlayItems.add(newUnderlayItem);
        overlayItem.setUnderlayItems(underlayItems);
        manager.addOverlayItem(overlayItem);
    }

    private NormalizedNode<?, ?> filterTerminationPoints(NormalizedNode<?, ?> node, MapNode tpMapNode) {
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        IdentifierGenerator idGenerator = new IdentifierGenerator();
        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
            Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(tpMapEntryNode, pathIdentifier);
            if (leafNode.isPresent()) {
                if (passedFiltration(leafNode.get())) {
                    if (model.equals(OpendaylightInventoryModel.class)) {
                        String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
                        Optional<NormalizedNode<?, ?>> nodeConnectorIdOptional = NormalizedNodes
                                .findNode(tpMapEntryNode, InstanceIdentifiers.INVENTORY_NODE_ID_IDENTIFIER);
                        MapEntryNode tp = ImmutableNodes
                                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tpId)
                                .withChild(ImmutableNodes.leafNode(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME,
                                        nodeConnectorIdOptional.get().getValue())).build();
                        tpBuilder.addChild(tp);
                    } else {
                        tpBuilder.addChild(tpMapEntryNode);
                    }
                }
            }
        }
        node = ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
        return node;
    }

    public void setPathIdentifier(YangInstanceIdentifier pathIdentifier) {
        this.pathIdentifier = pathIdentifier;
    }
}
