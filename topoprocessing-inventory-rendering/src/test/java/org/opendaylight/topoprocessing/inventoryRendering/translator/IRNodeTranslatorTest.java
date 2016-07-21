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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventoryRendering.util.IRQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;

/**
 * @author andrej.zan
 *
 */
public class IRNodeTranslatorTest {
    private IRNodeTranslator nodeTranslator;
    private NormalizedNode<?, ?> translatedNode;

    @Before
    public void startup() {
        nodeTranslator = new IRNodeTranslator();
        NormalizedNode<?, ?> inventoryNode = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.INVENTORY_NODE_ID_QNAME, "invNodeId1")
                .withChild(createInvNodeAugmentation())
                .withChild(createNodeConnectors())
                .build();
        NormalizedNode<?, ?> networkTopologyNode = ImmutableNodes
                .mapEntryBuilder(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                        .network.topology.topology.Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, "undItem")
                .withChild(createTerminationPoints())
                .build();
        Map<Integer, NormalizedNode<?, ?>> targetFields = new HashMap<>(1);
        targetFields.put(0, inventoryNode);
        UnderlayItem underlayItem = new UnderlayItem(networkTopologyNode, targetFields, "topology:1",
                "undItem", CorrelationItemEnum.Node);
        List<UnderlayItem> underlayItems = new ArrayList<UnderlayItem>();
        underlayItems.add(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems, CorrelationItemEnum.Node);
        OverlayItemWrapper wrapper = new OverlayItemWrapper("item:1", overlayItem);

