/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.Map;

/**
 * @author matus.marko
 */
public class TopologyStore {

    private String id;
    private final boolean aggregateInside;
    private Map<YangInstanceIdentifier, PhysicalNode> physicalNodes;

    /**
     * Default constructor
     * @param id topology-id of stored topology
     * @param aggregateInside signals if aggregation should happen even inside
     *        the same topology
     * @param physicalNode all nodes present in this topology
     */
    public TopologyStore(String id, boolean aggregateInside, Map<YangInstanceIdentifier, PhysicalNode> physicalNode) {
        this.id = id;
        this.aggregateInside = aggregateInside;
        this.physicalNodes = physicalNode;
    }

    /**
     * @return id of the {@link Topology} represented by this {@link TopologyStore}
     */
    public String getId() {
        return id;
    }

    /**
     * @return aggregateInside
     */
    public boolean isAggregateInside() {
        return aggregateInside;
    }

    /**
     * @return all {@link PhysicalNode}s present in this {@link TopologyStore}
     */
    public Map<YangInstanceIdentifier, PhysicalNode> getPhysicalNodes() {
        return physicalNodes;
    }

}
