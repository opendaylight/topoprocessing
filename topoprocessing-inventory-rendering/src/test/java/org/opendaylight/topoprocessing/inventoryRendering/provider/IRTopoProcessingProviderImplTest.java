/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.inventoryRendering.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.opendaylight.topoprocessing.inventoryRendering.adapter.IRModelAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.InventoryRenderingModel;

/**
 * @author matej.perina
 */
@RunWith(MockitoJUnitRunner.class)
public class IRTopoProcessingProviderImplTest {

    @Mock
    private TopoProcessingProviderImpl mockTopoProcessingProvider;

    @Test
    public void testStartup() throws Exception {
        TopoProcessingProviderIR irProvider = new TopoProcessingProviderIR();
        irProvider.startup(mockTopoProcessingProvider);
        Mockito.verify(mockTopoProcessingProvider).registerModelAdapter(Mockito.eq(InventoryRenderingModel.class),
                Mockito.any(IRModelAdapter.class));
    }
}
