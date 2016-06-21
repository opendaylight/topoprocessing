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
import org.opendaylight.topoprocessing.impl.operator.filtrator.Ipv6AddressFiltrator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.FilterTypeBody;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.Ipv6AddressFilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.ipv6.address.filter.type.Ipv6AddressFilter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class Ipv6FiltratorFactoryTest {

    private Ipv6FiltratorFactory factory;
    private Filter filterMock;
    private YangInstanceIdentifier identifierMock;
    private FilterTypeBody filterTypeBodyMock;
    private Ipv6AddressFilterType ipv6AdressFilterTypeMock;
    private Ipv6AddressFilter ipv6AdressFilterMock;
    private IpPrefix ipPrefixMock;
    private Ipv6Prefix ipv6PrefixMock;

    @Before
    public void before() {
        factory = new Ipv6FiltratorFactory();
        filterMock = mock(Filter.class);
        identifierMock = mock(YangInstanceIdentifier.class);
        filterTypeBodyMock = mock(FilterTypeBody.class);
        ipv6AdressFilterTypeMock = mock(Ipv6AddressFilterType.class);
        ipv6AdressFilterMock = mock(Ipv6AddressFilter.class);
        ipPrefixMock = mock(IpPrefix.class);
        ipv6PrefixMock = mock(Ipv6Prefix.class);

        when(filterMock.getFilterTypeBody()).thenReturn(ipv6AdressFilterTypeMock);
        when(ipv6AdressFilterTypeMock.getIpv6AddressFilter()).thenReturn(ipv6AdressFilterMock);
        when(ipv6AdressFilterMock.getIpv6Address()).thenReturn(ipPrefixMock);
        when(ipPrefixMock.getIpv6Prefix()).thenReturn(ipv6PrefixMock);
        when(ipv6PrefixMock.getValue()).thenReturn("1/1");
    }

    @Test
    public void testCreateFiltrator() {
        Filtrator filtrator = factory.createFiltrator(filterMock, identifierMock);
        Assert.assertTrue(filtrator instanceof Ipv6AddressFiltrator);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFiltratorExceptionTest() {
        when(filterMock.getFilterTypeBody()).thenReturn(filterTypeBodyMock);
        factory.createFiltrator(filterMock, identifierMock);
    }
}