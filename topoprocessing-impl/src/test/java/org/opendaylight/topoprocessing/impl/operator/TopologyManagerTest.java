/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.equality._case.EqualityBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyManagerTest {

    TopologyAggregator topologyManager = new TopologyAggregator();
    @Mock Map<YangInstanceIdentifier, PhysicalNode> entriesMap;
    @Mock List<YangInstanceIdentifier> entriesList;
    String topologyId;
    public static final String TOPOLOGY1 = "openflow:1";
    public static final String TOPOLOGY1_DUPLICATE = "openflow:1";
    public static final String TOPOLOGY2 = "bgp:1";

    /**
     * Checks that two topology stores are initialized for two different underlay topologies in one call
     */
    @Test
    public void testInitStructuresWithTwoDifferentTopologies() {
        List<Mapping> mappings = createMappings(TOPOLOGY1, TOPOLOGY2);
        EqualityCaseBuilder equalityCaseBuilder = createEqualityBuilder(mappings);
        CorrelationBuilder correlationBuilder = createCorrelationBuilder(equalityCaseBuilder);
        topologyManager.initializeStructures(correlationBuilder.build());
        assertEquals(topologyManager.getTopologyStores().size(), 2);
    }

    /**
     * Checks that two topology stores are initialized for two different underly topologies in
     * two different calls of initializeStructures method
     */
    @Test
    public void testInitStructuresWithTwoDifferentTopologiesInTwoCalls() {
        List<Mapping> mappings = createMappings(TOPOLOGY1);
        EqualityCaseBuilder equalityCaseBuilder = createEqualityBuilder(mappings);
        CorrelationBuilder correlationBuilder = createCorrelationBuilder(equalityCaseBuilder);
        topologyManager.initializeStructures(correlationBuilder.build());

        List<Mapping> mappings2 = createMappings(TOPOLOGY2);
        EqualityCaseBuilder equalityCaseBuilder2 = createEqualityBuilder(mappings2);
        CorrelationBuilder correlationBuilder2 = createCorrelationBuilder(equalityCaseBuilder2);
        topologyManager.initializeStructures(correlationBuilder2.build());

        assertEquals(topologyManager.getTopologyStores().size(), 2);
    }

    /**
     * Checks that in two different calls of initializeStructures method
     * only one topology store is initialized for two underly topologies with the same id 
     */
    @Test
    public void testInitStructuresWithTwoSameTopologiesInTwoCalls() {
        List<Mapping> mappings = createMappings(TOPOLOGY1);
        EqualityCaseBuilder equalityCaseBuilder = createEqualityBuilder(mappings);
        CorrelationBuilder correlationBuilder = createCorrelationBuilder(equalityCaseBuilder);
        topologyManager.initializeStructures(correlationBuilder.build());

        List<Mapping> mappings2 = createMappings(TOPOLOGY1_DUPLICATE);
        EqualityCaseBuilder equalityCaseBuilder2 = createEqualityBuilder(mappings2);
        CorrelationBuilder correlationBuilder2 = createCorrelationBuilder(equalityCaseBuilder2);
        topologyManager.initializeStructures(correlationBuilder2.build());

        assertEquals(topologyManager.getTopologyStores().size(), 1);
    }

    private static List<Mapping> createMappings(String... topologyIds) {
        List<Mapping> mappings = new ArrayList<>();
        for (String topologyId : topologyIds) {
            MappingBuilder mappingBuilder1 = new MappingBuilder();
            mappingBuilder1.setUnderlayTopology(topologyId);
            mappings.add(mappingBuilder1.build());
        }
        return mappings;
    }

private static EqualityCaseBuilder createEqualityBuilder(List<Mapping> mappings) {
    EqualityBuilder equalityBuilder = new EqualityBuilder();
    equalityBuilder.setMapping(mappings);
    EqualityCaseBuilder equalityCaseBuilder = new EqualityCaseBuilder();
    equalityCaseBuilder.setEquality(equalityBuilder.build());
    return equalityCaseBuilder;
}

private static CorrelationBuilder createCorrelationBuilder(EqualityCaseBuilder equalityCaseBuilder) {
    CorrelationBuilder correlationBuilder = new CorrelationBuilder();
    correlationBuilder.setName(Equality.class);
    correlationBuilder.setCorrelationType(equalityCaseBuilder.build());
    return correlationBuilder;
}

    /**
     * Checks that correlation with mappings set to null is handled correctly 
     * (no topology store is created, no NullPointerException)
     */
    @Test
    public void testMappingNull() {
        List<Mapping> mappings = null;
        EqualityCaseBuilder equalityCaseBuilder = createEqualityBuilder(mappings);
        CorrelationBuilder correlationBuilder = createCorrelationBuilder(equalityCaseBuilder);
        topologyManager.initializeStructures(correlationBuilder.build());
    }
}
