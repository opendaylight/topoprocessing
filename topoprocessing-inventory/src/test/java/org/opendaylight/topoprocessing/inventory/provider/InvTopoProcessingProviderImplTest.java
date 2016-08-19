/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventory.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.opendaylight.topoprocessing.inventory.adapter.InvModelAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class InvTopoProcessingProviderImplTest {

    @Mock
    private TopoProcessingProviderImpl mockTopoProcessingProvider;

    @Test
    public void testStartup() {
        TopoProcessingProviderInv invProvider = new TopoProcessingProviderInv();
        invProvider.startup(mockTopoProcessingProvider);
        Mockito.verify(mockTopoProcessingProvider).registerModelAdapter(Mockito.eq(OpendaylightInventoryModel.class),
                Mockito.any(InvModelAdapter.class));
    }
}
