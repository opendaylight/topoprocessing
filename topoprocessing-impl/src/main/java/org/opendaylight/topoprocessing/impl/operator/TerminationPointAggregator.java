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
public class TerminationPointAggregator extends UnificationAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointAggregator.class);
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
                NormalizedNode<?, ?> node = underlayItem.getItem();
                Optional<NormalizedNode<?, ?>> tpMapNode = NormalizedNodes.findNode(node,
                        YangInstanceIdentifier.of(TerminationPoint.QNAME));
                if (tpMapNode.isPresent()) {
                    MapNode initTpMapNode = (MapNode) tpMapNode.get();
                    List<TemporaryTerminationPoint> tmpTpList = aggregateTerminationPoints(initTpMapNode);
                    underlayItem.setItem(setTpToNode(tmpTpList, node));
                    underlayItem.setLeafNode(initTpMapNode);
                }
                OverlayItem overlayItem = new OverlayItem(
                        Collections.singletonList(underlayItem), CorrelationItemEnum.TerminationPoint);
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
                UnderlayItem underlayItem = ts.getUnderlayItems().get(updatedEntry.getKey());
                NormalizedNode<?, ?> node = updatedEntry.getValue().getItem();
                // look for TP MapNode in Updated Node
                Optional<NormalizedNode<?, ?>> updatedTpMapNode = NormalizedNodes.findNode(
                        node, YangInstanceIdentifier.of(TerminationPoint.QNAME));
                if (updatedTpMapNode.isPresent()
                    && (! underlayItem.getLeafNode().equals(updatedTpMapNode.get()))) {
                    MapNode initTpMapNode = (MapNode) updatedTpMapNode.get();
                    List<TemporaryTerminationPoint> tmpTpList = aggregateTerminationPoints(initTpMapNode);
                    node = setTpToNode(tmpTpList, node);
                    underlayItem.setLeafNode(updatedTpMapNode.get());
                }
                underlayItem.setItem(node);
                topologyManager.updateOverlayItem(underlayItem.getOverlayItem());
            }
        }
    }

    private List<TemporaryTerminationPoint> aggregateTerminationPoints(MapNode mapNode) {
        List<TemporaryTerminationPoint> terminationPointList = new ArrayList<>();
        for (MapEntryNode tpMapEntry : mapNode.getValue()) {
            Optional<NormalizedNode<?, ?>> targetField = NormalizedNodes.findNode(tpMapEntry, leafPath);
            if (targetField.isPresent()) {
                addItemToList(tpMapEntry, targetField.get(), terminationPointList);
            }
        }
        return terminationPointList;
    }

    private void addItemToList(MapEntryNode tpMapEntry, NormalizedNode<?, ?> targetField,
                               List<TemporaryTerminationPoint> terminationPointList) {
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

    private MapEntryNode setTpToNode(List<TemporaryTerminationPoint> terminationPointList, NormalizedNode<?, ?> node) {
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
        return ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
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
