/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.translator;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventoryRendering.util.InventoryRenderingQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author andrej.zan
 *
 */
public class InventoryRenderingNodeTranslator implements NodeTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryRenderingLinkTranslator.class);
    private static final YangInstanceIdentifier NODE_CONNECTOR_IDENTIFIER = YangInstanceIdentifier
            .of(NodeConnector.QNAME);

    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        LOG.debug("Transforming OverlayItemWrapper containing Nodes to datastore format");
        List<UnderlayItem> writtenNodes = new ArrayList<>();
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingNodes = ImmutableNodes.mapNodeBuilder(
                SupportingNode.QNAME);
        CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        AugmentationNode nodeAugmentation = null;
        // iterate through overlay items containing nodes
        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            // iterate through overlay item
            for (UnderlayItem underlayItem : overlayItem.getUnderlayItems()) {
                if (! writtenNodes.contains(underlayItem)) {
                    writtenNodes.add(underlayItem);
                    NormalizedNode<?, ?> inventoryItemNode = underlayItem.getItem();
                    // this will not be here
                    Map<QName, Object> keyValues = new HashMap<>();
                    keyValues.put(TopologyQNames.TOPOLOGY_REF, underlayItem.getTopologyId());
                    keyValues.put(TopologyQNames.NODE_REF, underlayItem.getItemId());
                    supportingNodes.withChild(ImmutableNodes.mapEntryBuilder().withNodeIdentifier(
                            new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                                    SupportingNode.QNAME, keyValues)).build());
                    // prepare termination points
                    Optional<NormalizedNode<?, ?>> nodeConnectorNode = NormalizedNodes.findNode(
                            inventoryItemNode, NODE_CONNECTOR_IDENTIFIER);
                    if (nodeConnectorNode.isPresent()) {
                        Collection<MapEntryNode> nodeConnectorEntries =
                                ((MapNode) nodeConnectorNode.get()).getValue();
                        for (MapEntryNode nodeConnectorEntry : nodeConnectorEntries) {
                            Object tpId = nodeConnectorEntry.getAttributeValue(
                                    TopologyQNames.NODE_CONNECTOR_ID_QNAME);
                            MapEntryNode terminationPoint = ImmutableNodes.mapEntryBuilder(
                                    TerminationPoint.QNAME, TopologyQNames.TP_ID_QNAME, tpId).build();
                            terminationPoints.addChild(terminationPoint);
                        }
                    }
                    // prepare node augments
                   nodeAugmentation = createNodeAugmentation(inventoryItemNode);
                }
            }
        }

        return ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, wrapper.getId())
                .withChild(supportingNodes.build())
                .withChild(terminationPoints.build())
                .withChild(nodeAugmentation)
                .build();
    }

    private AugmentationNode createNodeAugmentation(NormalizedNode<?, ?> inventoryNode) {
        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> nodeAugmentationBuilder =
                ImmutableAugmentationNodeBuilder.create();

        QName augmentationQname = QName.create(
                "(urn:opendaylight:topology:inventory:rendering revision=2015-08-31)node-augmentation");
        Set<QName> qnames = new HashSet<>();
        qnames.add(augmentationQname);
        AugmentationIdentifier augId = new AugmentationIdentifier(qnames);
        nodeAugmentationBuilder.withNodeIdentifier(augId)
                .withChild(ImmutableNodes.leafNode(InventoryRenderingQNames.NODE_AUG_MANUFACTURER_QNAME,
                        NormalizedNodes.findNode(inventoryNode, YangInstanceIdentifier.of(
                                InventoryRenderingQNames.OPEN_FLOW_NODE_MANUFACTURER_QNAME)).orNull()))
                .withChild(ImmutableNodes.leafNode(InventoryRenderingQNames.NODE_AUG_DESCRIPTION_QNAME,
                        NormalizedNodes.findNode(inventoryNode, YangInstanceIdentifier.of(
                                InventoryRenderingQNames.OPEN_FLOW_NODE_DESCRIPTION_QNAME)).orNull()))
                .withChild(ImmutableNodes.leafNode(InventoryRenderingQNames.NODE_AUG_HARDWARE_QNAME,
                        NormalizedNodes.findNode(inventoryNode, YangInstanceIdentifier.of(
                                InventoryRenderingQNames.OPEN_FLOW_NODE_HARDWARE_QNAME)).orNull()))
                .withChild(ImmutableNodes.leafNode(InventoryRenderingQNames.NODE_AUG_IP_ADDREESS_QNAME,
                        NormalizedNodes.findNode(inventoryNode, YangInstanceIdentifier.of(
                                InventoryRenderingQNames.OPEN_FLOW_NODE_IP_ADDRESS_QNAME)).orNull()))
                .withChild(ImmutableNodes.leafNode(InventoryRenderingQNames.NODE_AUG_SERIAL_NUMBER_QNAME,
                        NormalizedNodes.findNode(inventoryNode, YangInstanceIdentifier.of(
                                InventoryRenderingQNames.OPEN_FLOW_NODE_SERIAL_NUMBER_QNAME)).orNull()))
                .withChild(ImmutableNodes.leafNode(InventoryRenderingQNames.NODE_AUG_SOFTWARE_QNAME,
                        NormalizedNodes.findNode(inventoryNode, YangInstanceIdentifier.of(
                                InventoryRenderingQNames.OPEN_FLOW_NODE_SOFTWARE_QNAME)).orNull()));

        return nodeAugmentationBuilder.build();
    }

}
