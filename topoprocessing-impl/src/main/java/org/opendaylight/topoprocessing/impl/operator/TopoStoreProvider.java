/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author matus.marko
 */
public abstract class  TopoStoreProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TopoStoreProvider.class);

    protected List<TopologyStore> topologyStores = new ArrayList<>();

    /** for testing purpose only */
    public List<TopologyStore> getTopologyStores() {
        return topologyStores;
    }

    /**
     * @param topologyId
     * @return Return TopologyStore by given Topology Id
     */
    public TopologyStore getTopologyStore(String topologyId) {
        for (TopologyStore topologyStore : topologyStores) {
            if (topologyId == topologyStore.getId()) {
                return topologyStore;
            }
        }
        return null;
    }

    /** for testing purpose only */
    public void setTopologyStores(List<TopologyStore> topologyStores) {
        this.topologyStores = topologyStores;
    }

    /**
     * Initialize Topology Store
     * @param underlayTopologyId Underlay Topology ID
     */
    public void initializeStore(String underlayTopologyId) {
        for (TopologyStore topologyStore : topologyStores) {
            if (underlayTopologyId == topologyStore.getId()) {
                return;
            }
        }
        topologyStores.add(new TopologyStore(underlayTopologyId,
                new HashMap<YangInstanceIdentifier, PhysicalNode>()));
    }
}
