/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.rpc;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author michal.polkorab
 *
 */
public class UnderlayRpcImplementation implements DOMRpcImplementation {

    private DOMRpcService rpcService;

    /**
     * Default constructor
     * @param rpcService 
     */
    public UnderlayRpcImplementation(DOMRpcService rpcService) {
        this.rpcService = rpcService;
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(DOMRpcIdentifier rpc,
            NormalizedNode<?, ?> input) {
        return rpcService.invokeRpc(rpc.getType(), input);
    }

}
