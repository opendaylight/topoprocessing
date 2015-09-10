/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.linkComputation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author martin.uhlir
 *
 */
public class LinkCalculator implements TopologyOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCalculator.class);
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    YangInstanceIdentifier sourceNodeRelativeIdentifier = YangInstanceIdentifier.of(QName.create(Link.QNAME, "source-node"));
    YangInstanceIdentifier destNodeRelativeIdentifier = YangInstanceIdentifier.of(QName.create(Link.QNAME, "dest-node"));

    protected TopologyManager manager;

    private TopologyStore storedOverlayNodes = null;
//    private TopoStoreProvider waitingList;
//    private TopoStoreProvider matchedLinks;
//    private List<ComputedLink> matchedLinks;
    private Map<YangInstanceIdentifier, UnderlayItem> matchedLinks;
    private Map<YangInstanceIdentifier, UnderlayItem> waitingLinks;

    /**
     * Constructor
     */
    public LinkCalculator() {
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries, String topologyId) {
        LOGGER.trace("Processing createdChanges in " + getClass().toString());
        if (createdEntries != null) {
            for (Entry<YangInstanceIdentifier, UnderlayItem> createdItem : createdEntries.entrySet()) {
                if (CorrelationItemEnum.Node.equals(createdItem.getValue().getCorrelationItem())) {
                    // process nodes from overlay topology
                    if (storedOverlayNodes == null) {
                        storedOverlayNodes = new TopologyStore(topologyId, false,
                                new HashMap<YangInstanceIdentifier, UnderlayItem>());
                    }
                    storedOverlayNodes.getUnderlayItems().put(createdItem.getKey(),
                            createdItem.getValue());
                } else if (CorrelationItemEnum.Link.equals(createdItem.getValue().getCorrelationItem())) {
                    // process links from underlay topology
                    Optional<NormalizedNode<?,?>> sourceNodePresent =
                            NormalizedNodes.findNode(createdItem.getValue().getItem(), sourceNodeRelativeIdentifier);
                    NormalizedNode<?, ?> sourceNodeId = null;
                    if (sourceNodePresent.isPresent()) {
                        sourceNodeId = (NormalizedNode<?, ?>) sourceNodePresent.get().getValue();
                    }
                    Optional<NormalizedNode<?,?>> destNodePresent =
                            NormalizedNodes.findNode(createdItem.getValue().getItem(), destNodeRelativeIdentifier);
                    NormalizedNode<?, ?>  destNodeId = null;
                    if (destNodePresent.isPresent()) {
                        destNodeId = (NormalizedNode<?, ?>) sourceNodePresent.get().getValue();
                    }

                    if (sourceNodeId != null && destNodeId != null) {
                        String linkId = idGenerator.getNextIdentifier(CorrelationItemEnum.Link);
                        MapEntryNode linkValue = ImmutableNodes.mapEntry(Link.QNAME, TopologyQNames.NETWORK_LINK_ID_QNAME, linkId);
                        ComputedLink computedLink = new ComputedLink(linkValue, null, null,
                                storedOverlayNodes.getId(), linkId, CorrelationItemEnum.Link);
                        boolean srcFound = false;
                        boolean dstFound = false;
                        Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> overlayNodesIterator =
                                storedOverlayNodes.getUnderlayItems().entrySet().iterator();
                        //iterate over all overlay nodes
                        while (overlayNodesIterator.hasNext()) {
                            Entry<YangInstanceIdentifier, UnderlayItem> overlayNodeEntry = overlayNodesIterator.next();
                            NormalizedNode<?, ?> overlayNode = overlayNodeEntry.getValue().getItem();
                            Collection supportingNodes = (Collection) ((MapEntryNode) overlayNode)
                                    .getChild(new NodeIdentifier(SupportingNode.QNAME)).get().getValue();
                            Iterator supportingNodesIterator = supportingNodes.iterator();
                            while (supportingNodesIterator.hasNext()) {
                                NormalizedNode<?, ?> supportingNode = (NormalizedNode<?, ?>) supportingNodesIterator.next();
                                YangInstanceIdentifier yiidNodeRef = YangInstanceIdentifier.builder()
                                        .node(TopologyQNames.NODE_REF).build();
                                Optional<NormalizedNode<?, ?>> supportingNodeNodeRefOptional =
                                        NormalizedNodes.findNode(supportingNode, yiidNodeRef);
                                if (supportingNodeNodeRefOptional.isPresent()) {
                                    NormalizedNode<?, ?> supportingNodeNodeRef = supportingNodeNodeRefOptional.get();
                                    if (supportingNodeNodeRef.equals(sourceNodeId) && srcFound == false) {
                                        computedLink.setLeafNode(supportingNodeNodeRef);
                                        srcFound = true;
                                    }
                                    if (supportingNodeNodeRef.equals(destNodeId) && dstFound == false) {
                                        computedLink.setLeafNode2(supportingNodeNodeRef);
                                        dstFound = true;
                                    }
                                    if (srcFound && dstFound) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (srcFound && dstFound) {
                            matchedLinks.put(key, value);
                            manager.addOverlayItem(newOverlayItem);
                        } else {
                            waitingLinks.put(key, value);
                        }
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.opendaylight.topoprocessing.api.operator.TopologyOperator#processUpdatedChanges(java.util.Map, java.lang.String)
     */
    @Override
    public void processUpdatedChanges(
            Map<YangInstanceIdentifier, UnderlayItem> updatedEntries,
            String topologyId) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.opendaylight.topoprocessing.api.operator.TopologyOperator#processRemovedChanges(java.util.List, java.lang.String)
     */
    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers,
            String topologyId) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.opendaylight.topoprocessing.impl.operator.TopologyOperator#setTopologyManager(org.opendaylight.topoprocessing.impl.operator.TopologyManager)
     */
    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        // TODO Auto-generated method stub
        
    }

}
