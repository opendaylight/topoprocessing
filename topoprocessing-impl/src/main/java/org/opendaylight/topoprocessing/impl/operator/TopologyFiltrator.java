/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.NodeIpFiltrationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.node.ip.filtration._case.node.ip.filtration.Filter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TopologyFiltrator implements TopologyOperator {

//    private IdentifierGenerator idGenerator;
    private Correlation correlation;
//    private List<TopologyStore> topologyStores;

    /**
     * Constructor
     * @param correlation
     * @param topologyStores
     */
    public TopologyFiltrator(Correlation correlation, List<TopologyStore> topologyStores,
                             IdentifierGenerator idGenerator) {
        this.correlation = correlation;
//        this.topologyStores = topologyStores;
//        this.idGenerator = idGenerator;
    }

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, String topologyId) {
        Map<YangInstanceIdentifier, PhysicalNode> createdData = new HashMap<>();
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> changeEntry : createdEntries.entrySet()) {
            NodeIpFiltrationCase typeCase = (NodeIpFiltrationCase) correlation.getCorrelationType();
            List<Filter> filters = typeCase.getNodeIpFiltration().getFilter();
            Filter filter = filters.iterator().next();
            //TODO check leaf node value
            if (filter.getValue().getValue().equals(changeEntry.getValue().getLeafNode().getValue())) {
                createdData.put(changeEntry.getKey(), changeEntry.getValue());
            }
        }
        // TODO handle result
        return; //createdData
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries, String topologyId) {
        Map<YangInstanceIdentifier, PhysicalNode> updatedData = new HashMap<>();
        for (Map.Entry<YangInstanceIdentifier, PhysicalNode> changeEntry : updatedEntries.entrySet()) {
            NodeIpFiltrationCase typeCase = (NodeIpFiltrationCase) correlation.getCorrelationType();
            List<Filter> filters = typeCase.getNodeIpFiltration().getFilter();
            Filter filter = filters.iterator().next();
            //TODO check leaf node value
            if (filter.getValue().getValue().equals(changeEntry.getValue().getLeafNode().getValue())) {
                updatedData.put(changeEntry.getKey(), changeEntry.getValue());
            }
        }
        // TODO handle result
        return; //updatedData
    }

    @Override
    public void processRemovedChanges(ArrayList<YangInstanceIdentifier> identifiers, String topologyId) {
        //TODO handle result
        return; // identifiers
    }
}
