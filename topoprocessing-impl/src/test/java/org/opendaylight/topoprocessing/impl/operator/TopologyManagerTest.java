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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

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
    @Mock private SchemaContext mockSchemaContext;
    @Mock private DOMRpcProviderService mockDomRpcProviderService;
    @Mock private DOMRpcImplementationRegistration<DOMRpcImplementation> mockDomRpcImplementationRegistration;

    @Mock private DOMRpcImplementation mockDomRpcImplementation;
    @Mock private DOMRpcIdentifier mockDomRpcIdentifier;
    TopologyManager manager;
    private YangInstanceIdentifier identifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY1).build();
    private LogicalNode logicalNode;
    private LogicalNode logicalNode2;

    /**
     * If logical node is null or empty, no wrapper should be created and therefore writer's addNode function
     * should not be called 
     */
    @Test
    public void addLogicalNodeNullOrEmpty() {
        initializeMockitoRpcServiceCall();

        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
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
        logicalNode = new LogicalNode(physicalNodes);

        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        manager.addLogicalNode(logicalNode);
        Mockito.verify(writer, Mockito.times(1)).writeNode((LogicalNodeWrapper) Mockito.any());
    }

    /**
     * For one logical node with two physical nodes,
     * one wrapper shall be created and writer's writeNode() method shall be called once
     */
    @Test
    public void addLogicalNodeWithTwoPhysicalNodes() {
        List<PhysicalNode> physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode1);
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID2);
        physicalNodes.add(physicalNode2);

        logicalNode = new LogicalNode(physicalNodes);

        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        manager.addLogicalNode(logicalNode);
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
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        List<PhysicalNode> physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode1 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode1);
        logicalNode = new LogicalNode(physicalNodes);
        manager.addLogicalNode(logicalNode);

        physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode2 = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID2);
        physicalNodes.add(physicalNode2);
        logicalNode2 = new LogicalNode(physicalNodes);

        manager.addLogicalNode(logicalNode2);
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
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
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

    /**
     * If logical node is null or empty, writer's writeNode function should not be called 
     */
    @Test
    public void updateLogicalNodeNullOrEmpty() {
        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);
        manager.updateLogicalNode(null);
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());
        List<PhysicalNode> physicalNodes = null;
        LogicalNode newLogicalNode = new LogicalNode(physicalNodes);
        manager.updateLogicalNode(newLogicalNode);
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());
    }

    /**
     * Removing null LogicalNode should not produce any call of writer's methods
     */
    @Test
    public void removeNullNode() {
        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);
        addLogicalNode();
        Mockito.reset(writer);
        manager.removeLogicalNode(null);
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());
        Mockito.verify(writer, Mockito.times(0)).deleteNode((LogicalNodeWrapper) Mockito.any());
    }

    /**
     * When removing existing LogicalNode, it should be removed from wrapper and writer's deleteNode method
     * should also be called
     */
    @Test
    public void removeTheOnlyExistingNode() {
        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);
        addLogicalNode();
        Mockito.reset(writer);
        manager.removeLogicalNode(this.logicalNode);
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());
        Mockito.verify(writer, Mockito.times(1)).deleteNode((LogicalNodeWrapper) Mockito.any());
        Assert.assertEquals(0, manager.getWrappers().size());
    }

    
    /**
     * Trying to remove node not existing in wrapper shall not provoke call of writer's methods
     * nor have impact on the wrapper
     */
    @Test
    public void removeUnexistingNode() {
        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);
        addLogicalNode();
        Mockito.reset(writer);

        List<PhysicalNode> physicalNodes = new ArrayList<>();
        physicalNodes.add(logicalNode.getPhysicalNodes().get(0));
        LogicalNode nodeToRemove = new LogicalNode(physicalNodes);
        
        manager.removeLogicalNode(nodeToRemove);
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());
        Mockito.verify(writer, Mockito.times(0)).deleteNode((LogicalNodeWrapper) Mockito.any());
        Assert.assertEquals(1, manager.getWrappers().size());
    }

    /**
     * When removing one existing logical node, this and only this one should be removed from datastore
     */
    @Test
    public void  removeOneOfTwoExistingNodes() {
        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);

        addTwoLogicalNodesInTwoCalls();

        // remove node
        manager.removeLogicalNode(logicalNode2);
        Mockito.reset(writer);
        // remove that node again
        manager.removeLogicalNode(logicalNode2);
        
        Mockito.verify(writer, Mockito.times(0)).writeNode((LogicalNodeWrapper) Mockito.any());
        Mockito.verify(writer, Mockito.times(0)).deleteNode((LogicalNodeWrapper) Mockito.any());
        Assert.assertEquals(1, manager.getWrappers().size());
    }

    /**
     * If there is RPC registered on underlay's topology node,
     * it shall be republished onto the overlay's topology node.
     */
    @Test
    public void testRpcRepublishing() {
        initializeMockitoRpcServiceCall();
        manager = new TopologyManager(mockRpcServices, mockSchemaHolder, identifier);
        manager.setWriter(writer);
        Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(mockSchemaContext);
        Mockito.when(mockRpcServices.getRpcProviderService()).thenReturn(mockDomRpcProviderService);
        Mockito.when(mockRpcServices.getRpcProviderService().registerRpcImplementation(
                (DOMRpcImplementation) Mockito.any(),(DOMRpcIdentifier) Mockito.any()))
                .thenReturn(mockDomRpcImplementationRegistration);

        YangInstanceIdentifier contextReference = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY1)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID1)
                .build();

        SchemaPath schemaPath = SchemaPath.create(true, TopologyQNames.NETWORK_NODE_ID_QNAME);
        DOMRpcIdentifier domRpcIdentifier = DOMRpcIdentifier.create(schemaPath, contextReference);
        Collection<DOMRpcIdentifier> rpcs = new ArrayList<>();
        rpcs.add(domRpcIdentifier);
        manager.onRpcAvailable(rpcs);

        List<PhysicalNode> physicalNodes = new ArrayList<>();
        PhysicalNode physicalNode = new PhysicalNode(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1);
        physicalNodes.add(physicalNode);
        logicalNode = new LogicalNode(physicalNodes);

        manager.addLogicalNode(logicalNode);

        Mockito.verify(mockDomRpcProviderService, Mockito.times(1))
            .registerRpcImplementation((DOMRpcImplementation) Mockito.any(),(Set<DOMRpcIdentifier>) Mockito.any());
    }
}
