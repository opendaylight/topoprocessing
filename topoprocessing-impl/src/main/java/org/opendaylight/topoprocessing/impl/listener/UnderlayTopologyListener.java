/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

import java.util.*;

/**
 * Listens on underlay topology changes
 * @author matus.marko
 */
public class UnderlayTopologyListener implements DOMDataChangeListener {

    public enum RequestAction {
        CREATE, UPDATE, DELETE
    }

    private TopologyManager topologyManager;

    private YangInstanceIdentifier yangInstanceIdConfiguration;

    public UnderlayTopologyListener(TopologyManager topologyManager, YangInstanceIdentifier yangInstanceIdConfiguration) {
        this.topologyManager = topologyManager;
        Preconditions.checkNotNull(yangInstanceIdConfiguration, "yangInstanceIdConfiguration je null");
        this.yangInstanceIdConfiguration = yangInstanceIdConfiguration;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        this.proceedChangeRequest(change.getCreatedData(), RequestAction.CREATE);
        this.proceedChangeRequest(change.getUpdatedData(), RequestAction.UPDATE);
        this.proceedDeletionRequest(change.getRemovedPaths());
    }

    private void proceedChangeRequest(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map, RequestAction requestAction) {
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> resultEntries = new HashMap<>();
        Iterator<Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            if (entry.getValue().getNodeType().equals(Node.QNAME))
            {
                Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry.getValue(), yangInstanceIdConfiguration);
                if (node.isPresent()) {
                    resultEntries.put(entry.getKey(), entry.getValue());
                }
            }
        }
        // TODO - set entry to the TopologyManager with action
    }

    private void proceedDeletionRequest(Set<YangInstanceIdentifier> set) {
        Iterator<YangInstanceIdentifier> iterator = set.iterator();
        while (iterator.hasNext()) {
            YangInstanceIdentifier identifierOperational = iterator.next();
            // TODO check if is functioning
            if (identifierOperational.getLastPathArgument().getNodeType().equals(
                    yangInstanceIdConfiguration.getLastPathArgument().getNodeType()))
            {
                // TODO - set entry to the TopologyManager with action
            }
        }
    }
}
