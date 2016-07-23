/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.testUtilities.TestLinkCreator;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 *
 * @author martin.dindoffer
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LinkCalculatorTest {
    private static final String TOPOLOGY_ID = "mytopo:1";
    private static final TestLinkCreator DUMMY_LINK_CREATOR = new TestLinkCreator();

    private static final String UNDERLAY_NODE_ID_1 = "node:1:1";
    private static final String UNDERLAY_NODE_ID_2 = "node:1:2";
    private static final String UNDERLAY_NODE_ID_3 = "node:1:3";
    private static final String UNDERLAY_NODE_ID_4 = "node:1:4";
    private static final String OVERLAY_NODE_ID_1 = "node:1";
    private static final String OVERLAY_NODE_ID_2 = "node:2";
    private static final String OVERLAY_NODE_ID_3 = "node:3";

    private static final String LINK_ID_1 = "link:1";
    private static final String LINK_ID_2 = "link:2";
    private static final String LINK_ID_3 = "link:3";

    private LinkCalculator ntLinkCalculator;

    @Mock private TopologyManager mockManager;

    @Before
    public void setUp() {
        ntLinkCalculator = new LinkCalculator(TOPOLOGY_ID, NetworkTopologyModel.class);
        ntLinkCalculator.setTopologyManager(mockManager);
    }

    /**
     * Creates a dummy data structure for testing purposes. Consists of 3
     * underlying links, 3 underlying nodes, 3 overlay nodes and 2 matched
     * overlay links. Underlying structure (fields annotated with ~ don't
     * exist): link:1 (node:1:1, node:1:2) link:2 (node:1:3, ~node:1:4)
     * link:3(node:1:2, node:1:3)
     */
    private void createDummyData() {
        YangInstanceIdentifier yiid;
        // create 2 links (stored in waitingLinks)
        LeafNode<String> link1Source = ImmutableNodes.leafNode(TopologyQNames.LINK_SOURCE_NODE_QNAME,
                UNDERLAY_NODE_ID_1);
        LeafNode<String> link2Source = ImmutableNodes.leafNode(TopologyQNames.LINK_SOURCE_NODE_QNAME,
                UNDERLAY_NODE_ID_3);

        ContainerNode source1Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Source.QNAME)).withChild(link1Source).build();
        ContainerNode source2Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Source.QNAME)).withChild(link2Source).build();

        LeafNode<String> link1Dest = ImmutableNodes.leafNode(TopologyQNames.LINK_DEST_NODE_QNAME, UNDERLAY_NODE_ID_2);
        LeafNode<String> link2Dest = ImmutableNodes.leafNode(TopologyQNames.LINK_DEST_NODE_QNAME, UNDERLAY_NODE_ID_4);
        ContainerNode dest1Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Destination.QNAME)).withChild(link1Dest).build();
        ContainerNode dest2Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Destination.QNAME)).withChild(link2Dest).build();

        MapEntryNode link1 = ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, LINK_ID_1)
                .withChild(source1Container).withChild(dest1Container).build();
        MapEntryNode link2 = ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, LINK_ID_2)
                .withChild(source2Container).withChild(dest2Container).build();

        yiid = DUMMY_LINK_CREATOR.createNodeIdYiid(LINK_ID_1);
        UnderlayItem item = new UnderlayItem(link1, null, TOPOLOGY_ID, LINK_ID_1, CorrelationItemEnum.Link);
        ntLinkCalculator.processCreatedChanges(yiid, item, TOPOLOGY_ID);

        yiid = DUMMY_LINK_CREATOR.createNodeIdYiid(LINK_ID_2);
        item = new UnderlayItem(link2, null, TOPOLOGY_ID, LINK_ID_2, CorrelationItemEnum.Link);
        ntLinkCalculator.processCreatedChanges(yiid, item, TOPOLOGY_ID);

        // create 2 supporting nodes under two overlay nodes
        yiid = YangInstanceIdentifier.builder()
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_1).build();
        Map<QName, Object> suppNode1KeyValues = new HashMap<>();
        suppNode1KeyValues.put(TopologyQNames.TOPOLOGY_REF, TOPOLOGY_ID);
        suppNode1KeyValues.put(TopologyQNames.NODE_REF, UNDERLAY_NODE_ID_1);
        MapEntryNode menSuppNode1 = ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingNode.QNAME, suppNode1KeyValues)).build();
        MapNode suppNode1List = ImmutableNodes.mapNodeBuilder(SupportingNode.QNAME).addChild(menSuppNode1).build();
        MapEntryNode overlayNode1 = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_1)
                .addChild(suppNode1List).build();
        item = new UnderlayItem(overlayNode1, null, TOPOLOGY_ID, OVERLAY_NODE_ID_1, CorrelationItemEnum.Node);
        // overlayNode1 is created
        ntLinkCalculator.processCreatedChanges(yiid, item, TOPOLOGY_ID);

        yiid = YangInstanceIdentifier.builder()
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_2).build();
        Map<QName, Object> suppNode2KeyValues = new HashMap<>();
        suppNode2KeyValues.put(TopologyQNames.TOPOLOGY_REF, TOPOLOGY_ID);
        suppNode2KeyValues.put(TopologyQNames.NODE_REF, UNDERLAY_NODE_ID_2);
        MapEntryNode menSuppNode2 = ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingNode.QNAME, suppNode2KeyValues)).build();
        MapNode suppNode2List = ImmutableNodes.mapNodeBuilder(SupportingNode.QNAME).addChild(menSuppNode2).build();
        MapEntryNode overlayNode2 = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_2)
                .addChild(suppNode2List).build();
        item = new UnderlayItem(overlayNode2, null, TOPOLOGY_ID, OVERLAY_NODE_ID_2, CorrelationItemEnum.Node);
        // overlayNode2 is created and link:1 is matched
        ntLinkCalculator.processCreatedChanges(yiid, item, TOPOLOGY_ID);

        // create a third supporting node under a third overlay node
        yiid = YangInstanceIdentifier.builder()
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_3).build();
        Map<QName, Object> suppNode3KeyValues = new HashMap<>();
        suppNode3KeyValues.put(TopologyQNames.TOPOLOGY_REF, TOPOLOGY_ID);
        suppNode3KeyValues.put(TopologyQNames.NODE_REF, UNDERLAY_NODE_ID_3);
        MapEntryNode menSuppNode3 = ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingNode.QNAME, suppNode3KeyValues)).build();
        MapNode suppNode3List = ImmutableNodes.mapNodeBuilder(SupportingNode.QNAME).addChild(menSuppNode3).build();
        MapEntryNode node3 = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_3)
                .addChild(suppNode3List).build();
        item = new UnderlayItem(node3, null, TOPOLOGY_ID, OVERLAY_NODE_ID_3, CorrelationItemEnum.Node);

        ntLinkCalculator.processCreatedChanges(yiid, item, TOPOLOGY_ID);

        // create another matched link (link:3)
        LeafNode<String> link3Source = ImmutableNodes.leafNode(TopologyQNames.LINK_SOURCE_NODE_QNAME,
                UNDERLAY_NODE_ID_2);
        ContainerNode source3Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Source.QNAME)).withChild(link3Source).build();
        LeafNode<String> link3Dest = ImmutableNodes.leafNode(TopologyQNames.LINK_DEST_NODE_QNAME, UNDERLAY_NODE_ID_3);
        ContainerNode dest3Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Destination.QNAME)).withChild(link3Dest).build();
        MapEntryNode link3 = ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, LINK_ID_3)
                .withChild(source3Container).withChild(dest3Container).build();
        yiid = DUMMY_LINK_CREATOR.createNodeIdYiid(LINK_ID_3);
        item = new UnderlayItem(link3, null, TOPOLOGY_ID, LINK_ID_3, CorrelationItemEnum.Link);
        ntLinkCalculator.processCreatedChanges(yiid, item, TOPOLOGY_ID);
    }

    @Test
    public void testProcessCreatedChangesOnNTModel() {
        createDummyData();
        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Matchers.any());
    }

    @Test
    public void testProcessUpdatedChanges() {
        createDummyData();
        Mockito.reset(mockManager);
        // update a matched link destination to another existing node, expecting
        // it to remain matched
        LeafNode<String> link1Source = ImmutableNodes.leafNode(TopologyQNames.LINK_SOURCE_NODE_QNAME,
                UNDERLAY_NODE_ID_1);

        ContainerNode source1Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Source.QNAME)).withChild(link1Source).build();

        LeafNode<String> link1Dest = ImmutableNodes.leafNode(TopologyQNames.LINK_DEST_NODE_QNAME, UNDERLAY_NODE_ID_3);
        ContainerNode dest1Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Destination.QNAME)).withChild(link1Dest).build();

        MapEntryNode link1 = ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, LINK_ID_1)
                .withChild(source1Container).withChild(dest1Container).build();

        YangInstanceIdentifier yiid;
        yiid = DUMMY_LINK_CREATOR.createNodeIdYiid(LINK_ID_1);
        UnderlayItem item = new UnderlayItem(link1, null, TOPOLOGY_ID, LINK_ID_1, CorrelationItemEnum.Link);
        ntLinkCalculator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);
        Mockito.verify(mockManager).updateOverlayItem((OverlayItem) Matchers.any());

        // update a matched link destination to a non-existing node, expecting a
        // demotion to a waiting link
        link1Dest = ImmutableNodes.leafNode(TopologyQNames.LINK_DEST_NODE_QNAME, UNDERLAY_NODE_ID_4);
        dest1Container = ImmutableContainerNodeBuilder.create(ImmutableNodes.containerNode(Destination.QNAME))
                .withChild(link1Dest).build();
        link1 = ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, LINK_ID_1)
                .withChild(source1Container).withChild(dest1Container).build();
        item = new UnderlayItem(link1, null, TOPOLOGY_ID, LINK_ID_1, CorrelationItemEnum.Link);
        ntLinkCalculator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);
        Mockito.verify(mockManager).removeOverlayItem((OverlayItem) Matchers.any());

        // update a waiting link expecting promotion to a matched link
        LeafNode<String> link2Source = ImmutableNodes.leafNode(TopologyQNames.LINK_SOURCE_NODE_QNAME,
                UNDERLAY_NODE_ID_3);
        ContainerNode source2Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Source.QNAME)).withChild(link2Source).build();
        LeafNode<String> link2Dest = ImmutableNodes.leafNode(TopologyQNames.LINK_DEST_NODE_QNAME, UNDERLAY_NODE_ID_1);
        ContainerNode dest2Container = ImmutableContainerNodeBuilder
                .create(ImmutableNodes.containerNode(Destination.QNAME)).withChild(link2Dest).build();
        MapEntryNode link2 = ImmutableNodes.mapEntryBuilder(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, LINK_ID_2)
                .withChild(source2Container).withChild(dest2Container).build();
        yiid = DUMMY_LINK_CREATOR.createNodeIdYiid(LINK_ID_2);
        item = new UnderlayItem(link2, null, TOPOLOGY_ID, LINK_ID_2, CorrelationItemEnum.Link);
        ntLinkCalculator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);
        Mockito.verify(mockManager).addOverlayItem((OverlayItem) Matchers.any());

        // update an overlayNode expecting the waiting link to be matched
        // create an underlaying node:1:4 and wrap it into existing overlay
        // node:3
        yiid = YangInstanceIdentifier.builder()
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_3).build();
        Map<QName, Object> suppNode3KeyValues = new HashMap<>();
        suppNode3KeyValues.put(TopologyQNames.TOPOLOGY_REF, TOPOLOGY_ID);
        suppNode3KeyValues.put(TopologyQNames.NODE_REF, UNDERLAY_NODE_ID_3);
        MapEntryNode menSuppNode3 = ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingNode.QNAME, suppNode3KeyValues)).build();

        Map<QName, Object> suppNode4KeyValues = new HashMap<>();
        suppNode4KeyValues.put(TopologyQNames.TOPOLOGY_REF, TOPOLOGY_ID);
        suppNode4KeyValues.put(TopologyQNames.NODE_REF, UNDERLAY_NODE_ID_4);
        MapEntryNode menSuppNode4 = ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingNode.QNAME, suppNode4KeyValues)).build();

        MapNode suppNodeList = ImmutableNodes.mapNodeBuilder(SupportingNode.QNAME).addChild(menSuppNode3)
                .addChild(menSuppNode4).build();
        MapEntryNode node3 = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_3)
                .addChild(suppNodeList).build();
        item = new UnderlayItem(node3, null, TOPOLOGY_ID, OVERLAY_NODE_ID_3, CorrelationItemEnum.Node);
        ntLinkCalculator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);

        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Matchers.any());

        // update an overlay node expecting a matching link to be demoted to
        // waiting list
        String nonExistingULNode = "node:1:5";
        yiid = YangInstanceIdentifier.builder()
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_2).build();
        Map<QName, Object> suppNode2KeyValues = new HashMap<>();
        suppNode2KeyValues.put(TopologyQNames.TOPOLOGY_REF, TOPOLOGY_ID);
        suppNode2KeyValues.put(TopologyQNames.NODE_REF, nonExistingULNode);
        MapEntryNode menSuppNode2 = ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingNode.QNAME, suppNode2KeyValues)).build();
        MapNode suppNode2List = ImmutableNodes.mapNodeBuilder(SupportingNode.QNAME).addChild(menSuppNode2).build();
        MapEntryNode overlayNode2 = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_2)
                .addChild(suppNode2List).build();
        item = new UnderlayItem(overlayNode2, null, TOPOLOGY_ID, OVERLAY_NODE_ID_2, CorrelationItemEnum.Node);
        ntLinkCalculator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);

        Mockito.verify(mockManager, Mockito.times(2)).removeOverlayItem((OverlayItem) Matchers.any());
    }

    @Test
    public void testProcessRemovedChanges() {
        createDummyData();

        // remove a matched link
        YangInstanceIdentifier yiid = DUMMY_LINK_CREATOR.createNodeIdYiid(LINK_ID_3);
        ntLinkCalculator.processRemovedChanges(yiid, TOPOLOGY_ID);

        // remove an overlayNode connected to a matchedLink
        yiid = YangInstanceIdentifier.builder()
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, OVERLAY_NODE_ID_2).build();
        ntLinkCalculator.processRemovedChanges(yiid, TOPOLOGY_ID);
        Mockito.verify(mockManager, Mockito.times(2)).removeOverlayItem((OverlayItem) Matchers.any());

    }

}
