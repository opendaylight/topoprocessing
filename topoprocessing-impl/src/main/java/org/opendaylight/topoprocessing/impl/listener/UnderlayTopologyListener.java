/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 */

package org.opendaylight.topoprocessing.impl.listener;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listens on new overlay topology requests
 * Created by matus.marko on 6.3.2015.
 */
public class UnderlayTopologyListener implements DOMDataChangeListener {

    public enum RequestAction {
        CREATE, UPDATE, DELETE
    }

    private TopologyManager topologyManager;

    private YangInstanceIdentifier yangInstanceIdConfiguration;

    public UnderlayTopologyListener(TopologyManager topologyManager, YangInstanceIdentifier yangInstanceIdConfiguration) {
        this.topologyManager = topologyManager;
        this.yangInstanceIdConfiguration = yangInstanceIdConfiguration;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        this.proceedChangeRequest(change.getCreatedData(), RequestAction.CREATE);
        this.proceedChangeRequest(change.getUpdatedData(), RequestAction.UPDATE);
        this.proceedDeletionRequest(change.getRemovedPaths());
    }

    private void proceedChangeRequest(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map, RequestAction requestAction) {
        Iterator<Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> iterator = map.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry = iterator.next();
            Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(entry.getValue(), yangInstanceIdConfiguration);
            if (node.isPresent()) {
                // TODO - set entry to the TopologyManager with action
            }
        }
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
