/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 */
public class Ipv6AddressFiltrator extends AbstractFiltrator {
    private static final Logger LOG = LoggerFactory.getLogger(Ipv6AddressFiltrator.class);
    private static final int MAX_BITS = 128;

    private BitSet mask;
    private BitSet maskedValue;

    /**
     * Constructor
     * @param value IpAddress to compare with
     * @param pathIdentifier Path leading to value with ipAddress in examined node
     */
    public Ipv6AddressFiltrator(IpPrefix value, YangInstanceIdentifier pathIdentifier) {
        super(pathIdentifier);
        Preconditions.checkNotNull(value, "Filtering value can't be null");
        initialize(value);
    }

    @Override
    public boolean isFiltered(NormalizedNode<?, ?> node) {
        try {
            byte[] address = InetAddress.getByName(node.getValue().toString()).getAddress();
            BitSet bitSet = BitSet.valueOf(address);
            bitSet.and(mask);
            if (maskedValue.equals(bitSet)) {
                return false;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Node with value {} was filtered out", node);
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
