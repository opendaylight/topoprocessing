/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.linkComputation.api.ComputedLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author martin.uhlir
 *
 */
public class LinkCalculator implements TopologyOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCalculator.class);
    YangInstanceIdentifier sourceNodeRelativeIdentifier = YangInstanceIdentifier.of(QName.create(Link.QNAME, "source-node"));
    YangInstanceIdentifier destNodeRelativeIdentifier = YangInstanceIdentifier.of(QName.create(Link.QNAME, "dest-node"));

    protected TopologyManager manager;

    private TopologyStore storedOverlayNodes = null;
    private Map<YangInstanceIdentifier, ComputedLink> matchedLinks = new HashMap<>();
    private Map<YangInstanceIdentifier, UnderlayItem> waitingLinks = new HashMap<>();

    /**
     * Constructor
     */
    public LinkCalculator() {
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem createdItem, String topologyId) {
        LOGGER.trace("Processing createdChanges in " + getClass().toString());
        if (CorrelationItemEnum.Node.equals(createdItem.getCorrelationItem())) {
            // process nodes from overlay topology
            if (storedOverlayNodes == null) {
                storedOverlayNodes = new TopologyStore(topologyId, false,
                        new HashMap<YangInstanceIdentifier, UnderlayItem>());
            }
            storedOverlayNodes.getUnderlayItems().put(itemIdentifier, createdItem);
            for (Entry<YangInstanceIdentifier, UnderlayItem> waitingLink : waitingLinks.entrySet()) {
                calculatePossibleLink(waitingLink.getKey(),waitingLink.getValue());
            }
        } else if (CorrelationItemEnum.Link.equals(createdItem.getCorrelationItem())) {
            // process links from underlay topology
            calculatePossibleLink(itemIdentifier,createdItem);
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem updatedItem, String topologyId) {
        LOGGER.trace("Processing updatedChanges in " + getClass().toString());
        if (matchedLinks.containsKey(itemIdentifier)) {
            // updated item was link
            ComputedLink computedLink = matchedLinks.get(itemIdentifier);
            NormalizedNode<?, ?> sourceNode = computedLink.getSrcNode();
            NormalizedNode<?, ?> destNode = computedLink.getDstNode();
            NormalizedNode<?, ?> newLinkSourceNode = getLinkSourceNode(updatedItem);
            NormalizedNode<?, ?> newLinkDestNode = getLinkDestNode(updatedItem);
            if (!sourceNode.equals(newLinkSourceNode)) {
                computedLink.setSrcNode(null);
            }
            if (!destNode.equals(newLinkDestNode)) {
                computedLink.setDstNode(null);
            }
            ComputedLink newLink = updateComputedLink(computedLink);
            OverlayItem overlayItem = computedLink.getOverlayItem();
            if(newLink == null) {
                waitingLinks.put(itemIdentifier, computedLink);
                removeMatchedLink(itemIdentifier);
            } else {
                manager.updateOverlayItem(overlayItem);
            }
        } else if (waitingLinks.containsKey(itemIdentifier)) {
            calculatePossibleLink(itemIdentifier, updatedItem);
        } else if (storedOverlayNodes.getUnderlayItems().containsKey(itemIdentifier)) {
            storedOverlayNodes.getUnderlayItems().put(itemIdentifier, updatedItem);
            for (Entry<YangInstanceIdentifier, UnderlayItem> waitingLink : waitingLinks.entrySet()) {
                calculatePossibleLink(waitingLink.getKey(),waitingLink.getValue());
            }
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, final String topologyId) {
        LOGGER.trace("Processing removedChanges in " + getClass().toString());
        UnderlayItem removedOverlayNode = storedOverlayNodes.getUnderlayItems().remove(itemIdentifier);
        if (removedOverlayNode != null) {
            // removed item was an overlay node
            for (Entry<YangInstanceIdentifier, ComputedLink> matchedLink : matchedLinks.entrySet()) {
                if (removedOverlayNode.getItem().equals(matchedLink.getValue().getLeafNode())
                        || removedOverlayNode.getItem().equals(matchedLink.getValue().getDstNode())) {
                    // remove calculated link
                    removeMatchedLink(matchedLink.getKey());
                }
            }
        } else if (matchedLinks.containsKey(itemIdentifier)) {
            // removed item was link
            removeMatchedLink(itemIdentifier);
        }
    }

    private void removeMatchedLink(YangInstanceIdentifier itemIdentifier) {
        ComputedLink overlayLink = matchedLinks.remove(itemIdentifier);
        if (null != overlayLink) {
            manager.removeOverlayItem(overlayLink.getOverlayItem());
        }
    }

    private void calculatePossibleLink(YangInstanceIdentifier linkId, UnderlayItem link) {
        NormalizedNode<?, ?> sourceNode = getLinkSourceNode(link);
        NormalizedNode<?, ?> destNode = getLinkDestNode(link);

        if (sourceNode != null && destNode != null) {
            ComputedLink computedLink = new ComputedLink(link.getItem(), null, null,
                    storedOverlayNodes.getId(), link.getItemId(), CorrelationItemEnum.Link);
            boolean srcFound = false;
            boolean dstFound = false;
            Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> overlayNodesIterator =
                    storedOverlayNodes.getUnderlayItems().entrySet().iterator();
            //iterate over all overlay nodes
            while (overlayNodesIterator.hasNext()) {
                Entry<YangInstanceIdentifier, UnderlayItem> overlayNodeEntry = overlayNodesIterator.next();
                NormalizedNode<?, ?> overlayNode = overlayNodeEntry.getValue().getItem();
                Collection<?> supportingNodes = (Collection<?>) ((MapEntryNode) overlayNode)
                        .getChild(new NodeIdentifier(SupportingNode.QNAME)).get().getValue();
                Iterator<?> supportingNodesIterator = supportingNodes.iterator();
                while (supportingNodesIterator.hasNext()) {
                    NormalizedNode<?, ?> supportingNode = (NormalizedNode<?, ?>) supportingNodesIterator.next();
                    YangInstanceIdentifier yiidNodeRef = YangInstanceIdentifier.builder()
                            .node(TopologyQNames.NODE_REF).build();
                    Optional<NormalizedNode<?, ?>> supportingNodeNodeRefOptional =
                            NormalizedNodes.findNode(supportingNode, yiidNodeRef);
                    if (supportingNodeNodeRefOptional.isPresent()) {
                        NormalizedNode<?, ?> supportingNodeNodeRef = supportingNodeNodeRefOptional.get();
                        if (supportingNodeNodeRef.equals(sourceNode) && srcFound == false) {
                            computedLink.setLeafNode(supportingNodeNodeRef);
                            srcFound = true;
                        }
                        if (supportingNodeNodeRef.equals(destNode) && dstFound == false) {
                            computedLink.setDstNode(supportingNodeNodeRef);
                            dstFound = true;
                        }
                        if (srcFound && dstFound) {
                            break;
                        }
                    }
                }
            }
            if (srcFound && dstFound) {
                // if the waitingList map contains the link it will be removed
                waitingLinks.remove(linkId);
                // link is put into matchedLinks map
                matchedLinks.put(linkId, computedLink);
                OverlayItem overlayItem = wrapUnderlayItem(computedLink);
                manager.addOverlayItem(overlayItem);
            } else {
                waitingLinks.put(linkId, link);
            }
        }
    }

    private ComputedLink updateComputedLink(ComputedLink link) {
        NormalizedNode<?, ?> linkDstNode = link.getDstNode();
        NormalizedNode<?, ?> linkSrcNode = link.getSrcNode();

        if (linkSrcNode == null || linkDstNode == null) {
            NormalizedNode<?, ?> sourceNode = getLinkSourceNode(link);
            NormalizedNode<?, ?> destNode = getLinkDestNode(link);
            Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> overlayNodesIterator =
                    storedOverlayNodes.getUnderlayItems().entrySet().iterator();
            //iterate over all overlay nodes
            while (overlayNodesIterator.hasNext()) {
                Entry<YangInstanceIdentifier, UnderlayItem> overlayNodeEntry = overlayNodesIterator.next();
                NormalizedNode<?, ?> overlayNode = overlayNodeEntry.getValue().getItem();
                Collection<?> supportingNodes = (Collection<?>) ((MapEntryNode) overlayNode)
                        .getChild(new NodeIdentifier(SupportingNode.QNAME)).get().getValue();
                Iterator<?> supportingNodesIterator = supportingNodes.iterator();
                while (supportingNodesIterator.hasNext()) {
                    NormalizedNode<?, ?> supportingNode = (NormalizedNode<?, ?>) supportingNodesIterator.next();
                    YangInstanceIdentifier yiidNodeRef = YangInstanceIdentifier.builder()
                            .node(TopologyQNames.NODE_REF).build();
                    Optional<NormalizedNode<?, ?>> supportingNodeNodeRefOptional =
                            NormalizedNodes.findNode(supportingNode, yiidNodeRef);
                    if (supportingNodeNodeRefOptional.isPresent()) {
                        NormalizedNode<?, ?> supportingNodeNodeRef = supportingNodeNodeRefOptional.get();
                        if (linkSrcNode == null && supportingNodeNodeRef.equals(sourceNode)) {
                            link.setSrcNode(supportingNodeNodeRef);
                        }
                        if (linkDstNode == null && supportingNodeNodeRef.equals(destNode)) {
                            link.setDstNode(supportingNodeNodeRef);
                        }
                        if (linkSrcNode != null && linkDstNode != null) {
                            break;
                        }
                    }
                }
            }
            if (linkSrcNode == null || linkDstNode == null) {
                return null;
            }
        }
        return link;
    }

    private NormalizedNode<?, ?> getLinkSourceNode(UnderlayItem link) {
        NormalizedNode<?, ?> sourceNode = null;
        Optional<NormalizedNode<?,?>> sourceNodePresent =
                NormalizedNodes.findNode(link.getItem(), sourceNodeRelativeIdentifier);
        if (sourceNodePresent.isPresent()) {
            sourceNode = (NormalizedNode<?, ?>) sourceNodePresent.get().getValue();
        }
        return sourceNode;
    }

    private NormalizedNode<?, ?> getLinkDestNode(UnderlayItem createdItemValue) {
        NormalizedNode<?, ?> destNode = null;
        Optional<NormalizedNode<?,?>> destNodePresent =
                NormalizedNodes.findNode(createdItemValue.getItem(), destNodeRelativeIdentifier);
        if (destNodePresent.isPresent()) {
            destNode = (NormalizedNode<?, ?>) destNodePresent.get().getValue();
        }
        return destNode;
    }

    @Override
    public void setTopologyManager(TopologyManager topologyManager) {
        this.manager = topologyManager;
    }

    private OverlayItem wrapUnderlayItem(UnderlayItem underlayItem) {
        List<UnderlayItem> underlayItems = Collections.singletonList(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems, underlayItem.getCorrelationItem());
        underlayItem.setOverlayItem(overlayItem);
        return overlayItem;
    }
}
