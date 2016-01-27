/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.nt.translator;

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
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matej.perina
 *
 */
public class NTNodeTranslator implements NodeTranslator{

    private static final Logger LOG = LoggerFactory.getLogger(NTNodeTranslator.class);
    private static final YangInstanceIdentifier TP_IDENTIFIER = YangInstanceIdentifier.of(TerminationPoint.QNAME);

    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        IdentifierGenerator idGenerator = new IdentifierGenerator();
        LOG.debug("Transforming OverlayItemWrapper containing Nodes to datastore format");
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
                    keyValues.put(TopologyQNames.TOPOLOGY_REF, underlayItem.getTopologyId());
                    keyValues.put(TopologyQNames.NODE_REF, underlayItem.getItemId());
                    supportingNodes.withChild(ImmutableNodes.mapEntryBuilder().withNodeIdentifier(
                            new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                                    SupportingNode.QNAME, keyValues)).build());
                    // prepare termination points
                    Optional<NormalizedNode<?, ?>> terminationPointMapNode = NormalizedNodes.findNode(
                            itemNode, TP_IDENTIFIER);
                    if (terminationPointMapNode.isPresent()) {
                        if (overlayItem.getCorrelationItem() == CorrelationItemEnum.TerminationPoint) {
                            Collection<MapEntryNode> terminationPointMapEntries =
                                    ((MapNode) terminationPointMapNode.get()).getValue();
                            for (MapEntryNode terminationPointMapEntry : terminationPointMapEntries) {
                                terminationPoints.addChild(terminationPointMapEntry);
                            }
                        } else {
                            List<MapEntryNode> terminationPointEntries = createTerminationPoint(
                                    (MapNode) terminationPointMapNode.get(), underlayItem.getTopologyId(),
                                    underlayItem.getItemId(), idGenerator);
                            for (MapEntryNode terminationPointMapEntry : terminationPointEntries) {
                                terminationPoints.addChild(terminationPointMapEntry);
                            }
                        }
                    }
                }
            }
        }

        return ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, wrapper.getId())
                .withChild(supportingNodes.build())
                .withChild(terminationPoints.build())
                .build();
    }

    private List<MapEntryNode> createTerminationPoint(MapNode terminationPoints, String topologyId, String nodeId,
            IdentifierGenerator idGenerator) {
        List<MapEntryNode> terminationPointEntries = new ArrayList<>();
        for (MapEntryNode mapEntryNode : terminationPoints.getValue()) {
            Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> terminationPointIdOpt
                    = mapEntryNode.getChild(new YangInstanceIdentifier.NodeIdentifier(
                    TopologyQNames.NETWORK_TP_ID_QNAME));
            if (terminationPointIdOpt.isPresent()) {
                String value = "/network-topology:network-topology/topology/" + topologyId +
                        "/node/" + nodeId +
                        "/termination-point/" + (String) terminationPointIdOpt.get().getValue();
                LeafSetEntryNode<String> tpRef = ImmutableLeafSetEntryNodeBuilder.<String>create()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<String>(
                                TopologyQNames.TP_REF, value)).withValue(value).build();
                List<LeafSetEntryNode<String>> tpRefs = new ArrayList<>();
                tpRefs.add(tpRef);
                ListNodeBuilder<String, LeafSetEntryNode<String>> leafListBuilder =
                        ImmutableLeafSetNodeBuilder.<String>create().withNodeIdentifier(
                                new YangInstanceIdentifier.NodeIdentifier(TopologyQNames.TP_REF));
                leafListBuilder.withValue(tpRefs);
                String tpId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
                terminationPointEntries.add(ImmutableNodes.mapEntryBuilder(TerminationPoint.QNAME,
                        TopologyQNames.NETWORK_TP_ID_QNAME, tpId).withChild(leafListBuilder.build()).build());
            }
        }
        return terminationPointEntries;
    }
}
