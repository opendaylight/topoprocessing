/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.topology.mlmt.inventory.InventoryTopologyProvider;
import org.opendaylight.topology.mlmt.utility.MlmtConsequentAction;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtProviderFactory;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.topology.multilayer.MultilayerTopologyProvider;
import org.opendaylight.topology.multitechnology.MultitechnologyTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.FaTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.FaTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.forwarding.adjacency.topology.type.ForwardingAdjacencyTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.multilayer.topology.type.MultilayerTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

@RunWith(MockitoJUnitRunner.class)
public class MlmtProviderFactoryTest extends AbstractConcurrentDataBrokerTest {

    private static final String MLMT1 = "mlmt:1";
    private MlmtOperationProcessor processor;
    private MlmtProviderFactory providerFactory;

    public class MlmtRpcProviderRegistryMock implements RpcProviderRegistry {

        public MlmtRpcProviderRegistryMock() { }

        @Override
        public <T extends RpcService> BindingAwareBroker.RpcRegistration<T>
                addRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T> addRoutedRpcImplementation(
                Class<T> serviceInterface, T implementation) throws IllegalStateException {
            return Mockito.mock(RoutedRpcRegistration.class);
        }

        @Override
        public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L>
                registerRouteChangeListener(L listener) {
            return Mockito.mock(ListenerRegistration.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
            return null;
        }
    }

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() {
        final DataBroker dataBroker = getDataBroker();
        assertNotNull(dataBroker);
        this.processor = new MlmtOperationProcessor(dataBroker);
        this.providerFactory = new MlmtProviderFactoryImpl();
    }

    @Test
    public void testCreateProviderMap() throws Exception {
        MlmtRpcProviderRegistryMock rpcRegistry = new MlmtRpcProviderRegistryMock();
        Map<String, List<MlmtTopologyProvider>> providerMap = providerFactory.createProvidersMap(
                rpcRegistry, getDataBroker(), processor, MLMT1);

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

        assertTrue(bInventoryTopologyProvider & bMultitechnologyTopologyProvider & bMultilayerTopologyProvider);
    }

    @Test
    public void testConsequentAction() throws Exception {
        final ForwardingAdjacencyTopologyBuilder forwardingAdjacencyTopologyBuilder =
                new ForwardingAdjacencyTopologyBuilder();
        TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
        MlmtConsequentAction consequentAction = this.providerFactory.consequentAction(topologyTypesBuilder.build());
        assertEquals(consequentAction, MlmtConsequentAction.BUILD);

        final FaTopologyTypeBuilder faTopologyTypeBuilder = new FaTopologyTypeBuilder();
        faTopologyTypeBuilder.setForwardingAdjacencyTopology(forwardingAdjacencyTopologyBuilder.build());

        final MultilayerTopologyBuilder multilayerTopologyBuilder = new MultilayerTopologyBuilder();
        multilayerTopologyBuilder.addAugmentation(FaTopologyType.class, faTopologyTypeBuilder.build());
        final MlTopologyTypeBuilder mlTopologyTypeBuilder = new MlTopologyTypeBuilder();
        mlTopologyTypeBuilder.setMultilayerTopology(multilayerTopologyBuilder.build());

        final MultitechnologyTopologyBuilder multitechnologyTopologyBuilder = new MultitechnologyTopologyBuilder();
        multitechnologyTopologyBuilder.addAugmentation(MlTopologyType.class, mlTopologyTypeBuilder.build());

        final MtTopologyTypeBuilder mtTopologyTypeBuilder = new MtTopologyTypeBuilder();
        mtTopologyTypeBuilder.setMultitechnologyTopology(multitechnologyTopologyBuilder.build());

        MtTopologyType mtTopologyType = mtTopologyTypeBuilder.build();
        topologyTypesBuilder.addAugmentation(MtTopologyType.class, mtTopologyType);

        consequentAction = this.providerFactory.consequentAction(topologyTypesBuilder.build());
        assertEquals(consequentAction, MlmtConsequentAction.COPY);

        consequentAction = this.providerFactory.consequentAction(topologyTypesBuilder.build());
        assertEquals(consequentAction, MlmtConsequentAction.COPY);

        consequentAction = this.providerFactory.consequentAction(topologyTypesBuilder.build());
        assertEquals(consequentAction, MlmtConsequentAction.COPY);
    }

    @Test
    public void testconfigTopologyTypes() throws Exception {
        assertNotNull(providerFactory.configTopologyTypes());
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
