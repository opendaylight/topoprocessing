/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author michal.polkorab
 *
 */
public final class TopologyQNames {

    /** Network-topology topology-id QName */
    public static final QName TOPOLOGY_ID_QNAME = QName.create(Topology.QNAME, "topology-id");
    /** Network-topology node-id QName */
    public static final QName NETWORK_NODE_ID_QNAME = QName.create(Node.QNAME, "node-id");
    /** Network-topology link-id QName */
    public static final QName NETWORK_LINK_ID_QNAME = QName.create(Node.QNAME, "link-id");
    /** Network-topology tp-id QName */
    public static final QName NETWORK_TP_ID_QNAME = QName.create(Node.QNAME, "tp-id");
    /** Network-topology supporting-node topology-ref */
    public static final QName TOPOLOGY_REF = QName.create(SupportingNode.QNAME, "topology-ref");
    /** Network-topology supporting-node node-ref */
    public static final QName NODE_REF = QName.create(SupportingNode.QNAME, "node-ref");
    /** Network-topology link-id QName */
    public static final QName LINK_ID = QName.create(Link.QNAME, "link-id");
    /** Network-topology link-ref QName */
    public static final QName LINK_REF = QName.create(Link.QNAME, "link-ref");
    /** Network-topology termination-point tp-id */
    public static final QName TP_ID = QName.create(TerminationPoint.QNAME, "tp-id");
    /** Network-topology termination-point tp-ref */
    public static final QName TP_REF = QName.create(TerminationPoint.QNAME, "tp-ref");

    private TopologyQNames() {
        throw new UnsupportedOperationException("TopologyQNames can't be instantiated.");
    }

    public static QName buildItemQName(CorrelationItemEnum correlationItem) {
        QName itemQName;
        switch (correlationItem) {
        case Node:
            itemQName = Node.QNAME;
            break;
        case Link:
            itemQName = Link.QNAME;
            break;
        case TerminationPoint:
            itemQName = TerminationPoint.QNAME;
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
        }
        return itemQName;
    }

    public static QName buildItemIdQName(CorrelationItemEnum correlationItem) {
        QName itemIdQName;
        switch (correlationItem) {
        case Node:
            itemIdQName = QName.create(Node.QNAME, "node-id");
            break;
        case Link:
            itemIdQName = QName.create(Link.QNAME, "link-id");
            break;
        case TerminationPoint:
            itemIdQName = QName.create(TerminationPoint.QNAME, "tp-id");
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
        }
        return itemIdQName;
    }
}
