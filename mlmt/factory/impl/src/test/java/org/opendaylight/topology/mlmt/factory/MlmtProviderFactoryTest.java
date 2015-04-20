/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.factory;

import java.util.List;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.topology.mlmt.utility.MlmtProviderFactory;
import org.opendaylight.topology.mlmt.inventory.InventoryTopologyProvider;
import org.opendaylight.topology.multitechnology.MultitechnologyTopologyProvider;
import org.opendaylight.topology.multilayer.MultilayerTopologyProvider;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class MlmtProviderFactoryTest extends AbstractDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtProviderFactoryTest.class);
    private static final String MLMT1 = "mlmt:1";
    private MlmtOperationProcessor processor;
    private MlmtProviderFactory providerFactory;

    public class MlmtRpcProviderRegistryMock implements RpcProviderRegistry {

        public MlmtRpcProviderRegistryMock() { }

        @Override
        public <T extends RpcService> BindingAwareBroker.RpcRegistration<T> addRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(L listener) {
            return Mockito.mock(ListenerRegistration.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
            return null;
        }
    }

    @Override
    protected void setupWithDataBroker(DataBroker dataBroker) {
        // NOOP
    }

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        this.processor = new MlmtOperationProcessor(getDataBroker());
        this.providerFactory = new MlmtProviderFactoryImpl();
    }

    @Test
    public void testCreateProviderMap() throws Exception {
		MlmtRpcProviderRegistryMock rpcRegistry = new MlmtRpcProviderRegistryMock();
        Map<String, List<MlmtTopologyProvider>> providerMap = providerFactory.createProvidersMap(
                rpcRegistry, getDataBroker(), LOG, processor, MLMT1);

        List<MlmtTopologyProvider> lProvider = providerMap.get(MLMT1);
        assertNotNull(lProvider);

        boolean bInventoryTopologyProvider = false;
        boolean bMultitechnologyTopologyProvider = false;
        boolean bMultilayerTopologyProvider = false;

        for (MlmtTopologyProvider provider : lProvider) {
            assertNotNull(provider);
            if (provider instanceof InventoryTopologyProvider) {
                bInventoryTopologyProvider = true;
            }
            else if (provider instanceof MultitechnologyTopologyProvider) {
                bMultitechnologyTopologyProvider = true;
            }
            else if (provider instanceof MultilayerTopologyProvider) {
               bMultilayerTopologyProvider = true;
            }
        }

        boolean b = bInventoryTopologyProvider & bMultitechnologyTopologyProvider & bMultilayerTopologyProvider;
        assertTrue(b);
    }

    @After
    public void clear() {
        // NOOP
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }
}