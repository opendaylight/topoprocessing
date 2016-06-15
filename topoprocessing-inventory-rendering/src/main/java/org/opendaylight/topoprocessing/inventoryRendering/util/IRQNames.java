/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.inventory.rendering.rev150831.node.augmentation.grouping.NodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.inventory.rendering.rev150831.tp.augmentation.grouping.TpAugmentation;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * @author andrej.zan
 *
 */
public final class IRQNames {

    /** Inventory node-connector id */
    public static final QName NODE_CONNECTOR_ID_QNAME =
        QName.create(NodeConnector.QNAME, "id").intern();
    /** network-topology node node-augmentation */
    public static final QName NODE_AUGMENTATION_QNAME = QName
        .create("urn:opendaylight:topology:inventory:rendering", "2015-08-31", "node-augmentation")
        .intern();
    /** network-topology termination-point tp-augmentation */
    public static final QName TP_AUGMENTATION_QNAME = QName
        .create("urn:opendaylight:topology:inventory:rendering", "2015-08-31", "tp-augmentation")
        .intern();
    /** network-topology termination-point inventory-node-connector-ref*/
    public static final QName INVENTORY_NODE_CONNECTOR_REF_QNAME = QName
        .create("urn:opendaylight:model:topology:inventory", "2013-10-30",
            "inventory-node-connector-ref").intern();

    /** inventory-rendering manufacturer QName */
    public static final QName NODE_AUG_MANUFACTURER_QNAME =
        QName.create(NodeAugmentation.QNAME, "manufacturer").intern();
    /** inventory-rendering hardware QName */
    public static final QName NODE_AUG_HARDWARE_QNAME =
        QName.create(NodeAugmentation.QNAME, "hardware").intern();
    /** inventory-rendering software QName */
    public static final QName NODE_AUG_SOFTWARE_QNAME =
        QName.create(NodeAugmentation.QNAME, "software").intern();
    /** inventory-rendering serial-number QName */
    public static final QName NODE_AUG_SERIAL_NUMBER_QNAME =
        QName.create(NodeAugmentation.QNAME, "serial-number").intern();
    /** inventory-rendering description QName */
    public static final QName NODE_AUG_DESCRIPTION_QNAME =
        QName.create(NodeAugmentation.QNAME, "description").intern();
    /** inventory-rendering ip-address QName */
    public static final QName NODE_AUG_IP_ADDREESS_QNAME =
        QName.create(NodeAugmentation.QNAME, "ip-address").intern();

    /** inventory-rendering name QName */
    public static final QName TP_AUG_NAME_QNAME =
        QName.create(TpAugmentation.QNAME, "name").intern();
    /** inventory-rendering hardware-address QName */
    public static final QName TP_AUG_HARDWARE_ADDRESS_QNAME =
        QName.create(TpAugmentation.QNAME, "hardware-address").intern();
    /** inventory-rendering current-speed QName */
    public static final QName TP_AUG_CURRENT_SPEED_QNAME =
        QName.create(TpAugmentation.QNAME, "current-speed").intern();
    /** inventory-rendering maximum-speed QName */
    public static final QName TP_AUG_MAXIMUM_SPEED_QNAME =
        QName.create(TpAugmentation.QNAME, "maximum-speed").intern();

    /** flow-inventory manufacturer QName */
    public static final QName OPEN_FLOW_NODE_MANUFACTURER_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "manufacturer").intern();
    /** flow-inventory hardware QName */
    public static final QName OPEN_FLOW_NODE_HARDWARE_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "hardware").intern();
    /** flow-inventory software QName */
    public static final QName OPEN_FLOW_NODE_SOFTWARE_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "software").intern();
    /** flow-inventory serial-number QName */
    public static final QName OPEN_FLOW_NODE_SERIAL_NUMBER_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "serial-number").intern();
    /** flow-inventory description QName */
    public static final QName OPEN_FLOW_NODE_DESCRIPTION_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "description").intern();
    /** flow-inventory ip-address QName */
    public static final QName OPEN_FLOW_NODE_IP_ADDRESS_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "ip-address").intern();

    /** flow-inventory port name QName */
    public static final QName OPEN_FLOW_PORT_NAME_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "name").intern();
    /** flow-inventory port hardware-address QName */
    public static final QName OPEN_FLOW_PORT_HARDWARE_ADDRESS_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "hardware-address").intern();
    /** flow-inventory port current-speed QName */
    public static final QName OPEN_FLOW_PORT_CURRENT_SPEED_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "current-speed").intern();
    /** flow-inventory port maximum-speed QName */
    public static final QName OPEN_FLOW_PORT_MAXIMUM_SPEED_QNAME =
        QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "maximum-speed").intern();

    private IRQNames() {
        throw new UnsupportedOperationException("InventoryRenderingQNames can't be instantiated.");
    }
}
