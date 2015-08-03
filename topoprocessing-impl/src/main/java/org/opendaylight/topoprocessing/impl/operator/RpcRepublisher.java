/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;

import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.rpc.OverlayRpcImplementation;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author michal.polkorab
 *
 */
public class RpcRepublisher implements DOMRpcAvailabilityListener {

    private RpcServices rpcServices;
    private YangInstanceIdentifier topologyIdentifier;
    private GlobalSchemaContextHolder schemaHolder;
    private Collection<DOMRpcIdentifier> availableRpcs;

    /**
     * Default constructor
     * @param rpcServices services used for rpc republishing
     * @param topologyIdentifier identifities overlay topology
     * @param schemaHolder 
     */
    public RpcRepublisher(RpcServices rpcServices, YangInstanceIdentifier topologyIdentifier,
            GlobalSchemaContextHolder schemaHolder) {
        this.rpcServices = rpcServices;
        this.topologyIdentifier = topologyIdentifier;
        this.schemaHolder = schemaHolder;
        availableRpcs = new HashSet<>();
    }

    /**
     * Gathers RPCs for all {@link UnderlayItem}s present in the {@link OverlayItem} and registers them under
     * {@link OverlayItemWrapper} Id
     * @param wrapper wraps LogicalNode and contains id for republished rpc
     * @param overlayItem Which contains UnderlayItems with RPCs
     */
    public void registerRpcs(OverlayItemWrapper wrapper, OverlayItem overlayItem) {
        QName itemQName = TopologyQNames.buildItemQName(overlayItem.getCorrelationItem());
        QName itemIdQName = TopologyQNames.buildItemIdQName(overlayItem.getCorrelationItem());
        YangInstanceIdentifier contextIdentifier = YangInstanceIdentifier.builder(topologyIdentifier)
                .node(itemQName)
                .nodeWithKey(itemQName, itemIdQName, wrapper.getId()).build();
        YangInstanceIdentifier nodeEntryIdentifier;
        for (UnderlayItem item : overlayItem.getUnderlayItems()) {
            List<DOMRpcIdentifier> underlayRpcs = new ArrayList<>();
            nodeEntryIdentifier = YangInstanceIdentifier.builder(topologyIdentifier)
                    .node(itemQName)
                    .nodeWithKey(itemQName, itemIdQName, item.getItemId()).build();
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

    @Override
    public void onRpcAvailable(Collection<DOMRpcIdentifier> rpcs) {
        availableRpcs.addAll(rpcs);
    }

    @Override
    public void onRpcUnavailable(Collection<DOMRpcIdentifier> rpcs) {
        availableRpcs.removeAll(rpcs);
    }

}
