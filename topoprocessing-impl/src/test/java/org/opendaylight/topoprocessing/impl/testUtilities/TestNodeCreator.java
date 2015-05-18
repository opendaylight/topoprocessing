/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.testUtilities;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TestNodeCreator {

    private static final QName ROOT_QNAME = Node.QNAME;
    private static final QName NODE_ID_QNAME = TopologyQNames.NETWORK_NODE_ID_QNAME;
    private static final QName IP_ADDRESS_QNAME = QName.create(ROOT_QNAME, "ip-address");
//    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(IP_ADDRESS_QNAME).build();

    protected Map<YangInstanceIdentifier, PhysicalNode> createEntryThreeNodesSameTopology() {
        final String topologyName = "mytopo:1";
        return  new HashMap<YangInstanceIdentifier, PhysicalNode>() {{
            putAll(createEntry(topologyName, "node:1", "192.168.1.1"));
            putAll(createEntry(topologyName, "node:2", "192.168.1.2"));
            putAll(createEntry(topologyName, "node:3", "192.168.1.3"));
        }};
    }

    protected List<YangInstanceIdentifier> createYiidThreeNodesSameTopology() {
        return new ArrayList<YangInstanceIdentifier>() {{
            add(createYiid("node:1"));
            add(createYiid("node:2"));
            add(createYiid("node:3"));
        }};
    }

    protected Map<YangInstanceIdentifier, PhysicalNode> createEntry(final String topologyName, final String nodeId) {
        return new HashMap<YangInstanceIdentifier, PhysicalNode>() {{
            put(createYiid(nodeId), createPhysicalNode(topologyName, nodeId));
        }};
    }

    protected Map<YangInstanceIdentifier, PhysicalNode> createEntry(
            final String topologyName, final String nodeId, final String ipAddress) {
        return new HashMap<YangInstanceIdentifier, PhysicalNode>() {{
            put(createYiid(nodeId), createPhysicalNode(topologyName, nodeId, ipAddress));
        }};
    }

    protected YangInstanceIdentifier createYiid(String nodeId) {
        return YangInstanceIdentifier.builder().node(ROOT_QNAME)
                .nodeWithKey(ROOT_QNAME, NODE_ID_QNAME, nodeId)
                .node(NODE_ID_QNAME)
                .build();
    }

    protected PhysicalNode createPhysicalNode(String topologyName, String nodeId) {
        return new PhysicalNode(createNode(nodeId), null, topologyName, nodeId);
    }

    protected PhysicalNode createPhysicalNode(String topologyName, String nodeId, String ipAddress) {
        return new PhysicalNode(createNode(nodeId, ipAddress), null, topologyName, nodeId);
    }

    protected MapEntryNode createNode(String nodeId) {
        return ImmutableNodes.mapEntryBuilder(ROOT_QNAME, NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.leafNode(NODE_ID_QNAME, nodeId))
                .build();
    }

    protected MapEntryNode createNode(String nodeId, String ipAddress) {
        return ImmutableNodes.mapEntryBuilder(ROOT_QNAME, NODE_ID_QNAME, nodeId)
                .withChild(ImmutableNodes.leafNode(NODE_ID_QNAME, nodeId))
                .withChild(ImmutableNodes.leafNode(IP_ADDRESS_QNAME, ipAddress))
                .build();
    }
}
