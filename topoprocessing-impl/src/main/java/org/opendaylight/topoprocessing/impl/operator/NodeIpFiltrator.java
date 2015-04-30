/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.node.ip.filtration._case.node.ip.filtration.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matus.marko
 */
public class NodeIpFiltrator {

    private static final Logger LOG = LoggerFactory.getLogger(NodeIpFiltrator.class);

    private Filter filter;

    /**
     * Constructor
     * @param filter object
     */
    public NodeIpFiltrator(Filter filter) {
        this.filter = filter;
    }

    /**
     * Filters {@link PhysicalNode}
     * @param node {@link PhysicalNode} to be filtered
     * @return true if node was filtered out false otherwise
     */
    public boolean isFiltered(PhysicalNode node) {
        if (filter.getValue().getValue().equals(node.getLeafNode().getValue())) {
            return false;
        }
        LOG.debug("Node with value " + node.getLeafNode().getValue() + " was filtered out");
        return true;
    }
}
