/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator.filtratorFactory;

import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtrator.RangeNumberFiltrator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.RangeNumberFilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.range.number.filter.type.RangeNumberFilter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
public class RangeNumberFiltratorFactory implements FiltratorFactory {

    @Override
    public Filtrator createFiltrator(Filter filter, YangInstanceIdentifier pathIdentifier) {
        if (filter.getFilterTypeBody() instanceof RangeNumberFilterType) {
            RangeNumberFilter RNFilter = ((RangeNumberFilterType)filter.getFilterTypeBody()).getRangeNumberFilter();
            return new RangeNumberFiltrator(RNFilter.getMinNumberValue(), RNFilter.getMaxNumberValue(), pathIdentifier);
        } else {
            throw new IllegalStateException("Wrong filter type and body combination");
        }
    }
}
