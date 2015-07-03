/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.api.filtration;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.filtration._case.filtration.Filter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
public interface FiltratorFactory {

    /**
     * Creates filtrator for the particular filter.
     * @param filter - filter on which the filtrator will be created
     * @param pathIdentifier - path to target-field
     * @return filtrator
     */
    Filtrator createFiltrator(Filter filter, YangInstanceIdentifier pathIdentifier);
}
