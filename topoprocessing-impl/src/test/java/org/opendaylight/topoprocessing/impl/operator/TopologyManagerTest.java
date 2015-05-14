/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.LogicalNodeWrapper;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyManagerTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar");
    private static final QName QNAME_LEAF_IP = QName.create(ROOT_QNAME, "ip");

    private static final String TOPOLOGY1 = "pcep-topology:1";
    private static final String NODE_ID1 = "pcep:1";
    private static final String NODE_ID2 = "pcep:2";

//    @Mock Map<YangInstanceIdentifier, PhysicalNode> entriesMap;
//    @Mock List<YangInstanceIdentifier> entriesList;
//    String topologyId;

    @Mock private RpcServices mockRpcServices;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private NormalizedNode<?,?> mockNormalizedNode1;
    @Mock private DOMRpcService mockDOMRpcService;
    @Mock private ListenerRegistration<DOMRpcAvailabilityListener> mockListenerRegistration;
    @Mock private TopologyWriter writer;
    private YangInstanceIdentifier identifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY1).build();

    /**
     * If logical node is null or empty, no wrapper should be created and therefore writer's addNode function
     * should not be called 
     */
    @Test
    public void addLogicalNodeNullOrEmpty() {
        initializeMockitoRpcServiceCall();

        TopologyManager manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);
        manager.addLogicalNode(null);
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());

        List<PhysicalNode> physicalNodes = null;
        LogicalNode newLogicalNode = new LogicalNode(physicalNodes);
        manager.addLogicalNode(newLogicalNode);
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());
    }

    private void initializeMockitoRpcServiceCall() {
        Mockito.when(mockRpcServices.getRpcService()).thenReturn(mockDOMRpcService);
        Mockito.when(mockRpcServices.getRpcService().registerRpcListener((DOMRpcAvailabilityListener)any()))
            .thenReturn(mockListenerRegistration);
    }

    /**
     * For one logical node, new wrapper shall be created and then sent to writer's class
     */
    @Test
    public void addLogicalNode() {
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode);
        LogicalNode newLogicalNode = new LogicalNode(physicalNodes);

        initializeMockitoRpcServiceCall();
        TopologyManager manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        manager.addLogicalNode(newLogicalNode);
        Mockito.verify(writer, Mockito.times(1)).writeNode((LogicalNodeWrapper) Mockito.any());
    }

    /**
     * For two logical nodes, one wrapper shall be created and writer's writeNode() method shall be called once
     */
    @Test
    public void addTwoLogicalNodes() {
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode1);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID2);
        physicalNodes.add(physicalNode2);
        LogicalNode newLogicalNode = new LogicalNode(physicalNodes);

        initializeMockitoRpcServiceCall();
        TopologyManager manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        manager.addLogicalNode(newLogicalNode);
        Assert.assertEquals(1, manager.getWrappers().size());
        Mockito.verify(writer, Mockito.times(1)).writeNode((LogicalNodeWrapper) Mockito.any());
    }

    /**
     * For two calls of addLogicalNode method with one logical node in each call,
     * two wrappers shall be created and writer's writeNode() method shall be called twice as well
     */
    @Test
    public void addTwoLogicalNodesInTwoCalls() {
        initializeMockitoRpcServiceCall();
        TopologyManager manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        List<PhysicalNode> physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode1);
        LogicalNode newLogicalNode = new LogicalNode(physicalNodes);
        manager.addLogicalNode(newLogicalNode);

        physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID2);
        physicalNodes.add(physicalNode2);
        newLogicalNode = new LogicalNode(physicalNodes);

        manager.addLogicalNode(newLogicalNode);
        Assert.assertEquals(2, manager.getWrappers().size());
        Mockito.verify(writer, Mockito.times(2)).writeNode((LogicalNodeWrapper) Mockito.any());
    }

    /**
     * For two calls of addLogicalNode method with the same logical node,
     * one wrappers shall be created and writer's writeNode() method shall be called twice
     */
    @Test
    public void addTwoSameLogicalNodesInTwoCalls() {
        initializeMockitoRpcServiceCall();
        TopologyManager manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        List<PhysicalNode> physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode1);
        LogicalNode newLogicalNode = new LogicalNode(physicalNodes);
        manager.addLogicalNode(newLogicalNode);

        physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode2);
        newLogicalNode = new LogicalNode(physicalNodes);

        manager.addLogicalNode(newLogicalNode);
        Assert.assertEquals(1, manager.getWrappers().size());
        Mockito.verify(writer, Mockito.times(2)).writeNode((LogicalNodeWrapper) Mockito.any());
    }
}
