/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TerminationPointAggregator extends TopologyAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointAggregator.class);
    private static final int UNDERLAY_ITEMS_IN_OVERLAY_ITEM = 1;
    private static final boolean WRAP_SINGLE_UNDERLAY_ITEM = true;

    private YangInstanceIdentifier leafPath;
    private List<TemporaryTerminationPoint> terminationPointList;

    private class TemporaryTerminationPoint {
        private List<NormalizedNode<?, ?>> list = new ArrayList<>();
        private Object target;

        public List<NormalizedNode<?, ?>> getTerminationPoints() {
            return list;
        }

        public void add(NormalizedNode<?, ?> tp) {
            list.add(tp);
        }

        public Object getTarget() {
            return target;
        }

        public void setTarget(Object target) {
            this.target = target;
        }
    }

    @Override
    protected int getMinUnderlayItems() {
        return UNDERLAY_ITEMS_IN_OVERLAY_ITEM;
    }

    @Override
    protected boolean wrapSingleItem() {
        return WRAP_SINGLE_UNDERLAY_ITEM;
    }

    /**
     * Set path to the leaf which includes data necessary for comparing
     * @param path {@link LeafPath}
     */
    public void setTargetField(YangInstanceIdentifier path) {
        this.leafPath = path;
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries,
                                      final String topologyId) {
        LOG.trace("Processing createdChanges");
        if (createdEntries != null) {
            for (Map.Entry<YangInstanceIdentifier, UnderlayItem> createdEntry : createdEntries.entrySet()) {
                UnderlayItem underlayItem = createdEntry.getValue();
                for (TopologyStore ts : getTopologyStores()) {
                    if (ts.getId().equals(topologyId)) {
                        ts.getUnderlayItems().put(createdEntry.getKey(), underlayItem);
                        break;
                    }
                }
                Optional<NormalizedNode<?, ?>> optTpMapNode = NormalizedNodes.findNode(underlayItem.getItem(),
                        YangInstanceIdentifier.of(TerminationPoint.QNAME));
                if (optTpMapNode.isPresent()) {
                    aggregateTerminationPoints((MapNode) optTpMapNode.get());
                }

                //TODO send node to manager
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId) {
        LOG.trace("Processing updatedChanges");
        if (updatedEntries != null) {
            for (TopologyStore ts : getTopologyStores()) {
                if (ts.getId().equals(topologyId)) {
                    for (Map.Entry<YangInstanceIdentifier, UnderlayItem> updatedEntry : updatedEntries.entrySet()) {
                        NormalizedNode<?, ?> oldItem = ts.getUnderlayItems().get(updatedEntry.getKey()).getItem();
                        Optional<NormalizedNode<?, ?>> optOldTpMapNode = NormalizedNodes.findNode(oldItem,
                                YangInstanceIdentifier.of(TerminationPoint.QNAME));
                        if (! optOldTpMapNode.isPresent()) {
                            LOG.warn("Missing old Map with Termination Points");
                            break;
                        }
                        Optional<NormalizedNode<?, ?>> optUpdatedTpMapNode = NormalizedNodes.findNode(
                                updatedEntry.getValue().getItem(), YangInstanceIdentifier.of(TerminationPoint.QNAME));
                        if (! optUpdatedTpMapNode.isPresent()) {
                            LOG.warn("Missing new Map with Termination Points");
                            break;
                        }

                        if (! optOldTpMapNode.get().equals(optUpdatedTpMapNode.get())) {
                            aggregateTerminationPoints((MapNode) optUpdatedTpMapNode);
                        }
                        oldItem = updatedEntry.getValue().getItem();

                        //TODO send node to manager
                    }
                }
            }
        }
    }

    private void aggregateTerminationPoints(MapNode mapNode) {
        for (MapEntryNode tpMapEntry : mapNode.getValue()) {
            Optional<NormalizedNode<?, ?>> targetField = NormalizedNodes.findNode(tpMapEntry, leafPath);
            if (targetField.isPresent()) {
                aggregateTpItem(tpMapEntry, targetField.get());
            }
        }
    }

    private void aggregateTpItem(MapEntryNode tpMapEntry, NormalizedNode<?, ?> targetField) {
        for (TemporaryTerminationPoint item : terminationPointList) {
            if (item.getTarget().equals(targetField)) {
                item.add(tpMapEntry);
                return;
            }
        }
        TemporaryTerminationPoint tmpTp = new TemporaryTerminationPoint();
        tmpTp.setTarget(targetField);
        tmpTp.add(tpMapEntry);
        terminationPointList.add(tmpTp);
    }
}
