/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class IdentifierGenerator {

    /**
     * Value for internal counter
     */
    private int id = 0;

    private int getNextId() {
        id += 1;
        return id;
    }

    private YangInstanceIdentifier createIdentifier(String topologyId,
            CorrelationItemEnum correlationItem, int uniqueId) {
        YangInstanceIdentifier.InstanceIdentifierBuilder yiid = YangInstanceIdentifier.builder()
                .node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TopologyQNames.topologyIdQName, topologyId);

        switch (correlationItem) {
            case Node:
                yiid.node(Node.QNAME);
                yiid.nodeWithKey(Node.QNAME, TopologyQNames.networkNodeIdQName, uniqueId);
                break;
            case Link:
                yiid.node(Link.QNAME);
                yiid.nodeWithKey(Link.QNAME, TopologyQNames.linkId, uniqueId);
                break;
            case TerminationPoint:
                yiid.node(Node.QNAME);
                yiid.node(TerminationPoint.QNAME);
                yiid.nodeWithKey(TerminationPoint.QNAME, TopologyQNames.tpId, uniqueId);
                break;
            default:
                throw new IllegalStateException("Unknown Correlation item used: " + correlationItem);
        }
        return yiid.build();
    }

    /**
     * Create unique YangInstanceIdentifier
     * @param topologyId 
     * @param correlationItem 
     * @return unique 
     */
    public YangInstanceIdentifier getNextIdentifier(String topologyId, CorrelationItemEnum correlationItem) {
        return createIdentifier(topologyId, correlationItem, getNextId());
    }

}
