/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import java.util.ArrayList;

/**
 * @author matus.marko
 */
public class LogicalNode {
    private ArrayList<PhysicalNode> physicalNodes;

    public LogicalNode(ArrayList<PhysicalNode> physicalNodes) {
        this.physicalNodes = physicalNodes;
    }

    public ArrayList<PhysicalNode> getPhysicalNodes() {
        return physicalNodes;
    }

    public void setPhysicalNodes(ArrayList<PhysicalNode> physicalNodes) {
        this.physicalNodes = physicalNodes;
    }
}
