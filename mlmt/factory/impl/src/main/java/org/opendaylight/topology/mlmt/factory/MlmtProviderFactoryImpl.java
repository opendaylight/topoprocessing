/*
 * Copyright (c)2014 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.factory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.topology.mlmt.utility.MlmtProviderFactory;
import org.opendaylight.topology.multitechnology.MultitechnologyTopologyProvider;
import org.opendaylight.topology.multilayer.MultilayerTopologyProvider;
import org.opendaylight.topology.mlmt.parser.MultitechnologyAttributesParserImpl;
import org.opendaylight.topology.mlmt.inventory.InventoryTopologyProvider;
import org.opendaylight.topology.mlmt.parser.InventoryAttributesParserImpl;
import org.opendaylight.topology.mlmt.parser.MultilayerAttributesParserImpl;
import org.slf4j.Logger;

public class MlmtProviderFactoryImpl implements MlmtProviderFactory {

    @Override
    public Map<String, List<MlmtTopologyProvider>> createProvidersMap(final ProviderContext session,
            final DataBroker dataBroker, final Logger logger, MlmtOperationProcessor processor, String mlmtTopologyName) {
        final TopologyId tid = new TopologyId(mlmtTopologyName);
        final TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        final InstanceIdentifier<Topology> mlmtTopologyId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
        Map<String, List<MlmtTopologyProvider>> map = new HashMap(2);
        List<MlmtTopologyProvider> lProvider = new ArrayList<MlmtTopologyProvider>();
        /*
         * creating and adding inventory topology provider
         */
        InventoryAttributesParserImpl inventoryAttributesParser = new InventoryAttributesParserImpl();
        inventoryAttributesParser.init(logger);
        InventoryTopologyProvider inventoryTopologyProvider = new InventoryTopologyProvider();
        inventoryTopologyProvider.init(logger, processor, mlmtTopologyId, inventoryAttributesParser);
        inventoryTopologyProvider.setDataProvider(dataBroker);
        lProvider.add(inventoryTopologyProvider);
        /*
         * creating and adding multitechnology provider
         */
        MultitechnologyAttributesParserImpl multitechnologyAttributesParser = new MultitechnologyAttributesParserImpl();
        multitechnologyAttributesParser.init(logger);
        MultitechnologyTopologyProvider multitechnologyTopologyProvider = new MultitechnologyTopologyProvider();
        multitechnologyTopologyProvider.init(logger, processor, mlmtTopologyId, multitechnologyAttributesParser);
        multitechnologyTopologyProvider.setDataProvider(dataBroker);
        lProvider.add(multitechnologyTopologyProvider);
        /*
         * creating and adding multilayer provider
         */
        MultilayerAttributesParserImpl multilayerAttributesParser = new MultilayerAttributesParserImpl();
        multilayerAttributesParser.init(logger);
        MultilayerTopologyProvider multilayerTopologyProvider = new MultilayerTopologyProvider();
        multilayerTopologyProvider.init(logger, processor, mlmtTopologyId, multilayerAttributesParser);
        multilayerTopologyProvider.setDataProvider(dataBroker);
        multilayerTopologyProvider.registerRpcImpl(session);
        lProvider.add(multilayerTopologyProvider);
        /*
         * topologyname and related providers mapping configuration
         */
        map.put(mlmtTopologyName, lProvider);

        return map;
    }
}

