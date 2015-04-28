/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * @author michal.polkorab
 *
 */
public class TopologyQNames {

    /** Network-topology topology-id QName */
    public static final QName topologyIdQName = QName.create(Topology.QNAME, "topology-id");
    /** Network-topology supporting-node topology-ref */
    public static final QName topologyRef = QName.create(SupportingNode.QNAME, "topology-ref");
    /** Network-topology node-id QName */
    public static final QName networkNodeIdQName = QName.create(Node.QNAME, "node-id");
    /** Network-topology supporting-node node-ref */
    public static final QName nodeRef = QName.create(SupportingNode.QNAME, "node-ref");
    /** Network-topology link-id QName */
    public static final QName linkId = QName.create(Link.QNAME, "link-id");
    /** Network-topology link-id QName */
    public static final QName linkRef = QName.create(Link.QNAME, "link-ref");
    /** Network-topology termination-point tp-id */
    public static final QName tpId = QName.create(TerminationPoint.QNAME, "tp-id");
    /** Network-topology termination-point tp-ref */
    public static final QName tpRef = QName.create(TerminationPoint.QNAME, "tp-ref");

    private TopologyQNames() {
        throw new UnsupportedOperationException("TopologyQNames can't be instantiated.");
    }
}
