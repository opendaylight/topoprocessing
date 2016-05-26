/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.topoprocessing.api.structure.ComputedLink;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
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

/**
 * @author martin.uhlir
 *
 */
public class LinkCalculator implements TopologyOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCalculator.class);
    private static final YangInstanceIdentifier SOURCE_NODE_IDENTIFIER = YangInstanceIdentifier
            .of(QName.create(Link.QNAME, "source")).node(QName.create(Source.QNAME, "source-node"));
    private static final YangInstanceIdentifier DEST_NODE_IDENTIFIER = YangInstanceIdentifier
            .of(QName.create(Link.QNAME, "destination")).node(QName.create(Destination.QNAME, "dest-node"));

    protected ITopologyManager manager;
    private TopologyAggregator aggregator;
    private TopologyStore storedOverlayNodes;
    private Class<? extends Model> outputModel;
    private Map<YangInstanceIdentifier, ComputedLink> matchedLinks = new HashMap<>();
    private Map<YangInstanceIdentifier, UnderlayItem> waitingLinks = new HashMap<>();

    /**
     * Constructor
     * @param topologyId overlayTopologyId
     */
    public LinkCalculator(String topologyId, Class<? extends Model> outputModel) {
        storedOverlayNodes = new TopologyStore(topologyId, false,
                new ConcurrentHashMap<YangInstanceIdentifier, UnderlayItem>());
        this.outputModel = outputModel;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem item, String topologyId) {
        LOGGER.trace("Processing created item: {}", item);
        synchronized(this) {
            if (CorrelationItemEnum.Node.equals(item.getCorrelationItem())) {
                // process nodes from overlay topology
                storedOverlayNodes.getUnderlayItems().put(itemIdentifier, item);
                Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> waitingLinksIterator =
                        waitingLinks.entrySet().iterator();
                while (waitingLinksIterator.hasNext()) {
                    Entry<YangInstanceIdentifier, UnderlayItem> waitingLink = waitingLinksIterator.next();
                    if (calculatePossibleLink(waitingLink.getKey(),waitingLink.getValue(), false)) {
                        waitingLinksIterator.remove();
                    }
                }
            } else if (CorrelationItemEnum.Link.equals(item.getCorrelationItem())) {
                // process links from underlay topology
                calculatePossibleLink(itemIdentifier,item, false);
            }
        }
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem item, String topologyId) {
        LOGGER.trace("Processing updatedChanges");
        synchronized(this) {
            if (matchedLinks.containsKey(itemIdentifier)) {
                // updated item was a matched link
                ComputedLink computedLink = matchedLinks.get(itemIdentifier);
                computedLink.setItem(item.getItem());
                NormalizedNode<?, ?> updatedLinkSrc = getLinkSourceNode(item);
                NormalizedNode<?, ?> updatedLinkDst = getLinkDestNode(item);
                computedLink.setSrcNode(null);
                computedLink.setDstNode(null);
                ComputedLink newLink = updateComputedLink(computedLink, updatedLinkSrc, updatedLinkDst);
                OverlayItem overlayItem = computedLink.getOverlayItem();
                if(newLink == null) {
                    waitingLinks.put(itemIdentifier, computedLink);
                    removeMatchedLink(itemIdentifier);
                } else {
                    manager.updateOverlayItem(overlayItem);
                }
            } else if (waitingLinks.containsKey(itemIdentifier)) {
                if(calculatePossibleLink(itemIdentifier, item, true)){
                    waitingLinks.remove(itemIdentifier);
                }
            } else if (storedOverlayNodes.getUnderlayItems().containsKey(itemIdentifier)) {
                storedOverlayNodes.getUnderlayItems().put(itemIdentifier, item);
                Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> waitingLinksIterator =
                        waitingLinks.entrySet().iterator();
                while (waitingLinksIterator.hasNext()) {
                    Entry<YangInstanceIdentifier, UnderlayItem> waitingLink = waitingLinksIterator.next();
                    if(calculatePossibleLink(waitingLink.getKey(),waitingLink.getValue(), true)){
                        waitingLinksIterator.remove();
                    }
                }
                Iterator<Entry<YangInstanceIdentifier, ComputedLink>> matchedLinksIterator =
                        matchedLinks.entrySet().iterator();
                List<YangInstanceIdentifier> matchedLinksToRemove = new LinkedList<>();
                while(matchedLinksIterator.hasNext()){
                    Entry<YangInstanceIdentifier, ComputedLink> matchedLink = matchedLinksIterator.next();
                    ComputedLink computedLink = matchedLink.getValue();
                    NormalizedNode<?, ?> oldSrc = getLinkSourceNode(computedLink);
                    NormalizedNode<?, ?> oldDst = getLinkDestNode(computedLink);
                    computedLink.setSrcNode(null);
                    computedLink.setDstNode(null);
                    if(updateComputedLink(computedLink, oldSrc, oldDst) == null){
                        waitingLinks.put(matchedLink.getKey(), computedLink);
                        matchedLinksToRemove.add(matchedLink.getKey());
                    }
                }
                for(YangInstanceIdentifier yiid: matchedLinksToRemove){
                    removeMatchedLink(yiid);
                }
            }
        }
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, final String topologyId) {
        LOGGER.trace("Processing removedChanges");
        synchronized(this) {
            UnderlayItem removedOverlayNode = storedOverlayNodes.getUnderlayItems().remove(itemIdentifier);
            if (removedOverlayNode != null) {
                // removed item was an overlay node
                Iterator<Entry<YangInstanceIdentifier, ComputedLink>> matchedLinksIterator =
                        matchedLinks.entrySet().iterator();
                while (matchedLinksIterator.hasNext()) {
                    Entry<YangInstanceIdentifier, ComputedLink> matchedLink = matchedLinksIterator.next();
                    if (removedOverlayNode.getItem().equals(matchedLink.getValue().getSrcNode())
                            || removedOverlayNode.getItem().equals(matchedLink.getValue().getDstNode())) {
                        // remove calculated link
                        waitingLinks.put(matchedLink.getKey(), matchedLink.getValue());
                        if (aggregator != null) {
                            aggregator.processRemovedChanges(matchedLink.getKey(),
                                    matchedLink.getValue().getTopologyId());
                        } else {
                            manager.removeOverlayItem(matchedLink.getValue().getOverlayItem());
                        }
                        matchedLinksIterator.remove();
                    }
                }
            } else if (matchedLinks.containsKey(itemIdentifier)) {
                // removed item was matched link
                removeMatchedLink(itemIdentifier);
            } else if (waitingLinks.containsKey(itemIdentifier)) {
                // removed item was waiting link
                waitingLinks.remove(itemIdentifier);
            }
        }
    }

    private void removeMatchedLink(YangInstanceIdentifier itemIdentifier) {
        ComputedLink overlayLink = matchedLinks.remove(itemIdentifier);
        if (null != overlayLink) {
            if (aggregator != null) {
                aggregator.processRemovedChanges(itemIdentifier, overlayLink.getTopologyId());
            } else {
                manager.removeOverlayItem(overlayLink.getOverlayItem());
            }
        }
    }

    private boolean calculatePossibleLink(YangInstanceIdentifier linkId, UnderlayItem link, boolean update) {
        NormalizedNode<?, ?> sourceNode = getLinkSourceNode(link);
        NormalizedNode<?, ?> destNode = getLinkDestNode(link);
        if (sourceNode != null && destNode != null) {
            ComputedLink computedLink = new ComputedLink(link.getItem(), link.getLeafNodes(), null, null,
                    link.getTopologyId(), link.getItemId(), CorrelationItemEnum.Link);
            boolean srcFound = false;
            boolean dstFound = false;
            Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> overlayNodesIterator =
                    storedOverlayNodes.getUnderlayItems().entrySet().iterator();
            //iterate over all overlay nodes
            while (overlayNodesIterator.hasNext()) {
                Entry<YangInstanceIdentifier, UnderlayItem> overlayNodeEntry = overlayNodesIterator.next();
                NormalizedNode<?, ?> overlayNode = overlayNodeEntry.getValue().getItem();
                Collection<?> supportingNodes = null;
                if (outputModel.equals(I2rsModel.class)) {
                supportingNodes = (Collection<?>) ((MapEntryNode) overlayNode)
                        .getChild(new NodeIdentifier(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                .network.rev150608.network.node.SupportingNode.QNAME)).get().getValue();
                } else {
                    supportingNodes = (Collection<?>) ((MapEntryNode) overlayNode)
                            .getChild(new NodeIdentifier(SupportingNode.QNAME)).get().getValue();
                }
                Iterator<?> supportingNodesIterator = supportingNodes.iterator();
                while (supportingNodesIterator.hasNext()) {
                    NormalizedNode<?, ?> supportingNode = (NormalizedNode<?, ?>) supportingNodesIterator.next();
                    YangInstanceIdentifier yiidNodeRef = null;
                    if (outputModel.equals(I2rsModel.class)) {
                        yiidNodeRef = YangInstanceIdentifier.builder()
                            .node(TopologyQNames.I2RS_NODE_REF).build();
                    } else {
                        yiidNodeRef = YangInstanceIdentifier.builder()
                                .node(TopologyQNames.NODE_REF).build();
                    }
                    Optional<NormalizedNode<?, ?>> supportingNodeNodeRefOptional =
                            NormalizedNodes.findNode(supportingNode, yiidNodeRef);
                    if (supportingNodeNodeRefOptional.isPresent()) {
                        String supportingNodeNodeRef = (String)supportingNodeNodeRefOptional.get().getValue();
                        if (supportingNodeNodeRef.equals(sourceNode.getValue()) && srcFound == false) {
                            computedLink.setSrcNode(overlayNode);
                            srcFound = true;
                        }
                        if (supportingNodeNodeRef.equals(destNode.getValue()) && dstFound == false) {
                            computedLink.setDstNode(overlayNode);
                            dstFound = true;
                        }
                        if (srcFound && dstFound) {
                            break;
                        }
                    }
                }
            }
            if (srcFound && dstFound) {
                // link is put into matchedLinks map
                matchedLinks.put(linkId, computedLink);
                if (aggregator != null) {
                    if (update) {
                        aggregator.processUpdatedChanges(linkId, computedLink, computedLink.getTopologyId());
                    } else {
                        aggregator.processCreatedChanges(linkId, computedLink, computedLink.getTopologyId());
                    }
                }
                else {
                    OverlayItem overlayItem = wrapUnderlayItem(computedLink);
                    manager.addOverlayItem(overlayItem);
                }
                // if the waitingList map contains the link it will be removed
                return true;
            } else {
                waitingLinks.put(linkId, link);
            }
        }
        return false;
    }

    /**
     * Updates a preexisting matched link. If a match is found, the computed link is updated accordingly.
     * Else returns null.
     *
     * @param computedLink - an old matched link with null src and/or dst
     * @param updatedLinkSrc - new src of the updated link
     * @param updatedLinkDst - new dst of the updated link
     * @return updated computed link, null if no match is found
     */
    private ComputedLink updateComputedLink(ComputedLink computedLink, NormalizedNode<?,?> updatedLinkSrc,
            NormalizedNode<?,?> updatedLinkDst) {
        NormalizedNode<?, ?> computedLinkDstNode = computedLink.getDstNode();
        NormalizedNode<?, ?> computedLinkSrcNode = computedLink.getSrcNode();

        if (computedLinkSrcNode == null || computedLinkDstNode == null) {
            Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> overlayNodesIterator =
                    storedOverlayNodes.getUnderlayItems().entrySet().iterator();
            //iterate over all overlay nodes
            while (overlayNodesIterator.hasNext()) {
                Entry<YangInstanceIdentifier, UnderlayItem> overlayNodeEntry = overlayNodesIterator.next();
                NormalizedNode<?, ?> overlayNode = overlayNodeEntry.getValue().getItem();
                Collection<?> supportingNodes = null;
                if (outputModel.equals(I2rsModel.class)) {
                supportingNodes = (Collection<?>) ((MapEntryNode) overlayNode)
                        .getChild(new NodeIdentifier(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                .network.rev150608.network.node.SupportingNode.QNAME)).get().getValue();
                } else {
                    supportingNodes = (Collection<?>) ((MapEntryNode) overlayNode)
                            .getChild(new NodeIdentifier(SupportingNode.QNAME)).get().getValue();
                }
                Iterator<?> supportingNodesIterator = supportingNodes.iterator();
                while (supportingNodesIterator.hasNext()) {
                    NormalizedNode<?, ?> supportingNode = (NormalizedNode<?, ?>) supportingNodesIterator.next();
                    YangInstanceIdentifier yiidNodeRef = null;
                    if (outputModel.equals(I2rsModel.class)) {
                        yiidNodeRef = YangInstanceIdentifier.builder()
                            .node(TopologyQNames.I2RS_NODE_REF).build();
                    } else {
                        yiidNodeRef = YangInstanceIdentifier.builder()
                                .node(TopologyQNames.NODE_REF).build();
                    }
                    Optional<NormalizedNode<?, ?>> supportingNodeNodeRefOptional =
                            NormalizedNodes.findNode(supportingNode, yiidNodeRef);
                    if (supportingNodeNodeRefOptional.isPresent()) {
                        NormalizedNode<?, ?> supportingNodeNodeRef = supportingNodeNodeRefOptional.get();
                        String suppNodeNodeRefValue = (String) supportingNodeNodeRef.getValue();
                        if (computedLinkSrcNode == null && suppNodeNodeRefValue.equals(updatedLinkSrc.getValue())) {
                            computedLink.setSrcNode(overlayNode);
                        }
                        if (computedLinkDstNode == null && suppNodeNodeRefValue.equals(updatedLinkDst.getValue())) {
                            computedLink.setDstNode(overlayNode);
                        }
                        if (computedLink.getSrcNode() != null && computedLink.getDstNode() != null) {
                            break;
                        }
                    }
                }
            }
            if (computedLink.getSrcNode() == null || computedLink.getDstNode() == null) {
                return null;
            }
        }
        return computedLink;
    }

    private NormalizedNode<?, ?> getLinkSourceNode(UnderlayItem link) {
        NormalizedNode<?, ?> sourceNode = null;
        Optional<NormalizedNode<?,?>> sourceNodePresent =
                NormalizedNodes.findNode(link.getItem(), SOURCE_NODE_IDENTIFIER);
        if (sourceNodePresent.isPresent()) {
            sourceNode = (NormalizedNode<?, ?>) sourceNodePresent.get();
        }
        return sourceNode;
    }

    private NormalizedNode<?, ?> getLinkDestNode(UnderlayItem link) {
        NormalizedNode<?, ?> destNode = null;
        Optional<NormalizedNode<?,?>> destNodePresent =
                NormalizedNodes.findNode(link.getItem(), DEST_NODE_IDENTIFIER);
        if (destNodePresent.isPresent()) {
            destNode = (NormalizedNode<?, ?>) destNodePresent.get();
        }
        return destNode;
    }

    @Override
    public void setTopologyManager(ITopologyManager manager) {
        this.manager = manager;
    }

    private OverlayItem wrapUnderlayItem(UnderlayItem underlayItem) {
        List<UnderlayItem> underlayItems = Collections.singletonList(underlayItem);
        OverlayItem overlayItem = new OverlayItem(underlayItems, underlayItem.getCorrelationItem());
        underlayItem.setOverlayItem(overlayItem);
        return overlayItem;
    }

    public void setTopologyAggregator(TopologyAggregator aggregator){
        this.aggregator = aggregator;
    }

}
