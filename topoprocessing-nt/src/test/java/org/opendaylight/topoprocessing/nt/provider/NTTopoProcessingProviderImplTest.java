/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.nt.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.nt.adapter.NTModelAdapter;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class NTTopoProcessingProviderImplTest {

    @Mock
    private TopoProcessingProvider mockTopoProcessingProvider;

    @Test
    public void testStartup() throws Exception {
        TopoProcessingProviderNT NTProvider = new TopoProcessingProviderNT();
        NTProvider.startup(mockTopoProcessingProvider);
        Mockito.verify(mockTopoProcessingProvider).registerModelAdapter(NetworkTopologyModel.class, (NTModelAdapter) Matchers.any());
    }
}
