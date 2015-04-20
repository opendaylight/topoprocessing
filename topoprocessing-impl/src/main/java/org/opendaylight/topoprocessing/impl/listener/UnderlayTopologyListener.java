/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public class UnderlayTopologyListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayTopologyListener.class);
    private final YangInstanceIdentifier nodeIdentifier = YangInstanceIdentifier.builder().node(Node.QNAME).build();

    public enum RequestAction {
        CREATE, UPDATE, DELETE
    }

    private TopologyManager topologyManager;
    private YangInstanceIdentifier pathIdentifier;
    private String underlayTopologyId;

    /**
     * Default constructor
     * @param topologyManager processes received notifications (aggregates /filters them)
     * @param underlayTopologyId underlay topology identifier
     * @param pathIdentifier identifies leaf (node), which aggregation / filtering will be based on
     */
    public UnderlayTopologyListener(TopologyManager topologyManager, String underlayTopologyId,
            YangInstanceIdentifier pathIdentifier) {
        this.topologyManager = topologyManager;
        this.underlayTopologyId = underlayTopologyId;
        this.pathIdentifier = pathIdentifier;
    }


    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DataChangeEvent received: " + change);
        }
        if (! change.getCreatedData().isEmpty()) {
            LOGGER.debug("Received createdData");
            this.proceedChangeRequest(change.getCreatedData(), RequestAction.CREATE);
        }
        if (! change.getUpdatedData().isEmpty()) {
            LOGGER.debug("Received updatedData");
            this.proceedChangeRequest(change.getUpdatedData(), RequestAction.UPDATE);
        }
        if (! change.getRemovedPaths().isEmpty()) {
            LOGGER.debug("Received removedData");
            this.proceedDeletionRequest(change.getRemovedPaths());
        }
        LOGGER.debug("DataChangeEvent processed");
    }

    private void proceedChangeRequest(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map,
            RequestAction requestAction) {
        YangInstanceIdentifier nodeIdentifier = YangInstanceIdentifier.builder().node(Node.QNAME).build();
        Map<YangInstanceIdentifier, PhysicalNode> resultEntries = new HashMap<>();
        Iterator<Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            LOGGER.debug("Entry received: " + entry);
            if (! (entry.getValue() instanceof AugmentationNode)) {
                if (entry.getValue().getNodeType().equals(Node.QNAME)) {
                    if (! entry.getKey().getLastPathArgument().equals(nodeIdentifier.getLastPathArgument())) {
                        LOGGER.debug("Processing entry: " + entry.getValue());
                        LOGGER.debug("Finding node: " + pathIdentifier);
                        Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry.getValue(), pathIdentifier);
                        LOGGER.debug("Found node: " + node.get());
                        String value = "";
                        if (node.isPresent()) {
                            LeafNode<?> leafnode = (LeafNode<?>) node.get();
                            value = (String) leafnode.getValue();
                            PhysicalNode physicalNode = new PhysicalNode(entry.getValue(), leafnode);
                            resultEntries.put(entry.getKey(), physicalNode);
                            LOGGER.debug("Created PhysicalNode: " + physicalNode);
                        }
                    }
                }
            }
        }
        if (! resultEntries.isEmpty()) {
            if (requestAction == RequestAction.CREATE) {
                topologyManager.processCreatedChanges(resultEntries, underlayTopologyId);
            } else if (requestAction == RequestAction.UPDATE) {
                topologyManager.processUpdatedChanges(resultEntries, underlayTopologyId);
            }
        }
    }

    private void proceedDeletionRequest(Set<YangInstanceIdentifier> set) {
        ArrayList<YangInstanceIdentifier> identifiers = new ArrayList<>();
        Iterator<YangInstanceIdentifier> iterator = set.iterator();
        while (iterator.hasNext()) {
            YangInstanceIdentifier identifierOperational = iterator.next();
            YangInstanceIdentifier.PathArgument lastPathArgument = identifierOperational.getLastPathArgument();
            if (! (lastPathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier)) {
                if (lastPathArgument.getNodeType().equals(
                        nodeIdentifier.getLastPathArgument().getNodeType()))
                {
                    if (! lastPathArgument.equals(nodeIdentifier.getLastPathArgument())) {
                        identifiers.add(identifierOperational);
                    }
                }
            }
        }
        topologyManager.processRemovedChanges(identifiers, underlayTopologyId);
    }

}
