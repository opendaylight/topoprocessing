/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class MlmtAbstractTopologyUpdate implements MlmtTopologyUpdate {

    private MlmtTopologyUpdateType type;
    private LogicalDatastoreType storeType;
    private InstanceIdentifier<Topology> topologyId;

    public MlmtAbstractTopologyUpdate(MlmtTopologyUpdateType type,
            LogicalDatastoreType storeType, InstanceIdentifier<Topology> topologyId) {
        this.type = type;
        this.storeType = storeType;
        this.topologyId = topologyId;
    }

    @Override
    public MlmtTopologyUpdateType getType() {
        return type;
    }

    @Override
    public LogicalDatastoreType getStoreType() {
        return storeType;
    }

    @Override
    public InstanceIdentifier<Topology> getTopologyInstanceId() {
        return topologyId;
    }
}

