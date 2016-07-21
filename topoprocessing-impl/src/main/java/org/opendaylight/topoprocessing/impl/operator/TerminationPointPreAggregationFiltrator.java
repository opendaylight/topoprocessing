/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.filtrator.AbstractFiltrator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
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


/**
 * @author matus.marko
 */
public class TerminationPointPreAggregationFiltrator extends PreAggregationFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreAggregationFiltrator.class);
    private Optional<Map<Integer, YangInstanceIdentifier>> pathIdentifiers = Optional.absent();
    private Class<? extends Model> model;

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
        operator.processCreatedChanges(identifier, createdEntry, topologyId);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem olditem = getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().get(identifier);

        UnderlayItem newUnderlayItem = updatedEntry;
        NormalizedNode<?, ?> newNode = newUnderlayItem.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(newNode,
                InstanceIdentifiers.NT_TERMINATION_POINT);

        if (tpMapNodeOpt.isPresent()) {
            newNode = filterTerminationPoints(newNode, (MapNode) tpMapNodeOpt.get());
            updatedEntry.setItem(newNode);
        }
        if (olditem == null) {
            operator.processCreatedChanges(identifier, updatedEntry, topologyId);
        } else {
            operator.processUpdatedChanges(identifier, updatedEntry, topologyId);
        }
    }

    private NormalizedNode<?, ?> filterTerminationPoints(NormalizedNode<?, ?> node, MapNode tpMapNode) {
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        if (! pathIdentifiers.isPresent()) {
            pathIdentifiers = Optional.of(new HashMap<>());
            for (int i = 0; i < filtrators.size(); i++) {
                pathIdentifiers.get().put(i,((AbstractFiltrator)filtrators.get(i)).getPathIdentifier());
            }
        }
        boolean passed;
        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
            passed = true;
            for (Map.Entry<Integer, YangInstanceIdentifier> pathIdentifier: pathIdentifiers.get().entrySet()) {
                Optional<NormalizedNode<?, ?>> leafNode =
                        NormalizedNodes.findNode(tpMapEntryNode, pathIdentifier.getValue());
                if (! leafNode.isPresent() || ! passedFiltration(leafNode.get())){
                    passed = false;
                    break;
                }
            }
            //check if any Filtrator filtered out
            if (passed) {
                tpBuilder.addChild(tpMapEntryNode);
            }
        }
        node = ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
        return node;
    }

}
