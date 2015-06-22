/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator.filtrator;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.Ipv4FiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.Ipv6FiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.RangeNumberFiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.RangeStringFiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.SpecificNumberFiltratorFactory;
import org.opendaylight.topoprocessing.impl.operator.filtratorFactory.SpecificStringFiltratorFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.RangeNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.RangeString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.SpecificNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.SpecificString;

/**
 * @author martin.uhlir
 *
 */
public class DefaultFiltrators {

    public static Map<Class<? extends FilterBase>, FiltratorFactory> registerDefaultFiltrators() {
        Map<Class<? extends FilterBase>, FiltratorFactory> filtrators = new HashMap<>();
        filtrators.put(Ipv4Address.class, new Ipv4FiltratorFactory());
        filtrators.put(Ipv6Address.class, new Ipv6FiltratorFactory());
        filtrators.put(RangeNumber.class, new RangeNumberFiltratorFactory());
        filtrators.put(RangeString.class, new RangeStringFiltratorFactory());
        filtrators.put(SpecificString.class, new SpecificStringFiltratorFactory());
        filtrators.put(SpecificNumber.class, new SpecificNumberFiltratorFactory());
        return filtrators;
    }

}
