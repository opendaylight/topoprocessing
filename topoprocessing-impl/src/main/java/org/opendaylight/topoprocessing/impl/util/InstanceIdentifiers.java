/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Holds useful {@link YangInstanceIdentifier}s
 * @author michal.polkorab
 */
public final class InstanceIdentifiers {

    /** Network-topology {@link Topology} (MapNode) identifier. */
    public static final YangInstanceIdentifier TOPOLOGY_IDENTIFIER =
            YangInstanceIdentifier.of(NetworkTopology.QNAME).node(Topology.QNAME);
    /** Network-topology {@link Link} (MapNode) identifier. */
    public static final YangInstanceIdentifier LINK_IDENTIFIER = YangInstanceIdentifier.builder(TOPOLOGY_IDENTIFIER)
            .node(Link.QNAME).build();
    /** Network-topology {@link Node} (MapNode) identifier. */
    public static final YangInstanceIdentifier NODE_IDENTIFIER = YangInstanceIdentifier.builder(TOPOLOGY_IDENTIFIER)
            .node(Node.QNAME).build();
    private static final YangInstanceIdentifier RELATIVE_NODE_ID_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME);
    private static final YangInstanceIdentifier RELATIVE_LINK_ID_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.NETWORK_LINK_ID_QNAME);
    private static final YangInstanceIdentifier RELATIVE_TP_ID_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.NETWORK_TP_ID_QNAME);
    public static final YangInstanceIdentifier NT_TERMINATION_POINT =
            YangInstanceIdentifier.of(TerminationPoint.QNAME);
    public static final YangInstanceIdentifier NT_TP_ID_IDENTIFIER =
            YangInstanceIdentifier.of(TopologyQNames.NETWORK_TP_ID_QNAME);
    public static final YangInstanceIdentifier NT_TP_IDENTIFIER = YangInstanceIdentifier.of(org.opendaylight.yang.gen
            .v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node
            .TerminationPoint.QNAME);
    public static final YangInstanceIdentifier NT_TP_REF_IDENTIFIER =
                    YangInstanceIdentifier.of(TopologyQNames.TP_REF);
    //I2RS
    /** I2RS model {@link Network} (MapNode) identifier. */
    public static final YangInstanceIdentifier I2RS_NETWORK_IDENTIFIER =
            YangInstanceIdentifier.of(Network.QNAME);
    public static final YangInstanceIdentifier I2RS_TERMINATION_POINT =
            YangInstanceIdentifier.of(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev150608.network.node.TerminationPoint.QNAME);
    public static final YangInstanceIdentifier I2RS_TP_ID_IDENTIFIER =
            YangInstanceIdentifier.of(TopologyQNames.I2RS_TP_ID_QNAME);
    public static final YangInstanceIdentifier I2RS_TP_IDENTIFIER =
            YangInstanceIdentifier.of(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev150608.network.node.TerminationPoint.QNAME);
    private static final YangInstanceIdentifier I2RS_RELATIVE_NODE_ID_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.I2RS_NODE_ID_QNAME);
    private static final YangInstanceIdentifier I2RS_RELATIVE_LINK_ID_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.I2RS_LINK_ID_QNAME);
    private static final YangInstanceIdentifier I2RS_RELATIVE_TP_ID_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.I2RS_TP_ID_QNAME);
    //Inventory
    public static final YangInstanceIdentifier INVENTORY_NODE_ID_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.NODE_CONNECTOR_ID_QNAME);
    public static final YangInstanceIdentifier INVENTORY_NODE_CONNECTOR_IDENTIFIER =
                        YangInstanceIdentifier.of(NodeConnector.QNAME);

    public static final YangInstanceIdentifier INV_NODE_REF_IDENTIFIER =
            YangInstanceIdentifier.of(TopologyQNames.INVENTORY_NODE_REF_QNAME);

    public static final YangInstanceIdentifier INVENTORY_NODE_CONNECTOR_REF_IDENTIFIER =
                        YangInstanceIdentifier.of(TopologyQNames.INVENTORY_NODE_CONNECTOR_REF_QNAME);

    private InstanceIdentifiers() {
        throw new UnsupportedOperationException("InstanceIdentifiers can't be instantiated.");
    }

    /**
     * Returns relative item id identifier (e.g. for NormalizedNodes.findNode(...)).
     * @param correlationItem item type
     * @return item identifier
     */
    public static YangInstanceIdentifier relativeItemIdIdentifier(CorrelationItemEnum correlationItem,
            Class<? extends Model> model) {
        YangInstanceIdentifier itemIdIdentifier;
        switch (correlationItem) {
            case Node:
                itemIdIdentifier = ! model.equals(I2rsModel.class) ? RELATIVE_NODE_ID_IDENTIFIER
                        : I2RS_RELATIVE_NODE_ID_IDENTIFIER;
                break;
            case Link:
                itemIdIdentifier = ! model.equals(I2rsModel.class) ? RELATIVE_LINK_ID_IDENTIFIER
                        : I2RS_RELATIVE_LINK_ID_IDENTIFIER;
                break;
            case TerminationPoint:
                itemIdIdentifier = ! model.equals(I2rsModel.class) ? RELATIVE_TP_ID_IDENTIFIER
                        : I2RS_RELATIVE_TP_ID_IDENTIFIER;
                break;
            default:
                throw new IllegalArgumentException("Wrong Correlation item set: "
                        + correlationItem);
        }
        return itemIdIdentifier;
    }

}
