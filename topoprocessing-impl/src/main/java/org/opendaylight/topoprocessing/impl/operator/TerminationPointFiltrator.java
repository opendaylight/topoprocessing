/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.*;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TerminationPointFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationPointFiltrator.class);

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> itemEntry : createdEntries.entrySet()) {
            UnderlayItem underlayItem = itemEntry.getValue();
            NormalizedNode<?, ?> node = underlayItem.getItem();
            Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                    YangInstanceIdentifier.of(TerminationPoint.QNAME));
            if (tpMapNodeOpt.isPresent()) {
                node = filterTerminationPoints(node, (MapNode) tpMapNodeOpt.get());
                underlayItem.setItem(node);
            }
            getTopologyStore(topologyId).getUnderlayItems().put(itemEntry.getKey(), underlayItem);
            OverlayItem overlayItem = wrapUnderlayItem(underlayItem);
            manager.addOverlayItem(overlayItem);
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        Map<YangInstanceIdentifier, UnderlayItem> oldUnderlayItems = getTopologyStore(topologyId).getUnderlayItems();
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> itemEntry : updatedEntries.entrySet()) {
            OverlayItem overlayItem;
            UnderlayItem newUnderlayItem = itemEntry.getValue();
            NormalizedNode<?, ?> newNode = newUnderlayItem.getItem();
            Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(newNode,
                    YangInstanceIdentifier.of(TerminationPoint.QNAME));
            if (tpMapNodeOpt.isPresent()) {
                newNode = filterTerminationPoints(newNode, (MapNode) tpMapNodeOpt.get());
                newUnderlayItem.setItem(newNode);
            }
            overlayItem = wrapUnderlayItem(newUnderlayItem);
            manager.updateOverlayItem(overlayItem);
        }
    }

    private NormalizedNode<?, ?> filterTerminationPoints(NormalizedNode<?, ?> node, MapNode tpMapNode) {
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
            if (passedFiltration(tpMapEntryNode)) {
                tpBuilder.addChild(tpMapEntryNode);
            }
        }
        node = ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
        return node;
    }
}
