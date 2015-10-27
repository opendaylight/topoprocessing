/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TerminationPointPreAggregationFiltrator extends PreAggregationFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationPointPreAggregationFiltrator.class);

    public TerminationPointPreAggregationFiltrator(TopoStoreProvider topoStoreProvider) {
        super(topoStoreProvider);
    }
    
    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        NormalizedNode<?, ?> node = createdEntry.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        if (tpMapNodeOpt.isPresent()) {
            node = filterNode(node, (MapNode) tpMapNodeOpt.get());
            createdEntry.setItem(node);
        }
        aggregator.processCreatedChanges(identifier, createdEntry, topologyId);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        UnderlayItem underlayItem = updatedEntry;
        NormalizedNode<?, ?> node = underlayItem.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        if (tpMapNodeOpt.isPresent()) {
            node = filterNode(node, (MapNode) tpMapNodeOpt.get());
            underlayItem.setItem(node);
        }
        aggregator.processUpdatedChanges(identifier, underlayItem, topologyId);
    }

    private NormalizedNode<?, ?> filterNode(NormalizedNode<?, ?> node, MapNode tpMapNode) {
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
