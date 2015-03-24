/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NodeIpFiltration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.CorrelationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.NodeIpFiltrationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.UnificationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.node.ip.filtration._case.node.ip.filtration.Filter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class TopologyManager {

    private TopologyOperator aggregator = null;
    private TopologyOperator filtrator = null;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private List<TopologyStore> topologyStores = new ArrayList<>();
    /**
     * @param createdEntries
     * @param topologyId
     */
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, final String topologyId) {
        aggregator.processCreatedChanges(createdEntries, topologyId);
    }

    /**
     * Process Deleted changes
     * @param identifier Yang Instance Identifier
     * @param topologyId Topology Identification
     */
    public void processDeletedChanges(ArrayList<YangInstanceIdentifier> identifiers, final String topologyId) {
        aggregator.processRemovedChanges(identifiers, topologyId);
    }

    /**
     * @param resultEntries
     * @param underlayTopologyId
     */
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
            String topologyId) {
        aggregator.processUpdatedChanges(updatedEntries, topologyId);
        
    }

    /**
     * Set correlation and initialize stores
     * @param correlation Correlation
     */
    public void initializeStructures(Correlation correlation) {
        //TODO Pipelining
        if (correlation.getName().equals(Equality.class)) {
            EqualityCase typeCase = (EqualityCase) correlation.getCorrelationType();
            List<Mapping> mappings = typeCase.getEquality().getMapping();
            iterateMappings(mappings);

            aggregator = new TopologyAggregator(correlation.getCorrelationItem(), topologyStores, idGenerator);
        }
        else if (correlation.getName().equals(Unification.class)) {
            UnificationCase typeCase = (UnificationCase) correlation.getCorrelationType();
            List<Mapping> mappings = typeCase.getUnification().getMapping();
            iterateMappings(mappings);

            //TODO Unification Operator
        }
        else if (correlation.getName().equals(NodeIpFiltration.class)) {
            NodeIpFiltrationCase typeCase = (NodeIpFiltrationCase) correlation.getCorrelationType();
            List<Filter> filters = typeCase.getNodeIpFiltration().getFilter();
            for (Filter filter : filters) {
                initStore(filter.getUnderlayTopology());
            }

            filtrator = new TopologyFiltrator(correlation);
        }
    }

    private void iterateMappings(List<Mapping> mappings ) {
        for (Mapping mapping : mappings) {
            initStore(mapping.getUnderlayTopology());
        }
    }

    private void initStore(String id) {
        for (TopologyStore topologyStore : topologyStores) {
            if (id == topologyStore.getId()) {
                return;
            }
        }
        topologyStores.add(new TopologyStore(id, new HashMap<YangInstanceIdentifier, PhysicalNode>()));
    }
}
