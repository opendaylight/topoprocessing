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
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

/**
 * @author matus.marko
 */
public class TerminationPointFiltrator extends TopologyFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationPointFiltrator.class);
    private YangInstanceIdentifier pathIdentifier;
    private Class<? extends Model> model;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();

    public TerminationPointFiltrator(TopoStoreProvider topoStoreProvider, Class<? extends Model> model) {
        super(topoStoreProvider);
        this.model = model;
    }

    public TerminationPointFiltrator(TopoStoreProvider topoStoreProvider) {
        super(topoStoreProvider);
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier identifier, UnderlayItem createdEntry, String topologyId) {
        LOGGER.trace("Processing createdChanges");
        NormalizedNode<?, ?> node = createdEntry.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = null;
        if (model.equals(NetworkTopologyModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(node, InstanceIdentifiers.NT_TERMINATION_POINT);
        } else if (model.equals(I2rsModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(node, InstanceIdentifiers.I2RS_TERMINATION_POINT);
        } else if (model.equals(OpendaylightInventoryModel.class)) {
            tpMapNodeOpt = NormalizedNodes.findNode(createdEntry.getLeafNodes().values().iterator().next(),
                    InstanceIdentifiers.INVENTORY_NODE_CONNECTOR_IDENTIFIER);
        }
        if (tpMapNodeOpt.isPresent()) {
            node = filterTerminationPoints(node, (MapNode) tpMapNodeOpt.get(), topologyId, createdEntry.getItemId());
            createdEntry.setItem(node);
        }
        getTopoStoreProvider().getTopologyStore(topologyId).getUnderlayItems().put(identifier, createdEntry);
        OverlayItem overlayItem = wrapUnderlayItem(createdEntry);
        overlayItem.setCorrelationType(FiltrationOnly.class);
        manager.addOverlayItem(overlayItem);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier identifier, UnderlayItem updatedEntry, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        Map<YangInstanceIdentifier, UnderlayItem> oldUnderlayItems = getTopoStoreProvider().getTopologyStore(topologyId)
                .getUnderlayItems();
        OverlayItem overlayItem;
        UnderlayItem newUnderlayItem = updatedEntry;
        NormalizedNode<?, ?> newNode = newUnderlayItem.getItem();
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(newNode,
                YangInstanceIdentifier.of(TerminationPoint.QNAME));
        if (tpMapNodeOpt.isPresent()) {
            newNode = filterTerminationPoints(newNode, (MapNode) tpMapNodeOpt.get(), topologyId,
                    updatedEntry.getItemId());
            newUnderlayItem.setItem(newNode);
        }
        UnderlayItem oldUnderlayItem = oldUnderlayItems.get(identifier);
        overlayItem = oldUnderlayItem.getOverlayItem();
        overlayItem.setCorrelationType(FiltrationOnly.class);
        Queue<UnderlayItem> underlayItems = new ConcurrentLinkedQueue<>();
        underlayItems.add(newUnderlayItem);
        overlayItem.setUnderlayItems(underlayItems);
        manager.addOverlayItem(overlayItem);
    }

    private NormalizedNode<?, ?> filterTerminationPoints(NormalizedNode<?, ?> node, MapNode tpMapNode,
            String topologyId, String itemId) {
        CollectionNodeBuilder<MapEntryNode, MapNode> tpBuilder = ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME);
        for (MapEntryNode tpMapEntryNode : tpMapNode.getValue()) {
            Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(tpMapEntryNode, pathIdentifier);
            if (leafNode.isPresent()) {
                if (passedFiltration(leafNode.get())) {
                    if (model.equals(OpendaylightInventoryModel.class)) {
                        MapEntryNode tp = createInventoryTpEntry(tpMapEntryNode, node, topologyId, itemId);
                        tpBuilder.addChild(tp);
                    } else {
                        MapEntryNode tp = createI2rsTpEntry(tpMapEntryNode, node, topologyId, itemId);
                        tpBuilder.addChild(tp);
                    }
                }
            }
        }
        node = ImmutableMapEntryNodeBuilder.create((MapEntryNode) node).withChild(tpBuilder.build()).build();
        return node;
    }

    private MapEntryNode createInventoryTpEntry(MapEntryNode tpMapEntryNode, NormalizedNode<?, ?> node,
            String topologyId, String itemId) {
        String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingTermPoints = ImmutableNodes
                .mapNodeBuilder(SupportingTerminationPoint.QNAME);
        Optional<NormalizedNode<?, ?>> nodeConnectorIdOptional = NormalizedNodes.findNode(tpMapEntryNode,
                InstanceIdentifiers.INVENTORY_NODE_ID_IDENTIFIER);
        Optional<NormalizedNode<?, ?>> tpMapNodeOpt = NormalizedNodes.findNode(node,
                InstanceIdentifiers.NT_TERMINATION_POINT);
        String tpIdFromNt = null;
        if (nodeConnectorIdOptional.isPresent()) {
            if (tpMapNodeOpt.isPresent()) {
                for (MapEntryNode mapEntryNode : ((MapNode) tpMapNodeOpt.get()).getValue()) {
                    Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> tpIdOpt = mapEntryNode
                            .getChild(InstanceIdentifiers.NT_TP_ID_IDENTIFIER.getLastPathArgument());
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
        Map<QName, Object> keys = new HashMap<>();
        keys.put(TopologyQNames.I2RS_TP_REF, tpIdFromNt);
        keys.put(TopologyQNames.I2RS_TP_NETWORK_REF, topologyId);
        keys.put(TopologyQNames.I2RS_TP_NODE_REF, tpIdFromNt);
        supportingTermPoints.addChild(ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingTerminationPoint.QNAME, keys)).build());
        return ImmutableNodes
                .mapEntryBuilder(TopologyQNames.I2RS_TERMINATION_POINT_QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId)
                .withChild(supportingTermPoints.build()).build();
    }

    public void setPathIdentifier(YangInstanceIdentifier pathIdentifier) {
        this.pathIdentifier = pathIdentifier;
    }

    private MapEntryNode createI2rsTpEntry(MapEntryNode tpMapEntryNode, NormalizedNode<?, ?> node, String topologyId,
            String nodeId) {
        String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingTermPoints = ImmutableNodes
                .mapNodeBuilder(SupportingTerminationPoint.QNAME);
        Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> terminationPointIdOpt = null;
        if (model.equals(NetworkTopologyModel.class)) {
            terminationPointIdOpt = tpMapEntryNode
                    .getChild(InstanceIdentifiers.NT_TP_ID_IDENTIFIER.getLastPathArgument());
        } else if (model.equals(I2rsModel.class)) {
            terminationPointIdOpt = tpMapEntryNode
                    .getChild(InstanceIdentifiers.I2RS_TP_ID_IDENTIFIER.getLastPathArgument());
        }
        if (terminationPointIdOpt.isPresent()) {
            Map<QName, Object> keys = new HashMap<>();
            keys.put(TopologyQNames.I2RS_TP_REF, terminationPointIdOpt.get().getValue());
            keys.put(TopologyQNames.I2RS_TP_NETWORK_REF, topologyId);
            keys.put(TopologyQNames.I2RS_TP_NODE_REF, nodeId);
            supportingTermPoints.addChild(ImmutableNodes.mapEntryBuilder()
                    .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingTerminationPoint.QNAME, keys))
                    .build());
        }
        return ImmutableNodes.mapEntryBuilder(TopologyQNames.I2RS_TERMINATION_POINT_QNAME,
                TopologyQNames.I2RS_TP_ID_QNAME, tpId).withChild(supportingTermPoints.build()).build();
    }
}
