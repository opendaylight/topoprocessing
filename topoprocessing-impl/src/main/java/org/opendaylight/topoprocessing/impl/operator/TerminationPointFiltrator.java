/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.filtrator.AbstractFiltrator;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
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
import com.google.common.base.Optional;

/**
 * @author matus.marko
 */
public class TerminationPointFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationPointFiltrator.class);
    private Optional<Map<Integer, YangInstanceIdentifier>> pathIdentifiers = Optional.absent();
    private Class<? extends Model> model;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();

    public TerminationPointFiltrator(TopoStoreProvider topoStoreProvider, Class<? extends Model> model) {
        super(topoStoreProvider);
        this.model = model;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        NormalizedNode<?, ?> node = createdEntry.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = null;
        if (model.equals(NetworkTopologyModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(node,InstanceIdentifiers.NT_TERMINATION_POINT);
        } else if (model.equals(I2rsModel.class)){
            tpMapNodeOpt = NormalizedNodes.findNode(node,InstanceIdentifiers.I2RS_TERMINATION_POINT);
        } else if (model.equals(OpendaylightInventoryModel.class)){
            tpMapNodeOpt = NormalizedNodes.findNode(createdEntry.getLeafNodes().values().iterator().next(),
                    InstanceIdentifiers.INVENTORY_NODE_CONNECTOR_IDENTIFIER);
        }
        UnderlayItem createdEntryCopy = new UnderlayItem(createdEntry.getItem(), createdEntry.getLeafNodes(),
                createdEntry.getTopologyId(), createdEntry.getItemId(), createdEntry.getCorrelationItem());
        if (tpMapNodeOpt.isPresent()) {
            node = filterTerminationPoints(node, (MapNode) tpMapNodeOpt.get(), topologyId, createdEntry.getItemId());
            createdEntryCopy.setItem(node);
        }
        getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().put(identifier, createdEntryCopy);
        OverlayItem overlayItem = wrapUnderlayItem(createdEntryCopy);
        overlayItem.setCorrelationType(FiltrationOnly.class);
        manager.addOverlayItem(overlayItem);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        Map<YangInstanceIdentifier, UnderlayItem> oldUnderlayItems =
                getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems();
        UnderlayItem updatedEntryCopy = new UnderlayItem(updatedEntry.getItem(), updatedEntry.getLeafNodes(),
                updatedEntry.getTopologyId(), updatedEntry.getItemId(), updatedEntry.getCorrelationItem());
        NormalizedNode<?, ?> newNode = updatedEntryCopy.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = null;
        if (model.equals(NetworkTopologyModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(newNode,InstanceIdentifiers.NT_TERMINATION_POINT);
        } else if (model.equals(I2rsModel.class)){
            tpMapNodeOpt = NormalizedNodes.findNode(newNode,InstanceIdentifiers.I2RS_TERMINATION_POINT);
        } else if (model.equals(OpendaylightInventoryModel.class)){
            tpMapNodeOpt = NormalizedNodes.findNode(updatedEntry.getLeafNodes().values().iterator().next(),
                    InstanceIdentifiers.INVENTORY_NODE_CONNECTOR_IDENTIFIER);
        }
        if (tpMapNodeOpt.isPresent()) {
            newNode = filterTerminationPoints(newNode, (MapNode) tpMapNodeOpt.get(), topologyId,
                            updatedEntry.getItemId());
            updatedEntryCopy.setItem(newNode);
        }
        UnderlayItem oldUnderlayItem = oldUnderlayItems.get(identifier);
        OverlayItem overlayItem = oldUnderlayItem.getOverlayItem();
        overlayItem.setCorrelationType(FiltrationOnly.class);
        Queue<UnderlayItem> underlayItems = new ConcurrentLinkedQueue<>();
        underlayItems.add(updatedEntryCopy);
        overlayItem.setUnderlayItems(underlayItems);
        manager.addOverlayItem(overlayItem);
    }

    private NormalizedNode<?, ?> filterTerminationPoints(NormalizedNode<?, ?> node, MapNode tpMapNode,
            String topologyId, String itemId) {
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        // checks if pathIdentifiers are present if not initialize them from available filtrators
        if (! pathIdentifiers.isPresent()) {
            pathIdentifiers = Optional.of(
                    (Map<Integer, YangInstanceIdentifier>) new HashMap<Integer, YangInstanceIdentifier>());
            for (int i = 0; i < filtrators.size(); i++) {
                pathIdentifiers.get().put(i,((AbstractFiltrator)filtrators.get(i)).getPathIdentifier());
            }
        }
        boolean passed;
        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
            passed = true;
            for (Map.Entry<Integer, YangInstanceIdentifier> pathIdentifier: pathIdentifiers.get().entrySet()) {
                Optional<NormalizedNode<?, ?>> leafNode =
                        NormalizedNodes.findNode(tpMapEntryNode, pathIdentifier.getValue());
                    if (! leafNode.isPresent() || ! passedFiltration(leafNode.get())) {
                        passed = false;
                        break;
                    }
            }
            //check if any Filtrator filtered out
            if (passed) {
                if (model.equals(OpendaylightInventoryModel.class)) {
                    MapEntryNode tp = createInventoryTpEntry(tpMapEntryNode, node, topologyId, itemId);
                    if(tp != null) {
                        tpBuilder.addChild(tp);
                    }
                } else {
                    tpBuilder.addChild(tpMapEntryNode);
                }
            }
        }
        node = ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
        return node;
    }

    private MapEntryNode createInventoryTpEntry(MapEntryNode tpMapEntryNode, NormalizedNode<?,?> node,
            String topologyId, String itemId) {
        String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        Optional<NormalizedNode<?, ?>> nodeConnectorIdOptional = NormalizedNodes
                .findNode(tpMapEntryNode, InstanceIdentifiers.INVENTORY_NODE_ID_IDENTIFIER);
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt =
                NormalizedNodes.findNode(node,InstanceIdentifiers.NT_TERMINATION_POINT);
        String tpIdFromNt = null;
        ListNodeBuilder<String, LeafSetEntryNode<String>> leafListBuilder =
                ImmutableLeafSetNodeBuilder.<String>create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TopologyQNames.TP_REF));
        if(nodeConnectorIdOptional.isPresent()) {
            if(tpMapNodeOpt.isPresent()) {
                for (MapEntryNode mapEntryNode : ((MapNode) tpMapNodeOpt.get()).getValue()) {
                    Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> tpIdOpt =
                            mapEntryNode.getChild(InstanceIdentifiers.NT_TP_ID_IDENTIFIER.getLastPathArgument());
                    if (tpIdOpt.isPresent()) {
                        if (mapEntryNode.getValue().toString()
                                .contains(nodeConnectorIdOptional.get().getValue().toString())) {
                            tpIdFromNt = (String) tpIdOpt.get().getValue();
                            break;
                        }
                    } else {
                        LOGGER.debug("No Termination Point ID is present!");
                    }
                }
            } else {
                LOGGER.warn("No Termination Point MapNode is present!");
            }
        } else {
            LOGGER.warn("No Node Connector ID is present!");
        }
        if(tpIdFromNt == null) {
            return null;
        }
        String tpRefVal = "/network-topology:network-topology/topology/" + topologyId +
                "/node/" + itemId + "/termination-point/" + tpIdFromNt;
        LeafSetEntryNode<String> tpRef = ImmutableLeafSetEntryNodeBuilder.<String>create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<String>(
                        TopologyQNames.TP_REF, tpRefVal)).withValue(tpRefVal).build();
        leafListBuilder.addChild(tpRef);
        MapEntryNode tp = ImmutableNodes
                .mapEntryBuilder(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tpId)
                .withChild(leafListBuilder.build()).build();
        return tp;
    }

}