        translatedNode = nodeTranslator.translate(wrapper);
    }

    @Test
    public void testTranslatedNodeId() {
        Optional<DataContainerChild<? extends PathArgument, ?>> nodeIdOpt =
                ((MapEntryNode)translatedNode).getChild(new NodeIdentifier(TopologyQNames.NETWORK_NODE_ID_QNAME));
        Assert.assertTrue("node-id is missing", nodeIdOpt.isPresent());
        if (nodeIdOpt.isPresent()) {
            String nodeId = (String)nodeIdOpt.get().getValue();
            Assert.assertEquals("node-id is not correct: " + nodeId, "undItem", nodeId);
        }
    }

    @Test
    public void testTranslatedInventoryNodeAugment() {
        QName augmentationQname = QName.create(
                "urn:opendaylight:topology:inventory:rendering", "2015-08-31", "node-augmentation");
        Set<QName> qnames = new HashSet<>();
        qnames.add(augmentationQname);
        AugmentationIdentifier augId = new AugmentationIdentifier(qnames);

        Optional<DataContainerChild<? extends PathArgument, ?>> nodeAugOpt =
                ((MapEntryNode)translatedNode).getChild(augId);
        Assert.assertTrue("node-augmentation augmentation is missing", nodeAugOpt.isPresent());
        if (nodeAugOpt.isPresent()) {
            Optional<DataContainerChild<? extends PathArgument, ?>> nodeAugContainerOpt =
                    ((AugmentationNode)nodeAugOpt.get()).getChild(new NodeIdentifier(augmentationQname));
            Assert.assertTrue("node-augmentation container is missing", nodeAugContainerOpt.isPresent());
            if (nodeAugContainerOpt.isPresent()) {
                ContainerNode nodeAugContainer = (ContainerNode) nodeAugContainerOpt.get();
                String leafValue;
                Optional<DataContainerChild<? extends PathArgument, ?>> leafOpt =
                        nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_SOFTWARE_QNAME));
                Assert.assertTrue("software leaf in node augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("software leaf in node augmentation is not correct: " + leafValue,
                            "software", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_HARDWARE_QNAME));
                Assert.assertTrue("hardware leaf in node augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("hardware leaf in node augmentation is not correct: " + leafValue,
                            "hardware", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_IP_ADDREESS_QNAME));
                Assert.assertTrue("ip-address leaf in node augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("ip-address leaf in node augmentation is not correct: " + leafValue,
                            "ip-address", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_SERIAL_NUMBER_QNAME));
                Assert.assertTrue("serial-number leaf in node augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("serial-number leaf in node augmentation is not correct: " + leafValue,
                            "serial-number", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_MANUFACTURER_QNAME));
                Assert.assertTrue("manugacturer leaf in node augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("manufacturer leaf in node augmentation is not correct: " + leafValue,
                            "manufacturer", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_DESCRIPTION_QNAME));
                Assert.assertTrue("description leaf in node augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("description leaf in node augmentation is not correct: " + leafValue,
                            "description", leafValue);
                }
            }
        }
    }

    @Test
    public void testTranslatedTerminationPoint() {
        Optional<DataContainerChild<? extends PathArgument, ?>> terminationPointsOpt =
                ((MapEntryNode)translatedNode).getChild(new NodeIdentifier(TerminationPoint.QNAME));
        Assert.assertTrue("termination-point is missing", terminationPointsOpt.isPresent());
        if (terminationPointsOpt.isPresent()) {
            QName augmentationQname = QName.create(
                    "urn:opendaylight:topology:inventory:rendering", "2015-08-31", "tp-augmentation");
            Set<QName> qnames = new HashSet<>();
            qnames.add(augmentationQname);
            AugmentationIdentifier augId = new AugmentationIdentifier(qnames);
            // extract tp-id and termination point augmentations
            ArrayList<String> tpIds = new ArrayList<String>();
            ArrayList<ContainerNode> tpAugContainers = new ArrayList<ContainerNode>();
            MapNode terminationPoints = (MapNode) terminationPointsOpt.get();
            Collection<MapEntryNode> tpEntries = terminationPoints.getValue();
            Assert.assertEquals("Wrong number of termination points", 2, tpEntries.size());
            for (MapEntryNode terminationPoint : tpEntries) {
                Optional<DataContainerChild<? extends PathArgument, ?>> tpId =
                    terminationPoint.getChild(new NodeIdentifier(TopologyQNames.NETWORK_TP_ID_QNAME));
                Assert.assertTrue("tp-id is missing", tpId.isPresent());
                if (tpId.isPresent()) {
                    tpIds.add((String)tpId.get().getValue());
                }

                Optional<DataContainerChild<? extends PathArgument, ?>> tpAugOpt =
                        ((MapEntryNode)terminationPoint).getChild(augId);
                Assert.assertTrue("tp-augmentation is missing", tpAugOpt.isPresent());
                if (tpAugOpt.isPresent()) {
                    Optional<DataContainerChild<? extends PathArgument, ?>> tpAugContOpt =
                            ((AugmentationNode)tpAugOpt.get()).getChild(new NodeIdentifier(augmentationQname));
                    Assert.assertTrue("tp-augmentation container is missing", tpAugContOpt.isPresent());
                    if (tpAugContOpt.isPresent()) {
                        tpAugContainers.add((ContainerNode) tpAugContOpt.get());
                    }
                }
            }

            // test tp-id. termination-points tp-id has to be from network-topology model
            Assert.assertTrue("termination points tp-id is wrong", tpIds.contains("terminationPoint1"));
            Assert.assertTrue("termination points tp-id is wrong", tpIds.contains("terminationPoint2"));
            // test termination point augmentations
            for (int i = 0; i < 2; i++) {
                String suffix = "";
                if (tpIds.get(i).equals("terminationPoint2")) {
                    suffix = "2";
                }
                ContainerNode tpAugContainer = tpAugContainers.get(i);
                String leafValue;
                Optional<DataContainerChild<? extends PathArgument, ?>> leafOpt =
                        tpAugContainer.getChild(new NodeIdentifier(IRQNames.TP_AUG_CURRENT_SPEED_QNAME));
                Assert.assertTrue("current-speed leaf in t.p. augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("current-speed leaf in t.p. augmentation is not correct: " + leafValue,
                            "current-speed" + suffix, leafValue);
                }
                leafOpt = tpAugContainer.getChild(new NodeIdentifier(IRQNames.TP_AUG_HARDWARE_ADDRESS_QNAME));
                Assert.assertTrue("hardware-address leaf in t.p. augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("hardware-address leaf in t.p. augmentation is not correct: " + leafValue,
                            "hardware-address" + suffix, leafValue);
                }
                leafOpt = tpAugContainer.getChild(new NodeIdentifier(IRQNames.TP_AUG_MAXIMUM_SPEED_QNAME));
                Assert.assertTrue("maximum-speed leaf in t.p. augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("maximum-speed leaf in t.p. augmentation is not correct: " + leafValue,
                            "maximum-speed" + suffix, leafValue);
                }
                leafOpt = tpAugContainer.getChild(new NodeIdentifier(IRQNames.TP_AUG_NAME_QNAME));
                Assert.assertTrue("name leaf in t.p. augmentation is missing", leafOpt.isPresent());
                if (leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("name leaf in t.p. augmentation is not correct: " + leafValue,
                            "name" + suffix, leafValue);
                }
            }
        }
    }

    private AugmentationNode createInvNodeAugmentation() {
        // openflow node augmentation
        Set<QName> qnames = new HashSet<>();
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)meter"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)software"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)switch-features"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-actions"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)group"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)description"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)serial-number"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)ip-address"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)table"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)manufacturer"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-match-types"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-instructions"));
        AugmentationIdentifier invNodeAugmentationId = new AugmentationIdentifier(qnames);

        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> invNodeAugmentBuilder =
                ImmutableAugmentationNodeBuilder.create();
        invNodeAugmentBuilder.withNodeIdentifier(invNodeAugmentationId)
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)software"), "software"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware"), "hardware"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)serial-number"), "serial-number"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)ip-address"), "ip-address"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)manufacturer"), "manufacturer"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)description"), "description"));
        return invNodeAugmentBuilder.build();
    }

    private MapNode createNodeConnectors() {
        // openflow node connector augmentation
        Set<QName> qnames = new HashSet<>();
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)queue"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)port-number"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)advertised-features"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)state"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)current-speed"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)name"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)current-feature"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware-address"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)configuration"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)peer-features"));
        qnames.add(QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)maximum-speed"));
        AugmentationIdentifier invNodeConnectorAugmentationId = new AugmentationIdentifier(qnames);

        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> invNodeConnectorAugmentBuilder1 =
                ImmutableAugmentationNodeBuilder.create()
                .withNodeIdentifier(invNodeConnectorAugmentationId)
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)current-speed"), "current-speed"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware-address"), "hardware-address"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)maximum-speed"), "maximum-speed"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)name"), "name"));

        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> invNodeConnectorAugmentBuilder2 =
                ImmutableAugmentationNodeBuilder.create()
                .withNodeIdentifier(invNodeConnectorAugmentationId)
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)current-speed"), "current-speed2"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware-address"), "hardware-address2"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)maximum-speed"), "maximum-speed2"))
                .withChild(ImmutableNodes.leafNode(QName.create(
                        "(urn:opendaylight:flow:inventory?revision=2013-08-19)name"), "name2"));

        MapEntryNode nodeConnector1 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME,
                IRQNames.NODE_CONNECTOR_ID_QNAME, "nodeConnector1")
                .withChild(invNodeConnectorAugmentBuilder1.build()).build();
        MapEntryNode nodeConnector2 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME,
                IRQNames.NODE_CONNECTOR_ID_QNAME, "nodeConnector2")
                .withChild(invNodeConnectorAugmentBuilder2.build()).build();

        return ImmutableNodes
                .mapNodeBuilder(NodeConnector.QNAME)
                .withChild(nodeConnector1)
                .withChild(nodeConnector2)
                .build();
    }

    private MapNode createTerminationPoints() {
        Set<QName> qnames = new HashSet<>();
        qnames.add(IRQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME);
        AugmentationIdentifier ncRefAugmentIdentifier = new AugmentationIdentifier(qnames);

        YangInstanceIdentifier ncIdent1 = YangInstanceIdentifier.create(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NodeConnector.QNAME,
                        IRQNames.NODE_CONNECTOR_ID_QNAME, "nodeConnector1"));
        YangInstanceIdentifier ncIdent2 = YangInstanceIdentifier.create(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NodeConnector.QNAME,
                        IRQNames.NODE_CONNECTOR_ID_QNAME, "nodeConnector2"));

        DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> ncRefAugmentationBuilder =
                ImmutableAugmentationNodeBuilder.create();
        ncRefAugmentationBuilder.withNodeIdentifier(ncRefAugmentIdentifier)
                .withChild(ImmutableNodes.leafNode(IRQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, ncIdent1));
        MapEntryNode terminationPoint1 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                TopologyQNames.NETWORK_TP_ID_QNAME, "terminationPoint1")
                .withChild(ncRefAugmentationBuilder.build())
                .build();

        ncRefAugmentationBuilder = ImmutableAugmentationNodeBuilder.create();
        ncRefAugmentationBuilder.withNodeIdentifier(ncRefAugmentIdentifier)
                .withChild(ImmutableNodes.leafNode(IRQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME, ncIdent2));
        MapEntryNode terminationPoint2 = ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                TopologyQNames.NETWORK_TP_ID_QNAME, "terminationPoint2")
                .withChild(ncRefAugmentationBuilder.build())
                .build();

        return ImmutableNodes
                .mapNodeBuilder(TerminationPoint.QNAME)
                .withChild(terminationPoint1)
                .withChild(terminationPoint2)
                .build();
    }

}
