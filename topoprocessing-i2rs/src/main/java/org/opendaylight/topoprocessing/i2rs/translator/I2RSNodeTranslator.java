/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.translator;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I2RSNodeTranslator implements NodeTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(I2RSNodeTranslator.class);

    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        LOG.debug("Transforming OverlayItemWrapper containing Nodes to datastore format");
        IdentifierGenerator idGenerator = new IdentifierGenerator();
        List<UnderlayItem> writtenNodes = new ArrayList<>();
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingNodes = ImmutableNodes.mapNodeBuilder(
                SupportingNode.QNAME);
        CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        // iterate through overlay items containing nodes
        for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
            // iterate through overlay item
            for (UnderlayItem underlayItem : overlayItem.getUnderlayItems()) {
                if (! writtenNodes.contains(underlayItem)) {
                    writtenNodes.add(underlayItem);
                    addSupportingNodes(underlayItem, supportingNodes);
                    if (wrapper.getAggregatedTerminationPoints() == null) {
                        prepareTerminationPoints(underlayItem, idGenerator, terminationPoints);
                    }
                }
            }
        }
        if (wrapper.getAggregatedTerminationPoints() != null
                && !wrapper.getAggregatedTerminationPoints().getValue().isEmpty()) {
            MapNode aggregatedTPs = wrapper.getAggregatedTerminationPoints();
            Optional<NormalizedNode<?, ?>> tpId = NormalizedNodes.findNode(
                    aggregatedTPs.getValue().iterator().next(), InstanceIdentifiers.NT_TP_ID_IDENTIFIER);
            if (tpId.isPresent()) {
                aggregatedTPs = translateAggregatedTPsWithinNodesFromNT(idGenerator, aggregatedTPs);
            }
            return ImmutableNodes
                    .mapEntryBuilder(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, wrapper.getId())
                    .withChild(supportingNodes.build())
                    .withChild(aggregatedTPs)
                    .build();
        } else {
            return ImmutableNodes
                    .mapEntryBuilder(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, wrapper.getId())
                    .withChild(supportingNodes.build())
                    .withChild(terminationPoints.build())
                    .build();
        }
    }

    private void prepareTerminationPoints(UnderlayItem underlayItem, IdentifierGenerator idGenerator,
            CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints) {
        NormalizedNode<?, ?> itemNode = underlayItem.getItem();
        Class<? extends Model> model = I2rsModel.class;
        Optional<NormalizedNode<?, ?>> terminationPointMapNode = NormalizedNodes.findNode(
                itemNode, InstanceIdentifiers.I2RS_TP_IDENTIFIER);
        if (!terminationPointMapNode.isPresent()) {
            model = NetworkTopologyModel.class;
            terminationPointMapNode = NormalizedNodes.findNode(itemNode,
                    InstanceIdentifiers.NT_TP_IDENTIFIER);
        }
        if (terminationPointMapNode.isPresent()) {
            List<MapEntryNode> terminationPointEntries = createTerminationPoint(
                    (MapNode) terminationPointMapNode.get(), underlayItem.getTopologyId(),
                    underlayItem.getItemId(), idGenerator, model);
            for (MapEntryNode terminationPointMapEntry : terminationPointEntries) {
                terminationPoints.addChild(terminationPointMapEntry);
            }
        }
    }

    private void addSupportingNodes(UnderlayItem underlayItem,
            CollectionNodeBuilder<MapEntryNode, MapNode> supportingNodes) {
        Map<QName, Object> keyValues = new HashMap<>();
        keyValues.put(TopologyQNames.I2RS_NETWORK_REF, underlayItem.getTopologyId());
        keyValues.put(TopologyQNames.I2RS_NODE_REF, underlayItem.getItemId());
        supportingNodes.withChild(ImmutableNodes.mapEntryBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                        SupportingNode.QNAME, keyValues)).build());
    }

    private MapNode translateAggregatedTPsWithinNodesFromNT(IdentifierGenerator idGenerator, MapNode aggregatedTPs) {
        CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        for (MapEntryNode mapEntryNode : aggregatedTPs.getValue()) {
            Optional<NormalizedNode<?, ?>> tpRefsOpt = NormalizedNodes.findNode(mapEntryNode,
                    InstanceIdentifiers.NT_TP_REF_IDENTIFIER);
            if (tpRefsOpt.isPresent()) {
                CollectionNodeBuilder<MapEntryNode, MapNode> suppTPs = ImmutableNodes.mapNodeBuilder(
                        SupportingTerminationPoint.QNAME);
                LeafSetNode<String> tpRefs = (LeafSetNode<String>) tpRefsOpt.get();
                for (LeafSetEntryNode<String> tpRef : tpRefs.getValue()) {
                    /*
                     * tp-ref format is:
                     * 0                 1                    2          3        4     5            6           7
                     *  /network-topology:network-topology/topology/TOPOLOGY-ID/node/NODE-ID/termination-point/TP-ID
                     */
                    String[] split = tpRef.getValue().split("/");
                    suppTPs.addChild(createSupportingTerminationPoint(idGenerator, split[7], split[3], split[5]));
                }

                String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
                terminationPoints.addChild(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                        TopologyQNames.I2RS_TP_ID_QNAME, tpId).withChild(suppTPs.build()).build());
            } else {
                LOG.debug("Termination point does not contain any tp-ref");
            }
        }
        return terminationPoints.build();
    }

    private List<MapEntryNode> createTerminationPoint(MapNode terminationPoints, String topologyId, String nodeId,
            IdentifierGenerator idGenerator, Class<? extends Model> model) {
        List<MapEntryNode> terminationPointEntries = new ArrayList<>();
        for (MapEntryNode mapEntryNode : terminationPoints.getValue()) {
            Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> terminationPointIdOpt;
            if (model.equals(I2rsModel.class)) {
                terminationPointIdOpt = mapEntryNode.getChild(
                        InstanceIdentifiers.I2RS_TP_ID_IDENTIFIER.getLastPathArgument());
            } else {
                terminationPointIdOpt = mapEntryNode.getChild(
                        InstanceIdentifiers.NT_TP_ID_IDENTIFIER.getLastPathArgument());
            }
            if (terminationPointIdOpt.isPresent()) {
                String tpRefId = (String)terminationPointIdOpt.get().getValue();
                CollectionNodeBuilder<MapEntryNode, MapNode> supportingTermPoints = ImmutableNodes.mapNodeBuilder(
                        SupportingTerminationPoint.QNAME);
                supportingTermPoints.withChild(createSupportingTerminationPoint(idGenerator, tpRefId, topologyId,
                        nodeId));
                String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
                terminationPointEntries.add(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                        TopologyQNames.I2RS_TP_ID_QNAME, tpId).withChild(supportingTermPoints.build()).build());
            }
        }
        return terminationPointEntries;
    }

    private MapEntryNode createSupportingTerminationPoint(IdentifierGenerator idGenerator, String tpRefId,
            String topologyId, String nodeId) {
        Map<QName, Object> keys = new HashMap<>();
        keys.put(TopologyQNames.I2RS_TP_REF, tpRefId);
        keys.put(TopologyQNames.I2RS_TP_NETWORK_REF, topologyId);
        keys.put(TopologyQNames.I2RS_TP_NODE_REF, nodeId);
        return ImmutableNodes.mapEntryBuilder().withNodeIdentifier(
                new NodeIdentifierWithPredicates(SupportingTerminationPoint.QNAME, keys)).build();
    }
}
