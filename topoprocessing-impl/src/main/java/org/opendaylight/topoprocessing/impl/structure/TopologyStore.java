/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import java.util.concurrent.ConcurrentMap;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class TopologyStore {

    private String id;
    private final boolean aggregateInside;
    private ConcurrentMap<YangInstanceIdentifier, UnderlayItem> underlayItems;

    /**
     * Default constructor
     * @param id topology-id of stored topology
     * @param aggregateInside signals if aggregation should happen even inside
     *        the same topology
     * @param underlayItem all items (either nodes, links or termination points) present in this topology
     */
    public TopologyStore(String id, boolean aggregateInside, ConcurrentMap<YangInstanceIdentifier, UnderlayItem> underlayItem) {
        this.id = id;
        this.aggregateInside = aggregateInside;
        this.underlayItems = underlayItem;
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
     * @return all {@link UnderlayItem}s present in this {@link TopologyStore}
     */
    public ConcurrentMap<YangInstanceIdentifier, UnderlayItem> getUnderlayItems() {
        return underlayItems;
    }

}
