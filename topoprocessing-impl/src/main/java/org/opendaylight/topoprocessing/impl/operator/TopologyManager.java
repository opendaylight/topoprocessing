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
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.uhlir
 *
 */
public class TopologyManager implements DOMRpcAvailabilityListener, ITopologyManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyManager.class);

    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private Deque<OverlayItemWrapper> nodeWrappers = new ConcurrentLinkedDeque<>();
    private Deque<OverlayItemWrapper> linkWrappers = new ConcurrentLinkedDeque<>();
    private TopologyWriter writer;
    private RpcServices rpcServices;
    private Collection<DOMRpcIdentifier> availableRpcs;
    private YangInstanceIdentifier topologyIdentifier;
    private GlobalSchemaContextHolder schemaHolder;
    private Class<? extends Model> outputModel;

    /**
     * @param rpcServices used for rpc republishing
     * @param schemaHolder access to SchemaContext
     * @param topologyIdentifier topology identifier
     */
    public TopologyManager(RpcServices rpcServices, GlobalSchemaContextHolder schemaHolder,
            YangInstanceIdentifier topologyIdentifier, Class<? extends Model> outputModel) {
        this.rpcServices = rpcServices;
        this.schemaHolder = schemaHolder;
        this.topologyIdentifier = topologyIdentifier;
        availableRpcs = new HashSet<>();
        this.rpcServices.getRpcService().registerRpcListener(this);
        this.outputModel = outputModel;
    }

    /**
     * For testing purpose only.
     *
     * @return All overlayItem wrappers
     */
    public Deque<OverlayItemWrapper> getNodeWrappers() {
        return nodeWrappers;
    }

    /**
     * Adds new overlay item into:
     * - existing overlay item wrapper
     * - new overlay item wrapper.
     *
     * @param newOverlayItem - OverlayItem which shall be put into wrapper
     */
    @Override
    public void addOverlayItem(OverlayItem newOverlayItem) {
        if (newOverlayItem != null && !newOverlayItem.getUnderlayItems().isEmpty()) {
            OverlayItemWrapper wrapper = findOrCreateWrapper(newOverlayItem);
            writer.writeItem(wrapper, newOverlayItem.getCorrelationItem());
        }
    }


    /**
     * Adds new overlay item into existing wrapper or creates one.
     *
     * @param newOverlayItem OverlayItem which shall be put into wrapper
     * @return existing or new wrapper
     */
    public OverlayItemWrapper findOrCreateWrapper(OverlayItem newOverlayItem) {
        OverlayItemWrapper wrapper = findWrapperWithUnderlayItem(newOverlayItem.getUnderlayItems(),
                        newOverlayItem.getCorrelationItem());
        if (wrapper != null) {
            wrapper.addOverlayItem(newOverlayItem);
            registerOverlayRpcs(wrapper, newOverlayItem);
            return wrapper;
        } else {
            // create new overlay item wrapper with unique id and add the overlay item into it
            String wrapperId;
            if (newOverlayItem.getCorrelationItem().equals(CorrelationItemEnum.TerminationPoint)) {
                wrapperId = idGenerator.getNextIdentifier(CorrelationItemEnum.Node);
            } else {
                wrapperId = idGenerator.getNextIdentifier(newOverlayItem.getCorrelationItem());
            }
            OverlayItemWrapper newWrapper = new OverlayItemWrapper(wrapperId, newOverlayItem);
            getWrappersList(newOverlayItem.getCorrelationItem()).add(newWrapper);
            registerOverlayRpcs(newWrapper, newOverlayItem);
            return newWrapper;
        }
    }

    public void writeWrapper(OverlayItemWrapper wrapper, CorrelationItemEnum correlationItem) {
        writer.writeItem(wrapper, correlationItem);
    }

    /**
     * @param overlayItemIdentifier OverlayItem with new changes to update
     */
    @Override
    public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
        OverlayItemWrapper wrapper = findWrapper(overlayItemIdentifier);
        if (wrapper != null) {
            writer.writeItem(wrapper, overlayItemIdentifier.getCorrelationItem());
            registerOverlayRpcs(wrapper, overlayItemIdentifier);
        }
    }

    /**
     * @param overlayItemIdentifier OverlayItem to remove
     */
    @Override
    public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
        OverlayItemWrapper foundWrapper = findWrapper(overlayItemIdentifier);
        if (foundWrapper != null) {
            foundWrapper.getOverlayItems().remove(overlayItemIdentifier);
            if (foundWrapper.getOverlayItems().size() == 0) {
                // remove overlay item wrapper as well
                writer.deleteItem(foundWrapper, overlayItemIdentifier.getCorrelationItem());
                getWrappersList(overlayItemIdentifier.getCorrelationItem()).remove(foundWrapper);
            } else {
                writer.writeItem(foundWrapper, overlayItemIdentifier.getCorrelationItem());
            }
        }
    }

    /**
     * Tries to find wrapper containing OverlayItem in existing wrapper.
     *
     * @param overlayItemIdentifier
     * @return wrapper or null if wrapper is not found
     */
    public OverlayItemWrapper findWrapper(OverlayItem overlayItemIdentifier) {
        for (OverlayItemWrapper wrapper : getWrappersList(overlayItemIdentifier.getCorrelationItem())) {
            if (wrapper.getOverlayItems().contains(overlayItemIdentifier)) {
                return wrapper;
            }
        }
        return null;
    }

    /**
     * Tries to find wrapper containing AT LEAST ONE UnderlayItem from collection.
     *
     * @param underlayItems
     * @return wrapper or null if wrapper is not found
     */
    private OverlayItemWrapper findWrapperWithUnderlayItem(Collection<UnderlayItem> underlayItems,
                    CorrelationItemEnum correlationItem) {
        for (UnderlayItem underlayItem : underlayItems) {
            for (OverlayItemWrapper wrapper : getWrappersList(correlationItem)) {
                for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
                    for (UnderlayItem underlayItemFromWrapper : overlayItem.getUnderlayItems()) {
                        if (underlayItemFromWrapper.getItemId().equals(underlayItem.getItemId())
                                        && underlayItemFromWrapper.getTopologyId()
                                                .equals(underlayItem.getTopologyId())) {
                            return wrapper;
                        }
                    }
                }
            }
        }
        return null;
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
     * {@link OverlayItemWrapper} Id.
     *
     * @param wrapper wraps LogicalNode and contains id for republished rpc
     * @param overlayItem Which contains UnderlayItems with RPCs
     */
    private void registerOverlayRpcs(OverlayItemWrapper wrapper, OverlayItem overlayItem) {
        LOGGER.trace("Registering overlay RPCs");
        QName itemQName = TopologyQNames.buildItemQName(overlayItem.getCorrelationItem(), outputModel);
        QName itemIdQName = TopologyQNames.buildItemIdQName(overlayItem.getCorrelationItem(), outputModel);
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

    private Deque<OverlayItemWrapper> getWrappersList(CorrelationItemEnum correlationItem) {
        Deque<OverlayItemWrapper> resultList = null;
        switch (correlationItem) {
            case Node:
            case TerminationPoint:
                resultList = nodeWrappers;
                break;
            case Link:
                resultList = linkWrappers;
                break;
            default:
                throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
        }
        return resultList;
    }
}