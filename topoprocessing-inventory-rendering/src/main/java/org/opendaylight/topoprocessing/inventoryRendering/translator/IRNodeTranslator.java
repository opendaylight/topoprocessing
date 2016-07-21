/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.translator;

import com.google.common.base.Optional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventoryRendering.util.IRInstanceIdentifiers;
import org.opendaylight.topoprocessing.inventoryRendering.util.IRQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author andrej.zan
 * @author matej.perina
 *
 */
public class IRNodeTranslator implements NodeTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(IRNodeTranslator.class);
    // urn:opendaylight:topology:inventory:rendering node-augment augmentation - here we will store node information.
    // This is in network-topology model.
    private static final AugmentationIdentifier NODE_AUGMENTATION_IDENTIFIER = createNodeAugIdentifier();
     // urn:opendaylight:topology:inventory:rendering tp-augment augmentation - here we will store tp information. This
    // is in network-topology model.
    private static final AugmentationIdentifier TP_AUGMENTATION_IDENTIFIER = createTPAugIdentifier();
     // urn:opendaylight:flow:inventory flow-capable-node augmentation - here are information about node. This is in
    // inventory model
    private static final AugmentationIdentifier INV_NODE_AUGMENTATION_IDENTIFIER = createInvNodeAugIdentifier();
     // urn:opendaylight:flow:inventory flow-capable-node-connector-update-fields augmentation - here are
    // information about node-connector. This is in inventory model
    private static final AugmentationIdentifier NODE_CONNECTOR_AUGMENTATION_IDENTIFIER = createNCAugIdentifier();
     // urn:opendaylight:model:topology:inventory node-connector-ref augmentation - here are information about
    // node-connector. This is in network-topology model
    private static final AugmentationIdentifier INVENTORY_NODE_CONNECTOR_REF_AUGMENTATION_IDENTIFIER =
            createInventoryNodeConnectorRefAugIdentifier();

    /**
     * Translate {@link OverlayItem} item into {@linkplain NormalizedNode} node from network-topology model.
     * Expects that corresponding {@link UnderlayItem} item contains node from network-topology model in its item
     * attribute, node from inventory in its leafNode attribute and node-id from node from network-topology model in its
     * itemId attribute.
     *
     * @param wrapper wraps OverlayItems
     */
    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        LOG.debug("Transforming OverlayItemWrapper containing Nodes to datastore format");
        CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        AugmentationNode nodeAugmentation = null;
        // in this case, we always have only one overlay item with one underlay item
        OverlayItem overlayItem = wrapper.getOverlayItems().peek();
        UnderlayItem underlayItem = overlayItem.getUnderlayItems().peek();
        NormalizedNode<?, ?> inventoryItemNode = underlayItem.getLeafNodes().get(0);
        // prepare termination points
        Optional<NormalizedNode<?, ?>> nodeConnectorNode = NormalizedNodes.findNode(
                inventoryItemNode, IRInstanceIdentifiers.NODE_CONNECTOR_IDENTIFIER);
        if (nodeConnectorNode.isPresent()) {
            Optional<NormalizedNode<?,?>> terminationPointNode = NormalizedNodes.findNode(
                    underlayItem.getItem(), IRInstanceIdentifiers.TP_IDENTIFIER);
            // we want tp-ids from network-topology model, so we have to map them to corresponding node-connector id
            if (terminationPointNode.isPresent()) {
                Map<PathArgument, String> terminationPointsIds = getTerminationPointsIds(terminationPointNode.get());
                Collection<MapEntryNode> nodeConnectorEntries =
                        ((MapNode) nodeConnectorNode.get()).getValue();
                for (MapEntryNode nodeConnectorEntry : nodeConnectorEntries) {
                    MapEntryNode tp = createTerminationPoint(nodeConnectorEntry, terminationPointsIds);
                    if (tp != null) {
                        terminationPoints.addChild(tp);
                    }
                }
            }
        }
        // prepare node augments
        nodeAugmentation = createNodeAugmentation(inventoryItemNode);

        // we don't use id from wrapper for node-id of our new node. We have to change it to the correct one
        // without this step, writer is unable to write our node, because id in wrapper differs from node-id in our node
        wrapper.setId(underlayItem.getItemId());

        return ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, underlayItem.getItemId())
                .withChild(terminationPoints.build())
                .withChild(nodeAugmentation)
                .build();
    }

    private AugmentationNode createNodeAugmentation(NormalizedNode<?, ?> inventoryNode) {
        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> nodeAugmentationBuilder =
                ImmutableAugmentationNodeBuilder.create().withNodeIdentifier(NODE_AUGMENTATION_IDENTIFIER);
        Optional<NormalizedNode<?, ?>> inventoryNodeAugNode =
                NormalizedNodes.findNode(inventoryNode, INV_NODE_AUGMENTATION_IDENTIFIER);
        //node augmentation -> node augmentation container -> leaf nodes
        if (inventoryNodeAugNode.isPresent()) {
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> nodeAugContainerBuilder =
                    ImmutableContainerNodeBuilder.create();
            nodeAugContainerBuilder.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(
                    IRQNames.NODE_AUGMENTATION_QNAME));

            Optional<NormalizedNode<?, ?>> fieldNode = NormalizedNodes.findNode(inventoryNodeAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_NODE_MANUFACTURER_IDENTIFIER);
            if (fieldNode.isPresent()) {
                nodeAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_MANUFACTURER_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_NODE_DESCRIPTION_IDENTIFIER);
            if (fieldNode.isPresent()) {
                nodeAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_DESCRIPTION_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_NODE_HARDWARE_IDENTIFIER);
            if (fieldNode.isPresent()) {
                nodeAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_HARDWARE_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_NODE_IP_ADDRESS_IDENTIFIER);
            if (fieldNode.isPresent()) {
                nodeAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_IP_ADDREESS_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_NODE_SERIAL_NUMBER_IDENTIFIER);
            if (fieldNode.isPresent()) {
                nodeAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_SERIAL_NUMBER_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_NODE_SOFTWARE_IDENTIFIER);
            if (fieldNode.isPresent()) {
                nodeAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.NODE_AUG_SOFTWARE_QNAME,
                        fieldNode.get().getValue()));
            }

            nodeAugmentationBuilder.withChild(nodeAugContainerBuilder.build());
        }
        return nodeAugmentationBuilder.build();
    }

    private MapEntryNode createTerminationPoint(MapEntryNode nodeConnectorEntry,
            Map<PathArgument, String> terminationPointsIds) {
        String terminationPointId;
        //path to current node-connector
        PathArgument nodeConnectorPath = nodeConnectorEntry.getIdentifier();
        terminationPointId = terminationPointsIds.remove(nodeConnectorPath);

        if (terminationPointId == null) {
            return null;
        }

        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> tpAugmentationBuilder =
                ImmutableAugmentationNodeBuilder.create().withNodeIdentifier(TP_AUGMENTATION_IDENTIFIER);
        Optional<NormalizedNode<?, ?>> inventoryNodeConnectorAugNode =
                NormalizedNodes.findNode(nodeConnectorEntry, NODE_CONNECTOR_AUGMENTATION_IDENTIFIER);
        //t.p. augmentation -> t.p. augmentation container -> leaf nodes
        if (inventoryNodeConnectorAugNode.isPresent()) {
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> tpAugContainerBuilder =
                    ImmutableContainerNodeBuilder.create();
            tpAugContainerBuilder.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(
                    IRQNames.TP_AUGMENTATION_QNAME));

            Optional<NormalizedNode<?, ?>> fieldNode = NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_PORT_CURRENT_SPEED_IDENTIFIER);
            if (fieldNode.isPresent()) {
                tpAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_CURRENT_SPEED_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_PORT_HARDWARE_ADDRESS_IDENTIFIER);
            if (fieldNode.isPresent()) {
                tpAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_HARDWARE_ADDRESS_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_PORT_MAXIMUM_SPEED_IDENTIFIER);
            if (fieldNode.isPresent()) {
                tpAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_MAXIMUM_SPEED_QNAME,
                        fieldNode.get().getValue()));
            }
            fieldNode = NormalizedNodes.findNode(inventoryNodeConnectorAugNode.get(),
                    IRInstanceIdentifiers.OPEN_FLOW_PORT_NAME_IDENTIFIER);
            if (fieldNode.isPresent()) {
                tpAugContainerBuilder.withChild(ImmutableNodes.leafNode(IRQNames.TP_AUG_NAME_QNAME,
                        fieldNode.get().getValue()));
            }

            tpAugmentationBuilder.withChild(tpAugContainerBuilder.build());
        }
        return ImmutableNodes.mapEntryBuilder( TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                terminationPointId).withChild(tpAugmentationBuilder.build()).build();
    }

    private Map<PathArgument, String> getTerminationPointsIds(NormalizedNode<?, ?> terminationPointsNode) {
        Collection<MapEntryNode> terminationPointsEntries =
                ((MapNode) terminationPointsNode).getValue();
        Map<PathArgument, String> terminationPointsIds = new HashMap<PathArgument, String>();
        for (MapEntryNode terminationPointEntry : terminationPointsEntries) {
            String tpId = NormalizedNodes.findNode(terminationPointEntry,
                    IRInstanceIdentifiers.TP_ID_IDENTIFIER).get().getValue().toString();
            // termination-point inventory-node-connector-ref refer to specific node-connector id
            Optional<NormalizedNode<?, ?>> nodeConnectorRefAugment = NormalizedNodes.findNode(terminationPointEntry,
                    INVENTORY_NODE_CONNECTOR_REF_AUGMENTATION_IDENTIFIER);

            YangInstanceIdentifier ncRefIdent =
                    (YangInstanceIdentifier) NormalizedNodes.findNode(nodeConnectorRefAugment.get(),
                    IRInstanceIdentifiers.INVENTORY_NODE_CONNECTOR_REF_IDENTIFIER).get().getValue();
            // key is path to node-connector id and value is corresponding termination-point tp-id
            terminationPointsIds.put(ncRefIdent.getLastPathArgument(), tpId);
        }
        return terminationPointsIds;
    }

    private static AugmentationIdentifier createNodeAugIdentifier() {
        Set<QName> qnames = new HashSet<>();
        qnames.add(IRQNames.NODE_AUGMENTATION_QNAME);
        return new AugmentationIdentifier(qnames);
    }

    private static AugmentationIdentifier createTPAugIdentifier() {
        Set<QName> qnames = new HashSet<>();
        qnames.add(IRQNames.TP_AUGMENTATION_QNAME);
        return new AugmentationIdentifier(qnames);
    }

    private static AugmentationIdentifier createInventoryNodeConnectorRefAugIdentifier() {
        Set<QName> qnames = new HashSet<>();
        qnames.add(IRQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME);
        return new AugmentationIdentifier(qnames);
    }

    private static AugmentationIdentifier createInvNodeAugIdentifier() {
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
        return new AugmentationIdentifier(nodeIdentifier);
    }

    private static AugmentationIdentifier createNCAugIdentifier() {
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
        return new AugmentationIdentifier(nodeIdentifier);
    }

}