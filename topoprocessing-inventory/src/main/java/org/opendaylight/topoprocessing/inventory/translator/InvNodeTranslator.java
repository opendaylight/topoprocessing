/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventory.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author matej.perina
 *
 */
public class InvNodeTranslator implements NodeTranslator{

    private static final Logger LOG = LoggerFactory.getLogger(InvNodeTranslator.class);
    private static final YangInstanceIdentifier TP_IDENTIFIER = YangInstanceIdentifier.of(TerminationPoint.QNAME);

    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        LOG.debug("Transforming OverlayItemWrapper containing Nodes to datastore format");
        List<UnderlayItem> writtenNodes = new ArrayList<>();
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingNodes = ImmutableNodes.mapNodeBuilder(
                SupportingNode.QNAME);
        CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        // iterate through overlay items containing nodes
        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            // iterate through overlay item
            for (UnderlayItem underlayItem : overlayItem.getUnderlayItems()) {
                if (! writtenNodes.contains(underlayItem)) {
                    writtenNodes.add(underlayItem);
                    addSupportingNodes(underlayItem, supportingNodes);
                    if (wrapper.getAggregatedTerminationPoints() == null) {
                        prepareTerminationPoints(underlayItem, terminationPoints);
                    }
                }
            }
        }
        if (wrapper.getAggregatedTerminationPoints() == null) {
            return ImmutableNodes
                    .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, wrapper.getId())
                    .withChild(supportingNodes.build())
                    .withChild(terminationPoints.build())
                    .build();
        } else {
            return ImmutableNodes
                    .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, wrapper.getId())
                    .withChild(supportingNodes.build())
                    .withChild(wrapper.getAggregatedTerminationPoints())
                    .build();
        }
    }

    private void addSupportingNodes(UnderlayItem underlayItem,
            CollectionNodeBuilder<MapEntryNode, MapNode> supportingNodes) {
     // prepare supporting nodes
        Map<QName, Object> keyValues = new HashMap<>();
        keyValues.put(TopologyQNames.TOPOLOGY_REF, underlayItem.getTopologyId());
        keyValues.put(TopologyQNames.NODE_REF, underlayItem.getItemId());
        supportingNodes.withChild(ImmutableNodes.mapEntryBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                        SupportingNode.QNAME, keyValues)).build());
    }

    private void prepareTerminationPoints(UnderlayItem underlayItem,
            CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints) {
        NormalizedNode<?, ?> itemNode = underlayItem.getItem();
        Optional<NormalizedNode<?, ?>> terminationPointMapNode = NormalizedNodes.findNode(
                itemNode, TP_IDENTIFIER);
        if (terminationPointMapNode.isPresent()) {
            Collection<MapEntryNode> terminationPointMapEntries =
                    ((MapNode) terminationPointMapNode.get()).getValue();
            for (MapEntryNode terminationPointMapEntry : terminationPointMapEntries) {
                terminationPoints.addChild(terminationPointMapEntry);
            }
        }
    }
}
