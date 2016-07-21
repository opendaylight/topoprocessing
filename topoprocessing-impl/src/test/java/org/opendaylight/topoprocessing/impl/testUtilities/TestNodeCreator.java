/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.testUtilities;

import static org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers.NODE_IDENTIFIER;

import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author matus.marko
 */
public class TestNodeCreator {

    private static final QName IP_ADDRESS_QNAME = QName.create(Node.QNAME, "ip-address").intern();

    public YangInstanceIdentifier createNodeIdYiid(String nodeId) {
        return YangInstanceIdentifier.builder(NODE_IDENTIFIER)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId)
                .node(TopologyQNames.NETWORK_NODE_ID_QNAME).build();
    }

    public MapEntryNode createMapEntryNode(String nodeId) {
        return ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId).build();
    }

    public NormalizedNode createLeafNodeWithIpAddress(String ipAddress) {
        return ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ipAddress);
    }
}
