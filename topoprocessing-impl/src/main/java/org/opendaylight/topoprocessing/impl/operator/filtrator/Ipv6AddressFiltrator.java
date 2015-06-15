/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;

/**
 * @author matus.marko
 */
public class Ipv6AddressFiltrator implements Filtrator {
    private static final Logger LOG = LoggerFactory.getLogger(Ipv6AddressFiltrator.class);
    private static final int MAX_BITS = 128;

    private YangInstanceIdentifier pathIdentifier;
    private BitSet mask;
    private BitSet maskedValue;

    /**
     * Constructor
     * @param value IpAddress to compare with
     * @param pathIdentifier Path leading to value with ipAddress in examined node
     */
    public Ipv6AddressFiltrator(IpPrefix value, YangInstanceIdentifier pathIdentifier) {
        Preconditions.checkNotNull(value, "Filtering value can't be null");
        Preconditions.checkNotNull(pathIdentifier, "PathIdentifier can't be null");
        this.pathIdentifier = pathIdentifier;
        initialize(value);
    }

    @Override
    public boolean isFiltered(PhysicalNode node) {
        try {
            Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(node.getNode(), pathIdentifier);
            if (leafNode.isPresent()) {
                byte[] address = InetAddress.getByName((String) ((LeafNode) leafNode.get()).getValue()).getAddress();
                BitSet bitSet = BitSet.valueOf(address);
                bitSet.and(mask);
                if (maskedValue.equals(bitSet)) {
                    return false;
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Node with value {} was filtered out", node.getNode());
            }
        } catch (UnknownHostException e) {
            LOG.error("Wrong format of IP address: {}", e);
        }
        return true;
    }

    private void initialize(IpPrefix prefix) {
        String[] matches = prefix.getIpv6Prefix().getValue().split("/");
        String rangeIp = matches[0];
        int bits = Integer.parseInt(matches[1]);

        mask = new BitSet(MAX_BITS);
        mask.set(0, bits, true);
        byte[] inetAddr;
        try {
            inetAddr = InetAddress.getByName(rangeIp).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Filtrator initialization failed, "
                    + "couldn't recognize ip address: " + rangeIp, e);
        }
        maskedValue = BitSet.valueOf(inetAddr);
        maskedValue.and(mask);
    }
}
