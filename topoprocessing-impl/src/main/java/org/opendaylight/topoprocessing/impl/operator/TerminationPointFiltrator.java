/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
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
public class TerminationPointFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationPointFiltrator.class);

    private YangInstanceIdentifier pathIdentifier;

    public TerminationPointFiltrator(TopoStoreProvider topoStoreProvider) {
        super(topoStoreProvider);
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        NormalizedNode<?, ?> node = createdEntry.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
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
        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
            Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(tpMapEntryNode, pathIdentifier);
            if (leafNode.isPresent()) {
                if (passedFiltration(leafNode.get())) {
                    tpBuilder.addChild(tpMapEntryNode);
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
