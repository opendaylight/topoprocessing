/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.List;
import java.util.Map;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public interface TopologyOperator {

    /**
     * Process newly created changes
     * @param createdEntries
     * @param topologyId
     */
    void processCreatedChanges(Map<YangInstanceIdentifier, UnderlayItem> createdEntries, final String topologyId);

    /**
     * Process newly updated changes
     * @param updatedEntries
     * @param topologyId
     */
    void processUpdatedChanges(Map<YangInstanceIdentifier, UnderlayItem> updatedEntries, String topologyId);

    /**
     * Process newly deleted changes
     * @param identifiers Yang instance identifier
     * @param topologyId Topology Identification
     */
    void processRemovedChanges(List<YangInstanceIdentifier> identifiers, final String topologyId);

    /**
     * @param topologyManager handles aggregated items from all correlations
     */
    void setTopologyManager(TopologyManager topologyManager);

    /**
     * Initialize Topology store with given Topology Id
     * @param underlayTopologyId Topology Id
     * @param aggregateInside true if aggregation should happen even inside
     *        the same topology
     */
    void initializeStore(String underlayTopologyId, boolean aggregateInside);
}
