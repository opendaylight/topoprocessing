/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author samuel.kontris
 *
 */
public class NodeAndTPAggregator implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(NodeAndTPAggregator.class);
    private TopologyAggregator nodeAggregator;
    private TerminationPointPreAggregationFiltrator tpFiltrator;
    private TerminationPointAggregator tpAggregator;
    private TopologyManager manager;
    private Class<? extends Model> inputModel;
    private Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTopology;

    private OverlayItemWrapper wrapper;
    private String topologyId;
    private YangInstanceIdentifier itemIdentifier;

    private Manager nodeManager = new Manager() {

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            nodeCreatedOverlayItem(newOverlayItem);
        }

        @Override
        public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
            nodeUpdatedOverlayItem(overlayItemIdentifier);
        }

        @Override
        public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
            nodeRemoveOverlayItem(overlayItemIdentifier);
        }
    };

    private Manager tpManager = new Manager() {

        @Override
        public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
            // should be never called
        }

        @Override
        public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
            // should be never called
        }

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            if(newOverlayItem.getUnderlayItems().size() != 1) {
                throw new IllegalStateException("In newOverlayItemnewOverlayItem should be only one underlay item");
            }

            Optional<NormalizedNode<?, ?>> aggregatedTPsOpt = Optional.absent();
            if(inputModel.equals(NetworkTopologyModel.class) || inputModel.equals(OpendaylightInventoryModel.class)) {
                aggregatedTPsOpt = NormalizedNodes.findNode(newOverlayItem.getUnderlayItems().peek().getItem(),
                                InstanceIdentifiers.NT_TERMINATION_POINT);
            } else if(inputModel.equals(I2rsModel.class)) {
                aggregatedTPsOpt = NormalizedNodes.findNode(newOverlayItem.getUnderlayItems().peek().getItem(),
                                InstanceIdentifiers.I2RS_TERMINATION_POINT);
            } else {
                throw new IllegalStateException("Not supported model - " + inputModel);
            }

            if(aggregatedTPsOpt.isPresent()) {
                MapNode aggregatedTPs = (MapNode) aggregatedTPsOpt.get();
                wrapper.setAggregatedTerminationPoints(aggregatedTPs);
            }
        }
    };


    public NodeAndTPAggregator(TopologyAggregator nodeAggregator, TerminationPointAggregator tpAggregator, TerminationPointPreAggregationFiltrator tpFiltrator,
                    Class<? extends Model> inputModel, Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsperTopology) {
        this.nodeAggregator = nodeAggregator;
        this.tpFiltrator = tpFiltrator;
        this.tpAggregator = tpAggregator;
        nodeAggregator.setTopologyManager(nodeManager);
        tpAggregator.setTopologyManager(tpManager);
        this.inputModel = inputModel;
        this.targetFieldsPerTopology = targetFieldsperTopology;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem createdItem,
                    String topologyId) {
        this.itemIdentifier = itemIdentifier;
        this.topologyId = topologyId;
        nodeAggregator.processCreatedChanges(itemIdentifier, createdItem, topologyId);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem updatedItem,
                    String topologyId) {
        this.itemIdentifier = itemIdentifier;
        this.topologyId = topologyId;
        nodeAggregator.processUpdatedChanges(itemIdentifier, updatedItem, topologyId);
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, String topologyId) {
        this.itemIdentifier = itemIdentifier;
        this.topologyId = topologyId;
        nodeAggregator.processRemovedChanges(itemIdentifier, topologyId);
    }

    @Override
    public void setTopologyManager(Manager manager) {
        if(manager instanceof TopologyManager) {
            this.manager = (TopologyManager) manager;
        } else {
            throw new IllegalStateException("Received manager should be instance of " + TopologyManager.class);
        }
    }

    private void addTPsToWrapper(OverlayItemWrapper wrapper) {
        addTPsToWrapperExceptRemoved(wrapper, null);
    }

    private void addTPsToWrapperExceptRemoved(OverlayItemWrapper wrapper, OverlayItem removedOverlayItem) {
        Collection<MapEntryNode> terminationPoints = new LinkedList<>();
        YangInstanceIdentifier tpIdentifier = null;
        YangInstanceIdentifier tpIdIdentifier = null;
        QName tpQName = null;
        QName nodeQName = null;
        QName nodeIdQName = null;
        if(inputModel.equals(NetworkTopologyModel.class) || inputModel.equals(OpendaylightInventoryModel.class)){
            tpIdentifier = InstanceIdentifiers.NT_TERMINATION_POINT;
            tpIdIdentifier = InstanceIdentifiers.NT_TP_ID_IDENTIFIER;
            tpQName = TerminationPoint.QNAME;
            nodeQName = Node.QNAME;
            nodeIdQName = TopologyQNames.NETWORK_NODE_ID_QNAME;
        } else if(inputModel.equals(I2rsModel.class)) {
            tpIdentifier = InstanceIdentifiers.I2RS_TERMINATION_POINT;
            tpIdIdentifier = InstanceIdentifiers.I2RS_TP_ID_IDENTIFIER;
            tpQName = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608
                            .network.node.TerminationPoint.QNAME;
            nodeQName = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network
                            .Node.QNAME;
            nodeIdQName = TopologyQNames.I2RS_NODE_ID_QNAME;
        } else {
            throw new IllegalStateException("Not supported model - " + inputModel);
        }

        Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTP = new HashMap<>();

        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            if(removedOverlayItem != null && !overlayItem.equals(removedOverlayItem)) {
                for (UnderlayItem underlayItemFromWrapper : overlayItem.getUnderlayItems()) {
                    Map<Integer, YangInstanceIdentifier> targetFields = targetFieldsPerTopology.get(underlayItemFromWrapper.getTopologyId());
                    Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes
                                    .findNode(underlayItemFromWrapper.getItem(), tpIdentifier);
                    if(tpMapNodeOpt.isPresent()) {
                        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
                        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
                            Optional<NormalizedNode<?, ?>> tpIdOpt = NormalizedNodes.findNode(tpMapEntryNode, tpIdIdentifier);
                            if(tpIdOpt.isPresent()) {
                                targetFieldsPerTP.put(tpIdOpt.get().getValue().toString(), targetFields);
                                terminationPoints.add(tpMapEntryNode);
                            }
                        }
                    }
                }
            }
        }

        String nodeId = "fake-node-with-all-TPs";
        Collection<DataContainerChild<? extends PathArgument, ?>> tps = new LinkedList<>();
        tps.add(ImmutableNodes.mapNodeBuilder(tpQName).withValue(terminationPoints).build());
        MapEntryNode nodeMapEntry = ImmutableNodes.mapEntry(nodeQName, nodeIdQName, nodeId);
        NormalizedNode<?, ?> item = ImmutableMapEntryNodeBuilder.create(nodeMapEntry).withValue(tps).build();

        UnderlayItem underlayItem = new UnderlayItem(item, null, topologyId, nodeId, CorrelationItemEnum.Node);

        if(inputModel.equals(OpendaylightInventoryModel.class)) {
            Collection<MapEntryNode> leafNodesTPs = new LinkedList<>();
            for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
                if(removedOverlayItem != null && !overlayItem.equals(removedOverlayItem)) {
                    for (UnderlayItem underlayItemFromWrapper : overlayItem.getUnderlayItems()) {
                        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes
                                        .findNode(underlayItemFromWrapper.getLeafNodes().values().iterator().next(),
                                                        InstanceIdentifiers.INVENTORY_NODE_CONNECTOR_IDENTIFIER);
                        if(tpMapNodeOpt.isPresent()) {
                            MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
                            for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
                                leafNodesTPs.add(tpMapEntryNode);
                            }
                        }
                    }
                }
            }

            Collection<DataContainerChild<? extends PathArgument, ?>> leafNodesTPsList = new LinkedList<>();
            leafNodesTPsList.add(ImmutableNodes.mapNodeBuilder(NodeConnector.QNAME).withValue(leafNodesTPs).build());
            MapEntryNode leafNodesNodeMapEntry = ImmutableNodes.mapEntry(
                            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME,
                            TopologyQNames.INVENTORY_NODE_ID_QNAME, nodeId);
            NormalizedNode<?, ?> leafNode = ImmutableMapEntryNodeBuilder.create(leafNodesNodeMapEntry)
                            .withValue(leafNodesTPsList).build();
            Map<Integer, NormalizedNode<?, ?>> leafNodes = new HashMap<>(1);
            leafNodes.put(0, leafNode);
            underlayItem.setLeafNodes(leafNodes);
        }

        tpAggregator.setTargetFieldsPerTP(targetFieldsPerTP);
        // this will add termination points to wrapper - after aggregating it calls addOverlayItem in tpManager
        if (tpFiltrator == null) {
            tpAggregator.processCreatedChanges(itemIdentifier, underlayItem, topologyId);
        } else {
            tpFiltrator.processCreatedChanges(itemIdentifier, underlayItem, topologyId);
        }
    }

    private void nodeCreatedOverlayItem(OverlayItem newOverlayItem) {
        wrapper = manager.findOrCreateWrapper(newOverlayItem);
        addTPsToWrapper(wrapper);
        manager.writeWrapper(wrapper, newOverlayItem.getCorrelationItem());
    }

    private void nodeUpdatedOverlayItem(OverlayItem overlayItemIdentifier) {
        wrapper = manager.findWrapper(overlayItemIdentifier);
        addTPsToWrapper(wrapper);
        manager.updateOverlayItem(overlayItemIdentifier);
    }

    private void nodeRemoveOverlayItem(OverlayItem overlayItemIdentifier) {
        wrapper = manager.findWrapper(overlayItemIdentifier);
        addTPsToWrapperExceptRemoved(wrapper, overlayItemIdentifier);
        manager.removeOverlayItem(overlayItemIdentifier);
    }
}
