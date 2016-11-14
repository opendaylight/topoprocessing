/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.rpc;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Stores {@link DOMRpcService} and {@link DOMRpcProviderService} for RPC republishing operations.
 * @author michal.polkorab
 */
public class RpcServices implements DOMRpcProviderService, DOMRpcService, AutoCloseable {

    private DOMRpcService rpcService;
    private DOMRpcProviderService rpcProviderService;

    private List<DOMRpcImplementationRegistration> rpcImplementationRegs = Lists.newArrayList();
    private List<ListenerRegistration> rpcListenerRegs= Lists.newArrayList();

    /**
     * Default constructor.
     * @param rpcService            registers new RpcListeners and invokes Rpcs
     * @param rpcProviderService    registers new RpcImplementations
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

    @Nonnull
    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(@Nonnull T t, @Nonnull DOMRpcIdentifier... domRpcIdentifiers) {
        DOMRpcImplementationRegistration<T> reg = rpcProviderService.registerRpcImplementation(t, domRpcIdentifiers);
        rpcImplementationRegs.add(reg);
        return reg;
    }

    @Nonnull
    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(@Nonnull T t, @Nonnull Set<DOMRpcIdentifier> set) {
        DOMRpcImplementationRegistration<T> reg = rpcProviderService.registerRpcImplementation(t, set);
        rpcImplementationRegs.add(reg);
        return reg;
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull SchemaPath schemaPath, @Nullable NormalizedNode<?, ?> normalizedNode) {
        return rpcService.invokeRpc(schemaPath, normalizedNode);
    }

    @Nonnull
    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull T t) {
        ListenerRegistration<T> reg = rpcService.registerRpcListener(t);
        rpcListenerRegs.add(reg);
        return reg;
    }

    @Override
    public void close() throws Exception {
        rpcImplementationRegs.forEach(r -> r.close());
        rpcListenerRegs.forEach(r -> r.close());
    }
}
