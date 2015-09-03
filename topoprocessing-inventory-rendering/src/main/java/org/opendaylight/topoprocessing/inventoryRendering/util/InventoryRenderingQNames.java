/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.inventory.rendering.rev150831.node.augmentation.grouping.NodeAugmentation;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * @author andrej.zan
 *
 */
public final class InventoryRenderingQNames {

    /** inventory-rendering manufacturer QName */
    public static final QName NODE_AUG_MANUFACTURER_QNAME = QName.create(NodeAugmentation.QNAME, "manufacturer");
    /** inventory-rendering hardware QName */
    public static final QName NODE_AUG_HARDWARE_QNAME = QName.create(NodeAugmentation.QNAME, "hardware");
    /** inventory-rendering software QName */
    public static final QName NODE_AUG_SOFTWARE_QNAME = QName.create(NodeAugmentation.QNAME, "software");
    /** inventory-rendering serial-number QName */
    public static final QName NODE_AUG_SERIAL_NUMBER_QNAME = QName.create(NodeAugmentation.QNAME, "serial-number");
    /** inventory-rendering description QName */
    public static final QName NODE_AUG_DESCRIPTION_QNAME = QName.create(NodeAugmentation.QNAME, "description");
    /** inventory-rendering ip-address QName */
    public static final QName NODE_AUG_IP_ADDREESS_QNAME = QName.create(NodeAugmentation.QNAME, "ip-address");

    /** inventory-rendering name QName */
    public static final QName TP_AUG_NAME_QNAME = QName.create(NodeAugmentation.QNAME, "name");
    /** inventory-rendering hardware-address QName */
    public static final QName TP_AUG_HARDWARE_ADDRESS_QNAME = QName.create(NodeAugmentation.QNAME, "hardware-address");
    /** inventory-rendering current-speed QName */
    public static final QName TP_AUG_CURRENT_SPEED_QNAME = QName.create(NodeAugmentation.QNAME, "current-speed");
    /** inventory-rendering maximum-speed QName */
    public static final QName TP_AUG_MAXIMUM_SPEED_QNAME = QName.create(NodeAugmentation.QNAME, "maximum-speed");

    /** flow-inventory manufacturer QName */
    public static final QName OPEN_FLOW_NODE_MANUFACTURER_QNAME =
            QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "manufacturer");
    /** flow-inventory hardware QName */
    public static final QName OPEN_FLOW_NODE_HARDWARE_QNAME =
            QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "hardware");
    /** flow-inventory software QName */
    public static final QName OPEN_FLOW_NODE_SOFTWARE_QNAME =
            QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "software");
    /** flow-inventory serial-number QName */
    public static final QName OPEN_FLOW_NODE_SERIAL_NUMBER_QNAME =
            QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "serial-number");
    /** flow-inventory description QName */
    public static final QName OPEN_FLOW_NODE_DESCRIPTION_QNAME =
            QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "description");
    /** flow-inventory ip-address QName */
    public static final QName OPEN_FLOW_NODE_IP_ADDRESS_QNAME =
            QName.create("urn:opendaylight:flow:inventory", "2013-08-19", "ip-address");

    private InventoryRenderingQNames() {
        throw new UnsupportedOperationException("InventoryRenderingQNames can't be instantiated.");
    }
}
