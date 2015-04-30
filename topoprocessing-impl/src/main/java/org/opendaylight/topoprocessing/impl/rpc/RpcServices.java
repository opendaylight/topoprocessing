/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.rpc;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;

/**
 * Stores {@link DOMRpcService} and {@link DOMRpcProviderService} for RPC republishing operations
 * @author michal.polkorab
 */
public class RpcServices {

    private DOMRpcService rpcService;
    private DOMRpcProviderService rpcProviderService;

    /**
     * Default constructor
     * @param rpcService
     * @param rpcProviderService
     */
    public RpcServices(DOMRpcService rpcService, DOMRpcProviderService rpcProviderService) {
        this.rpcService = rpcService;
        this.rpcProviderService = rpcProviderService;
    }

    /**
     * @return {@link DOMRpcService}
     */
    public DOMRpcService getRpcService() {
        return rpcService;
    }

    /**
     * @return {@link DOMRpcProviderService}
     */
    public DOMRpcProviderService getRpcProviderService() {
        return rpcProviderService;
    }

}
