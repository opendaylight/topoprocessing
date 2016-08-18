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

    private static final String INVENTORY_NAMESPACE = "urn:opendaylight:flow:inventory";
    private static final String REV_2013_08_19 = "2013-08-19";

    /** Inventory node-connector id. */
    public static final QName NODE_CONNECTOR_ID_QNAME =
        QName.create(NodeConnector.QNAME, "id").intern();
    /** Network-topology node node-augmentation. */
    public static final QName NODE_AUGMENTATION_QNAME = QName
        .create("urn:opendaylight:topology:inventory:rendering", "2015-08-31", "node-augmentation")
        .intern();
    /** Network-topology termination-point tp-augmentation. */
    public static final QName TP_AUGMENTATION_QNAME = QName
        .create("urn:opendaylight:topology:inventory:rendering", "2015-08-31", "tp-augmentation")
        .intern();
    /** Network-topology termination-point inventory-node-connector-ref. */
    public static final QName INVENTORY_NODE_CONNECTOR_REF_QNAME = QName
        .create("urn:opendaylight:model:topology:inventory", "2013-10-30",
            "inventory-node-connector-ref").intern();

    /** Inventory-rendering manufacturer QName. */
    public static final QName NODE_AUG_MANUFACTURER_QNAME =
        QName.create(NodeAugmentation.QNAME, "manufacturer").intern();
    /** Inventory-rendering hardware QName. */
    public static final QName NODE_AUG_HARDWARE_QNAME =
        QName.create(NodeAugmentation.QNAME, "hardware").intern();
    /** Inventory-rendering software QName. */
    public static final QName NODE_AUG_SOFTWARE_QNAME =
        QName.create(NodeAugmentation.QNAME, "software").intern();
    /** Inventory-rendering serial-number QName. */
    public static final QName NODE_AUG_SERIAL_NUMBER_QNAME =
        QName.create(NodeAugmentation.QNAME, "serial-number").intern();
    /** Inventory-rendering description QName. */
    public static final QName NODE_AUG_DESCRIPTION_QNAME =
        QName.create(NodeAugmentation.QNAME, "description").intern();
    /** Inventory-rendering ip-address QName. */
    public static final QName NODE_AUG_IP_ADDREESS_QNAME =
        QName.create(NodeAugmentation.QNAME, "ip-address").intern();

    /** Inventory-rendering name QName. */
    public static final QName TP_AUG_NAME_QNAME =
        QName.create(TpAugmentation.QNAME, "name").intern();
    /** Inventory-rendering hardware-address QName. */
    public static final QName TP_AUG_HARDWARE_ADDRESS_QNAME =
        QName.create(TpAugmentation.QNAME, "hardware-address").intern();
    /** Inventory-rendering current-speed QName. */
    public static final QName TP_AUG_CURRENT_SPEED_QNAME =
        QName.create(TpAugmentation.QNAME, "current-speed").intern();
    /** Inventory-rendering maximum-speed QName. */
    public static final QName TP_AUG_MAXIMUM_SPEED_QNAME =
        QName.create(TpAugmentation.QNAME, "maximum-speed").intern();

    /** Flow-inventory manufacturer QName. */
    public static final QName OPEN_FLOW_NODE_MANUFACTURER_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "manufacturer").intern();
    /** Flow-inventory hardware QName. */
    public static final QName OPEN_FLOW_NODE_HARDWARE_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "hardware").intern();
    /** Flow-inventory software QName. */
    public static final QName OPEN_FLOW_NODE_SOFTWARE_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "software").intern();
    /** Flow-inventory serial-number QName. */
    public static final QName OPEN_FLOW_NODE_SERIAL_NUMBER_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "serial-number").intern();
    /** Flow-inventory description QName. */
    public static final QName OPEN_FLOW_NODE_DESCRIPTION_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "description").intern();
    /** Flow-inventory ip-address QName. */
    public static final QName OPEN_FLOW_NODE_IP_ADDRESS_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "ip-address").intern();

    /** Flow-inventory port name QName. */
    public static final QName OPEN_FLOW_PORT_NAME_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "name").intern();
    /** Flow-inventory port hardware-address QName. */
    public static final QName OPEN_FLOW_PORT_HARDWARE_ADDRESS_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "hardware-address").intern();
    /** Flow-inventory port current-speed QName. */
    public static final QName OPEN_FLOW_PORT_CURRENT_SPEED_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "current-speed").intern();
    /** Flow-inventory port maximum-speed QName. */
    public static final QName OPEN_FLOW_PORT_MAXIMUM_SPEED_QNAME =
        QName.create(INVENTORY_NAMESPACE, REV_2013_08_19, "maximum-speed").intern();

    private IRQNames() {
        throw new UnsupportedOperationException("InventoryRenderingQNames can't be instantiated.");
    }
}
