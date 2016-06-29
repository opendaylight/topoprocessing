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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.topoprocessing.impl.translator.TranslatorHelper;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author matej.perina
 *
 */
public class InvNodeTranslator implements NodeTranslator{

    private static final Logger LOG = LoggerFactory.getLogger(InvNodeTranslator.class);
    private static final AugmentationIdentifier NODE_CONNECTOR_AUGMENTATION_IDENTIFIER =
            createNodeConnectorAugmentationIdentifier();


    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        LOG.debug("Transforming OverlayItemWrapper containing Nodes to datastore format");
        IdentifierGenerator idGenerator = new IdentifierGenerator();
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
                    NormalizedNode<?, ?> itemNode = underlayItem.getItem();
                    // prepare supporting nodes
                    Map<QName, Object> keyValues = new HashMap<>();
                    keyValues.put(TopologyQNames.TOPOLOGY_REF, underlayItem.getTopologyId());
                    keyValues.put(TopologyQNames.NODE_REF, underlayItem.getItemId());
                    supportingNodes.withChild(ImmutableNodes.mapEntryBuilder().withNodeIdentifier(
                            new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                                    SupportingNode.QNAME, keyValues)).build());
                    if (wrapper.getAggregatedTerminationPoints() == null) {
                        // prepare termination points
                        Optional<NormalizedNode<?, ?>> terminationPointMapNode = NormalizedNodes.findNode(
                                itemNode, InstanceIdentifiers.NT_TERMINATION_POINT);
                        if (terminationPointMapNode.isPresent()) {
                            Collection<MapEntryNode> terminationPointMapEntries =
                                    ((MapNode) terminationPointMapNode.get()).getValue();
                            for (MapEntryNode terminationPointMapEntry : terminationPointMapEntries) {
                                Optional<NormalizedNode<?, ?>> connectorAugmentationNode =
                                        NormalizedNodes.findNode(terminationPointMapEntry,
                                                NODE_CONNECTOR_AUGMENTATION_IDENTIFIER);
                                if (connectorAugmentationNode.isPresent()){
                                    //if we need to transform the node connector ref into a tp-ref
                                    terminationPoints.addChild(createTerminationPoint(connectorAugmentationNode.get(),
                                            underlayItem.getTopologyId(), underlayItem.getItemId(), idGenerator));
                                } else {
                                    terminationPoints.addChild(terminationPointMapEntry);
                                }
                            }
                        }
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

    /**
     *  Creates a termination point with tp-ref from a given node-connector-ref augmentation node
     *
     * @param connectorAugmentationNode node-connector-ref augmentation node
     * @param topologyId topology ID
     * @param nodeId ID of the enclosing node
     * @param idGenerator generator for creating a new overlay termination-point ID
     * @return overlay termination point with a tp-ref
     */
    private MapEntryNode createTerminationPoint(NormalizedNode<?, ?> connectorAugmentationNode, String topologyId,
            String nodeId, IdentifierGenerator idGenerator) {
        Optional<NormalizedNode<?, ?>> nodeConnectorRef = NormalizedNodes.findNode(connectorAugmentationNode,
                InstanceIdentifiers.INVENTORY_NODE_CONNECTOR_REF_IDENTIFIER.getLastPathArgument());
        Map<QName, Object> nodeConnectorIDMap = ((YangInstanceIdentifier.NodeIdentifierWithPredicates)
                ((YangInstanceIdentifier) nodeConnectorRef.get().getValue()).getLastPathArgument()).getKeyValues();
        String nodeConnectorRefID = (String) nodeConnectorIDMap.get(TopologyQNames.INVENTORY_NODE_ID_QNAME);
        String tpRefValue = TranslatorHelper.createTpRefNT(topologyId, nodeId, nodeConnectorRefID);
        LeafSetEntryNode<String> tpRef = ImmutableLeafSetEntryNodeBuilder.<String>create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<String>(TopologyQNames.TP_REF, tpRefValue))
                .withValue(tpRefValue).build();
        List<LeafSetEntryNode<String>> tpRefs = new ArrayList<>();
        tpRefs.add(tpRef);
        ListNodeBuilder<String, LeafSetEntryNode<String>> leafListBuilder =
                ImmutableLeafSetNodeBuilder.<String>create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TopologyQNames.TP_REF));
        leafListBuilder.withValue(tpRefs);
        String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        return ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                TopologyQNames.NETWORK_TP_ID_QNAME, tpId).withChild(leafListBuilder.build()).build();
    }

    private static AugmentationIdentifier createNodeConnectorAugmentationIdentifier(){
        Set<QName> nodeConnectorRefIdentifier = new HashSet<>();
        nodeConnectorRefIdentifier.add(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME);
        return new AugmentationIdentifier(nodeConnectorRefIdentifier);
    }
}
