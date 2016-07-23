/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class Ipv6AddressFiltratorTest {

    private static final QName ROOT_QNAME = Node.QNAME;
    private static final QName IP_QNAME = QName.create(ROOT_QNAME, "ip-address").intern();
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(IP_QNAME).build();
    private TestNodeCreator creator = new TestNodeCreator();

    @Test
    public void testMask0() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0:0:0:0:0:0:0:0/0"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);
    }

    @Test
    public void testMask16() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:0:0:0:0:0:0:0/16"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0124:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testMask32() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:0:0:0:0:0:0/32"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4568:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testMask48() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:89ab:0:0:0:0:0/48"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ac:cdef:0123:4567:89ab:cdef"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testMask64() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:89ab:cdef:0:0:0:0/64"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cde0:0123:4567:89ab:cdef"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testMask80() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:89ab:cdef:0123:0:0:0/80"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0124:4567:89ab:cdef"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testMask96() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:89ab:cdef:0123:4567:0:0/96"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4568:89ab:cdef"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testMask112() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:89ab:cdef:0123:4567:89ab:0/112"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ac:cdef"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testMask128() {
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:89ab:cdef:0123:4567:89ab:cdef/128"));
        Ipv6AddressFiltrator nodeIpv6 = new Ipv6AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(creator.createLeafNodeWithIpAddress(
                        "0123:4567:89ab:cdef:0123:4567:89ab:cde0"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }
}
