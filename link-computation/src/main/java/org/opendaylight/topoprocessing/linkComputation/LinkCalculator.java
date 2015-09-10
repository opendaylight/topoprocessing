/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.linkComputation;

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
                    storedOverlayNodes.getUnderlayItems().put(createdItem.getKey(), createdItem.getValue());
                    for (Entry<YangInstanceIdentifier, UnderlayItem> waitingLink : waitingLinks.entrySet()) {
                        calculatePossibleLink(waitingLink);
                    }
                } else if (CorrelationItemEnum.Link.equals(createdItem.getValue().getCorrelationItem())) {
                    // process links from underlay topology
                    calculatePossibleLink(createdItem);
                }
            }
        }
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId) {
        LOGGER.trace("Processing updatedChanges in " + getClass().toString());
        for (Entry<YangInstanceIdentifier, UnderlayItem> updatedEntry : updatedEntries.entrySet()) {
            if (matchedLinks.containsKey(updatedEntry.getKey())) {
                // updated item was link
                ComputedLink computedLink = matchedLinks.get(updatedEntry.getKey());
                NormalizedNode<?, ?> sourceNode = computedLink.getLeafNode();
                NormalizedNode<?, ?> destNode = computedLink.getLeafNode2();
                NormalizedNode<?, ?> newLinkSourceNode = getLinkSourceNode(updatedEntry.getValue());
                NormalizedNode<?, ?> newLinkDestNode = getLinkDestNode(updatedEntry.getValue());
                if (!sourceNode.equals(newLinkSourceNode) || !destNode.equals(newLinkDestNode)) {
                    OverlayItem overlayItem = computedLink.getOverlayItem();
                    matchedLinks.remove(updatedEntry.getKey());
                    manager.removeOverlayItem(overlayItem);
                    calculatePossibleLink(updatedEntry);
                }
                // else do nothing
            } else if (storedOverlayNodes.getUnderlayItems().containsKey(updatedEntry.getKey())) {
                // updated item was node
            }
        }
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOGGER.trace("Processing removedChanges in " + getClass().toString());
        for (YangInstanceIdentifier itemIdentifier : identifiers) {
            UnderlayItem removedOverlayNode = storedOverlayNodes.getUnderlayItems().remove(itemIdentifier);
            if (removedOverlayNode != null) {
                // removed item was an overlay node
                for (Entry<YangInstanceIdentifier, ComputedLink> matchedLink : matchedLinks.entrySet()) {
                    if (removedOverlayNode.getItem().equals(matchedLink.getValue().getLeafNode())
                            || removedOverlayNode.getItem().equals(matchedLink.getValue().getLeafNode2())) {
                        // remove calculated link
                        removeMatchedLink(matchedLink.getKey());
                    }
                }
            } else if (matchedLinks.containsKey(itemIdentifier)) {
                // removed item was link
                removeMatchedLink(itemIdentifier);
            }
        }
    }

    private void removeMatchedLink(YangInstanceIdentifier itemIdentifier) {
        ComputedLink overlayLink = matchedLinks.remove(itemIdentifier);
        if (null != overlayLink) {
            manager.removeOverlayItem(overlayLink.getOverlayItem());
        }
    }

    private void calculatePossibleLink(
            Entry<YangInstanceIdentifier, UnderlayItem> link) {
        NormalizedNode<?, ?> sourceNode = getLinkSourceNode(link.getValue());
        NormalizedNode<?, ?> destNode = getLinkDestNode(link.getValue());

        if (sourceNode != null && destNode != null) {
            ComputedLink computedLink = new ComputedLink(link.getValue().getItem(), null, null,
                    storedOverlayNodes.getId(), link.getValue().getItemId(), CorrelationItemEnum.Link);
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
                // if the waitingList map contains the link it will be removed 
                waitingLinks.remove(link.getKey());
                // link is put into matchedLinks map
                matchedLinks.put(link.getKey(), computedLink);
                OverlayItem overlayItem = wrapUnderlayItem(computedLink);
                manager.addOverlayItem(overlayItem);
            } else {
                waitingLinks.put(link.getKey(), link.getValue());
            }
        }
    }

    private NormalizedNode<?, ?> getLinkSourceNode(UnderlayItem createdItemValue) {
        NormalizedNode<?, ?> sourceNode = null;
        Optional<NormalizedNode<?,?>> sourceNodePresent =
                NormalizedNodes.findNode(createdItemValue.getItem(), sourceNodeRelativeIdentifier);
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
