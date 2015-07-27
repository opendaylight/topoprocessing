/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener.RequestAction;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.polkorab
 *
 */
public class InventoryListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryListener.class);
    private static final YangInstanceIdentifier itemIdentifier = YangInstanceIdentifier.of(Node.QNAME);

    private TopologyOperator operator;
    private String topologyId;

    private YangInstanceIdentifier pathIdentifier;

    /**
     * Default constructor
     * @param underlayTopologyId underlay topology id
     */
    public InventoryListener(String underlayTopologyId) {
        this.topologyId = underlayTopologyId;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        if (! change.getCreatedData().isEmpty()) {
            LOGGER.debug("Processing createdData");
            proceedChangeRequest(change.getCreatedData(), RequestAction.CREATE);
        }
        if (! change.getUpdatedData().isEmpty()) {
            LOGGER.debug("Processing updatedData");
            proceedChangeRequest(change.getUpdatedData(), RequestAction.UPDATE);
        }
        if (! change.getRemovedPaths().isEmpty()) {
            LOGGER.debug("Processing removedData");
            processRemovedChanges(change.getRemovedPaths());
        }
        LOGGER.debug("DataChangeEvent processed");
    }

    private void proceedChangeRequest(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData,
            RequestAction requestAction) {
        Map<YangInstanceIdentifier, UnderlayItem> resultEntries = new HashMap<>();
        Iterator<Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator =
                createdData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            if (entry.getValue() instanceof MapEntryNode && entry.getValue().getNodeType().equals(Node.QNAME)) {
                Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry.getValue(), pathIdentifier);
                if (node.isPresent()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Found node: {}", node.get());
                    }
                    NormalizedNode<?, ?> leafnode = node.get();
                    UnderlayItem underlayItem =
                            new UnderlayItem(null, leafnode, topologyId, null, CorrelationItemEnum.Node);
                    resultEntries.put(entry.getKey(), underlayItem);
                } else {
                    continue;
                }
            }
        }
        if (! resultEntries.isEmpty()) {
            if (requestAction == RequestAction.CREATE) {
                operator.processCreatedChanges(resultEntries, topologyId);
            } else if (requestAction == RequestAction.UPDATE) {
                operator.processUpdatedChanges(resultEntries, topologyId);
            }
        }
    }

    /**
     * @param removedPaths identifies removed structures
     */
    private void processRemovedChanges(Set<YangInstanceIdentifier> removedPaths) {
        List<YangInstanceIdentifier> identifiers = new ArrayList<>();
        Iterator<YangInstanceIdentifier> iterator = removedPaths.iterator();
        while (iterator.hasNext()) {
            YangInstanceIdentifier identifierOperational = iterator.next();
            PathArgument lastPathArgument = identifierOperational.getLastPathArgument();
            if (! (lastPathArgument instanceof AugmentationIdentifier)
                    && lastPathArgument.getNodeType().equals(Node.QNAME)
                    && ! lastPathArgument.equals(itemIdentifier.getLastPathArgument())) {
                identifiers.add(identifierOperational);
            }
        }
        operator.processRemovedChanges(identifiers, topologyId);
    }

    /**
     * @param operator processes received notifications (aggregates / filters them)
     */
    public void setOperator(TopologyOperator operator) {
        this.operator = operator;
    }

    /**
     * @param pathIdentifier identifies leaf (node), which aggregation / filtering will be based on
     */
    public void setPathIdentifier(YangInstanceIdentifier pathIdentifier) {
        this.pathIdentifier = pathIdentifier;
    }
}
