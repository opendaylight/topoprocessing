/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

/**
 * @author matus.marko
 */
public class IdentifierGenerator {

    /**
     * Value for internal counter
     */
    private volatile int id = 0;
    AtomicIntegerFieldUpdater<IdentifierGenerator> idUpdater =
            AtomicIntegerFieldUpdater.newUpdater(IdentifierGenerator.class, "id");

    private int getNextId() {
        return idUpdater.incrementAndGet(this);
    }

    /**
     * Create unique identifier
     * @param correlationItem 
     * @return unique identifier
     */
    public String getNextIdentifier(CorrelationItemEnum correlationItem) {
        String identifier = null;
        switch (correlationItem) {
        case Node:
            identifier = new String("node:" + getNextId());
            break;
        case Link:
            identifier = new String("link:" + getNextId());
            break;
        case TerminationPoint:
            identifier = new String("tp:" + getNextId());
            break;
        default:
            throw new IllegalStateException("Unknown Correlation item used: " + correlationItem);
        }
        return identifier;
    }

}
