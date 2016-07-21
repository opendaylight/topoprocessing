/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.inventory;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

public class InventoryAttributesParserTest implements InventoryAttributesParser {

    @Override
    public NodeRef parseInventoryNodeAttributes(final Node node) {
        final InventoryNode inventoryNode = node.getAugmentation(InventoryNode.class);
        if (inventoryNode == null) {
            return null;
        }
        return inventoryNode.getInventoryNodeRef();
    }

    @Override
    public NodeConnectorRef parseInventoryNodeConnectorAttributes(final TerminationPoint tp) {
        final InventoryNodeConnector inventoryNodeConnector = tp.getAugmentation(InventoryNodeConnector.class);
        if (inventoryNodeConnector == null) {
            return null;
        }
        return inventoryNodeConnector.getInventoryNodeConnectorRef();
    }
}
