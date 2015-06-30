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
import org.opendaylight.yangtools.yang.common.QName;
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
    private List<OverlayItemWrapper> nodeWrappers = new ArrayList<>();
    private List<OverlayItemWrapper> linkWrappers = new ArrayList<>();
    private List<OverlayItemWrapper> terminationPointWrappers = new ArrayList<>();
    private TopologyWriter writer;
    private RpcServices rpcServices;
    private Collection<DOMRpcIdentifier> availableRpcs;
    private YangInstanceIdentifier topologyIdentifier;
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
        this.topologyIdentifier = topologyIdentifier;
        availableRpcs = new HashSet<>();
        this.rpcServices.getRpcService().registerRpcListener(this);
    }

    /**
     * Adds new overlay item into
     * - existing overlay item wrapper
     * - new overlay item wrapper
     * @param newOverlayItem - overlay item which shall be put into wrapper
     */
    public void addOverlayItem(OverlayItem newOverlayItem) {
        if (newOverlayItem != null && !newOverlayItem.getUnderlayItems().isEmpty()) {
            for (UnderlayItem newUnderlayItem : newOverlayItem.getUnderlayItems()) {
                for (OverlayItemWrapper wrapper : getWrappersList(newOverlayItem.getCorrelationItem())) {
                    for (OverlayItem overlayItemFromWrapper : wrapper.getOverlayItems()) {
                        for (UnderlayItem underlayItemFromWrapper : overlayItemFromWrapper.getUnderlayItems()) {
                            if (underlayItemFromWrapper.getItemId().equals(newUnderlayItem.getItemId())) {
                                // update existing wrapper
                                wrapper.addOverlayItem(newOverlayItem);
                                writer.writeItem(wrapper);
                                registerOverlayRpcs(wrapper, newOverlayItem);
                                return;
                            }
                        }
                    }
                }
            }
            // create new overlay item wrapper with unique id and add the overlay item into it
            String wrapperId = idGenerator.getNextIdentifier(newOverlayItem.getCorrelationItem());
            OverlayItemWrapper newWrapper = new OverlayItemWrapper(wrapperId, newOverlayItem);
            getWrappersList(newOverlayItem.getCorrelationItem()).add(newWrapper);
            writer.writeItem(newWrapper);
            registerOverlayRpcs(newWrapper, newOverlayItem);
        }
    }

    /**
     * @param overlayItemIdentifier
     */
    public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
        for (OverlayItemWrapper wrapper : getWrappersList(overlayItemIdentifier.getCorrelationItem())) {
            for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
                if (overlayItem.equals(overlayItemIdentifier)) {
                    writer.writeItem(wrapper);
                    registerOverlayRpcs(wrapper, overlayItem);
                }
            }
        }
    }

    /**
     * @param overlayItemIdentifier
     */
    public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
        OverlayItemWrapper foundWrapper = null;
        for (OverlayItemWrapper wrapper : getWrappersList(overlayItemIdentifier.getCorrelationItem())) {
            if (wrapper.getOverlayItems().contains(overlayItemIdentifier)) {
                wrapper.getOverlayItems().remove(overlayItemIdentifier);
                foundWrapper = wrapper;
                break;
            }
        }
        if (foundWrapper != null) {
            if (foundWrapper.getOverlayItems().size() == 0) {
                // remove overlay item wrapper as well
                writer.deleteItem(foundWrapper);
                getWrappersList(overlayItemIdentifier.getCorrelationItem()).remove(foundWrapper);
            } else {
                writer.writeItem(foundWrapper);
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
        QName itemQName = TopologyQNames.buildItemQName(overlayItem.getCorrelationItem());
        QName itemIdQName = TopologyQNames.buildItemIdQName(overlayItem.getCorrelationItem());
        YangInstanceIdentifier contextIdentifier = YangInstanceIdentifier.builder(topologyIdentifier)
                .node(itemQName)
                .nodeWithKey(itemQName, itemIdQName, wrapper.getId()).build();
        YangInstanceIdentifier nodeEntryIdentifier;
        for (UnderlayItem node : overlayItem.getUnderlayItems()) {
            List<DOMRpcIdentifier> underlayRpcs = new ArrayList<>();
            nodeEntryIdentifier = YangInstanceIdentifier.builder(topologyIdentifier)
                    .node(itemQName)
                    .nodeWithKey(itemQName, itemIdQName, node.getItemId()).build();
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

    public List<OverlayItemWrapper> getWrappersList(CorrelationItemEnum correlationItem) {
        List<OverlayItemWrapper> resultList = null;
        switch (correlationItem) {
        case Node:
            resultList = nodeWrappers; 
            break;
        case Link:
            resultList = linkWrappers;
            break;
        case TerminationPoint:
            resultList = terminationPointWrappers;
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
        }
        return resultList;
    }
}