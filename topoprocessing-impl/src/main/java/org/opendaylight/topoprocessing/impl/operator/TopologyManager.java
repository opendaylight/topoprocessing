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

import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.CorrelationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class TopologyManager {

    TopologyAggregator aggregator = null;
    private List<TopologyStore> topologyStores = new ArrayList<>();
    /**
     * @param newEntries
     * @param topologyId
     */
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> newEntries, final String topologyId) {
        aggregator.processCreatedChanges(newEntries, topologyId);
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
     * @param correlation
     */
    public void initializeStructure(Correlation correlation) {
        aggregator = new TopologyAggregator(correlation.getCorrelationItem(), topologyStores);

        CorrelationType correlationType = correlation.getCorrelationType();
        EqualityCase equalityCase = (EqualityCase) correlationType;
        List<Mapping> mappings = equalityCase.getEquality().getMapping();
        for (Mapping mapping : mappings) {
            topologyStores.add(new TopologyStore(mapping.getUnderlayTopology(),
                    new HashMap<YangInstanceIdentifier, PhysicalNode>()));
        }
    }
}
