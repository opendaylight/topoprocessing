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
public class TopologyManager implements DOMRpcAvailabilityListener {

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
     * For testing purpose only
     * @return All overlayItem wrappers
     */
    public Deque<OverlayItemWrapper> getNodeWrappers(){
        return nodeWrappers;
    }

    /**
     * Adds new overlay item into
     * - existing overlay item wrapper
     * - new overlay item wrapper
     * @param newOverlayItem - OverlayItem which shall be put into wrapper
     */
    public void addOverlayItem(OverlayItem newOverlayItem) {
        if (newOverlayItem != null && !newOverlayItem.getUnderlayItems().isEmpty()) {
            for (UnderlayItem newUnderlayItem : newOverlayItem.getUnderlayItems()) {
                for (OverlayItemWrapper wrapper : getWrappersList(newOverlayItem.getCorrelationItem())) {
                    for (OverlayItem overlayItemFromWrapper : wrapper.getOverlayItems()) {
                        for (UnderlayItem underlayItemFromWrapper : overlayItemFromWrapper.getUnderlayItems()) {
                            if (underlayItemFromWrapper.getItemId().equals(newUnderlayItem.getItemId())
                                    && underlayItemFromWrapper.getTopologyId()
                                            .equals(newUnderlayItem.getTopologyId())) {
                                // update existing wrapper
                                wrapper.addOverlayItem(newOverlayItem);
                                writer.writeItem(wrapper, newOverlayItem.getCorrelationItem());
                                registerOverlayRpcs(wrapper, newOverlayItem);
                                return;
                            }
                        }
                    }
                }
            }
            // create new overlay item wrapper with unique id and add the overlay item into it
            String wrapperId;
            if (newOverlayItem.getCorrelationItem().equals(CorrelationItemEnum.TerminationPoint)) {
                wrapperId = idGenerator.getNextIdentifier(CorrelationItemEnum.Node);
            } else {
                wrapperId = idGenerator.getNextIdentifier(newOverlayItem.getCorrelationItem());
            }
            OverlayItemWrapper newWrapper = new OverlayItemWrapper(wrapperId, newOverlayItem);
            getWrappersList(newOverlayItem.getCorrelationItem()).add(newWrapper);
            writer.writeItem(newWrapper, newOverlayItem.getCorrelationItem());
            registerOverlayRpcs(newWrapper, newOverlayItem);
        }
    }

    /**
     * @param overlayItemIdentifier OverlayItem with new changes to update
     */
    public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
        for (OverlayItemWrapper wrapper : getWrappersList(overlayItemIdentifier.getCorrelationItem())) {
            for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
                if (overlayItem.equals(overlayItemIdentifier)) {
                    writer.writeItem(wrapper, overlayItemIdentifier.getCorrelationItem());
                    registerOverlayRpcs(wrapper, overlayItem);
                }
            }
        }
    }

    /**
     * @param overlayItemIdentifier OverlayItem to remove
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
                writer.deleteItem(foundWrapper, overlayItemIdentifier.getCorrelationItem());
                getWrappersList(overlayItemIdentifier.getCorrelationItem()).remove(foundWrapper);
            } else {
                writer.writeItem(foundWrapper, overlayItemIdentifier.getCorrelationItem());
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