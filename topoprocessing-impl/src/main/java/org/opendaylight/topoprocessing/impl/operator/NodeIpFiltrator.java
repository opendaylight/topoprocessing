/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 */
public class NodeIpFiltrator {

    private static final Logger LOG = LoggerFactory.getLogger(NodeIpFiltrator.class);

    private IpPrefix value;
    private YangInstanceIdentifier pathIdentifier;

    /**
     * Constructor
     * @param value
     * @param pathIdentifier
     */
    public NodeIpFiltrator(IpPrefix value, YangInstanceIdentifier pathIdentifier) {
        Preconditions.checkNotNull(value, "Filtering value can't be null");
        Preconditions.checkNotNull(pathIdentifier, "PathIdentifier can't be null");
        this.value = value;
        this.pathIdentifier = pathIdentifier;
    }

    /**
     * Filters {@link PhysicalNode}
     * @param node {@link PhysicalNode} to be filtered
     * @return true if node was filtered out false otherwise
     */
    public boolean isFiltered(PhysicalNode node) {
        Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(node.getNode(), pathIdentifier);
        if (leafNode.isPresent()) {
            if (value.equals(((LeafNode<?>) leafNode.get()).getValue())) {
                return false;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node with value " + node.getNode() + " was filtered out");
        }
        return true;
    }
}
