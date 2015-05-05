/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public interface TopologyOperator {

    /**
     * Process newly created changes
     * @param createdEntries
     * @param topologyId
     */
    void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, final String topologyId);

    /**
     * Process newly updated changes
     * @param updatedEntries
     * @param topologyId
     */
    void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries, String topologyId);

    /**
     * Process newly deleted changes
     * @param identifiers Yang instance identifier
     * @param topologyId Topology Identification
     */
    void processRemovedChanges(List<YangInstanceIdentifier> identifiers, final String topologyId);

    /**
     * @param topologyManager handles aggregated nodes from all correlations
     */
    void setTopologyManager(TopologyManager topologyManager);

    /**
     * Initialize Topology store by given Topology Id
     * @param underlayTopologyId Topology Id
     */
    void initializeStore(String underlayTopologyId);
}
