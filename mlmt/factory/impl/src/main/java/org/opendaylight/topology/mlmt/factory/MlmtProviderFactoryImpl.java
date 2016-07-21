/*
 * Copyright (c) 2014 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.factory;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.topology.forwarding.adjacency.ForwardingAdjacencyTopologyProvider;
import org.opendaylight.topology.mlmt.inventory.InventoryTopologyProvider;
import org.opendaylight.topology.mlmt.parser.InventoryAttributesParserImpl;
import org.opendaylight.topology.mlmt.parser.MultilayerAttributesParserImpl;
import org.opendaylight.topology.mlmt.parser.MultitechnologyAttributesParserImpl;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtTopologyOpaqueAttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtTopologyOpaqueAttributeTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.multitechnology.topology.opaque.attribute.type.MultitechnologyOpaqueAttributeTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MlmtProviderFactoryImpl implements MlmtProviderFactory {

    @Override
    public Map<String, List<MlmtTopologyProvider>> createProvidersMap(final RpcProviderRegistry rpcProviderRegistry,
            final DataBroker dataBroker, final MlmtOperationProcessor processor,
                    final String mlmtTopologyName) {
        final TopologyId tid = new TopologyId(mlmtTopologyName);
        final TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        final InstanceIdentifier<Topology> mlmtTopologyId =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
        Map<String, List<MlmtTopologyProvider>> map = new HashMap();
        List<MlmtTopologyProvider> lProvider = new ArrayList<MlmtTopologyProvider>();
        /*
         * creating and adding inventory topology provider
         */
        InventoryAttributesParserImpl inventoryAttributesParser = new InventoryAttributesParserImpl();
        InventoryTopologyProvider inventoryTopologyProvider = new InventoryTopologyProvider();
        inventoryTopologyProvider.init(processor, mlmtTopologyId, inventoryAttributesParser);
        inventoryTopologyProvider.setDataProvider(dataBroker);
        lProvider.add(inventoryTopologyProvider);
        /*
         * creating and adding multitechnology provider
         */
        MultitechnologyAttributesParserImpl multitechnologyAttributesParser =
                new MultitechnologyAttributesParserImpl();
        MultitechnologyTopologyProvider multitechnologyTopologyProvider = new MultitechnologyTopologyProvider();
        multitechnologyTopologyProvider.init(processor, mlmtTopologyId, multitechnologyAttributesParser);
        multitechnologyTopologyProvider.setDataProvider(dataBroker);
        lProvider.add(multitechnologyTopologyProvider);
        /*
         * creating and adding forwarding adjacency provider
         */
        ForwardingAdjacencyTopologyProvider forwardingAdjacencyTopologyProvider =
                new ForwardingAdjacencyTopologyProvider();
        forwardingAdjacencyTopologyProvider.init(processor, mlmtTopologyId);
        forwardingAdjacencyTopologyProvider.setDataProvider(dataBroker);
        /*
         * creating and adding multilayer provider
         */
        MultilayerAttributesParserImpl multilayerAttributesParser = new MultilayerAttributesParserImpl();
        multilayerAttributesParser.init();
        MultilayerTopologyProvider multilayerTopologyProvider = new MultilayerTopologyProvider();
        multilayerTopologyProvider.init(processor, mlmtTopologyId, multilayerAttributesParser,
                forwardingAdjacencyTopologyProvider);
        multilayerTopologyProvider.setDataProvider(dataBroker);
        multilayerTopologyProvider.registerRpcImpl(rpcProviderRegistry, mlmtTopologyId);
        lProvider.add(multilayerTopologyProvider);
        /*
         * topologyname and related providers mapping configuration
         */
        map.put(mlmtTopologyName, lProvider);

        return map;
    }

    @Override
    public MlmtConsequentAction consequentAction(TopologyTypes topologyType) {
        MtTopologyType mtTopologyType = topologyType.getAugmentation(MtTopologyType.class);
        if (mtTopologyType != null) {
            return MlmtConsequentAction.COPY;
        }

        return MlmtConsequentAction.BUILD;
    }

    @Override
    public TopologyTypesBuilder configTopologyTypes() {
        final ForwardingAdjacencyTopologyBuilder forwardingAdjacencyTopologyBuilder =
                new ForwardingAdjacencyTopologyBuilder();
        final FaTopologyTypeBuilder faTopologyTypeBuilder = new FaTopologyTypeBuilder();
        faTopologyTypeBuilder.setForwardingAdjacencyTopology(forwardingAdjacencyTopologyBuilder.build());

        final MultilayerTopologyBuilder multilayerTopologyBuilder = new MultilayerTopologyBuilder();
        multilayerTopologyBuilder.addAugmentation(FaTopologyType.class, faTopologyTypeBuilder.build());
        final MlTopologyTypeBuilder mlTopologyTypeBuilder = new MlTopologyTypeBuilder();
        mlTopologyTypeBuilder.setMultilayerTopology(multilayerTopologyBuilder.build());

        final MultitechnologyOpaqueAttributeTopologyBuilder multitechnologyOpaqueAttributeTopologyBuilder =
                new MultitechnologyOpaqueAttributeTopologyBuilder();
        final MtTopologyOpaqueAttributeTypeBuilder mtTopologyOpaqueAttributeTypeBuilder =
                new MtTopologyOpaqueAttributeTypeBuilder();
        mtTopologyOpaqueAttributeTypeBuilder.setMultitechnologyOpaqueAttributeTopology(
                multitechnologyOpaqueAttributeTopologyBuilder.build());

        final MultitechnologyTopologyBuilder multitechnologyTopologyBuilder =
                new MultitechnologyTopologyBuilder();
        multitechnologyTopologyBuilder.addAugmentation(MtTopologyOpaqueAttributeType.class,
                mtTopologyOpaqueAttributeTypeBuilder.build());
        multitechnologyTopologyBuilder.addAugmentation(MlTopologyType.class, mlTopologyTypeBuilder.build());

        final MtTopologyTypeBuilder mtTopologyTypeBuilder = new MtTopologyTypeBuilder();
        mtTopologyTypeBuilder.setMultitechnologyTopology(multitechnologyTopologyBuilder.build());

        MtTopologyType mtTopologyType = mtTopologyTypeBuilder.build();
        TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
        topologyTypesBuilder.addAugmentation(MtTopologyType.class, mtTopologyType);

        return topologyTypesBuilder;
    }
}
