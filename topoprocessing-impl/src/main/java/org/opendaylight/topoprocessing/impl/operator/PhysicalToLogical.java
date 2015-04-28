/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author matus.marko
 */
public class PhysicalToLogical implements TopologyOperator {

    TopologyManager next;

    @Override
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, String topologyId) {
    }

    @Override
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries, String topologyId) {

    }

    @Override
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, String topologyId) {
    }

    public void setNext(TopologyManager next) {
        this.next = next;
    }

    private LogicalNode translate(PhysicalNode physical) {

        LogicalNode logicalNode = new LogicalNode();
        logicalNode.addPhysicalNode(physical);
        return logicalNode;
    }
}
