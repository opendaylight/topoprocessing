/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.rpc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class RpcServicesTest {

    @Mock private DOMRpcService rpcService;
    @Mock private DOMRpcProviderService rpcProviderService;
    private RpcServices rpcServices;

    @Before
    public void setUp() {
        rpcServices = new RpcServices(rpcService, rpcProviderService);
    }

    @Test
    public void testGetRpcService() {
        Assert.assertEquals("Wrong object received", rpcService, rpcServices.getRpcService());
    }

    @Test
    public void testGetRpcProviderService() {
        Assert.assertEquals("Wrong object received", rpcProviderService, rpcServices.getRpcProviderService());
    }
}
