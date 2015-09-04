/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventoryRendering.operator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.NotificationInterConnector;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
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
 *
 * @author matej.perina
 *
 */
public class IRNotificationInterConnector implements TopologyOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationInterConnector.class);
    private TopologyOperator operator;
    private TopoStoreProvider topoStoreProvider;
    private CorrelationItemEnum itemType;
    private Map<YangInstanceIdentifier, YangInstanceIdentifier> topoToInvIds = new HashMap<>();
    private static final QName INVENTORY_NODE_REF_QNAME =
            QName.create("(urn:opendaylight:model:topology:inventory?revision=2013-10-30)inventory-node-ref");
    private YangInstanceIdentifier INV_NODE_REF_IDENTIFIER = YangInstanceIdentifier.of(INVENTORY_NODE_REF_QNAME);
    private TopologyManager manager;


    /**
     * @param underlayTopologyId    underlay topology id
     * @param itemType              item type
     * @param topoStoreProvider     topology store provider
     */
    public IRNotificationInterConnector(String underlayTopologyId, CorrelationItemEnum itemType,
            TopoStoreProvider topoStoreProvider) {
        this.itemType = itemType;
        this.topoStoreProvider = topoStoreProvider;
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries,
            String topologyId) {
        if (createdEntries != null) {
            LOGGER.trace("Processing created changes");
            Map<YangInstanceIdentifier, UnderlayItem> items = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            for (Entry<YangInstanceIdentifier, UnderlayItem> createdEntry : createdEntries.entrySet()) {
                YangInstanceIdentifier key = null;
                if (itemFromTopology(createdEntry)) {
                    key = extractInventoryNodeRefIdentifier(createdEntry.getValue());
                    if (key != null) {
                        topoToInvIds.put(createdEntry.getKey(), key);
                    } else {
                        continue;
                    }
                } else {
                    key = createdEntry.getKey();
                }
                UnderlayItem item = items.get(key);
                if (item != null) {
                    UnderlayItem combinedItem = null;
                    LOGGER.debug("Created changes - item exists");
                    if (item.getItem() != null) {
                        combinedItem = new UnderlayItem(item.getItem(), createdEntry.getValue().getLeafNode(),
                                topologyId, item.getItemId(), item.getCorrelationItem());
                    } else {
                        UnderlayItem newItem = createdEntry.getValue();
                        combinedItem = new UnderlayItem(newItem.getItem(), item.getLeafNode(), topologyId,
                                newItem.getItemId(), newItem.getCorrelationItem());
                    }
                    items.put(key, combinedItem);
                    OverlayItem overlayItem = wrapUnderlayItem(combinedItem);
                    combinedItem.setOverlayItem(overlayItem);
                    manager.addOverlayItem(overlayItem);
                } else {
                    LOGGER.debug("Created changes - item doesn't exist");
                    items.put(key, createdEntry.getValue());
                }
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries,
            String topologyId) {
        if (updatedEntries != null) {
            LOGGER.trace("Processing updated changes");
            Map<YangInstanceIdentifier, UnderlayItem> items = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            for (Entry<YangInstanceIdentifier, UnderlayItem> updatedEntry : updatedEntries.entrySet()) {
                YangInstanceIdentifier key = null;
                if (itemFromTopology(updatedEntry)){
                    key = extractInventoryNodeRefIdentifier(updatedEntry.getValue());
                    if (key != null) {
                        topoToInvIds.put(updatedEntry.getKey(), key);
                    } else {
                        continue;
                    }
                } else {
                    key = updatedEntry.getKey();
                }
                UnderlayItem item = items.get(key);
                UnderlayItem updatedItem = updatedEntry.getValue();
                if (item != null) {
                    UnderlayItem resultingItem = null;
                    LOGGER.debug("Updated changes - item exists");
                    if ((item.getItem() != null) && (item.getLeafNode() != null)) {
                        resultingItem = updateItemFields(item, updatedItem);
                        OverlayItem overlayItem = wrapUnderlayItem(resultingItem);
                        resultingItem.setOverlayItem(overlayItem);
                        manager.addOverlayItem(overlayItem);
                    } else {
                        resultingItem = updateItemFields(item, updatedItem);
                    }
                    items.put(key, resultingItem);
                } else {
                    // might happen only in case when item was created without inventory-node-ref
                    LOGGER.debug("Updated changes - item doesn't exist");
                    if (itemFromTopology(updatedEntry)) {
                        if (itemType.equals(CorrelationItemEnum.Node)){
                            items.put(key, updatedEntry.getValue());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        if (identifiers != null) {
            LOGGER.trace("Processing removed changes");
            Map<YangInstanceIdentifier, UnderlayItem> underlayItems = topoStoreProvider.getTopologyStore(topologyId).getUnderlayItems();
            for (YangInstanceIdentifier identifier : identifiers) {
                YangInstanceIdentifier removalIdentifier = identifier;
                YangInstanceIdentifier checkedIdentifier = topoToInvIds.remove(identifier);
                if (checkedIdentifier != null) {
                    removalIdentifier = checkedIdentifier;
                }
                UnderlayItem underlayItem = underlayItems.remove(removalIdentifier);
                // if identifier exists in topology store
                if (underlayItem != null) {
                    manager.removeOverlayItem(underlayItem.getOverlayItem());
                }
            }
        }
    }

    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        this.manager = topologyManager;

    }

    /**
     * @param operator processes received notifications (aggregates / filters them)
     */
    public void setOperator(TopologyOperator operator) {
        this.operator = operator;
    }

    private boolean itemFromTopology(Entry<YangInstanceIdentifier, UnderlayItem> createdEntry) {
        return createdEntry.getValue().getItem() != null;
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

    private UnderlayItem updateItemFields(UnderlayItem oldItem, UnderlayItem newItem) {
        if (newItem.getItem() != null) {
            oldItem.setItem(newItem.getItem());
        }
        if (newItem.getLeafNode() != null) {
            oldItem.setLeafNode(newItem.getLeafNode());
        }
        return oldItem;
    }

    private OverlayItem wrapUnderlayItem(UnderlayItem underlayItem) {
        List<UnderlayItem> underlayItems = Collections.singletonList(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems, underlayItem.getCorrelationItem());
        underlayItem.setOverlayItem(overlayItem);
        return overlayItem;
    }
}
