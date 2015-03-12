/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.handler.TopologyRequestHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Listens on new overlay topology requests
 *
 * @author michal.polkorab
 */
public class TopologyRequestListener implements DOMDataChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyRequestListener.class);

    private DOMDataBroker dataBroker;
    private HashMap<YangInstanceIdentifier, TopologyRequestHandler> topoRequestHandlers = new HashMap<>();

    /**
     * Default contructor
     * @param dataBroker
     */
    public TopologyRequestListener(DOMDataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker, "DOMDataBroker can't be null");
        this.dataBroker = dataBroker;
        LOGGER.debug("Topology Request Listener created");
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        LOGGER.debug("DataChange event notification received");
        processCreatedData(change.getCreatedData());
        processRemovedData(change.getRemovedPaths());
    }

    private void processCreatedData(Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map) {
        LOGGER.debug("Processing created data changes");
        for(Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry : map.entrySet()) {
            YangInstanceIdentifier yangInstanceIdentifier = entry.getKey();
            NormalizedNode<?, ?> normalizedNode = entry.getValue();
            if(normalizedNode.getNodeType().equals(Topology.QNAME)) {
                if (((Topology) normalizedNode).getAugmentation(CorrelationAugment.class) != null) {
                    TopologyRequestHandler requestHandler = new TopologyRequestHandler(dataBroker);
                    topoRequestHandlers.put(yangInstanceIdentifier,requestHandler);
                    requestHandler.processNewRequest((Topology) normalizedNode);
                }
            }
        }
        LOGGER.debug("Created data processed");
    }

    private void processRemovedData(Set<YangInstanceIdentifier> removedPaths) {
        LOGGER.debug("Processing removed data changes");
        Iterator<YangInstanceIdentifier> iterator = removedPaths.iterator();
        while (iterator.hasNext()) {
            YangInstanceIdentifier yangInstanceIdentifier = iterator.next();
            if (topoRequestHandlers.containsKey(yangInstanceIdentifier)) {
                TopologyRequestHandler topologyRequestHandler = topoRequestHandlers.get(yangInstanceIdentifier);
                topologyRequestHandler.processDeletionRequest();
                topoRequestHandlers.remove(yangInstanceIdentifier);
                break;
            }
        }
        LOGGER.debug("Removed data processed");
    }
}
