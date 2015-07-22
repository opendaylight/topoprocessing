/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.*;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author matus.marko
 */
public class TerminationPointAggregator extends TopologyAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointAggregator.class);
    private static final int UNDERLAY_ITEMS_IN_OVERLAY_ITEM = 1;
    private static final boolean WRAP_SINGLE_UNDERLAY_ITEM = true;

    private YangInstanceIdentifier leafPath;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();

    private class TemporaryTerminationPoint {
        private List<MapEntryNode> list = new ArrayList<>();
        private Object target;

        public List<MapEntryNode> getTerminationPoints() {
            return list;
        }

        public void add(MapEntryNode tp) {
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
            // iterate through nodes
            for (Map.Entry<YangInstanceIdentifier, UnderlayItem> createdEntry : createdEntries.entrySet()) {
                UnderlayItem underlayItem = createdEntry.getValue();
                getTopologyStore(topologyId).getUnderlayItems().put(createdEntry.getKey(), underlayItem);
                Optional<NormalizedNode<?, ?>> optTpMapNode = NormalizedNodes.findNode(underlayItem.getItem(),
                        YangInstanceIdentifier.of(TerminationPoint.QNAME));
                if (optTpMapNode.isPresent()) {
                    List<TemporaryTerminationPoint> tmpTpList = aggregateTerminationPoints((MapNode) optTpMapNode.get());
                    updateUnderlayItem(tmpTpList, underlayItem);
                }
                OverlayItem overlayItem = new OverlayItem(
                        Collections.singletonList(underlayItem), CorrelationItemEnum.Node);
                underlayItem.setOverlayItem(overlayItem);
                topologyManager.addOverlayItem(overlayItem);
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId) {
        LOG.trace("Processing updatedChanges");
        if (updatedEntries != null) {
            TopologyStore ts = getTopologyStore(topologyId);
            for (Map.Entry<YangInstanceIdentifier, UnderlayItem> updatedEntry : updatedEntries.entrySet()) {
                UnderlayItem oldUnderlayItem = ts.getUnderlayItems().get(updatedEntry.getKey());
                // look for TP MapNode in Updated Node
                UnderlayItem underlayItem = updatedEntry.getValue();
                Optional<NormalizedNode<?, ?>> optUpdatedTpMapNode = NormalizedNodes.findNode(
                        underlayItem.getItem(), YangInstanceIdentifier.of(TerminationPoint.QNAME));
                if (optUpdatedTpMapNode.isPresent()
                    && (! oldUnderlayItem.getLeafNode().equals(optUpdatedTpMapNode.get()))) {
                    List<TemporaryTerminationPoint> tmpTpList
                            = aggregateTerminationPoints((MapNode) optUpdatedTpMapNode.get());
                    updateUnderlayItem(tmpTpList, underlayItem);
                }
                OverlayItem overlayItem = oldUnderlayItem.getOverlayItem();
                overlayItem.setUnderlayItems(Collections.singletonList(underlayItem));
                underlayItem.setOverlayItem(overlayItem);
                topologyManager.updateOverlayItem(overlayItem);
            }
        }
    }

    private List<TemporaryTerminationPoint> aggregateTerminationPoints(MapNode mapNode) {
        List<TemporaryTerminationPoint> terminationPointList = new ArrayList<>();
        for (MapEntryNode tpMapEntry : mapNode.getValue()) {
            Optional<NormalizedNode<?, ?>> targetField = NormalizedNodes.findNode(tpMapEntry, leafPath);
            if (targetField.isPresent()) {
                addItemToList(terminationPointList, tpMapEntry, targetField.get());
            }
        }
        return terminationPointList;
    }

    private void addItemToList(List<TemporaryTerminationPoint> terminationPointList,
                               MapEntryNode tpMapEntry, NormalizedNode<?, ?> targetField) {
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

    private void updateUnderlayItem(List<TemporaryTerminationPoint> terminationPointList, UnderlayItem underlay) {
        // create TP MapNode Builder
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME);
        // set children (TPs)
        for (TemporaryTerminationPoint tmpTp : terminationPointList) {
            if (1 < tmpTp.getTerminationPoints().size()) {
                tpBuilder.addChild(createTpEntry(tmpTp));
            } else {
                tpBuilder.addChild(tmpTp.getTerminationPoints().get(0));
            }
        }
        // create clone from old Node and set new TP MapNode to it
        MapNode tpMap = tpBuilder.build();
        MapEntryNode newNode = ImmutableMapEntryNodeBuilder.create((MapEntryNode) underlay.getItem())
                .withChild(tpMap).build();
        underlay.setItemId(idGenerator.getNextIdentifier(CorrelationItemEnum.Node));
        underlay.setLeafNode(tpMap);
        underlay.setItem(newNode);
    }

    private MapEntryNode createTpEntry(TemporaryTerminationPoint tmpTp) {
        String id = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafListBuilder = ImmutableLeafSetNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(TopologyQNames.TP_REF));
        for (MapEntryNode mapEntryNode : tmpTp.getTerminationPoints()) {
            Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> leaf
                    = mapEntryNode.<LeafNode>getChild(new NodeIdentifier(TopologyQNames.NETWORK_TP_ID_QNAME));
            if (leaf.isPresent()) {
                Object value = leaf.get().getValue();
                leafListBuilder.withChildValue(ImmutableLeafSetEntryNodeBuilder.create()
                        .withNodeIdentifier(new NodeWithValue(TopologyQNames.TP_REF, value)).withValue(value).build());
            }
        }
        return ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, id)
                .withChild(leafListBuilder.build()).build();
    }
}
