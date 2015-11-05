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
import org.opendaylight.topoprocessing.impl.operator.filtrator.RangeStringFiltrator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.FilterTypeBody;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.RangeStringFilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.range.string.filter.type.RangeStringFilter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RangeStringFiltratorFactoryTest {

    private RangeStringFiltratorFactory factory;
    private Filter filterMock;
    private YangInstanceIdentifier identifierMock;
    private FilterTypeBody filterTypeBodyMock;
    private RangeStringFilterType rangeStringFilterTypeMock;
    private RangeStringFilter rangeStringFilterMock;

    @Before
    public void before() {
        factory = new RangeStringFiltratorFactory();
        filterMock = mock(Filter.class);
        identifierMock = mock(YangInstanceIdentifier.class);
        filterTypeBodyMock = mock(FilterTypeBody.class);
        rangeStringFilterTypeMock = mock(RangeStringFilterType.class);
        rangeStringFilterMock = mock(RangeStringFilter.class);

        when(filterMock.getFilterTypeBody()).thenReturn(rangeStringFilterTypeMock);
        when(rangeStringFilterTypeMock.getRangeStringFilter()).thenReturn(rangeStringFilterMock);
        when(rangeStringFilterMock.getMinStringValue()).thenReturn("min val");
        when(rangeStringFilterMock.getMaxStringValue()).thenReturn("max val");
    }

    @Test
    public void testCreateFiltrator() {
        Filtrator filtrator = factory.createFiltrator(filterMock, identifierMock);
        Assert.assertTrue(filtrator instanceof RangeStringFiltrator);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFiltratorExceptionTest() {
        when(filterMock.getFilterTypeBody()).thenReturn(filterTypeBodyMock);
        factory.createFiltrator(filterMock, identifierMock);
    }
}
