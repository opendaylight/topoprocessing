/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.util;

import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author andrej.zan
 *
 */
public final class IRInstanceIdentifiers {

    /** flow-inventory manufacturer YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_NODE_MANUFACTURER_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_NODE_MANUFACTURER_QNAME);
    /** flow-inventory hardware YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_NODE_HARDWARE_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_NODE_HARDWARE_QNAME);
    /** flow-inventory software YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_NODE_SOFTWARE_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_NODE_SOFTWARE_QNAME);
    /** flow-inventory serial-number YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_NODE_SERIAL_NUMBER_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_NODE_SERIAL_NUMBER_QNAME);
    /** flow-inventory description YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_NODE_DESCRIPTION_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_NODE_DESCRIPTION_QNAME);
    /** flow-inventory ip-address YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_NODE_IP_ADDRESS_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_NODE_IP_ADDRESS_QNAME);

    /** flow-inventory port name YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_PORT_NAME_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_PORT_NAME_QNAME);
    /** flow-inventory port hardware-address YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_PORT_HARDWARE_ADDRESS_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_PORT_HARDWARE_ADDRESS_QNAME);
    /** flow-inventory port current-speed YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_PORT_CURRENT_SPEED_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_PORT_CURRENT_SPEED_QNAME );
    /** flow-inventory port maximum-speed YangInstanceIdentifier */
    public static final YangInstanceIdentifier OPEN_FLOW_PORT_MAXIMUM_SPEED_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.OPEN_FLOW_PORT_MAXIMUM_SPEED_QNAME );

    /** termination-point YangInstanceIdentifier */
    public static final YangInstanceIdentifier TP_IDENTIFIER =
            YangInstanceIdentifier.of(TerminationPoint.QNAME);
    /** termination-point tp-id YangInstanceIdentifier */
    public static final YangInstanceIdentifier TP_ID_IDENTIFIER =
            YangInstanceIdentifier.of(TopologyQNames.NETWORK_TP_ID_QNAME);
    /** termination-point inventory-node-connector-ref YangInstanceIdentifier */
    public static final YangInstanceIdentifier INVENTORY_NODE_CONNECTOR_REF_IDENTIFIER =
            YangInstanceIdentifier.of(IRQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME);

    /** node-connector YangInstanceIdentifier */
    public static final YangInstanceIdentifier NODE_CONNECTOR_IDENTIFIER = YangInstanceIdentifier
            .of(NodeConnector.QNAME);
}
