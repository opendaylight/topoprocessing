/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author matus.marko
 */
public class NodeIpFiltrator {

    private static final Logger LOG = LoggerFactory.getLogger(NodeIpFiltrator.class);

    private int address;
    private int mask;
    private YangInstanceIdentifier pathIdentifier;

    /**
     * Constructor
     * @param value
     * @param pathIdentifier
     */
    public NodeIpFiltrator(IpPrefix value, YangInstanceIdentifier pathIdentifier) {
        Preconditions.checkNotNull(value, "Filtering value can't be null");
        Preconditions.checkNotNull(pathIdentifier, "PathIdentifier can't be null");
        this.pathIdentifier = pathIdentifier;
        initialize(value);
    }

    /**
     * Filters {@link PhysicalNode}
     * @param node {@link PhysicalNode} to be filtered
     * @return true if node was filtered out false otherwise
     */
    public boolean isFiltered(PhysicalNode node) {
        try {
            Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(node.getNode(), pathIdentifier);
            if (leafNode.isPresent()) {
                int value = ipToInt((String) ((LeafNode) leafNode.get()).getValue());
                if ((address & mask) == (value & mask)) {
                    return false;
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Node with value " + node.getNode() + " was filtered out");
            }
        } catch (UnknownHostException e) {
            LOG.error("Wrong format of IP address");
        }
        return true;
    }

    private void initialize(IpPrefix prefix) {
        String strValue = prefix.getIpv4Prefix().getValue();
        String[] matches = strValue.split("/");
        try {
            address = ipToInt(matches[0]);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Filtrator initialization failed, "
                    + "couldn't recognize ip address: " + matches[0]);
        }
        mask = (int) ((long) -1 << (32 - Integer.parseInt(matches[1])));
    }

    private int ipToInt(String strAddress) throws UnknownHostException {
        Inet4Address inetAddr = (Inet4Address) InetAddress.getByName(strAddress);
        byte[] bytes = inetAddr.getAddress();
        return  ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8)  |
                ((bytes[3] & 0xFF) << 0);
    }
}
