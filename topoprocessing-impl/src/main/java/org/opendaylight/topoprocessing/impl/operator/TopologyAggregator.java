/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.opendaylight.topoprocessing.api.structure.ComputedLink;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.ScriptResult;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.Scripting;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class handling aggregation correlation
 * @author matus.marko
 * @author martin.uhlir
 */
public abstract class TopologyAggregator implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAggregator.class);
    protected ITopologyManager manager;
    private ScriptEngine scriptEngine;
    private String script;
    private TopoStoreProvider topoStoreProvider;

    public TopologyAggregator(TopoStoreProvider topoStoreProvider) {
        this.topoStoreProvider = topoStoreProvider;
    }

    protected TopoStoreProvider getTopoStoreProvider() {
        return topoStoreProvider;
    }

    /**
     * @param manager handles aggregated nodes from all correlations
     */
    @Override
    public void setTopologyManager(ITopologyManager manager) {
        this.manager = manager;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdItem,
                                                final String topologyId) {
        LOG.trace("Processing createdChanges");
        for (TopologyStore ts : topoStoreProvider.getTopologyStores()) {
            //Link aggregation has always only one topology store
            if (ts.getId().equals(topologyId) || createdItem.getCorrelationItem() == CorrelationItemEnum.Link) {
                ts.getUnderlayItems().put(identifier, createdItem);
            }
        }
        checkForPossibleAggregation(createdItem, topologyId);
    }

    /**
     * Creates new overlay item or adds new underlay item into existing overlay item if the condition
     * for correlation is satisfied
     * @param newItem - new underlay item on which the correlation is created
     */
    private void checkForPossibleAggregation(UnderlayItem newItem, String topologyId) {
        for (TopologyStore ts : topoStoreProvider.getTopologyStores()) {
            if ((! ts.getId().equals(topologyId)) || ts.isAggregateInside()) {
                for (Entry<YangInstanceIdentifier, UnderlayItem> topoStoreEntry : ts.getUnderlayItems().entrySet()) {
                    UnderlayItem topoStoreItem = topoStoreEntry.getValue();
                    if(newItem.getCorrelationItem() == CorrelationItemEnum.Link) {
                        checkForPossibleAggregationOfLinks(newItem, topoStoreItem);
                    }
                    else {
                        if (! newItem.equals(topoStoreItem)) {
                            if (scriptEngine != null) {
                                if (aggregableWithScript(newItem, topoStoreItem)) {
                                    aggregateItems(newItem, topoStoreItem);
                                    return;
                                }
                            } else if (matchTargetFields(newItem, topoStoreItem)) {
                                // no previous aggregation on this node
                                aggregateItems(newItem, topoStoreItem);
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (wrapSingleItem()) {
            List<UnderlayItem> itemsToAggregate = new ArrayList<>();
            itemsToAggregate.add(newItem);
            OverlayItem overlayItem = new OverlayItem(itemsToAggregate, newItem.getCorrelationItem());
            newItem.setOverlayItem(overlayItem);
            manager.addOverlayItem(overlayItem);
        }
    }

    private void checkForPossibleAggregationOfLinks(UnderlayItem newItem, UnderlayItem topoStoreItem)
    {
        if(newItem instanceof ComputedLink && topoStoreItem instanceof ComputedLink)
        {
            ComputedLink newLink = (ComputedLink) newItem;
            ComputedLink topoStoreLink = (ComputedLink) topoStoreItem;
            if (scriptEngine != null) {
                if (aggregableWithScript(newItem, topoStoreItem) &&
                        newLink.getSrcNode().equals(topoStoreLink.getSrcNode()) &&
                        newLink.getDstNode().equals(topoStoreLink.getDstNode())) {
                    aggregateItems(newItem, topoStoreItem);
                    return;
                }
            } else if ((!newItem.equals(topoStoreItem)) && matchTargetFields(newItem, topoStoreItem) &&
                    newLink.getSrcNode().equals(topoStoreLink.getSrcNode()) &&
                    newLink.getDstNode().equals(topoStoreLink.getDstNode())) {
                // no previous aggregation on this link
                aggregateItems(newItem, topoStoreItem);
                return;
            }
        }
    }

    private boolean matchTargetFields(UnderlayItem item1, UnderlayItem item2) {
        boolean targetFieldsMatch = false;
        if (item1.getLeafNodes().size() == item2.getLeafNodes().size()) {
            targetFieldsMatch = true;
            for (Entry<Integer, NormalizedNode<?, ?>> targetFieldEntryOfItem1 : item1.getLeafNodes().entrySet()) {
                NormalizedNode<?, ?> targetFieldOfItem2 = item2.getLeafNodes().get(targetFieldEntryOfItem1.getKey());
                if (!targetFieldEntryOfItem1.getValue().getValue().equals(targetFieldOfItem2.getValue())) {
                    return false;
                }
            }
        }
        return targetFieldsMatch;
    }

    private synchronized boolean aggregableWithScript(UnderlayItem newItem, UnderlayItem topoStoreItem) {
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
            manager.addOverlayItem(overlayItem);
            return;
        }
        LOG.debug("Adding physical node to existing Logical Node");
        // add new physical node into existing logical node
        OverlayItem overlayItem = topoStoreItem.getOverlayItem();
        newItem.setOverlayItem(overlayItem);
        overlayItem.addUnderlayItem(newItem);
        manager.updateOverlayItem(overlayItem);
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier identifier, final String topologyId) {
        LOG.trace("Processing removedChanges");
        for (TopologyStore ts : topoStoreProvider.getTopologyStores()) {
            if (ts.getId().equals(topologyId)) {
                Map<YangInstanceIdentifier, UnderlayItem> underlayItems = ts.getUnderlayItems();
                UnderlayItem underlayItem = underlayItems.remove(identifier);
                // if identifier exists in topology store
                if (underlayItem != null) {
                    // if underlay item is part of some overlay item
                    removeUnderlayItemFromOverlayItem(underlayItem);
                }
            }
        }
    }

    private void removeUnderlayItemFromOverlayItem(UnderlayItem itemToRemove) {
        OverlayItem overlayItemIdentifier = itemToRemove.getOverlayItem();
        if (null != overlayItemIdentifier) {
            Queue<UnderlayItem> underlayItems = overlayItemIdentifier.getUnderlayItems();
            underlayItems.remove(itemToRemove);
            itemToRemove.setOverlayItem(null);
            if (underlayItems.size() < getMinUnderlayItems()) {
                LOG.debug("Removing overlay item");
                for (UnderlayItem remainingNode : underlayItems) {
                    remainingNode.setOverlayItem(null);
                }
                manager.removeOverlayItem(overlayItemIdentifier);
            } else {
                LOG.debug("Removing underlay item from overlay item");
                manager.updateOverlayItem(overlayItemIdentifier);
            }
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedItem,
                                                String topologyId) {
        LOG.trace("Processing updatedChanges");
        for (TopologyStore ts : topoStoreProvider.getTopologyStores()) {
            if (ts.getId().equals(topologyId)) {
                LOG.debug("Updating overlay item");
                UnderlayItem underlayItem = ts.getUnderlayItems().get(identifier);
                Preconditions.checkNotNull(underlayItem, "Updated underlay item not found in the Topology store");
                underlayItem.setItem(updatedItem.getItem());
                if(underlayItem.getCorrelationItem() == CorrelationItemEnum.Link) {
                    if(underlayItem instanceof ComputedLink && updatedItem instanceof ComputedLink)
                    {
                        updateLinks(underlayItem, updatedItem);
                        break;
                    }
                }
                else {
                    // if Leaf Node was changed
                    if (! matchTargetFields(underlayItem, updatedItem)) {
                        underlayItem.setLeafNodes(updatedItem.getLeafNodes());
                        underlayItem.setItemId(updatedItem.getItemId());
                        if (underlayItem.getOverlayItem() != null) {
                            removeUnderlayItemFromOverlayItem(underlayItem);
                        }
                        checkForPossibleAggregation(underlayItem, topologyId);
                    } else if (underlayItem.getOverlayItem() != null) {
                        // in case that only Node value was changed
                        manager.updateOverlayItem(underlayItem.getOverlayItem());
                    }
                    break;
                }
            }
        }
    }

    private void updateLinks(UnderlayItem underlayItem, UnderlayItem updatedItem) {
        ComputedLink underlayLink = (ComputedLink) underlayItem;
        ComputedLink updatedLink = (ComputedLink) updatedItem;
        if (! matchTargetFields(underlayItem, updatedItem) ||
                ! underlayLink.getSrcNode().equals(updatedLink.getSrcNode()) ||
                ! underlayLink.getDstNode().equals(updatedLink.getDstNode())) {
            underlayLink.setLeafNodes(updatedLink.getLeafNodes());
            underlayLink.setSrcNode(updatedLink.getSrcNode());
            underlayLink.setDstNode(updatedLink.getDstNode());
            if (underlayItem.getOverlayItem() != null) {
                removeUnderlayItemFromOverlayItem(underlayItem);
            }
            checkForPossibleAggregationOfLinks(updatedLink, underlayLink);
        } else if (underlayItem.getOverlayItem() != null) {
            // in case that only Link value was changed
            manager.updateOverlayItem(underlayItem.getOverlayItem());
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
