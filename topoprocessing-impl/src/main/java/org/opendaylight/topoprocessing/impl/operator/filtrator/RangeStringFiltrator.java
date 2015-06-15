/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matus.marko
 */
public class RangeStringFiltrator implements Filtrator {

    private static final Logger LOG = LoggerFactory.getLogger(RangeStringFiltrator.class);

    protected final String min;
    protected final String max;
    protected final YangInstanceIdentifier pathIdentifier;

    protected RangeStringFiltrator(String min, String max, YangInstanceIdentifier pathIdentifier) {
        Preconditions.checkNotNull(min, "Filtering min value can't be null");
        Preconditions.checkNotNull(max, "Filtering max value can't be null");
        Preconditions.checkNotNull(pathIdentifier, "PathIdentifier can't be null");
        this.min = min;
        this.max = max;
        this.pathIdentifier = pathIdentifier;
    }

    @Override
    public boolean isFiltered(PhysicalNode node) {
        Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(node.getNode(), pathIdentifier);
        if (leafNode.isPresent()) {
            String value = (String) ((LeafNode) leafNode.get()).getValue();
            if (0 <= value.compareTo(this.min) && 0 >= value.compareTo(this.max)) {
                return false;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node with value {} was filtered out", node.getNode());
        }
        return true;
    }
}
