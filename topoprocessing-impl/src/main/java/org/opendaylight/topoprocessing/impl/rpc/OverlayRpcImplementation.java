/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.rpc;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.broker.spi.rpc.RpcRoutingStrategy;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for delegation of RPC calls.
 * Topoprocessing project exposes RPCs of underlay items and republishes them
 * as {@link OverlayRpcImplementation} which stores original underlay item
 * to invoke received RPC on
 * @author michal.polkorab
 */
public class OverlayRpcImplementation implements DOMRpcImplementation {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayRpcImplementation.class);
    private RpcServices rpcServices;
    private SchemaContext schemaContext;
    private YangInstanceIdentifier underlayItemIdentifier;

    /**
     * Default constructor
     * @param schemaContext used to get {@link RpcDefinition} {@link RpcRoutingStrategy}
     * @param rpcServices instance of {@link RpcServices}
     * @param underlayNodeIdentifier identifies node which the RPC should be really invoked on
     */
    public OverlayRpcImplementation(SchemaContext schemaContext, RpcServices rpcServices,
            YangInstanceIdentifier underlayNodeIdentifier) {
        this.schemaContext = schemaContext;
        this.underlayItemIdentifier = underlayNodeIdentifier;
        this.rpcServices = rpcServices;
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(DOMRpcIdentifier rpc,
            NormalizedNode<?, ?> input) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Invoked rpc: " + rpc);
        }
        RpcDefinition rpcDefinition = findRpcDefinition(schemaContext, rpc.getType());
        if (null == rpcDefinition) {
            return Futures.immediateFailedCheckedFuture(
                    (DOMRpcException) new DOMRpcException("Rpc definition not found") { });
        }
        RpcRoutingStrategy routingStrategy = RpcRoutingStrategy.from(rpcDefinition);
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBuilder =
                ImmutableContainerNodeBuilder.create(ImmutableNodes.containerNode(input.getNodeType()));
        Collection<DataContainerChild<? extends PathArgument, ?>> inputChilds = ((ContainerNode) input).getValue();
        for (DataContainerChild<? extends PathArgument, ?> child : inputChilds) {
            if (! (child instanceof AugmentationNode)) {
                if (child.getNodeType().equals(routingStrategy.getLeaf())) {
                    containerBuilder.addChild(ImmutableNodes.leafNode(child.getNodeType(), underlayItemIdentifier));
                } else {
                    containerBuilder.addChild(child);
                }
            } else {
                containerBuilder.addChild(child);
            }
        }
        ContainerNode underlayRpcInput = containerBuilder.build();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reinvoking RPC at: {} with input: {}", underlayItemIdentifier, underlayRpcInput);
        }
        return rpcServices.invokeRpc(rpc.getType(), underlayRpcInput);
    }

    private static RpcDefinition findRpcDefinition(final SchemaContext context, final SchemaPath schemaPath) {
        if (context != null) {
            final QName qname = schemaPath.getPathFromRoot().iterator().next();
            final Module module = context.findModuleByNamespaceAndRevision(qname.getNamespace(), qname.getRevision());
            if (module != null && module.getRpcs() != null) {
                for (RpcDefinition rpc : module.getRpcs()) {
                    if (qname.equals(rpc.getQName())) {
                        return rpc;
                    }
                }
            }
        }
        LOGGER.debug("RpcDefinition not found");
        return null;
    }
}
