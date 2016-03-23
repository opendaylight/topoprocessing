/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.*;
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TerminationPointPreAggregationFiltrator extends PreAggregationFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreAggregationFiltrator.class);
    private YangInstanceIdentifier pathIdentifier;
    private Class<? extends Model> model;

    public TerminationPointPreAggregationFiltrator(TopoStoreProvider topoStoreProvider) {
        super(topoStoreProvider);
    }


    public TerminationPointPreAggregationFiltrator(TopoStoreProvider topoStoreProvider, Class<? extends Model> model) {
        super(topoStoreProvider);
        this.model = model;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        NormalizedNode<?, ?> node = createdEntry.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = null;
        if (model.equals(NetworkTopologyModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(node, InstanceIdentifiers.NT_TERMINATION_POINT);
        } else if (model.equals(I2rsModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(node, InstanceIdentifiers.I2RS_TERMINATION_POINT);
        } else if (model.equals(OpendaylightInventoryModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(createdEntry.getLeafNodes().values().iterator().next(),
                    InstanceIdentifiers.INVENTORY_NODE_CONNECTOR_IDENTIFIER);
        }
        if (tpMapNodeOpt.isPresent()) {
            node = filterTerminationPoints(node, (MapNode) tpMapNodeOpt.get());
            createdEntry.setItem(node);
        }
        aggregator.processCreatedChanges(identifier, createdEntry, topologyId);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem olditem = getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().get(identifier);

        UnderlayItem newUnderlayItem = updatedEntry;
        NormalizedNode<?, ?> newNode = newUnderlayItem.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(newNode,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));

        if (tpMapNodeOpt.isPresent()) {
            newNode = filterTerminationPoints(newNode, (MapNode) tpMapNodeOpt.get());
            updatedEntry.setItem(newNode);
        }
        if (olditem == null) {
            aggregator.processCreatedChanges(identifier, updatedEntry, topologyId);
        } else {
            aggregator.processUpdatedChanges(identifier, updatedEntry, topologyId);
        }
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
