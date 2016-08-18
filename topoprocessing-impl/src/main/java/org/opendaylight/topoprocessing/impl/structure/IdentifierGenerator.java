/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

/**
 * @author matus.marko
 */
public class IdentifierGenerator {

    /**
     * Value for internal counter.
     */
    private volatile int nodeId = 0;
    private volatile int linkId = 0;
    private volatile int tpId = 0;
    AtomicIntegerFieldUpdater<IdentifierGenerator> nodeIdUpdater =
            AtomicIntegerFieldUpdater.newUpdater(IdentifierGenerator.class, "nodeId");
    AtomicIntegerFieldUpdater<IdentifierGenerator> linkIdUpdater =
            AtomicIntegerFieldUpdater.newUpdater(IdentifierGenerator.class, "linkId");
    AtomicIntegerFieldUpdater<IdentifierGenerator> tpIdUpdater =
            AtomicIntegerFieldUpdater.newUpdater(IdentifierGenerator.class, "tpId");

    private int getNextNodeId() {
        return nodeIdUpdater.incrementAndGet(this);
    }

    private int getNextLinkId() {
        return linkIdUpdater.incrementAndGet(this);
    }

    private int getNextTpId() {
        return tpIdUpdater.incrementAndGet(this);
    }

    /**
     * Create unique identifier for nodes, links and termination points.
     * @param correlationItem Type of the Item to generate ID for
     * @return unique identifier
     */
    public String getNextIdentifier(CorrelationItemEnum correlationItem) {
        String identifier = null;
        switch (correlationItem) {
            case Node:
                identifier = "node:" + getNextNodeId();
                break;
            case Link:
                identifier = "link:" + getNextLinkId();
                break;
            case TerminationPoint:
                identifier = "tp:" + getNextTpId();
                break;
            default:
                throw new IllegalStateException("Unknown Correlation item used: " + correlationItem);
        }
        return identifier;
    }

}
