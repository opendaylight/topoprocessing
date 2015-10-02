/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

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
    private static final QName INVENTORY_NODE_REF_QNAME =
            QName.create("(urn:opendaylight:model:topology:inventory?revision=2013-10-30)inventory-node-ref");
    private YangInstanceIdentifier INV_NODE_REF_IDENTIFIER = YangInstanceIdentifier.of(INVENTORY_NODE_REF_QNAME);

    /**
     * @param underlayTopologyId    underlay topology id
     * @param itemType              item type
     * @param topoStoreProvider     topology store provider
     */
    public NotificationInterConnector(String underlayTopologyId, CorrelationItemEnum itemType,
            TopoStoreProvider topoStoreProvider) {
        this.itemType = itemType;
        this.topoStoreProvider = topoStoreProvider;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry,
            String topologyId) {
        if (createdEntry != null) {
            LOGGER.trace("Processing created changes");
            Map<YangInstanceIdentifier, UnderlayItem> items = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            YangInstanceIdentifier key = null;
            if (itemFromTopology(createdEntry)) {
                key = extractInventoryNodeRefIdentifier(createdEntry);
                if (key != null) {
                    topoToInvIds.put(identifier, key);
                } else {
                    return;
                }
            } else {
                key = identifier;
            }
            UnderlayItem item = items.get(key);
            if (item != null) {
                UnderlayItem combinedItem = null;
                LOGGER.debug("Created changes - item exists");
                if (item.getItem() != null) {
                    combinedItem = new UnderlayItem(item.getItem(), createdEntry.getLeafNode(),
                            topologyId, item.getItemId(), item.getCorrelationItem());
                } else {
                    UnderlayItem newItem = createdEntry;
                    combinedItem = new UnderlayItem(newItem.getItem(), item.getLeafNode(), topologyId,
                            newItem.getItemId(), newItem.getCorrelationItem());
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
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry,
            String topologyId) {
        if (updatedEntry != null) {
            LOGGER.trace("Processing updated changes");
            Map<YangInstanceIdentifier, UnderlayItem> items = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            YangInstanceIdentifier key = null;
            if (itemFromTopology(updatedEntry)){
                key = extractInventoryNodeRefIdentifier(updatedEntry);
                if (key != null) {
                    topoToInvIds.put(identifier, key);
                } else {
                    return;
                }
            } else {
                key = identifier;
            }
            UnderlayItem item = items.get(key);
            UnderlayItem updatedItem = updatedEntry;
            if (item != null) {
                UnderlayItem resultingItem = null;
                LOGGER.debug("Updated changes - item exists");
                if ((item.getItem() != null) && (item.getLeafNode() != null)) {
                    resultingItem = updateItemFields(item, updatedItem);
                    operator.processUpdatedChanges(key, resultingItem, topologyId);
                } else {
                    resultingItem = updateItemFields(item, updatedItem);
                }
                items.put(key, resultingItem);
            } else {
                // might happen only in case when item was created without inventory-node-ref
                LOGGER.debug("Updated changes - item doesn't exist");
                if (itemFromTopology(updatedEntry)) {
                    if (itemType.equals(CorrelationItemEnum.Node)){
                        items.put(key, updatedEntry);
                    }
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier identifier, String topologyId) {
        if (identifier != null) {
            LOGGER.trace("Processing removed changes");
            Map<YangInstanceIdentifier, UnderlayItem> underlayItems = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            YangInstanceIdentifier removalIdentifier = identifier;
            YangInstanceIdentifier checkedIdentifier = topoToInvIds.remove(identifier);
            if (checkedIdentifier != null) {
                removalIdentifier = checkedIdentifier;
            }
            UnderlayItem underlayItem = underlayItems.remove(removalIdentifier);
            // if identifier exists in topology store
            if (underlayItem != null) {
                operator.processRemovedChanges(removalIdentifier, topologyId);
            }
        }
    }

    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        throw new UnsupportedOperationException("NotificationInterConnector can't have TopologyManager set,"
                + " it uses TopologyOperator instead.");

    }

    /**
     * @param operator processes received notifications (aggregates / filters them)
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
        hashSet.add(INVENTORY_NODE_REF_QNAME);
        AugmentationIdentifier ai = new AugmentationIdentifier(hashSet);
        LOGGER.debug("finding inventory-node-ref");
        Optional<NormalizedNode<?, ?>> inventoryNodeRefAugNode = NormalizedNodes.findNode(underlayItem.getItem(), ai);
        if (inventoryNodeRefAugNode.isPresent()) {
            Optional<NormalizedNode<?,?>> nodeRefNode =
                    NormalizedNodes.findNode(inventoryNodeRefAugNode.get(), INV_NODE_REF_IDENTIFIER);
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
        if (newItem.getItem() != null) {
            oldItem.setItem(newItem.getItem());
        }
        if (newItem.getLeafNode() != null) {
            oldItem.setLeafNode(newItem.getLeafNode());
        }
        return oldItem;
    }
}
