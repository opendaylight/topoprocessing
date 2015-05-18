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

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public abstract class  TopoStoreProvider {

    protected List<TopologyStore> topologyStores = new ArrayList<>();

    /**
     * @param topologyId
     * @return TopologyStore by given Topology Id
     */
    public TopologyStore getTopologyStore(String topologyId) {
        for (TopologyStore topologyStore : topologyStores) {
            if (topologyId == topologyStore.getId()) {
                return topologyStore;
            }
        }
        return null;
    }

    /**
     * Initialize Topology Store
     * @param underlayTopologyId Underlay Topology ID
     */
    public void initializeStore(String underlayTopologyId, boolean aggregateInside) {
        for (TopologyStore topologyStore : topologyStores) {
            if (underlayTopologyId.equals(topologyStore.getId())) {
                return;
            }
        }
        topologyStores.add(new TopologyStore(underlayTopologyId, aggregateInside,
                new HashMap<YangInstanceIdentifier, PhysicalNode>()));
    }
}
