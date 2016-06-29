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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 */
public class Ipv4AddressFiltrator extends AbstractFiltrator {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressFiltrator.class);

    private int mask;
    private int maskedValue;


    /**
     * Constructor
     * @param value IpAddress to compare with
     * @param pathIdentifier Path leading to value with ipAddress in examined node
     */
    public Ipv4AddressFiltrator(IpPrefix value, YangInstanceIdentifier pathIdentifier) {
        super(pathIdentifier);
        Preconditions.checkNotNull(value, "Filtering value can't be null");
        maskedValue = initialize(value);
    }

    @Override
    public boolean isFiltered(NormalizedNode<?, ?> node) {
        try {
            int value = ipToInt(node.getValue().toString());
            if (maskedValue == (value & mask)) {
                return false;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Item with value {} was filtered out", node);
            }
        } catch (UnknownHostException e) {
            LOG.error("Wrong format of IP address: {}", e);
        }
        return true;
    }

    private int initialize(IpPrefix prefix) {
        String strValue = prefix.getIpv4Prefix().getValue();
        String[] matches = strValue.split("/");
        int address;
        try {
            address = ipToInt(matches[0]);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Filtrator initialization failed, "
                    + "couldn't recognize ip address: " + matches[0], e);
        }
        mask = (int) ((long) -1 << (32 - Integer.parseInt(matches[1])));
        return address & mask;
    }

    private int ipToInt(String strAddress) throws UnknownHostException {
        InetAddress inetAddr = InetAddress.getByName(strAddress);
        byte[] bytes = inetAddr.getAddress();
        return  ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | ((bytes[3] & 0xFF) << 0);
    }
}
