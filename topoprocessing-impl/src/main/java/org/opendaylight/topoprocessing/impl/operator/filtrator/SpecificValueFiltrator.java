/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import java.math.BigDecimal;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 * @param <T> filtration type
 */
public class SpecificValueFiltrator<T> extends AbstractFiltrator {

    private static final Logger LOG = LoggerFactory.getLogger(SpecificValueFiltrator.class);
    private final T value;

    /**
     * Constructor
     * @param value used for filtering
     * @param pathIdentifier defines path to {@link NormalizedNode}, which contains value that will be used for filtering
     */
    public SpecificValueFiltrator(T value, YangInstanceIdentifier pathIdentifier) {
        super(pathIdentifier);
        Preconditions.checkNotNull(value, "Filtering value can't be null");
        this.value = value;
    }

    @Override
    public boolean isFiltered(NormalizedNode<?, ?> node) {
        T value = (T) node.getValue();
        boolean isFiltered;
        if (value instanceof String && this.value instanceof String)  {
            isFiltered =  !this.value.equals(value);
        } else if(value instanceof Number && this.value instanceof Number){
            isFiltered =  !numberEquals(value, this.value);
        } else {
            isFiltered = true;
            LOG.warn("Comparing values of these types is not supported: {}; {}", value.getClass().getName(),
                            this.value.getClass().getName());
        }
        if (LOG.isDebugEnabled() && isFiltered) {
            LOG.debug("Node with value {} was filtered out", node);
        }
        return isFiltered;
    }

    private BigDecimal createBigDecimal(Number value) {
        BigDecimal result = null;
        if (value instanceof Short) {
            result = BigDecimal.valueOf(value.shortValue());
        } else if (value instanceof Long) {
            result = BigDecimal.valueOf(value.longValue());
        } else if (value instanceof Float) {
            result = BigDecimal.valueOf(value.floatValue());
        } else if (value instanceof Double) {
            result = BigDecimal.valueOf(value.doubleValue());
        } else if (value instanceof Integer) {
            result = BigDecimal.valueOf(value.intValue());
        } else {
            LOG.warn("Unsupported Number subtype: {}", value.getClass().getName());
        }

        return result;
    }

    private boolean numberEquals(T num1, T num2) {
        BigDecimal bigDecimal1 = createBigDecimal((Number) num1);
        BigDecimal bigDecimal2 = createBigDecimal((Number) num2);
        if(bigDecimal1 == null || bigDecimal2 == null) {
            return false;
        }
        return bigDecimal1.compareTo(bigDecimal2) == 0;
    }
}
