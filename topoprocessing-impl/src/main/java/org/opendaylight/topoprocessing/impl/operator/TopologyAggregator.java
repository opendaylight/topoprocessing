/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.ScriptResult;

import javax.script.ScriptException;
import javax.script.ScriptEngine;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.Scripting;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;

/**
 * Class handling aggregation correlation
 * @author matus.marko
 * @author martin.uhlir
 */
public abstract class TopologyAggregator extends TopoStoreProvider implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAggregator.class);
    private TopologyManager topologyManager;
    private ScriptEngine scriptEngine;
    private String script;

    /**
     * @param topologyManager handles aggregated nodes from all correlations
     */
    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        this.topologyManager = topologyManager;
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries,
                                                final String topologyId) {
        LOG.trace("Processing createdChanges");
        if (createdEntries != null) {
            for (Entry<YangInstanceIdentifier, UnderlayItem> createdEntry : createdEntries.entrySet()) {
                for (TopologyStore ts : getTopologyStores()) {
                    if (ts.getId().equals(topologyId)) {
                        ts.getUnderlayItems().put(createdEntry.getKey(), createdEntry.getValue());
                    }
                }
                checkForPossibleAggregation(createdEntry.getValue(), topologyId);
            }
        }
    }

    /**
     * Creates new overlay item or adds new underlay item into existing overlay item if the condition
     * for correlation is satisfied
     * @param newItem - new underlay item on which the correlation is created
     */
    private void checkForPossibleAggregation(UnderlayItem newItem, String topologyId) {
        for (TopologyStore ts : getTopologyStores()) {
            if ((! ts.getId().equals(topologyId)) || ts.isAggregateInside()) {
                for (Entry<YangInstanceIdentifier, UnderlayItem> topoStoreEntry : ts.getUnderlayItems().entrySet()) {
                    UnderlayItem topoStoreItem = topoStoreEntry.getValue();
                    if (scriptEngine != null) {
                        if (aggregableWithScript(newItem, topoStoreItem)) {
                            aggregateItems(newItem, topoStoreItem);
                            return;
                        }
                    } else if (! newItem.equals(topoStoreItem) &&
                            topoStoreItem.getLeafNode().getValue().equals(newItem.getLeafNode().getValue())) {
                        // no previous aggregation on this node
                        aggregateItems(newItem, topoStoreItem);
                        return;
                    }
                }
            }
        }
        if (wrapSingleItem()) {
            List<UnderlayItem> itemsToAggregate = new ArrayList<>();
            itemsToAggregate.add(newItem);
            OverlayItem overlayItem = new OverlayItem(itemsToAggregate, newItem.getCorrelationItem());
            newItem.setOverlayItem(overlayItem);
            topologyManager.addOverlayItem(overlayItem);
        }
    }

    private boolean aggregableWithScript(UnderlayItem newItem, UnderlayItem topoStoreItem) {
        ScriptResult scriptResult = new ScriptResult();
        scriptEngine.put("aggregable", scriptResult);
        scriptEngine.put("originalItem", topoStoreItem);
        scriptEngine.put("newItem", newItem);
        try {
            scriptEngine.eval(script);
            return scriptResult.getResult();
        } catch (ScriptException e) {
            throw new IllegalStateException("Exception during script evaluation: " + script, e);
        }
    }

    /**
     * @param newItem item received from notification
     * @param topoStoreItem item already stored in topostore
     */
    private void aggregateItems(UnderlayItem newItem, UnderlayItem topoStoreItem) {
        if (topoStoreItem.getOverlayItem() == null) {
            LOG.debug("Creating new Logical Node");
            // create new logical node
            List<UnderlayItem> nodesToAggregate = new ArrayList<>();
            nodesToAggregate.add(newItem);
            nodesToAggregate.add(topoStoreItem);
            OverlayItem overlayItem = new OverlayItem(nodesToAggregate, topoStoreItem.getCorrelationItem());
            topoStoreItem.setOverlayItem(overlayItem);
            newItem.setOverlayItem(overlayItem);
            topologyManager.addOverlayItem(overlayItem);
            return;
        }
        LOG.debug("Adding physical node to existing Logical Node");
        // add new physical node into existing logical node
        OverlayItem overlayItem = topoStoreItem.getOverlayItem();
        newItem.setOverlayItem(overlayItem);
        overlayItem.addUnderlayItem(newItem);
        topologyManager.updateOverlayItem(overlayItem);
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, final String topologyId) {
        LOG.trace("Processing removedChanges");
        for (TopologyStore ts : getTopologyStores()) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, UnderlayItem> underlayItems = ts.getUnderlayItems();
                if (identifiers != null) {
                    for (YangInstanceIdentifier identifier : identifiers) {
                        UnderlayItem underlayItem = underlayItems.remove(identifier);
                        // if identifier exists in topology store
                        if (underlayItem != null) {
                            // if underlay item is part of some overlay item
                            removeUnderlayItemFromOverlayItem(underlayItem);
                        }
                    }
                }
            }
        }
    }

    private void removeUnderlayItemFromOverlayItem(UnderlayItem itemToRemove) {
        OverlayItem overlayItemIdentifier = itemToRemove.getOverlayItem();
        if (null != overlayItemIdentifier) {
            List<UnderlayItem> underlayItems = overlayItemIdentifier.getUnderlayItems();
            underlayItems.remove(itemToRemove);
            itemToRemove.setOverlayItem(null);
            if (underlayItems.size() < getMinUnderlayItems()) {
                LOG.debug("Removing overlay item");
                for (UnderlayItem remainingNode : underlayItems) {
                    remainingNode.setOverlayItem(null);
                }
                topologyManager.removeOverlayItem(overlayItemIdentifier);
            } else {
                LOG.debug("Removing underlay item from overlay item");
                topologyManager.updateOverlayItem(overlayItemIdentifier);
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries,
                                                String topologyId) {
        LOG.trace("Processing updatedChanges");
        if (updatedEntries != null) {
            for (Entry<YangInstanceIdentifier, UnderlayItem> updatedEntry : updatedEntries.entrySet()) {
                for (TopologyStore ts : getTopologyStores()) {
                    if (ts.getId().equals(topologyId)) {
                        LOG.debug("Updating overlay item");
                        UnderlayItem underlayItem = ts.getUnderlayItems().get(updatedEntry.getKey());
                        Preconditions.checkNotNull(underlayItem, "Updated underlay item not found in the Topology store");
                        UnderlayItem updatedEntryValue = updatedEntry.getValue();
                        underlayItem.setItem(updatedEntryValue.getItem());
                        NormalizedNode<?, ?> leafNode = underlayItem.getLeafNode();
                        // if Leaf Node was changed
                        if (! leafNode.equals(updatedEntryValue.getLeafNode())) {
                            underlayItem.setLeafNode(updatedEntryValue.getLeafNode());
                            if (underlayItem.getOverlayItem() != null) {
                                removeUnderlayItemFromOverlayItem(underlayItem);
                            }
                            checkForPossibleAggregation(underlayItem, topologyId);
                        } else if (underlayItem.getOverlayItem() != null) {
                            // in case that only Node value was changed
                            topologyManager.updateOverlayItem(underlayItem.getOverlayItem());
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * @return minimal number of {@link UnderlayItem}s that must be present in {@link OverlayItem}
     */
    protected abstract int getMinUnderlayItems();

    /**
     * @return true if a single {@link UnderlayItem} should be wrapped into {@link OverlayItem}
     */
    protected abstract boolean wrapSingleItem();

    /**
     * Overrides default behavior of aggregation with the one programmed in script
     * @param scripting script definition
     */
    public void initCustomAggregation(Scripting scripting) {
        ScriptEngineManager factory = new ScriptEngineManager();
        scriptEngine = factory.getEngineByName(scripting.getLanguage());
        Preconditions.checkNotNull(scriptEngine, "ScriptEngine for language {} was not found.",
                scripting.getLanguage());
        script = scripting.getScript();
        LOG.debug("Next script will be used for custom aggregation: {}", script);
    }
}
