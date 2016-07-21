/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.parser;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Preconditions;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class InventoryAttributesParserImplTest  {
    private InventoryAttributesParserImpl parser;
    private static final String topologyName = "example:1";
    private static final String inventoryNodeName = "inv:example-node:1";
    private static final String inventoryTpName = "inv:example-tp:1";
    private InstanceIdentifier<InventoryNode> nodeInstanceId;
    private InstanceIdentifier<InventoryNodeConnector> nodeConnectorInstanceId;

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        this.parser = new InventoryAttributesParserImpl();
        TopologyId tid = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        NodeId nodeId = new NodeId(inventoryNodeName);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeInstanceId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key)
                .child(Node.class, nodeKey).augmentation(InventoryNode.class);
        TpId tpId = new TpId(inventoryTpName);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        nodeConnectorInstanceId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key)
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey)
                .augmentation(InventoryNodeConnector.class);
    }

    @Test
    public void parseInventoryNodeAttributesTest() {
        InventoryNodeBuilder invNodeBuilder = new InventoryNodeBuilder();
        NodeRef nodeRef = new NodeRef(nodeInstanceId);
        invNodeBuilder.setInventoryNodeRef(nodeRef);
        InventoryNode inventoryNode = invNodeBuilder.build();
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId(inventoryNodeName);
        nodeBuilder.setKey(new NodeKey(nodeId));
        nodeBuilder.setNodeId(nodeId);
        nodeBuilder.addAugmentation(InventoryNode.class, inventoryNode);
        NodeRef parsedNodeRef = parser.parseInventoryNodeAttributes(nodeBuilder.build());
        assertEquals(parsedNodeRef.getValue().toString(), nodeRef.getValue().toString());
    }

   @Test
   public void parseInventoryNodeConnectorAttributesTest() {
       InventoryNodeConnectorBuilder invNodeConnectorBuilder = new InventoryNodeConnectorBuilder();
       NodeConnectorRef invNodeConnectorRef = new NodeConnectorRef(nodeConnectorInstanceId);
       invNodeConnectorBuilder.setInventoryNodeConnectorRef(invNodeConnectorRef);
       InventoryNodeConnector invNodeConnector = invNodeConnectorBuilder.build();
       TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
       TpId tpId = new TpId(inventoryTpName);
       TerminationPointKey tpKey = new TerminationPointKey(tpId);
       tpBuilder.setTpId(tpId);
       tpBuilder.setKey(tpKey);
       tpBuilder.addAugmentation(InventoryNodeConnector.class, invNodeConnector);
       NodeConnectorRef parsedNodeConnectorRef = parser.parseInventoryNodeConnectorAttributes(tpBuilder.build());
       assertEquals(parsedNodeConnectorRef.getValue().toString(), invNodeConnectorRef.getValue().toString());
   }

   @After
    public void clear() {
        // NOOP
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }
}
