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
import org.opendaylight.topoprocessing.impl.operator.filtrator.Ipv4AddressFiltrator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv4AddressAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
public class Ipv4FiltratorFactory implements FiltratorFactory {

    @Override
    public Filtrator createFiltrator(Filter filter, YangInstanceIdentifier pathIdentifier) {
        return new Ipv4AddressFiltrator(filter.getAugmentation(Ipv4AddressAugment.class).getIpv4Address(), pathIdentifier);
    }
}
