/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import com.google.common.base.Optional;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.LogicalNodeWrapper;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author matus.marko
 */
public class LogicalNodeToNodeTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(LogicalNodeToNodeTranslator.class);

    /**
     * Convert LogicalNode to Node
     * @param logicalNode LogicalNode
     * @param logicalIdentifier Yang Identifier of this node
     * @return Node
     */
    public NormalizedNode<?, ?> convert(LogicalNodeWrapper wrapper) {

        LOG.debug("Transforming LogicalNodeWrapper to Node");
        List<PhysicalNode> writtenNodes = new ArrayList<>();
        CollectionNodeBuilder<MapEntryNode, MapNode> supportingNodes = ImmutableNodes.mapNodeBuilder(
                SupportingNode.QNAME);
        CollectionNodeBuilder<MapEntryNode, MapNode> terminationPoints = ImmutableNodes.mapNodeBuilder(
                TerminationPoint.QNAME);
        // iterate through logical nodes
        for (LogicalNode logicalNode : wrapper.getLogicalNodes()) {
            // iterate through physical nodes
            for (PhysicalNode physicalNode : logicalNode.getPhysicalNodes()) {
                if (! writtenNodes.contains(physicalNode)) {
                    writtenNodes.add(physicalNode);
                    NormalizedNode<?, ?> physicalWholeNode = physicalNode.getNode();
                    // prepare supporting nodes
                    supportingNodes
                            .withChild(ImmutableNodes.mapEntry(
                                    SupportingNode.QNAME, TopologyQNames.topologyRef, new TopologyId(physicalNode.getTopologyRef())))
                            .addChild(ImmutableNodes.mapEntry(SupportingNode.QNAME, TopologyQNames.nodeRef,
                                    physicalWholeNode.getIdentifier().toRelativeString(
                                            new YangInstanceIdentifier.NodeIdentifier(Node.QNAME))));
                    // prepare termination points
                    Optional<NormalizedNode<?, ?>> terminationPointMapNode = NormalizedNodes.findNode(
                            physicalWholeNode, YangInstanceIdentifier.of(TerminationPoint.QNAME));
                    if (terminationPointMapNode.isPresent()) {
                        Collection<MapEntryNode> terminationPointMapEntries = ((MapNode) terminationPointMapNode).getValue();
                        for (MapEntryNode terminationPointMapEntry : terminationPointMapEntries) {
                            terminationPoints.withChild(ImmutableNodes
                                    .mapEntry(TerminationPoint.QNAME, TopologyQNames.tpId,
                                            terminationPointMapEntry.getAttributeValue(TopologyQNames.tpId)))
                                    .addChild(ImmutableNodes.mapEntry(TerminationPoint.QNAME,
                                            TopologyQNames.tpRef, terminationPointMapEntry.getAttributeValue(TopologyQNames.tpRef)));
                        }
                    }
                }
            }

        }


        MapEntryNode normalizedNode = ImmutableNodes
                .mapEntryBuilder(Node.QNAME, TopologyQNames.networkNodeIdQName, wrapper.getNodeId())
                .withChild(supportingNodes.build())
                .withChild(terminationPoints.build())
                .build();

        return normalizedNode;
    }
}
