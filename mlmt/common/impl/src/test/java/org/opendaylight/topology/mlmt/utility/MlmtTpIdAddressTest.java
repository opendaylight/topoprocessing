/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;

public class MlmtTpIdAddressTest {

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        // NOOP
    }

    @Test
    public void testTpIpAddress() throws Exception {
        // valid ipv4-address
        String fakeBgp = "bgpls://IsisLevel1:0/type=tp&ipv4=172.16.15.1&id=0";
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setTpId(new TpId(fakeBgp));
        String ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "172.16.15.1");

        // valid ipv4-address
        fakeBgp = "bgpls://IsisLevel1:0/type=tp&ipv4=10.0.0.8&id=0";
        tpBuilder.setTpId(new TpId(fakeBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "10.0.0.8");

        // valid ipv4-address
        fakeBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=0.0.0.8&id=0";
        tpBuilder.setTpId(new TpId(fakeBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "0.0.0.8");

        // invalid ipv4-address
        fakeBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=300.0.0.8&id=0";
        tpBuilder.setTpId(new TpId(fakeBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNull(ipAddress);

        // valid ipv4-address
        fakeBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=0.0.0.0";
        tpBuilder.setTpId(new TpId(fakeBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "0.0.0.0");

        // valid ipv4-address
        String realBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=172.16.15.1";
        tpBuilder.setTpId(new TpId(realBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "172.16.15.1");

        // valid ipv4-address
        realBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=10.0.0.8";
        tpBuilder.setTpId(new TpId(realBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "10.0.0.8");

        // valid ipv4-address (see RFC 5735)
        realBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=0.0.0.8";
        tpBuilder.setTpId(new TpId(realBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "0.0.0.8");

        // invalid ipv4-address
        realBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=300.0.0.8";
        tpBuilder.setTpId(new TpId(realBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNull(ipAddress);

        // valid ipv4-address
        realBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=0.0.0.0";
        tpBuilder.setTpId(new TpId(realBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "0.0.0.0");
    }

    @After
    public void clear() {
        // NOOP
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }
}
