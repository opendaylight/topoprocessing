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
    private Map<YangInstanceIdentifier, List<TemporaryTerminationPoint>> tpStore = new HashMap<>();

    private class TemporaryTerminationPoint {
        private NormalizedNode<?, ?> targetField;
        private String tpId;
        private List<MapEntryNode> list = new ArrayList<>();

        public TemporaryTerminationPoint(NormalizedNode<?, ?> targetField) {
            this.targetField = targetField;
            this.tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        }

        public MapEntryNode getByIdentifie(YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier) {
            for (MapEntryNode tp : list) {
                if (nodeIdentifier.equals(tp.getIdentifier())) {
                    return tp;
                }
            }
            return null;
        }

        public List<MapEntryNode> getEntries() {
            return list;
        }

        public void setEntires(List<MapEntryNode> list) {
            this.list = list;
        }

        public NormalizedNode<?, ?> getTargetField() {
            return targetField;
        }

        public String getTpId() {
            return tpId;
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
                // save underlayItem to local datastore
                getTopologyStore(topologyId).getUnderlayItems().put(createdEntry.getKey(), underlayItem);
                // find TerminationPoint Map in underlayItem
                NormalizedNode<?, ?> newNode = underlayItem.getItem();
                Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(newNode,
                        YangInstanceIdentifier.of(TerminationPoint.QNAME));
                if (tpMapNodeOpt.isPresent()) {
                    MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
                    // set TPMapNode to Items leafnode for further looking for changes
                    underlayItem.setLeafNode(tpMapNode);
                    // aggregate Termination points to Temporary TP
                    List<TemporaryTerminationPoint> tmpTpList = addTerminationPoints(tpMapNode);
                    // add Temporary TP to map
                    tpStore.put(createdEntry.getKey(), tmpTpList);
                    underlayItem.setItem(setTpToNode(tmpTpList, newNode));
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
                List<TemporaryTerminationPoint> nodeTps = tpStore.get(updatedEntry.getKey());
                // load underlay item by given key
                UnderlayItem underlayItem = ts.getUnderlayItems().get(updatedEntry.getKey());
                MapEntryNode newNode = (MapEntryNode) updatedEntry.getValue().getItem();
                Optional<NormalizedNode<?, ?>> updatedTpMapNode = NormalizedNodes.findNode(
                        newNode, YangInstanceIdentifier.of(TerminationPoint.QNAME));
                // if node contains Termination points
                // and those TP have some changes inside
                if (updatedTpMapNode.isPresent()
                        && (! underlayItem.getLeafNode().equals(updatedTpMapNode.get()))) {
                    MapNode newTpMap = (MapNode) updatedTpMapNode.get();
                    underlayItem.setLeafNode(newTpMap);
                    removeTerminationPoints(newTpMap, nodeTps);
                    updateTerminationPoints(newTpMap, nodeTps);
                }
                underlayItem.setItem(setTpToNode(nodeTps, newNode));
                topologyManager.updateOverlayItem(underlayItem.getOverlayItem());
            }
        }
    }

    private void addItemToList(MapEntryNode tpMapEntry, NormalizedNode<?, ?> targetField,
                               List<TemporaryTerminationPoint> tempTpList) {
        for (TemporaryTerminationPoint item : tempTpList) {
            if (item.getTargetField().equals(targetField)) {
                item.getEntries().add(tpMapEntry);
                return;
            }
        }
        TemporaryTerminationPoint tmpTp = new TemporaryTerminationPoint(targetField);
        tmpTp.getEntries().add(tpMapEntry);
        tempTpList.add(tmpTp);
    }

    private boolean mapContainsEntry(MapNode newMapNode, MapEntryNode oldEntry) {
        for (MapEntryNode newTpEntry : newMapNode.getValue()) {
            if (oldEntry.getIdentifier().equals(newTpEntry.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    private List<TemporaryTerminationPoint> addTerminationPoints(MapNode tpMapNode) {
        List<TemporaryTerminationPoint> terminationPointList = new ArrayList<>();
        for (MapEntryNode tpMapEntry : tpMapNode.getValue()) {
            Optional<NormalizedNode<?, ?>> targetField = NormalizedNodes.findNode(tpMapEntry, leafPath);
            if (targetField.isPresent()) {
                addItemToList(tpMapEntry, targetField.get(), terminationPointList);
            }
        }
        return terminationPointList;
    }

    private void updateTerminationPoints(MapNode newTpMap, List<TemporaryTerminationPoint> tempTpList) {
        boolean isNew;
        for (MapEntryNode newTpEntry : newTpMap.getValue()) {
            Optional<NormalizedNode<?, ?>> targetFieldOpt = NormalizedNodes.findNode(newTpEntry, leafPath);
            if (targetFieldOpt.isPresent()) {
                isNew = true;
                for (TemporaryTerminationPoint tmpTp : tempTpList) {
                    MapEntryNode oldTpEntry = tmpTp.getByIdentifie(newTpEntry.getIdentifier());
                    // check if node with same ID exists in TP-store
                    if (null != oldTpEntry) {
                        // if nodes are equal
                        if (newTpEntry.equals(oldTpEntry)) {
                            isNew = false;
                        } else {
                            // remove it
                            tempTpList.remove(oldTpEntry);
                            if (targetFieldOpt.get().equals(tmpTp.getTargetField())) {
                                isNew = false;
                                tmpTp.getEntries().add(newTpEntry);
                            }
                        }
                    } else if (targetFieldOpt.get().equals(tmpTp.getTargetField())) {
                        isNew = false;
                        tmpTp.getEntries().add(newTpEntry);
                    }
                }
                if (isNew) {
                    TemporaryTerminationPoint ttp = new TemporaryTerminationPoint(targetFieldOpt.get());
                    ttp.setEntires(Collections.singletonList(newTpEntry));
                    tempTpList.add(ttp);
                }
            }
        }
    }

    private void removeTerminationPoints(MapNode newTpMap, List<TemporaryTerminationPoint> tempTpList) {
        for (TemporaryTerminationPoint tempTp : tempTpList) {
            for (MapEntryNode oldTpEntry : tempTp.getEntries()) {
                if (! mapContainsEntry(newTpMap, oldTpEntry)) {
                    tempTpList.remove(oldTpEntry);
                }
            }
        }
    }

    private MapEntryNode setTpToNode(List<TemporaryTerminationPoint> tempTpList, NormalizedNode<?, ?> node) {
        // create TP MapNode Builder
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME);
        // set children (TPs)
        for (TemporaryTerminationPoint tmpTp : tempTpList) {
            if (1 < tmpTp.getEntries().size()) {
                tpBuilder.addChild(createTpEntry(tmpTp));
            } else {
                tpBuilder.addChild(tmpTp.getEntries().iterator().next());
            }
        }
        // create clone from old Node and set new TP MapNode to it
        return ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
    }

    private MapEntryNode createTpEntry(TemporaryTerminationPoint tmpTp) {
        ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafListBuilder = ImmutableLeafSetNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TopologyQNames.TP_REF));
        for (MapEntryNode mapEntryNode : tmpTp.getEntries()) {
            Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> leaf
                    = mapEntryNode.<LeafNode>getChild(new YangInstanceIdentifier.NodeIdentifier(
                    TopologyQNames.NETWORK_TP_ID_QNAME));
            if (leaf.isPresent()) {
                Object value = leaf.get().getValue();
                leafListBuilder.withChildValue(ImmutableLeafSetEntryNodeBuilder.create()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue(
                                TopologyQNames.TP_REF, value)).withValue(value).build());
            }
        }
        return ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tmpTp.getTpId())
                .withChild(leafListBuilder.build()).build();
    }
}
