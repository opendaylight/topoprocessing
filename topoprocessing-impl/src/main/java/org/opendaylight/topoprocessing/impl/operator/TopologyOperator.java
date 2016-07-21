/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public interface TopologyOperator {

    /**
     * Process newly created changes.
     * @param itemIdentifier identifies item which is being created
     * @param createdItem item which is being created
     * @param topologyId identifies topology, which the new changes came from
     */
    void processCreatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem createdItem,
            final String topologyId);

    /**
     * Process newly updated changes.
     * @param itemIdentifier identifies updated item
     * @param updatedItem item which is being updated
     * @param topologyId identifies topology, which the updated changes came from
     */
    void processUpdatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem updatedItem, String topologyId);

    /**
     * Process newly deleted changes.
     * @param itemIdentifier removed item identifier
     * @param topologyId identifies topology, which the removed changes came from
     */
    void processRemovedChanges(YangInstanceIdentifier itemIdentifier, final String topologyId);

    /**
     * @param manager handles aggregated items from all correlations
     */
    void setTopologyManager(ITopologyManager manager);

}
