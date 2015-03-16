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

import java.util.HashMap;
import java.util.Map;

/**
 * @author matus.marko
 */
public class TopologyManager {

    private HashMap<YangInstanceIdentifier, LogicalNode> map;

    public void create(Map<YangInstanceIdentifier, PhysicalNode> resultEntries) {

    }

    public void update(Map<YangInstanceIdentifier, PhysicalNode> resultEntries) {

    }

    public void delete(YangInstanceIdentifier identifier) {

    }
}
