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
import java.util.Collection;
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
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I2RSNodeTranslator implements NodeTranslator{

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
                    NormalizedNode<?, ?> itemNode = underlayItem.getItem();
                    // prepare supporting nodes
                    Map<QName, Object> keyValues = new HashMap<>();
                    keyValues.put(TopologyQNames.I2RS_NETWORK_REF, underlayItem.getTopologyId());
                    keyValues.put(TopologyQNames.I2RS_NODE_REF, underlayItem.getItemId());
                    supportingNodes.withChild(ImmutableNodes.mapEntryBuilder().withNodeIdentifier(
                            new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                                    SupportingNode.QNAME, keyValues)).build());
                    if (wrapper.getAggregatedTerminationPoints() == null) {
                        // prepare termination points
                        Class<? extends Model> model = I2rsModel.class;
                        Optional<NormalizedNode<?, ?>> terminationPointMapNode = NormalizedNodes.findNode(
                                itemNode, InstanceIdentifiers.I2RS_TP_IDENTIFIER);
                        if (!terminationPointMapNode.isPresent()) {
                            model = NetworkTopologyModel.class;
                            terminationPointMapNode = NormalizedNodes.findNode(itemNode,
                                    InstanceIdentifiers.NT_TP_IDENTIFIER);
                        }
                        if (terminationPointMapNode.isPresent()) {
                            /*
                            if (overlayItem.getCorrelationItem() == CorrelationItemEnum.TerminationPoint
                                    && model.equals(I2rsModel.class)) {
                                Collection<MapEntryNode> terminationPointMapEntries =
                                        ((MapNode) terminationPointMapNode.get()).getValue();
                                for (MapEntryNode terminationPointMapEntry : terminationPointMapEntries) {
                                    terminationPoints.addChild(terminationPointMapEntry);
                                }
                            } else {
                            */
                                List<MapEntryNode> terminationPointEntries = createTerminationPoint(
                                        (MapNode) terminationPointMapNode.get(), underlayItem.getTopologyId(),
                                        underlayItem.getItemId(), idGenerator, model);
                                for (MapEntryNode terminationPointMapEntry : terminationPointEntries) {
                                    terminationPoints.addChild(terminationPointMapEntry);
                                }
                            //}
                    }
                    }
                }
            }
        }


        if (wrapper.getAggregatedTerminationPoints() == null) {
            return ImmutableNodes
                    .mapEntryBuilder(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, wrapper.getId())
                    .withChild(supportingNodes.build())
                    .withChild(terminationPoints.build())
                    .build();
        } else {
            return ImmutableNodes
                    .mapEntryBuilder(Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, wrapper.getId())
                    .withChild(supportingNodes.build())
                    .withChild(wrapper.getAggregatedTerminationPoints())
                    .build();
        }
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
                Map<QName, Object> keys = new HashMap<>();
                keys.put(TopologyQNames.I2RS_TP_REF, tpRefId);
                keys.put(TopologyQNames.I2RS_TP_NETWORK_REF, topologyId);
                keys.put(TopologyQNames.I2RS_TP_NODE_REF, nodeId);
                supportingTermPoints.withChild(ImmutableNodes.mapEntryBuilder()
                        .withNodeIdentifier(new NodeIdentifierWithPredicates(SupportingTerminationPoint.QNAME, keys))
                        .build());
                String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
                terminationPointEntries.add(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                        TopologyQNames.I2RS_TP_ID_QNAME, tpId).withChild(supportingTermPoints.build()).build());
            }
        }
        return terminationPointEntries;
    }
}
