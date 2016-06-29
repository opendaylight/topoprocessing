/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 */
public class RangeStringFiltrator extends AbstractFiltrator {

    private static final Logger LOG = LoggerFactory.getLogger(RangeStringFiltrator.class);

    protected final String min;
    protected final String max;

    public RangeStringFiltrator(String min, String max, YangInstanceIdentifier pathIdentifier) {
        super(pathIdentifier);
        Preconditions.checkNotNull(min, "Filtering min value can't be null");
        Preconditions.checkNotNull(max, "Filtering max value can't be null");
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean isFiltered(NormalizedNode<?, ?> node) {
        String value = node.getValue().toString();
        if (0 <= value.compareTo(this.min) && 0 >= value.compareTo(this.max)) {
            return false;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node with value {} was filtered out", node);
        }
        return true;
    }

}
