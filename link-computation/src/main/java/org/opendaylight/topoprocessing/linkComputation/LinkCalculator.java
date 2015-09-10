/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.linkComputation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author martin.uhlir
 *
 */
public class LinkCalculator implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(LinkCalculator.class);
    private TopoStoreProvider storedOverlayNodes;
    private TopoStoreProvider waitingList;
    private TopoStoreProvider matchedLinks;

    /**
     * Constructor
     */
    public LinkCalculator() {
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries, String topologyId) {
        LOG.trace("Processing createdChanges in " + getClass().toString());
        if (createdEntries != null) {
            for (Entry<YangInstanceIdentifier, UnderlayItem> createdItem : createdEntries.entrySet()) {
                if (CorrelationItemEnum.Node.equals(createdItem.getValue().getCorrelationItem())) {
                    // process nodes from overlay topology
                    storedOverlayNodes.getTopologyStore(topologyId).getUnderlayItems().put(createdItem.getKey(),
                            createdItem.getValue());
                    else if (CorrelationItemEnum.Link.equals(createdItem.getValue().getCorrelationItem())) {
                    // process links from underlay topology
                    ComputedLink computedLink = (ComputedLink) createdItem.getValue();
                    Object leafNode1 = computedLink.getLeafNode().getValue();
                    Object leafNode2 = computedLink.getLeafNode2().getValue();
                    Iterator<TopologyStore> topologyStoresIterator = storedOverlayNodes.getTopologyStores().iterator();
                    //iterate over all Overlay Node topology stores
                    while (topologyStoresIterator.hasNext()) {
                        TopologyStore ts = topologyStoresIterator.next();
                        Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> overlayNodesIterator =
                                ts.getUnderlayItems().entrySet().iterator();
                        boolean srcFound = false;
                        boolean dstFound = false;
                        //iterate over all overlay nodes in particular topology store
                        while (overlayNodesIterator.hasNext()) {
                            Entry<YangInstanceIdentifier, UnderlayItem> overlayNodeEntry = overlayNodesIterator.next();
                            MapEntryNode overlayNode = (MapEntryNode) overlayNodeEntry.getValue().getItem();
                            Optional<DataContainerChild<? extends PathArgument, ?>> nodeRefOptional =
                                    overlayNode.getChild(NodeIdentifier.create(TopologyQNames.NODE_REF));
                            if (nodeRefOptional.isPresent()) {
                                Object underlayNodeId = nodeRefOptional.get().getValue();
                                if (underlayNodeId.equals(leafNode1) && srcFound == false) {
                                    srcFound = true;
                                }
                                if (underlayNodeId.equals(leafNode2) && dstFound == false) {
                                    dstFound = true;
                                }
                                if (srcFound && dstFound) {
                                    break;
                                }
                            }
                        }
                        if (srcFound && dstFound) {
                            TopologyStore matchedLinksStore = matchedLinks.getTopologyStore(ts.getTopologyId());
                            matchedLinksStore.getUnderlayItems().put(createdItem.getKey(), createdItem.getValue());
                            
                        } else {
                            TopologyStore waitingListStore = waitingList.getTopologyStore(ts.getTopologyId());
                            waitingListStore.getUnderlayItems().put(createdItem.getKey(), createdItem.getValue());
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
