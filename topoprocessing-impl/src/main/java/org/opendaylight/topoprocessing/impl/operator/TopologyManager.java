/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.rpc.OverlayRpcImplementation;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.uhlir
 *
 */
public class TopologyManager implements DOMRpcAvailabilityListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyManager.class);

    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private List<OverlayItemWrapper> wrappers = new ArrayList<>();
    private TopologyWriter writer;
    private RpcServices rpcServices;
    private Collection<DOMRpcIdentifier> availableRpcs;
    private YangInstanceIdentifier itemIdentifier;
    private GlobalSchemaContextHolder schemaHolder;

    /**
     * @param rpcServices
     * @param schemaHolder
     * @param topologyIdentifier
     */
    public TopologyManager(RpcServices rpcServices, GlobalSchemaContextHolder schemaHolder,
            YangInstanceIdentifier topologyIdentifier) {
        this.rpcServices = rpcServices;
        this.schemaHolder = schemaHolder;
        this.itemIdentifier = topologyIdentifier.node(Node.QNAME);
        availableRpcs = new HashSet<>();
        this.rpcServices.getRpcService().registerRpcListener(this);
    }

    /** for testing purpose only */
    public List<OverlayItemWrapper> getWrappers() {
        return wrappers;
    }

    /**
     * Adds new Logical node into
     * - existing Logical node wrapper
     * - new Logical node wrapper
     * @param newOverlayItem - logical node which shall be put into wrapper
     */
    public void addOverlayItem(OverlayItem newOverlayItem) {
        if (newOverlayItem != null && !newOverlayItem.getUnderlayItems().isEmpty()) {
            for (UnderlayItem newUnderlayItem : newOverlayItem.getUnderlayItems()) {
                for (OverlayItemWrapper wrapper : wrappers) {
                    for (OverlayItem overlayItemFromWrapper : wrapper.getOverlayItems()) {
                        for (UnderlayItem underlayItemFromWrapper : overlayItemFromWrapper.getUnderlayItems()) {
                            if (underlayItemFromWrapper.getItemId().equals(newUnderlayItem.getItemId())) {
                                // update existing wrapper
                                wrapper.addOverlayItem(newOverlayItem);
                                writer.writeNode(wrapper);
                                registerOverlayRpcs(wrapper, newOverlayItem);
                                return;
                            }
                        }
                    }
                }
            }
            // create new Logical node wrapper with unique id and add the logical node into it
            String wrapperId = idGenerator.getNextIdentifier(CorrelationItemEnum.Node);
            OverlayItemWrapper newWrapper = new OverlayItemWrapper(wrapperId, newOverlayItem);
            wrappers.add(newWrapper);
            writer.writeNode(newWrapper);
            registerOverlayRpcs(newWrapper, newOverlayItem);
        }
    }

    /**
     * @param overlayItemIdentifier
     */
    public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
        for (OverlayItemWrapper wrapper : wrappers) {
            for (OverlayItem logicalNode : wrapper.getOverlayItems()) {
                if (logicalNode.equals(overlayItemIdentifier)) {
                    writer.writeNode(wrapper);
                    registerOverlayRpcs(wrapper, logicalNode);
                }
            }
        }
    }

    /**
     * @param overlayItemIdentifier
     */
    public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
        OverlayItemWrapper foundWrapper = null;
        for (OverlayItemWrapper wrapper : wrappers) {
            if (wrapper.getOverlayItems().contains(overlayItemIdentifier)) {
                wrapper.getOverlayItems().remove(overlayItemIdentifier);
                foundWrapper = wrapper;
                break;
            }
        }
        if (foundWrapper != null) {
            if (foundWrapper.getOverlayItems().size() == 0) {
                // remove logical node wrapper as well
                writer.deleteNode(foundWrapper);
                wrappers.remove(foundWrapper);
            } else {
                writer.writeNode(foundWrapper);
            }
        }
    }

    /**
     * @param writer writes into the operational datastore
     */
    public void setWriter(TopologyWriter writer) {
        this.writer = writer;
    }

    @Override
    public void onRpcAvailable(Collection<DOMRpcIdentifier> rpcs) {
        availableRpcs.addAll(rpcs);
    }

    @Override
    public void onRpcUnavailable(Collection<DOMRpcIdentifier> rpcs) {
        availableRpcs.removeAll(rpcs);
    }

    /**
     * Gathers RPCs for all {@link UnderlayItem}s present in the {@link OverlayItem} and registers them under
     * {@link OverlayItemWrapper} Id
     * @param wrapper
     * @param overlayItem
     */
    private void registerOverlayRpcs(OverlayItemWrapper wrapper, OverlayItem overlayItem) {
        LOGGER.trace("Registering overlay RPCs");
        YangInstanceIdentifier contextIdentifier = YangInstanceIdentifier.builder(itemIdentifier)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, wrapper.getId()).build();
        YangInstanceIdentifier nodeEntryIdentifier;
        for (UnderlayItem item : overlayItem.getUnderlayItems()) {
            List<DOMRpcIdentifier> underlayRpcs = new ArrayList<>();
            nodeEntryIdentifier = YangInstanceIdentifier.builder(itemIdentifier)
                    .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, item.getItemId()).build();
            for (Iterator<DOMRpcIdentifier> iterator = availableRpcs.iterator(); iterator.hasNext();) {
                DOMRpcIdentifier rpcIdentifier = iterator.next();
                if (rpcIdentifier.getContextReference().equals(nodeEntryIdentifier)) {
                    underlayRpcs.add(rpcIdentifier);
                }
            }
            OverlayRpcImplementation overlayImplementation =
                    new OverlayRpcImplementation(rpcServices.getRpcService(), schemaHolder.getSchemaContext(),
                            nodeEntryIdentifier);
            Set<DOMRpcIdentifier> overlayRpcIdentifiers = new HashSet<>();
            for (DOMRpcIdentifier underlayRpcIdentifier : underlayRpcs) {
                overlayRpcIdentifiers.add(DOMRpcIdentifier.create(underlayRpcIdentifier.getType(), contextIdentifier));
            }
            if (!overlayRpcIdentifiers.isEmpty()) {
                rpcServices.getRpcProviderService()
                    .registerRpcImplementation(overlayImplementation, overlayRpcIdentifiers);
            }
        }
    }
}
