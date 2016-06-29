/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.translator.TranslatorHelper;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matus.marko
 */
public class TerminationPointAggregator extends UnificationAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointAggregator.class);
    private static final QName I2RS_TERMINATION_POINT_QNAME = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.
            ietf.network.topology.rev150608.network.node.TerminationPoint.QNAME;
    private Map<Integer, YangInstanceIdentifier> leafPaths;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private Map<YangInstanceIdentifier, List<TemporaryTerminationPoint>> tpStore = new HashMap<>();
    private Class<? extends Model> model;

    private Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTP = new HashMap<>();
    private boolean isAgregationInsideAggregatedNodes = false;

    public TerminationPointAggregator(TopoStoreProvider topoStoreProvider, Class<? extends Model> model) {
        super(topoStoreProvider);
        this.model = model;
    }

    private class TemporaryTerminationPoint {
        private Map<Integer, Object> targetFieldsValues;
        private String tpId;
        private List<MapEntryNode> terminationPointEntries = new ArrayList<>();

        public TemporaryTerminationPoint(Map<Integer, Object> targetFieldsValues) {
            this.targetFieldsValues = targetFieldsValues;
            this.tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        }

        public MapEntryNode getByIdentifier(YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier) {
            for (MapEntryNode tp : terminationPointEntries) {
                if (nodeIdentifier.equals(tp.getIdentifier())) {
                    return tp;
                }
            }
            return null;
        }

        public List<MapEntryNode> getEntries() {
            return terminationPointEntries;
        }

        public void setEntries(List<MapEntryNode> terminationPointEntries) {
            this.terminationPointEntries = terminationPointEntries;
        }

        public Map<Integer, Object> getTargetFieldsValues() {
            return targetFieldsValues;
        }

        public String getTpId() {
            return tpId;
        }
    }

    /**
     * Set path to the leaf which includes data necessary for comparing
     * @param path {@link LeafPath}
     */
    public void setTargetField(Map<Integer, YangInstanceIdentifier> path) {
        this.leafPaths = path;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry,
                                      final String topologyId) {
        LOG.trace("Processing createdChanges");
        if (!isAgregationInsideAggregatedNodes) {
            // save underlayItem to local datastore
            getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().put(identifier, createdEntry);
        }
        // find TerminationPoint Map in underlayItem
        NormalizedNode<?, ?> newNode = createdEntry.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = findTPBasedOnModel(createdEntry);
        if (tpMapNodeOpt.isPresent()) {
            MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
            // set TPMapNode to Items leafnode for further looking for changes
            Map<Integer, NormalizedNode<?, ?>> terminationPointMapNode = new HashMap<>(1);
            terminationPointMapNode.put(0, tpMapNode);
            createdEntry.setLeafNodes(terminationPointMapNode);
            // aggregate Termination points to Temporary TP
            List<TemporaryTerminationPoint> tmpTpList = addTerminationPoints(tpMapNode);
            // add Temporary TP to map
            tpStore.put(identifier, tmpTpList);
            createdEntry.setItem(setTpToNode(tmpTpList, newNode, topologyId, createdEntry.getItemId(), model));
        }
        OverlayItem overlayItem = new OverlayItem(
                Collections.singletonList(createdEntry), CorrelationItemEnum.TerminationPoint);
        createdEntry.setOverlayItem(overlayItem);
        manager.addOverlayItem(overlayItem);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOG.trace("Processing updatedChanges");
        TopologyStore ts = getTopoStoreProvider().getTopologyStore(topologyId);
        List<TemporaryTerminationPoint> nodeTps = tpStore.get(identifier);
        if (nodeTps == null) {
            nodeTps = new ArrayList<>();
        }
        // load underlay item by given key
        UnderlayItem underlayItem = ts.getUnderlayItems().get(identifier);
        MapEntryNode newNode = (MapEntryNode) updatedEntry.getItem();
        Optional<NormalizedNode<?, ?>> updatedTpMapNode = findTPBasedOnModel(updatedEntry);
        if ((!updatedTpMapNode.isPresent() || ((MapNode)updatedTpMapNode.get()).getValue().size() == 0)) {
            nodeTps.clear();
        } else {
            // if node contains Termination points
            // and those TP have some changes inside
            if (updatedTpMapNode.isPresent() && (underlayItem.getLeafNodes() == null ||
                    (underlayItem.getLeafNodes() != null &&
                            !underlayItem.getLeafNodes().equals(updatedTpMapNode.get())))) {
                MapNode newTpMap = (MapNode) updatedTpMapNode.get();
                Map<Integer, NormalizedNode<?, ?>> terminationPointMapNode = new HashMap<>(1);
                terminationPointMapNode.put(0, newTpMap);
                underlayItem.setLeafNodes(terminationPointMapNode);
                removeTerminationPoints(newTpMap, nodeTps);
                updateTerminationPoints(newTpMap, nodeTps);
            }
        }
        underlayItem.setItem(setTpToNode(nodeTps, newNode, topologyId, underlayItem.getItemId(), model));
        manager.updateOverlayItem(underlayItem.getOverlayItem());
    }

    private Optional<NormalizedNode<?, ?>> findTPBasedOnModel(UnderlayItem uItem) {
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = null;
        if (model.equals(NetworkTopologyModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(uItem.getItem(),InstanceIdentifiers.NT_TERMINATION_POINT);
        } else if (model.equals(I2rsModel.class)){
            tpMapNodeOpt = NormalizedNodes.findNode(uItem.getItem(),InstanceIdentifiers.I2RS_TERMINATION_POINT);
        } else if (model.equals(OpendaylightInventoryModel.class)){
            tpMapNodeOpt = NormalizedNodes.findNode(uItem.getLeafNodes().values().iterator().next(),
                    InstanceIdentifiers.INVENTORY_NODE_CONNECTOR_IDENTIFIER);
        }
        return tpMapNodeOpt;
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
            Map<Integer, Object> targetFieldsValues = getTargetFieldsValues(tpMapEntry);
            boolean isNew = true;
            for (TemporaryTerminationPoint item : terminationPointList) {
                boolean targetFieldsMatch = matchTargetFields(item, targetFieldsValues);
                if (targetFieldsMatch) {
                    item.getEntries().add(tpMapEntry);
                    isNew = false;
                    break;
                }
            }
            if (isNew) {
                TemporaryTerminationPoint tmpTp = new TemporaryTerminationPoint(targetFieldsValues);
                tmpTp.getEntries().add(tpMapEntry);
                terminationPointList.add(tmpTp);
            }
        }
        return terminationPointList;
    }

    private void updateTerminationPoints(MapNode newTpMap, List<TemporaryTerminationPoint> tempTpList) {
        boolean isNew;
        for (MapEntryNode newTpEntry : newTpMap.getValue()) {
            Map<Integer, Object> targetFieldsValues = getTargetFieldsValues(newTpEntry);
            if (!targetFieldsValues.isEmpty()) {
                isNew = true;
                for (TemporaryTerminationPoint tmpTp : tempTpList) {
                    MapEntryNode oldTpEntry = tmpTp.getByIdentifier(newTpEntry.getIdentifier());
                    // check if node with same ID exists in TP-store
                    if (null != oldTpEntry) {
                        // if nodes are equal do nothing
                        if (newTpEntry.equals(oldTpEntry)) {
                            isNew = false;
                        } else {
                            // remove it from tmpTp and look if updated newTpEntry should be added to tmpTp
                            tmpTp.getEntries().remove(oldTpEntry);
                            if (matchTargetFields(tmpTp, targetFieldsValues)) {
                                isNew = false;
                                tmpTp.getEntries().add(newTpEntry);
                            }
                        }
                    } else if (matchTargetFields(tmpTp, targetFieldsValues)) {
                        isNew = false;
                        tmpTp.getEntries().add(newTpEntry);
                    }
                }
                if (isNew) {
                    TemporaryTerminationPoint ttp = new TemporaryTerminationPoint(targetFieldsValues);
                    List<MapEntryNode> terminationPointEntries = new ArrayList<>();
                    terminationPointEntries.add(newTpEntry);
                    ttp.setEntries(terminationPointEntries);
                    tempTpList.add(ttp);
                }
            }
        }
    }

    private boolean matchTargetFields(TemporaryTerminationPoint tempTerminationPoint,
            Map<Integer, Object> targetFieldsValues) {
        boolean targetFieldsMatch = false;
        if (targetFieldsValues.size() == tempTerminationPoint.getTargetFieldsValues().size()) {
            targetFieldsMatch = true;
            for (Entry<Integer, Object> targetFieldValueEntry : tempTerminationPoint.getTargetFieldsValues()
                    .entrySet()) {
                Object terminationPointToAddTargetFieldValue =
                        targetFieldsValues.get(targetFieldValueEntry.getKey());
                if (!targetFieldValueEntry.getValue().equals(terminationPointToAddTargetFieldValue)) {
                    targetFieldsMatch = false;
                    break;
                }
            }
        }
        return targetFieldsMatch;
    }

    private String getTerminationPointId(MapEntryNode terminationPoint) {
        YangInstanceIdentifier tpIdIdentifier;
        if(model.equals(NetworkTopologyModel.class) || model.equals(OpendaylightInventoryModel.class)){
            tpIdIdentifier = InstanceIdentifiers.NT_TP_ID_IDENTIFIER;
        } else if(model.equals(I2rsModel.class)) {
            tpIdIdentifier = InstanceIdentifiers.I2RS_TP_ID_IDENTIFIER;
        } else {
            throw new IllegalStateException("Not supported model - " + model);
        }
        Optional<NormalizedNode<?, ?>> tpIdOpt = NormalizedNodes.findNode(terminationPoint, tpIdIdentifier);
        if(tpIdOpt.isPresent()) {
            return tpIdOpt.get().getValue().toString();
        } else {
            throw new IllegalStateException("Termination point must contain id");
        }
    }

    private Map<Integer, Object> getTargetFieldsValues(MapEntryNode terminationPoint) {
        Map<Integer, YangInstanceIdentifier> paths;
        if (isAgregationInsideAggregatedNodes) {
            paths = targetFieldsPerTP.get(getTerminationPointId(terminationPoint));
        } else {
            paths = leafPaths;
        }
        Map<Integer, Object> targetFieldsValues = new HashMap<>(paths.size());
        for (Entry<Integer, YangInstanceIdentifier> leafPathEntry : paths.entrySet()) {
            Optional<NormalizedNode<?, ?>> targetFieldOpt =
                    NormalizedNodes.findNode(terminationPoint, leafPathEntry.getValue());
            if (targetFieldOpt.isPresent()) {
                targetFieldsValues.put(leafPathEntry.getKey(), targetFieldOpt.get().getValue());
            }
        }
        return targetFieldsValues;
    }

    private void removeTerminationPoints(MapNode newTpMap, List<TemporaryTerminationPoint> tempTpList) {
        for (TemporaryTerminationPoint tempTp : tempTpList) {
                Iterator<MapEntryNode> itr = tempTp.getEntries().iterator();
                while(itr.hasNext()) {
                    MapEntryNode mapEN = itr.next();
                    if (!mapContainsEntry(newTpMap, mapEN)) {
                        itr.remove();
                    }
                }
        }
    }

    private MapEntryNode setTpToNode(List<TemporaryTerminationPoint> tempTpList, NormalizedNode<?, ?> node,
            String topologyId, String nodeId, Class<? extends Model> model) {
        // create TP MapNode Builder
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = null;
        if (model.equals(NetworkTopologyModel.class)) {
            tpBuilder = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME);
            for (TemporaryTerminationPoint tmpTp : tempTpList) {
                tpBuilder.addChild(createNetworkTopologyTpEntry(tmpTp, topologyId, nodeId));
            }
        } else if (model.equals(I2rsModel.class)) {
            tpBuilder = ImmutableNodes.mapNodeBuilder(I2RS_TERMINATION_POINT_QNAME);
            for (TemporaryTerminationPoint tmpTp : tempTpList) {
                tpBuilder.addChild(createI2rsTpEntry(tmpTp, topologyId, nodeId));
            }
        } else if (model.equals(OpendaylightInventoryModel.class)) {
            tpBuilder = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME);
            for (TemporaryTerminationPoint tmpTp : tempTpList) {
                tpBuilder.addChild(createInventoryTpEntry(tmpTp, topologyId, nodeId, node));
            }
        }
        // set children (TPs)
        // create clone from old Node and set new TP MapNode to it
        return ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
    }

    private MapEntryNode createInventoryTpEntry(TemporaryTerminationPoint tmpTp, String topologyId,
            String nodeId, NormalizedNode<?, ?> node) {
        ListNodeBuilder<String, LeafSetEntryNode<String>> leafListBuilder = ImmutableLeafSetNodeBuilder.<String>create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TopologyQNames.TP_REF));
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt =
                NormalizedNodes.findNode(node,InstanceIdentifiers.NT_TERMINATION_POINT);
        for (MapEntryNode mapEntryNode : tmpTp.getEntries()) {
            Optional<NormalizedNode<?, ?>> nodeConnectorIdOptional = NormalizedNodes
                    .findNode(mapEntryNode, InstanceIdentifiers.INVENTORY_NODE_ID_IDENTIFIER);
            String tpIdFromNt = null;
            if (nodeConnectorIdOptional.isPresent()) {
                if (tpMapNodeOpt.isPresent()) {
                    for (MapEntryNode mapEntryNodeNt : ((MapNode) tpMapNodeOpt.get()).getValue()) {
                        Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> tpIdOpt =
                                mapEntryNodeNt.getChild(InstanceIdentifiers.NT_TP_ID_IDENTIFIER.getLastPathArgument());
                        if (tpIdOpt.isPresent()) {
                            if (mapEntryNodeNt.getValue().toString()
                                    .contains(nodeConnectorIdOptional.get().getValue().toString())) {
                                tpIdFromNt = (String) tpIdOpt.get().getValue();
                                break;
                            }
                        } else {
                            LOG.debug("No Termination Point ID is present!");
                        }
                    }
                } else {
                    LOG.warn("No Termination Point MapNode is present!");
                }
            } else {
                LOG.warn("No Node Connector ID is present!");
            }
            String tpRefval = TranslatorHelper.createTpRefNT(topologyId, nodeId, tpIdFromNt);
            LeafSetEntryNode<String> tpRef = ImmutableLeafSetEntryNodeBuilder.<String>create()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<String>(
                            TopologyQNames.TP_REF, tpRefval)).withValue(tpRefval).build();
            leafListBuilder.addChild(tpRef);
        }
        MapEntryNode tp = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tmpTp.getTpId())
                .withChild(leafListBuilder.build()).build();
        return tp;
    }

    private MapEntryNode createNetworkTopologyTpEntry(TemporaryTerminationPoint tmpTp, String topologyId,
            String nodeId) {
        ListNodeBuilder<String, LeafSetEntryNode<String>> leafListBuilder = ImmutableLeafSetNodeBuilder.<String>create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TopologyQNames.TP_REF));
        List<LeafSetEntryNode<String>> tpRefs = new ArrayList<>();
        for (MapEntryNode mapEntryNode : tmpTp.getEntries()) {
            Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> leaf =
                    mapEntryNode.getChild(InstanceIdentifiers.NT_TP_ID_IDENTIFIER.getLastPathArgument());
            if (leaf.isPresent()) {
                String tpId;
                if(isAgregationInsideAggregatedNodes) {
                    String[] ids = ((String) leaf.get().getValue()).split("/");
                    topologyId = ids[0];
                    nodeId = ids[1];
                    tpId = ids[2];
                } else {
                    tpId = (String) leaf.get().getValue();
                }
                String value = TranslatorHelper.createTpRefNT(topologyId, nodeId, tpId);
                LeafSetEntryNode<String> tpRef = ImmutableLeafSetEntryNodeBuilder.<String>create()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<String>(
                                TopologyQNames.TP_REF, value)).withValue(value).build();
                tpRefs.add(tpRef);
            }
        }
        leafListBuilder.withValue(tpRefs);
        return ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME,
                tmpTp.getTpId()).withChild(leafListBuilder.build()).build();
    }

    private MapEntryNode createI2rsTpEntry(TemporaryTerminationPoint tmpTp, String topologyId,
            String nodeId) {
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingTermPoints = ImmutableNodes.mapNodeBuilder(
                SupportingTerminationPoint.QNAME);
        for (MapEntryNode mapEntryNode : tmpTp.getEntries()) {
            Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> terminationPointIdOpt =
                    mapEntryNode.getChild(InstanceIdentifiers.I2RS_TP_ID_IDENTIFIER.getLastPathArgument());
            if (terminationPointIdOpt.isPresent()) {
                String tpId;
                if(isAgregationInsideAggregatedNodes) {
                    String[] ids = ((String) terminationPointIdOpt.get().getValue()).split("/");
                    topologyId = ids[0];
                    nodeId = ids[1];
                    tpId = ids[2];
                } else {
                    tpId = (String) terminationPointIdOpt.get().getValue();
                }
                Map<QName, Object> keys = new HashMap<>();
                keys.put(TopologyQNames.I2RS_TP_REF, tpId);
                keys.put(TopologyQNames.I2RS_TP_NETWORK_REF, topologyId);
                keys.put(TopologyQNames.I2RS_TP_NODE_REF, nodeId);
                supportingTermPoints.withChild(ImmutableNodes.mapEntryBuilder()
                        .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingTerminationPoint.QNAME, keys))
                        .build());
            }
        }
        return ImmutableNodes.mapEntryBuilder(I2RS_TERMINATION_POINT_QNAME, TopologyQNames.I2RS_TP_ID_QNAME,
                tmpTp.getTpId()).withChild(supportingTermPoints.build()).build();
    }

    public void setTargetFieldsPerTP(Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTP) {
        this.targetFieldsPerTP.putAll(targetFieldsPerTP);
    }

    public void setAgregationInsideAggregatedNodes(boolean isAgregationInsideAggregatedNodes) {
        this.isAgregationInsideAggregatedNodes = isAgregationInsideAggregatedNodes;
    }
}
