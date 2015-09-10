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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.inventoryRendering.util.IRQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
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
@RunWith(MockitoJUnitRunner.class)
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
        UnderlayItem underlayItem = new UnderlayItem(null, inventoryNode, "topology:1",
                "undItem:1", CorrelationItemEnum.Node);
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
        if(nodeIdOpt.isPresent()){
            String nodeId = (String)nodeIdOpt.get().getValue();
            //Assert.assertEquals("node-id is not correct: " + nodeId, "invNodeId1", nodeId);
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
        if(nodeAugOpt.isPresent()) {
            Optional<DataContainerChild<? extends PathArgument, ?>> nodeAugContainerOpt =
                    ((AugmentationNode)nodeAugOpt.get()).getChild(new NodeIdentifier(augmentationQname));
            Assert.assertTrue("node-augmentation container is missing", nodeAugContainerOpt.isPresent());
            if(nodeAugContainerOpt.isPresent()) {
                ContainerNode nodeAugContainer = (ContainerNode) nodeAugContainerOpt.get();
                String leafValue;
                Optional<DataContainerChild<? extends PathArgument, ?>> leafOpt =
                        nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_SOFTWARE_QNAME));
                Assert.assertTrue("software leaf in node augmentation is missing", leafOpt.isPresent());
                if(leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("software leaf in node augmentation is not correct: " + leafValue,
                            "software", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_HARDWARE_QNAME));
                Assert.assertTrue("hardware leaf in node augmentation is missing", leafOpt.isPresent());
                if(leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("hardware leaf in node augmentation is not correct: " + leafValue,
                            "hardware", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_IP_ADDREESS_QNAME));
                Assert.assertTrue("ip-address leaf in node augmentation is missing", leafOpt.isPresent());
                if(leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("ip-address leaf in node augmentation is not correct: " + leafValue,
                            "ip-address", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_SERIAL_NUMBER_QNAME));
                Assert.assertTrue("serial-number leaf in node augmentation is missing", leafOpt.isPresent());
                if(leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("serial-number leaf in node augmentation is not correct: " + leafValue,
                            "serial-number", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_MANUFACTURER_QNAME));
                Assert.assertTrue("manugacturer leaf in node augmentation is missing", leafOpt.isPresent());
                if(leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("manufacturer leaf in node augmentation is not correct: " + leafValue,
                            "manufacturer", leafValue);
                }
                leafOpt = nodeAugContainer.getChild(new NodeIdentifier(IRQNames.NODE_AUG_DESCRIPTION_QNAME));
                Assert.assertTrue("description leaf in node augmentation is missing", leafOpt.isPresent());
                if(leafOpt.isPresent()) {
                    leafValue = (String) leafOpt.get().getValue();
                    Assert.assertEquals("description leaf in node augmentation is not correct: " + leafValue,
                            "description", leafValue);
                }
            }
        }
    }

    private AugmentationNode createInvNodeAugmentation() {
        //openflow node augmentation
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
        //openflow node connector augmentation
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
                TopologyQNames.NODE_CONNECTOR_ID_QNAME, "nodeConnector1")
                .withChild(invNodeConnectorAugmentBuilder1.build()).build();
        MapEntryNode nodeConnector2 = ImmutableNodes.mapEntryBuilder(NodeConnector.QNAME,
                TopologyQNames.NODE_CONNECTOR_ID_QNAME, "nodeConnector2")
                .withChild(invNodeConnectorAugmentBuilder2.build()).build();

        return ImmutableNodes
                .mapNodeBuilder(NodeConnector.QNAME)
                .withChild(nodeConnector1)
                .withChild(nodeConnector2)
                .build();
    }
}
