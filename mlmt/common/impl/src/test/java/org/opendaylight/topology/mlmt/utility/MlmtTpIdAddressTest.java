/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

public class MlmtTpIdAddressTest{

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

        final String fakeBgp = "bgpls://IsisLevel1:0/type=tp&ipv4=10.0.1.11&id=0";
        final String realBgp = "bgpls://IsisLevel1:100/type=tp&mt=0&ipv4=172.16.15.1";

        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setTpId(new TpId(fakeBgp));
        String ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "10.0.1.11");

        tpBuilder.setTpId(new TpId(realBgp));
        ipAddress = MlmtTpIdIpAddress.ipv4Address(tpBuilder.build());
        assertNotNull(ipAddress);
        assertEquals(ipAddress, "172.16.15.1");
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
