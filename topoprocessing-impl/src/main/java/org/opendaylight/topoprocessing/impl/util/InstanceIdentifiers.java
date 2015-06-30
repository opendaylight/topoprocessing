/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yangtools.yang.data.api.schema.MapNode;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;

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
    private static final YangInstanceIdentifier RELATIVE_NODE_ID_IDENTIFIER = 
                        YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME);
    private static final YangInstanceIdentifier RELATIVE_LINK_ID_IDENTIFIER = 
                        YangInstanceIdentifier.of(TopologyQNames.NETWORK_LINK_ID_QNAME);
    private static final YangInstanceIdentifier RELATIVE_TP_ID_IDENTIFIER = 
                        YangInstanceIdentifier.of(TopologyQNames.NETWORK_TP_ID_QNAME);

    private InstanceIdentifiers() {
        throw new UnsupportedOperationException("InstanceIdentifiers can't be instantiated.");
    }

    /**
     * Builds item identifier (identifies item {@link MapNode})
     * @param builder starting builder (set with specific topology) that will be appended
     * with corresponding item QName
     * @param correlationItemEnum item type
     * @return item identifier (identifies item {@link MapNode})
     */
    public static YangInstanceIdentifier buildItemIdentifier(YangInstanceIdentifier.InstanceIdentifierBuilder builder,
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

    /**
     * Returns relative item id identifier (e.g. for NormalizedNodes.findNode(...))
     * @param correlationItem item type
     * @return item identifier
     */
    public static YangInstanceIdentifier buildRelativeItemIdIdentifier(CorrelationItemEnum correlationItem) {
        YangInstanceIdentifier itemIdIdentifier;
        switch (correlationItem) {
        case Node:
            itemIdIdentifier = RELATIVE_NODE_ID_IDENTIFIER;
            break;
        case Link:
            itemIdIdentifier = RELATIVE_LINK_ID_IDENTIFIER;
            break;
        case TerminationPoint:
            itemIdIdentifier = RELATIVE_TP_ID_IDENTIFIER;
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: "
                    + correlationItem);
        }
        return itemIdIdentifier;
    }

    /**
     * Returns absolute item id identifier (e.g. for writting into / deleting from datastore)
     * @param topologyIdentifier identifies topology
     * @param itemId item identificator
     * @param correlationItem item type
     * @return absolute item identifier
     */
    public static YangInstanceIdentifier buildItemIdIdentifier(YangInstanceIdentifier topologyIdentifier,
            String itemId, CorrelationItemEnum correlationItem) {
        InstanceIdentifierBuilder builder = null;
        switch (correlationItem) {
        case Node:
            builder = YangInstanceIdentifier
                    .builder(topologyIdentifier)
                    .node(Node.QNAME)
                    .nodeWithKey(Node.QNAME,
                            TopologyQNames.NETWORK_NODE_ID_QNAME, itemId);
            break;
        case Link:
            builder = YangInstanceIdentifier
                    .builder(topologyIdentifier)
                    .node(Link.QNAME)
                    .nodeWithKey(Link.QNAME,
                            TopologyQNames.NETWORK_LINK_ID_QNAME, itemId);
            break;
        case TerminationPoint:
            builder = YangInstanceIdentifier
                    .builder(topologyIdentifier)
                    .node(TerminationPoint.QNAME)
                    .nodeWithKey(TerminationPoint.QNAME,
                            TopologyQNames.NETWORK_TP_ID_QNAME, itemId);
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: "
                    + correlationItem);
        }
        return builder.build();
    }

}
