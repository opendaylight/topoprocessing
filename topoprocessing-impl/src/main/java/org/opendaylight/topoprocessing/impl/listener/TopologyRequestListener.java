/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.topoprocessing.impl.handler.TopologyRequestHandler;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

/**
 * Listens on new overlay topology requests
 * @author michal.polkorab
 */
public class TopologyRequestListener implements DataChangeListener {

    private ArrayList<TopologyRequestHandler> topoRequestHandlers = new ArrayList<>();

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        processCreatedData(change.getCreatedData());
        processRemovedData(change.getRemovedPaths());
    }

    private void processCreatedData(Map<InstanceIdentifier<?>, DataObject> map) {
        Iterator<DataObject> iterator = map.values().iterator();
        while (iterator.hasNext()) {
            DataObject dataObject = iterator.next();
            if (dataObject instanceof Topology) {
                TopologyRequestHandler requestHandler = new TopologyRequestHandler();
                topoRequestHandlers.add(requestHandler);
                requestHandler.processNewRequest((Topology) dataObject);
            }
        }
    }

    private void processRemovedData(Set<InstanceIdentifier<?>> removedPaths) {
        Iterator<InstanceIdentifier<?>> iterator = removedPaths.iterator();
        while (iterator.hasNext()) {
            InstanceIdentifier<?> identifier = iterator.next();
            if (identifier.getTargetType().equals(Topology.class)) {
                TopologyKey topologyKey = identifier.firstKeyOf(Topology.class, TopologyKey.class);
                String topologyId = topologyKey.getTopologyId().getValue();
                TopologyRequestHandler requestHandler = findTopologyRequestHandlerById(topologyId);
                Preconditions.checkNotNull(requestHandler, "TopologyRequest handler for topology-id: "
                            + topologyId + " was not found");
                requestHandler.processDeletionRequest();
            }
        }
    }

    /**
     * @param topologyId
     * @return {@link TopologyRequestHandler} that handles topology with specified topology id
     * or null if no handler was found
     */
    private TopologyRequestHandler findTopologyRequestHandlerById(String topologyId) {
        for (TopologyRequestHandler handler : topoRequestHandlers) {
            if (handler.getTopologyId().equals(topologyId)) {
                return handler;
            }
        }
        return null;
    }

}
