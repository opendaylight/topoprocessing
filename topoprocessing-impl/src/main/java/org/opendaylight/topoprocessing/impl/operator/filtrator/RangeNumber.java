/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.filtration.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
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
public class RangeNumber implements Filtrator {

    private static final Logger LOG = LoggerFactory.getLogger(RangeNumber.class);

    protected final int min;
    protected final int max;
    protected final YangInstanceIdentifier pathIdentifier;

    public RangeNumber(int min, int max, YangInstanceIdentifier pathIdentifier) {
        Preconditions.checkNotNull(min, "Filtering min value can't be null");
        Preconditions.checkNotNull(max, "Filtering max value can't be null");
        Preconditions.checkNotNull(pathIdentifier, "PathIdentifier can't be null");
        this.min = min;
        this.max = max;
        this.pathIdentifier = pathIdentifier;
    }

    @Override
    public boolean isFiltered(UnderlayItem node) {
        Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(node.getNode(), pathIdentifier);
        if (leafNode.isPresent()) {
            int value = (int) ((LeafNode) leafNode.get()).getValue();
            if (this.min <= value && this.max >= value) {
                return false;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node with value {} was filtered out", node.getNode());
        }
        return true;
    }
}
