/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
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
 * @param <T> filtration type
 */
public class SpecificValueFiltrator<T> implements Filtrator {

    private static final Logger LOG = LoggerFactory.getLogger(SpecificValueFiltrator.class);
    private final YangInstanceIdentifier pathIdentifier;
    private final T value;

    /**
     * Constructor
     * @param value used for filtering
     * @param pathIdentifier defines path to {@link NormalizedNode}, which contains value that will be used for filterin
     */
    public SpecificValueFiltrator(T value, YangInstanceIdentifier pathIdentifier) {
        Preconditions.checkNotNull(value, "Filtering value can't be null");
        Preconditions.checkNotNull(pathIdentifier, "PathIdentifier can't be null");
        this.pathIdentifier = pathIdentifier;
        this.value = value;
    }

    @Override
    public boolean isFiltered(UnderlayItem node) {
        Optional<NormalizedNode<?, ?>> leafNode = NormalizedNodes.findNode(node.getItem(), pathIdentifier);
        if (leafNode.isPresent()) {
            T value = (T) ((LeafNode) leafNode.get()).getValue();
            if (this.value.equals(value)) {
                return false;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node with value {} was filtered out", node.getItem());
        }
        return true;
    }
}
