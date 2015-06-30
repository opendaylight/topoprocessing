/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
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

    /** Network-topology {@link Topology} (MapNode) identifier */
    public static final YangInstanceIdentifier TOPOLOGY_IDENTIFIER =
            YangInstanceIdentifier.of(NetworkTopology.QNAME).node(Topology.QNAME);
    /** Network-topology {@link Link} (MapNode) identifier */
    public static final YangInstanceIdentifier LINK_IDENTIFIER = YangInstanceIdentifier.builder(TOPOLOGY_IDENTIFIER)
            .node(Link.QNAME).build();
    /** Network-topology {@link Node} (MapNode) identifier */
    public static final YangInstanceIdentifier NODE_IDENTIFIER = YangInstanceIdentifier.builder(TOPOLOGY_IDENTIFIER)
            .node(Node.QNAME).build();

    private InstanceIdentifiers() {
        throw new UnsupportedOperationException("InstanceIdentifiers can't be instantiated.");
    }

    public static YangInstanceIdentifier buildItemIdentifier(
            YangInstanceIdentifier.InstanceIdentifierBuilder builder,
            CorrelationItemEnum correlationItemEnum) {
        switch (correlationItemEnum) {
        case Node:
            builder.node(Node.QNAME);
            break;
        case Link:
            builder.node(Link.QNAME);
            break;
        case TerminationPoint:
            builder.node(Node.QNAME);
            builder.node(TerminationPoint.QNAME);
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: "
                    + correlationItemEnum);
        }
        return builder.build();
    }

    public static YangInstanceIdentifier buildRelativeItemIdIdentifier(CorrelationItemEnum correlationItem) {
        YangInstanceIdentifier itemIdIdentifier;
        switch (correlationItem) {
        case Node:
            itemIdIdentifier = YangInstanceIdentifier
                    .of(TopologyQNames.NETWORK_NODE_ID_QNAME);
            break;
        case Link:
            itemIdIdentifier = YangInstanceIdentifier
                    .of(TopologyQNames.NETWORK_LINK_ID_QNAME);
            break;
        case TerminationPoint:
            itemIdIdentifier = YangInstanceIdentifier
                    .of(TopologyQNames.NETWORK_TP_ID_QNAME);
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: "
                    + correlationItem);
        }
        return itemIdIdentifier;
    }

    public static YangInstanceIdentifier buildItemWithItemIdIdentifier(YangInstanceIdentifier topologyIdentifier,
            String nodeId, CorrelationItemEnum correlationItem) {
        InstanceIdentifierBuilder builder = null;
        switch (correlationItem) {
        case Node:
            builder = YangInstanceIdentifier
                    .builder(topologyIdentifier)
                    .node(Node.QNAME)
                    .nodeWithKey(Node.QNAME,
                            TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId);
            break;
        case Link:
            builder = YangInstanceIdentifier
                    .builder(topologyIdentifier)
                    .node(Link.QNAME)
                    .nodeWithKey(Link.QNAME,
                            TopologyQNames.NETWORK_LINK_ID_QNAME, nodeId);
            break;
        case TerminationPoint:
            builder = YangInstanceIdentifier
                    .builder(topologyIdentifier)
                    .node(TerminationPoint.QNAME)
                    .nodeWithKey(TerminationPoint.QNAME,
                            TopologyQNames.NETWORK_TP_ID_QNAME, nodeId);
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: "
                    + correlationItem);
        }
        return builder.build();
    }

}
