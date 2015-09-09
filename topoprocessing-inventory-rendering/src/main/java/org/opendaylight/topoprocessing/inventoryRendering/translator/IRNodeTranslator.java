/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventoryRendering.util.IRQNames;
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
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author andrej.zan
 * @author matej.perina
 *
 */
public class IRNodeTranslator implements NodeTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(IRLinkTranslator.class);
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
        UnderlayItem undItem = null;
        // iterate through overlay items containing nodes
        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            // iterate through overlay item
            for (UnderlayItem underlayItem : overlayItem.getUnderlayItems()) {
                undItem = underlayItem;
                if (! writtenNodes.contains(underlayItem)) {
                    writtenNodes.add(underlayItem);
                    NormalizedNode<?, ?> inventoryItemNode = underlayItem.getLeafNode();
                    // prepare termination points
                    Optional<NormalizedNode<?, ?>> nodeConnectorNode = NormalizedNodes.findNode(
                            inventoryItemNode, NODE_CONNECTOR_IDENTIFIER);
                    if (nodeConnectorNode.isPresent()) {
                        Collection<MapEntryNode> nodeConnectorEntries =
                                ((MapNode) nodeConnectorNode.get()).getValue();
                        for (MapEntryNode nodeConnectorEntry : nodeConnectorEntries) {
                            terminationPoints.addChild(createTerminationPoint(nodeConnectorEntry));
                        }
                    }
                    // prepare node augments
                    nodeAugmentation = createNodeAugmentation(inventoryItemNode);
                }
            }
        }

        return ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, undItem.getItemId())
                .withChild(supportingNodes.build())
                .withChild(terminationPoints.build())
                .withChild(nodeAugmentation)
                .build();
    }

    private AugmentationNode createNodeAugmentation(NormalizedNode<?, ?> inventoryNode) {
        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> nodeAugmentationBuilder =
                ImmutableAugmentationNodeBuilder.create();
        QName augmentationQname = QName.create(
                "urn:opendaylight:topology:inventory:rendering", "2015-08-31", "node-augmentation");
        Set<QName> qnames = new HashSet<>();
        qnames.add(augmentationQname);
        AugmentationIdentifier augId = new AugmentationIdentifier(qnames);
        Set<QName> nodeIdentifier = new HashSet<>();
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)meter"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)software"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)switch-features"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-actions"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)group"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)description"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)serial-number"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)ip-address"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)table"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)manufacturer"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-match-types"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-instructions"));
        AugmentationIdentifier augmentFindId = new AugmentationIdentifier(nodeIdentifier);
        Optional<NormalizedNode<?, ?>> inventoryNodeAugNode = NormalizedNodes.findNode(inventoryNode, augmentFindId);

        nodeAugmentationBuilder.withNodeIdentifier(augId)
                .withChild(ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(augmentationQname))
                        .withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_MANUFACTURER_QNAME,
                                NormalizedNodes.findNode(inventoryNodeAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_NODE_MANUFACTURER_QNAME)).get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_DESCRIPTION_QNAME,
                                NormalizedNodes.findNode(inventoryNodeAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_NODE_DESCRIPTION_QNAME)).get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_HARDWARE_QNAME,
                                NormalizedNodes.findNode(inventoryNodeAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_NODE_HARDWARE_QNAME)).get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_IP_ADDREESS_QNAME,
                                NormalizedNodes.findNode(inventoryNodeAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_NODE_IP_ADDRESS_QNAME)).get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_SERIAL_NUMBER_QNAME,
                                NormalizedNodes.findNode(inventoryNodeAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_NODE_SERIAL_NUMBER_QNAME)).get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_SOFTWARE_QNAME,
                                NormalizedNodes.findNode(inventoryNodeAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_NODE_SOFTWARE_QNAME)).get().getValue()))
                        .build());

        return nodeAugmentationBuilder.build();
    }

    private MapEntryNode createTerminationPoint(MapEntryNode nodeConnectorEntry) {
        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> tpAugmentationBuilder =
                ImmutableAugmentationNodeBuilder.create();

        QName augmentationQname = QName.create(
                "urn:opendaylight:topology:inventory:rendering","2015-08-31","tp-augmentation");
        Set<QName> qnames = new HashSet<>();
        qnames.add(augmentationQname);
        AugmentationIdentifier augId = new AugmentationIdentifier(qnames);
        Set<QName> nodeIdentifier = new HashSet<>();
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)queue"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)port-number"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)advertised-features"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)state"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)current-speed"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)name"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)current-feature"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware-address"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)configuration"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)peer-features"));
        nodeIdentifier.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)maximum-speed"));
        AugmentationIdentifier augmentFindId = new AugmentationIdentifier(nodeIdentifier);
        Optional<NormalizedNode<?, ?>> inventoryNodeConnectorAugNode = NormalizedNodes.findNode(nodeConnectorEntry, augmentFindId);

        tpAugmentationBuilder.withNodeIdentifier(augId)
                .withChild(ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(augmentationQname))
                        .withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_CURRENT_SPEED_QNAME,
                                NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_PORT_CURRENT_SPEED_QNAME))
                                        .get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_HARDWARE_ADDRESS_QNAME,
                                NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_PORT_HARDWARE_ADDRESS_QNAME))
                                        .get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_MAXIMUM_SPEED_QNAME,
                                NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_PORT_MAXIMUM_SPEED_QNAME))
                                        .get().getValue()))
                        .withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_NAME_QNAME,
                                NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(), YangInstanceIdentifier.of(
                                        IRQNames.OPEN_FLOW_PORT_NAME_QNAME))
                                        .get().getValue()))
                        .build());

        Object tpId = nodeConnectorEntry.getChild(
                YangInstanceIdentifier.of(TopologyQNames.NODE_CONNECTOR_ID_QNAME)
                        .getLastPathArgument()).get().getValue();
        MapEntryNode terminationPoint = ImmutableNodes.mapEntryBuilder(
                TerminationPoint.QNAME, TopologyQNames.TP_ID_QNAME, tpId)
                        .withChild(tpAugmentationBuilder.build()).build();
        return terminationPoint;
    }

}