/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtratorFactory;

import com.google.common.base.Optional;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.PreAggregationFiltrator;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TerminationPointPreAggregationFiltrator extends PreAggregationFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationPointPreAggregationFiltrator.class);

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> itemEntry : createdEntries.entrySet()) {
            UnderlayItem underlayItem = itemEntry.getValue();
            NormalizedNode<?, ?> node = underlayItem.getItem();
            Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                    YangInstanceIdentifier.of(TerminationPoint.QNAME));
            if (tpMapNodeOpt.isPresent()) {
                node = buildNode(node, (MapNode) tpMapNodeOpt.get());
                underlayItem.setItem(node);
            }
            aggregator.processCreatedChanges(Collections.singletonMap(itemEntry.getKey(), underlayItem), topologyId);
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        for (Map.Entry<YangInstanceIdentifier, UnderlayItem> itemEntry : updatedEntries.entrySet()) {
            UnderlayItem underlayItem = itemEntry.getValue();
            NormalizedNode<?, ?> node = underlayItem.getItem();
            Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                    YangInstanceIdentifier.of(TerminationPoint.QNAME));
            if (tpMapNodeOpt.isPresent()
                    && (! underlayItem.getLeafNode().equals(tpMapNodeOpt.get()))) {
                node = buildNode(node, (MapNode) tpMapNodeOpt.get());
                underlayItem.setItem(node);
            }
            aggregator.processUpdatedChanges(Collections.singletonMap(itemEntry.getKey(), underlayItem), topologyId);
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOGGER.trace("Processing removedChanges");
        aggregator.processRemovedChanges(identifiers, topologyId);
    }

    private NormalizedNode<?, ?> buildNode(NormalizedNode<?, ?> node, MapNode tpMapNode) {
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
