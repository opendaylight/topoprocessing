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
import org.opendaylight.topoprocessing.impl.operator.filtrator.AbstractFiltrator;
import org.opendaylight.topoprocessing.impl.testUtilities.TestLinkCreator;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class LinkFiltratorTest {

    private static final String TOPOLOGY_NAME = "mytopo:1";
    private static final QName PATH_QNAME = QName.create(Node.QNAME,"ip-address").intern();


    private TestNodeCreator nodeCreator = new TestNodeCreator();
    private TestLinkCreator linkCreator = new TestLinkCreator();
    private TopologyFiltrator filtrator;
    @Mock private AbstractFiltrator mockFiltrator;
    @Mock private TopologyManager mockTopologyManager;

    @Before
    public void setUp() {
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        filtrator = new LinkFiltrator(topoStoreProvider);
        topoStoreProvider.initializeStore(TOPOLOGY_NAME, false);
        filtrator.addFilter(mockFiltrator);
        filtrator.setTopologyManager(mockTopologyManager);
    }

    @Test
    public void testCreateChanges() {
        Map<Integer, NormalizedNode<?,?>> leafNodes = new HashMap<>();
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(PATH_QNAME).build();
        Mockito.when(mockFiltrator.isFiltered(Matchers.any())).thenReturn(false);
        Mockito.when(mockFiltrator.getPathIdentifier()).thenReturn(path);

        // create 3 nodes
        String nodeId = "node:1";
        leafNodes.put(0, nodeCreator.createLeafNodeWithIpAddress("192.168.1.1"));
        filtrator.processCreatedChanges(nodeCreator.createNodeIdYiid(nodeId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);

        nodeId = "node:2";
        leafNodes.put(0, nodeCreator.createLeafNodeWithIpAddress("192.168.1.2"));
        filtrator.processCreatedChanges(nodeCreator.createNodeIdYiid(nodeId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);

        nodeId = "node:3";
        NormalizedNode filterOutNode = nodeCreator.createLeafNodeWithIpAddress("192.168.1.3");
        leafNodes.put(0, filterOutNode);
        Mockito.when(mockFiltrator.isFiltered(filterOutNode)).thenReturn(true);
        filtrator.processCreatedChanges(nodeCreator.createNodeIdYiid(nodeId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);

        // create 3 links
        String linkId = "link:1";
        leafNodes.put(0, linkCreator.createLeafNodeWithIpAddress("192.168.1.1"));
        filtrator.processCreatedChanges(linkCreator.createNodeIdYiid(linkId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, linkId, CorrelationItemEnum.Link), TOPOLOGY_NAME);

        linkId = "link:2";
        leafNodes.put(0, linkCreator.createLeafNodeWithIpAddress("192.168.1.2"));
        filtrator.processCreatedChanges(linkCreator.createNodeIdYiid(linkId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, linkId, CorrelationItemEnum.Link), TOPOLOGY_NAME);

        linkId = "link:3";
        NormalizedNode filterOutLink = linkCreator.createLeafNodeWithIpAddress("192.168.1.3");
        leafNodes.put(0, filterOutLink);
        Mockito.when(mockFiltrator.isFiltered(filterOutLink)).thenReturn(true);
        filtrator.processCreatedChanges(linkCreator.createNodeIdYiid(linkId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, linkId, CorrelationItemEnum.Link), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(5)).addOverlayItem(Matchers.any());

        // update link which didn't exist before
        linkId = "link:4";
        leafNodes.put(0, linkCreator.createLeafNodeWithIpAddress("192.168.1.4"));
        filtrator.processUpdatedChanges(linkCreator.createNodeIdYiid(linkId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, linkId, CorrelationItemEnum.Link), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(6)).addOverlayItem(Matchers.any());

        // update existing link
        linkId = "link:4";
        leafNodes.put(0, linkCreator.createLeafNodeWithIpAddress("192.168.1.14"));
        filtrator.processUpdatedChanges(linkCreator.createNodeIdYiid(linkId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, linkId, CorrelationItemEnum.Link), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager).updateOverlayItem(Matchers.any());

        // update existing link - should be filtered out
        linkId = "link:4";
        filterOutLink = linkCreator.createLeafNodeWithIpAddress("192.168.1.14");
        leafNodes.put(0, filterOutLink);
        Mockito.when(mockFiltrator.isFiltered(filterOutLink)).thenReturn(true);
        filtrator.processUpdatedChanges(linkCreator.createNodeIdYiid(linkId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, linkId, CorrelationItemEnum.Link), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager).removeOverlayItem(Matchers.any());

        // update existing node
        nodeId = "node:1";
        leafNodes.put(0, linkCreator.createLeafNodeWithIpAddress("192.168.1.14"));
        filtrator.processUpdatedChanges(linkCreator.createNodeIdYiid(nodeId), new UnderlayItem(null, leafNodes,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager).updateOverlayItem(Matchers.any());

        // remove 3 existing links with one new
        filtrator.processRemovedChanges(linkCreator.createNodeIdYiid("link:1"), TOPOLOGY_NAME);
        filtrator.processRemovedChanges(linkCreator.createNodeIdYiid("link:2"), TOPOLOGY_NAME);
        filtrator.processRemovedChanges(linkCreator.createNodeIdYiid("link:3"), TOPOLOGY_NAME);
        filtrator.processRemovedChanges(linkCreator.createNodeIdYiid("link:5"), TOPOLOGY_NAME);

        Mockito.verify(mockTopologyManager, Mockito.times(3)).removeOverlayItem((OverlayItem) Matchers.any());
    }

}
