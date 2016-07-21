/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.polkorab
 *
 */
public class NotificationInterConnector implements TopologyOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationInterConnector.class);
    private TopologyOperator operator;
    private TopoStoreProvider topoStoreProvider;
    private CorrelationItemEnum itemType;
    private Map<YangInstanceIdentifier, YangInstanceIdentifier> topoToInvIds = new HashMap<>();

    /**
     * @param itemType item type
     * @param topoStoreProvider topology store provider
     */
    public NotificationInterConnector(CorrelationItemEnum itemType, TopoStoreProvider topoStoreProvider) {
        this.itemType = itemType;
        this.topoStoreProvider = topoStoreProvider;
    }

    @Override
    public synchronized void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String
            topologyId) {
        if (createdEntry != null) {
            LOGGER.trace("Processing created changes");
            Map<YangInstanceIdentifier, UnderlayItem> items = topoStoreProvider.getTopologyStore(topologyId)
                    .getUnderlayItems();
            YangInstanceIdentifier key = null;
            if (itemFromTopology(createdEntry)) {
                key = extractInventoryNodeRefIdentifier(createdEntry);
                if (key == null) {
                    return;
                }
                if (items.containsKey(key)) {
                    if (createdEntry.getItemId()!= null && items.get(key).getItemId()!= null
                            && !items.get(key).getItemId().equals(createdEntry.getItemId())) {
                        for (YangInstanceIdentifier id : topoToInvIds.keySet()) {
                            if (items.get(topoToInvIds.get(id)).getItemId().equals(items.get(key).getItemId())) {
                                processRemovedChanges(id, topologyId);
                                break;
                            }
                        }
                    }
                }
                topoToInvIds.put(identifier, key);
            } else {
                key = identifier;
            }
            UnderlayItem item = items.get(key);
            if (item != null) {
                UnderlayItem combinedItem = null;
                LOGGER.debug("Created changes - item exists");
                if (createdEntry.getLeafNodes() != null) {
                    combinedItem = new UnderlayItem(item.getItem(), createdEntry.getLeafNodes(), topologyId,
                            item.getItemId(), item.getCorrelationItem());
                } else {
                    combinedItem = new UnderlayItem(createdEntry.getItem(), item.getLeafNodes(), topologyId,
                            createdEntry.getItemId(), createdEntry.getCorrelationItem());
                }
                items.put(key, combinedItem);
                operator.processCreatedChanges(key, combinedItem, topologyId);
            } else {
                LOGGER.debug("Created changes - item doesn't exist");
                items.put(key, createdEntry);
            }
        }
    }

    @Override
    public synchronized void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String
            topologyId) {
        if (updatedEntry != null) {
            LOGGER.trace("Processing updated changes");
            Map<YangInstanceIdentifier, UnderlayItem> items = topoStoreProvider.getTopologyStore(topologyId)
                    .getUnderlayItems();
            YangInstanceIdentifier key = null;
            if (topoToInvIds.containsKey(identifier)) {
                key = extractInventoryNodeRefIdentifier(updatedEntry);
                if (!topoToInvIds.get(identifier).equals(key)) {
                    items.get(topoToInvIds.get(identifier)).setItem(null);
                    operator.processRemovedChanges(topoToInvIds.get(identifier), topologyId);
                    processCreatedChanges(identifier, updatedEntry, topologyId);
                    return;
                }
            } else {
                if (itemFromTopology(updatedEntry)) {
                    key = extractInventoryNodeRefIdentifier(updatedEntry);
                    if (key == null) {
                        return;
                    }
                    topoToInvIds.put(identifier, key);
                } else {
                    key = identifier;
                }
            }
            UnderlayItem item = items.get(key);
            if (item != null) {
                UnderlayItem resultingItem = updateItemFields(item, updatedEntry);
                LOGGER.debug("Updated changes - item exists");
                if ((item.getItem() != null) && (item.getLeafNodes() != null)) {
                    operator.processUpdatedChanges(key, resultingItem, topologyId);
                }
                items.put(key, resultingItem);
            } else {
                // might happen only in case when item was created without
                // inventory-node-ref
                LOGGER.debug("Updated changes - item doesn't exist");
                if (itemFromTopology(updatedEntry)) {
                    if (itemType.equals(CorrelationItemEnum.Node)) {
                        items.put(key, updatedEntry);
                    }
                }
            }
        }
    }

    @Override
    public synchronized void processRemovedChanges(YangInstanceIdentifier identifier, String topologyId) {
        if (identifier != null) {
            LOGGER.trace("Processing removed changes");
            Map<YangInstanceIdentifier, UnderlayItem> underlayItems = topoStoreProvider.getTopologyStore(topologyId)
                    .getUnderlayItems();
            YangInstanceIdentifier removalIdentifier = identifier;
            if (topoToInvIds.containsKey(removalIdentifier)) {
                removalIdentifier = topoToInvIds.remove(removalIdentifier);
                underlayItems.get(removalIdentifier).setItem(null);
            } else {
                if (underlayItems.containsKey(removalIdentifier)) {
                    underlayItems.get(removalIdentifier).setLeafNodes(null);
                } else {
                    return;
                }
            }
            UnderlayItem underlayItem = underlayItems.get(removalIdentifier);
            if (underlayItems.get(removalIdentifier).getItem() == null
                    && underlayItems.get(removalIdentifier).getLeafNodes() == null) {
                underlayItems.remove(removalIdentifier);
            }
            // if identifier exists in topology store
            if (underlayItem != null) {
                operator.processRemovedChanges(removalIdentifier, topologyId);
            }
        }
    }

    @Override
    public void setTopologyManager(ITopologyManager manager) {
        throw new UnsupportedOperationException(
                "NotificationInterConnector can't have TopologyManager set," + " it uses TopologyOperator instead.");
    }

    /**
     * @param operator
     *            processes received notifications (aggregates / filters them)
     */
    public void setOperator(TopologyOperator operator) {
        this.operator = operator;
    }

    public TopologyOperator getOperator() {
        return this.operator;
    }

    private static boolean itemFromTopology(UnderlayItem createdEntry) {
        return createdEntry.getItem() != null;
    }

    private YangInstanceIdentifier extractInventoryNodeRefIdentifier(UnderlayItem underlayItem) {
        YangInstanceIdentifier yiid = null;
        HashSet<QName> hashSet = new HashSet<>();
        hashSet.add(TopologyQNames.INVENTORY_NODE_REF_QNAME);
        AugmentationIdentifier ai = new AugmentationIdentifier(hashSet);
        LOGGER.debug("finding inventory-node-ref");
        Optional<NormalizedNode<?, ?>> inventoryNodeRefAugNode = NormalizedNodes.findNode(underlayItem.getItem(), ai);
        if (inventoryNodeRefAugNode.isPresent()) {
            Optional<NormalizedNode<?, ?>> nodeRefNode = NormalizedNodes.findNode(inventoryNodeRefAugNode.get(),
                    InstanceIdentifiers.INV_NODE_REF_IDENTIFIER);
            if (nodeRefNode.isPresent()) {
                yiid = (YangInstanceIdentifier) nodeRefNode.get().getValue();
                LOGGER.debug("inventory-node-ref identifier: {}", yiid);
                return yiid;
            }
        }
        LOGGER.debug("inventory-node-ref identifier is absent");
        return null;
    }

    private static UnderlayItem updateItemFields(UnderlayItem oldItem, UnderlayItem newItem) {
        if (newItem.getItem() == null) {
            newItem.setItem(oldItem.getItem());
        }
        if (newItem.getLeafNodes() == null) {
            newItem.setLeafNodes(oldItem.getLeafNodes());
        }
        if (newItem.getItemId() == null) {
            newItem.setItemId(oldItem.getItemId());
        }
        return newItem;
    }

    public TopoStoreProvider getTopoStoreProvider() {
        return topoStoreProvider;
    }
}
