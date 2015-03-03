/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.parser;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.slf4j.Logger;
import org.opendaylight.topology.mlmt.inventory.InventoryAttributesParser;

public class InventoryAttributesParserImpl implements InventoryAttributesParser {

     private static Logger log;

     public void init(final Logger logger) {
         log = logger;
     }

     @Override
     public NodeRef parseInventoryNodeAttributes(final Node node) {

         final InventoryNode node1 = node.getAugmentation(InventoryNode.class);
         if (node1 == null) {
             return null;
         }

         return node1.getInventoryNodeRef();
     }
}
