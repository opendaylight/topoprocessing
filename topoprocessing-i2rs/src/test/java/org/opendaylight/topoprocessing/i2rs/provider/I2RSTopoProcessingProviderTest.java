/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.provider;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.i2rs.adapter.I2RSModelAdapter;
import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;

/**
 * @author andrej.zan
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class I2RSTopoProcessingProviderTest {

    @Mock
    private TopoProcessingProviderImpl mockTopoProcessingProvider;

    @Test
    public void testStartup() throws Exception {
        TopoProcessingProviderI2RS i2rsProvider = new TopoProcessingProviderI2RS();
        i2rsProvider.setTopoProvider(mockTopoProcessingProvider);
        i2rsProvider.startup();
        Mockito.verify(mockTopoProcessingProvider).registerModelAdapter(Mockito.eq(I2rsModel.class),
                Mockito.any(I2RSModelAdapter.class));
    }
}
