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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                UnderlayItem createdItemValue = createdItem.getValue();
                if (CorrelationItemEnum.Node.equals(createdItemValue.getCorrelationItem())) {
                    // process nodes from overlay topology
                    storedOverlayNodes.getTopologyStore(topologyId).getUnderlayItems().put(createdItem.getKey(),
                            createdItemValue);
                } else if (CorrelationItemEnum.Link.equals(createdItemValue.getCorrelationItem())) {
                    // process links from underlay topology
                    int linkInTopoStoreOccurence = 0;
                    ComputedLink computedLink = (ComputedLink) createdItem.getValue();
                    NormalizedNode<?, ?> leafNode1 = computedLink.getLeafNode();
                    NormalizedNode<?, ?> leafNode2 = computedLink.getLeafNode2();
                    Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> overlayNodesIterator =
                            storedOverlayNodes.getTopologyStore(topologyId).getUnderlayItems().entrySet().iterator();
                    //iterate over all overlay nodes in the particular topology
                    while (overlayNodesIterator.hasNext()) {
                        Entry<YangInstanceIdentifier, UnderlayItem> entry = overlayNodesIterator.next();
                        UnderlayItem value = entry.getValue();
                        MapEntryNode item = (MapEntryNode) value.getItem();
                        Collection<DataContainerChild<? extends PathArgument, ?>> value2 = item.getValue();
                        Iterator<DataContainerChild<? extends PathArgument, ?>> iterator2 = value2.iterator();
                        // iterate over supporting nodes of overlay item
                        while (iterator2.hasNext()) {
                            DataContainerChild<? extends PathArgument, ?> dataContainerChild = iterator2.next();
                            QName nodeType = dataContainerChild.getNodeType();
                            if (nodeType.equals(SupportingNode.QNAME)) {
                                if (dataContainerChild.getIdentifier().equals(leafNode1.getIdentifier())) {
                                    linkInTopoStoreOccurence++;
                                }
                                if (dataContainerChild.getIdentifier().equals(leafNode2.getIdentifier())) {
                                    linkInTopoStoreOccurence++;
                                }
                                break;
                            }
                        }
                        if (linkInTopoStoreOccurence < 2) {
                            waitingList.getTopologyStore(topologyId).getUnderlayItems().put(createdItem.getKey(),
                                    createdItemValue);
                        } else if (linkInTopoStoreOccurence == 2) {
                            matchedLinks.getTopologyStore(topologyId).getUnderlayItems().put(createdItem.getKey(),
                                    createdItemValue);
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
