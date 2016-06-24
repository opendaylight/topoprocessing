/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator.filtratorFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.impl.operator.filtrator.Ipv4AddressFiltrator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.FilterTypeBody;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.Ipv4AddressFilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.ipv4.address.filter.type.Ipv4AddressFilter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class Ipv4FiltratorFactoryTest {

    private Ipv4FiltratorFactory factory;
    private Filter filterMock;
    private YangInstanceIdentifier identifierMock;
    private FilterTypeBody filterTypeBodyMock;
    private Ipv4AddressFilterType ipv4AdressFilterTypeMock;
    private Ipv4AddressFilter ipv4AdressFilterMock;
    private IpPrefix ipPrefixMock;
    private Ipv4Prefix ipv4PrefixMock;

    @Before
    public void before() {
        factory = new Ipv4FiltratorFactory();
        filterMock = mock(Filter.class);
        identifierMock = mock(YangInstanceIdentifier.class);
        filterTypeBodyMock = mock(FilterTypeBody.class);
        ipv4AdressFilterTypeMock = mock(Ipv4AddressFilterType.class);
        ipv4AdressFilterMock = mock(Ipv4AddressFilter.class);
        ipPrefixMock = mock(IpPrefix.class);
        ipv4PrefixMock = mock(Ipv4Prefix.class);

        when(filterMock.getFilterTypeBody()).thenReturn(ipv4AdressFilterTypeMock);
        when(ipv4AdressFilterTypeMock.getIpv4AddressFilter()).thenReturn(ipv4AdressFilterMock);
        when(ipv4AdressFilterMock.getIpv4Address()).thenReturn(ipPrefixMock);
        when(ipPrefixMock.getIpv4Prefix()).thenReturn(ipv4PrefixMock);
        when(ipv4PrefixMock.getValue()).thenReturn("1/1");
    }

    @Test
    public void testCreateFiltrator() {
        Filtrator filtrator = factory.createFiltrator(filterMock, identifierMock);
        Assert.assertTrue(filtrator instanceof Ipv4AddressFiltrator);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFiltratorExceptionTest() {
        when(filterMock.getFilterTypeBody()).thenReturn(filterTypeBodyMock);
        factory.createFiltrator(filterMock, identifierMock);
    }
}
