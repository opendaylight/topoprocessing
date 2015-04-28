/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.node.ip.filtration._case.node.ip.filtration.Filter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TopologyFiltrator implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyFiltrator.class);

    private Filter filter;
    private TopologyManager next;

    /**
     * Constructor
     * @param correlation
     */
    public TopologyFiltrator(Filter filter) {

        this.filter = filter;
        LOG.debug("Initializing Filtrator");
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, String topologyId) {
        LOG.debug("Processing createdChanges");
        Map<YangInstanceIdentifier, PhysicalNode> createdData = new HashMap<>();
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> changeEntry : createdEntries.entrySet()) {
            //TODO check leaf expected node value (Ipv4Address)
            if (filter.getValue().getValue().equals(changeEntry.getValue().getLeafNode().getValue())) {
                createdData.put(changeEntry.getKey(), changeEntry.getValue());
            }
        }
        LOG.debug("CreatedChanges processed");
        next.processCreatedChanges(createdData, topologyId);
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries, String topologyId) {
        LOG.debug("Processing updatedChanges");
        Map<YangInstanceIdentifier, PhysicalNode> updatedData = new HashMap<>();
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> changeEntry : updatedEntries.entrySet()) {
            //TODO check leaf expected node value (Ipv4Address)
            if (filter.getValue().getValue().equals(changeEntry.getValue().getLeafNode().getValue())) {
                updatedData.put(changeEntry.getKey(), changeEntry.getValue());
            }
        }
        LOG.debug("CreatedChanges processed");
        next.processUpdatedChanges(updatedEntries, topologyId);
    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
        LOG.debug("Processing removedChanges");
        next.processRemovedChanges(identifiers, topologyId);
    }

    public void setNext(TopologyManager next) {
        this.next = next;
    }
}
