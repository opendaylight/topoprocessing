/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
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
 * This class consists of node aggregator and termination point aggregator. Aggregators contains custom managers.
 * Flow after changes are made:
 *     1. Node aggregation using node aggregator
 *     2. After aggregation of nodes is called manager (nodeManager) in node aggregator to write OverlayItem
 *     3. "fake" node with all termination points from wrapper is created
 *     4. Termination points from "fake" node are aggregated in termination point aggregator
 *     5. After aggregation of tps is called manager (tpManager) in tp aggregator
 *     6. From "fake" node are extracted aggregated termination points and inserted in wrapper
 */
public class NodeAndTPAggregator implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(NodeAndTPAggregator.class);
    private TopologyAggregator nodeAggregator;
    private TerminationPointPreAggregationFiltrator tpFiltrator;
    private TerminationPointAggregator tpAggregator;
    private TopologyManager manager;
    private Class<? extends Model> inputModel;
    private Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTopology;

    YangInstanceIdentifier tpIdentifier;
    YangInstanceIdentifier tpIdIdentifier;
    QName tpIdQname;
    QName tpQName;
    QName nodeQName;
    QName nodeIdQName;

    private int wrapperIdGenerator = 0;
    private Map<Integer, OverlayItemWrapper> wrappers = new HashMap<>();

    /**
     * Manager for node aggregator
     */
    private ITopologyManager nodeManager = new ITopologyManager() {

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            OverlayItemWrapper wrapper = manager.findOrCreateWrapper(newOverlayItem);
            int id = wrapperIdGenerator++;
            wrappers.put(id, wrapper);
            addTPsToWrapper(wrapper, id);
            manager.writeWrapper(wrappers.get(id), newOverlayItem.getCorrelationItem());
        }

        @Override
        public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
            OverlayItemWrapper wrapper = manager.findWrapper(overlayItemIdentifier);
            if (wrapper != null) {
                int id = wrapperIdGenerator++;
                wrappers.put(id, wrapper);
                addTPsToWrapper(wrapper, id);
            } else {
                LOG.debug("Update - Wrapper not found");
            }
            manager.updateOverlayItem(overlayItemIdentifier);
        }

        @Override
        public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
            OverlayItemWrapper wrapper = manager.findWrapper(overlayItemIdentifier);
            if (wrapper != null) {
                int id = wrapperIdGenerator++;
                wrappers.put(id, wrapper);
                addTPsToWrapperExceptRemoved(wrapper, id, overlayItemIdentifier);
            } else {
                LOG.debug("Remove - Wrapper not found");
            }
            manager.removeOverlayItem(overlayItemIdentifier);
        }
    };

    /**
     * Manager for termination points aggregator
     */
    private ITopologyManager tpManager = new ITopologyManager() {

        @Override
        public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
            throw new UnsupportedOperationException("This method should be never called.");
        }

        @Override
        public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
            throw new UnsupportedOperationException("This method should be never called.");
        }

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            if(newOverlayItem.getUnderlayItems().size() != 1) {
                throw new IllegalStateException("In newOverlayItem should be only one underlay item");
            }

            UnderlayItem underlayItem = newOverlayItem.getUnderlayItems().peek();
            Optional<NormalizedNode<?, ?>> aggregatedTPsOpt = Optional.absent();
            if(inputModel.equals(NetworkTopologyModel.class) || inputModel.equals(OpendaylightInventoryModel.class)) {
                aggregatedTPsOpt = NormalizedNodes.findNode(underlayItem.getItem(),
                        InstanceIdentifiers.NT_TERMINATION_POINT);
            } else if(inputModel.equals(I2rsModel.class)) {
                aggregatedTPsOpt = NormalizedNodes.findNode(underlayItem.getItem(),
                        InstanceIdentifiers.I2RS_TERMINATION_POINT);
            } else {
                throw new IllegalStateException("Not supported model - " + inputModel);
            }

            if(aggregatedTPsOpt.isPresent()) {
                Optional<NormalizedNode<?, ?>> nodeIdOpt = NormalizedNodes.findNode(underlayItem.getItem(),
                        InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node, inputModel));
                if(nodeIdOpt.isPresent()) {
                    MapNode aggregatedTPs = (MapNode) aggregatedTPsOpt.get();
                    wrappers.get(Integer.parseInt(nodeIdOpt.get().getValue().toString()))
                            .setAggregatedTerminationPoints(aggregatedTPs);
                } else {
                    LOG.warn("Node ID is not present!");
                }
            } else {
                LOG.warn("Aggregater Termination Points are not present");
            }
        }
    };

    public NodeAndTPAggregator(TopologyAggregator nodeAggregator, TerminationPointAggregator tpAggregator,
            TerminationPointPreAggregationFiltrator tpFiltrator,
            Class<? extends Model> inputModel,
            Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsperTopology) {
        this.nodeAggregator = nodeAggregator;
        this.tpFiltrator = tpFiltrator;
        this.tpAggregator = tpAggregator;
        nodeAggregator.setTopologyManager(nodeManager);
        tpAggregator.setTopologyManager(tpManager);
        tpAggregator.setAgregationInsideAggregatedNodes(true);
        this.inputModel = inputModel;
        this.targetFieldsPerTopology = targetFieldsperTopology;

        if(inputModel.equals(NetworkTopologyModel.class) || inputModel.equals(OpendaylightInventoryModel.class)){
            tpIdentifier = InstanceIdentifiers.NT_TERMINATION_POINT;
            tpIdIdentifier = InstanceIdentifiers.NT_TP_ID_IDENTIFIER;
            tpIdQname = TopologyQNames.NETWORK_TP_ID_QNAME;
            tpQName = TerminationPoint.QNAME;
            nodeQName = Node.QNAME;
            nodeIdQName = TopologyQNames.NETWORK_NODE_ID_QNAME;
        } else if(inputModel.equals(I2rsModel.class)) {
            tpIdentifier = InstanceIdentifiers.I2RS_TERMINATION_POINT;
            tpIdIdentifier = InstanceIdentifiers.I2RS_TP_ID_IDENTIFIER;
            tpIdQname = TopologyQNames.I2RS_TP_ID_QNAME;
            tpQName = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608
                    .network.node.TerminationPoint.QNAME;
            nodeQName = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network
                    .Node.QNAME;
            nodeIdQName = TopologyQNames.I2RS_NODE_ID_QNAME;
        } else {
            throw new IllegalStateException("Not supported model - " + inputModel);
        }
    }


    @Override
    public void processCreatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem createdItem,
            String topologyId) {
        nodeAggregator.processCreatedChanges(itemIdentifier, createdItem, topologyId);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem updatedItem,
            String topologyId) {
        nodeAggregator.processUpdatedChanges(itemIdentifier, updatedItem, topologyId);
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, String topologyId) {
        nodeAggregator.processRemovedChanges(itemIdentifier, topologyId);
    }

    @Override
    public void setTopologyManager(ITopologyManager manager) {
        if(manager instanceof TopologyManager) {
            this.manager = (TopologyManager) manager;
        } else {
            throw new IllegalStateException("Received manager should be instance of " + TopologyManager.class);
        }
    }

    private void addTPsToWrapper(OverlayItemWrapper wrapper, int wrapperId) {
        addTPsToWrapperExceptRemoved(wrapper, wrapperId, null);
    }

    private void addTPsToWrapperExceptRemoved(OverlayItemWrapper wrapper, int wrapperId,
            OverlayItem removedOverlayItem) {
        Collection<MapEntryNode> terminationPoints = new LinkedList<>();

        Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTP = new HashMap<>();

        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            if(removedOverlayItem == null || removedOverlayItem != null && !overlayItem.equals(removedOverlayItem)) {
                for (UnderlayItem underlayItemFromWrapper : overlayItem.getUnderlayItems()) {
                    Map<Integer, YangInstanceIdentifier> targetFields = targetFieldsPerTopology.get(
                            underlayItemFromWrapper.getTopologyId());
                    Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes
                            .findNode(underlayItemFromWrapper.getItem(), tpIdentifier);
                    if(tpMapNodeOpt.isPresent()) {
                        MapNode tpMapNode = (MapNode) tpMapNodeOpt.get();
                        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
                            Optional<NormalizedNode<?, ?>> tpIdOpt = NormalizedNodes.findNode(
                                    tpMapEntryNode, tpIdIdentifier);
                            if(tpIdOpt.isPresent()) {
                                // copy termination point except id
                                ArrayList<DataContainerChild<? extends PathArgument, ?>> value =
                                        new ArrayList<>(tpMapEntryNode.getValue());
                                for (int i = 0; i < value.size(); i++) {
                                    try {
                                        if (value.get(i).getNodeType().equals(tpIdQname)) {
                                            value.remove(i);
                                            break;
                                        }
                                    } catch (UnsupportedOperationException ex) {
                                    }
                                }

                                String id = createTerminationPointId(underlayItemFromWrapper, tpIdOpt.get());
                                targetFieldsPerTP.put(id, targetFields);
                                terminationPoints.add(ImmutableNodes.mapEntryBuilder(tpQName, tpIdQname, id)
                                        .withValue(value).build());
                            } else {
                                LOG.trace("Termination point ID is not present!");
                            }
                        }
                    } else {
                        LOG.trace("Termination point MapNode is not present!");
                    }
                }
            }
        }

        String nodeId = Integer.toString(wrapperId);
        Collection<DataContainerChild<? extends PathArgument, ?>> tps = new LinkedList<>();
        tps.add(ImmutableNodes.mapNodeBuilder(tpQName).withValue(terminationPoints).build());
        MapEntryNode nodeMapEntry = ImmutableNodes.mapEntry(nodeQName, nodeIdQName, nodeId);
        NormalizedNode<?, ?> item = ImmutableMapEntryNodeBuilder.create(nodeMapEntry).withValue(tps).build();

        UnderlayItem underlayItem = new UnderlayItem(item, null, null, nodeId, CorrelationItemEnum.Node);

        tpAggregator.setTargetFieldsPerTP(targetFieldsPerTP);
        // this will add termination points to wrapper - after aggregating it calls addOverlayItem in tpManager
        if (tpFiltrator == null) {
            tpAggregator.processCreatedChanges(null, underlayItem, null);
        } else {
            tpFiltrator.processCreatedChanges(null, underlayItem, null);
        }
    }


    private String createTerminationPointId(UnderlayItem underlayItemFromWrapper, NormalizedNode<?, ?> tpId) {
        return underlayItemFromWrapper.getTopologyId() + "/"
                + underlayItemFromWrapper.getItemId() + "/"
                + tpId.getValue().toString();
    }
}
