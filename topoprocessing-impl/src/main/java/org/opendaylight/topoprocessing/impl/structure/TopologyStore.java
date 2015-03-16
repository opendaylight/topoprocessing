/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.Map;

/**
 * @author matus.marko
 */
public class TopologyStore {
    private String id;
    private Map<YangInstanceIdentifier, PhysicalNode> physicalNode;

    public TopologyStore(String id, Map<YangInstanceIdentifier, PhysicalNode> physicalNode) {
        this.id = id;
        this.physicalNode = physicalNode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<YangInstanceIdentifier, PhysicalNode> getPhysicalNode() {
        return physicalNode;
    }

    public void setPhysicalNode(Map<YangInstanceIdentifier, PhysicalNode> physicalNode) {
        this.physicalNode = physicalNode;
    }
}
