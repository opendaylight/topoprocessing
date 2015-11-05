/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator.filtratorFactory;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.impl.operator.filtrator.SpecificValueFiltrator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.FilterTypeBody;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.SpecificStringFilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.filter.filter.type.body.specific.string.filter.type.SpecificStringFilter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author samuel.kontris
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SpecificStringFiltratorFactoryTest {

    private SpecificStringFiltratorFactory factory;
    private Filter filterMock;
    private YangInstanceIdentifier identifierMock;
    private SpecificStringFilterType specificStringFilterTypeMock;
    private FilterTypeBody filterTypeBodyMock;
    private SpecificStringFilter specificStringFilterMock;

    @Before
    public void before() {
        factory = new SpecificStringFiltratorFactory();
        filterMock = mock(Filter.class);
        identifierMock = mock(YangInstanceIdentifier.class);
        specificStringFilterTypeMock = mock(SpecificStringFilterType.class);
        filterTypeBodyMock = mock(FilterTypeBody.class);
        specificStringFilterMock = mock(SpecificStringFilter.class);

        when(filterMock.getFilterTypeBody()).thenReturn(specificStringFilterTypeMock);
        when(specificStringFilterTypeMock.getSpecificStringFilter()).thenReturn(specificStringFilterMock);
        when(specificStringFilterMock.getSpecificString()).thenReturn("specific string");
    }

    @Test
    public void testCreateFiltrator() {
        Filtrator filtrator = factory.createFiltrator(filterMock, identifierMock);
        assertTrue(filtrator instanceof SpecificValueFiltrator);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFiltratorExceptionTest() {
        when(filterMock.getFilterTypeBody()).thenReturn(filterTypeBodyMock);
        factory.createFiltrator(filterMock, identifierMock);
    }
}