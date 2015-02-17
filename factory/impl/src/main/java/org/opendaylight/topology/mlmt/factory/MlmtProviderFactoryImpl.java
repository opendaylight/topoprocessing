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
import java.util.HashMap;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.topology.mlmt.utility.MlmtProviderFactory;
import org.opendaylight.topology.multitechnology.MultitechnologyTopologyProvider;
import org.opendaylight.topology.mlmt.parser.MultitechnologyAttributesParserImpl;
import org.slf4j.Logger;

public class MlmtProviderFactoryImpl implements MlmtProviderFactory {

    @Override
    public HashMap<String, List<MlmtTopologyProvider>> createProvidersMap(DataBroker dataBroker,
            final Logger logger, MlmtOperationProcessor processor, String mlmtTopologyName) {
        try {
            final TopologyId tid = new TopologyId(mlmtTopologyName);
            final TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
            final InstanceIdentifier<Topology> MLMT_TOPOLOGY_IID = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
            HashMap<String, List<MlmtTopologyProvider>> map = new HashMap();
            ArrayList<MlmtTopologyProvider> lProvider = new ArrayList<MlmtTopologyProvider>();
            MultitechnologyAttributesParserImpl multitechnologyAttributesParser = new MultitechnologyAttributesParserImpl();
            multitechnologyAttributesParser.init(logger);
            MultitechnologyTopologyProvider multitechnologyTopologyProvider = new MultitechnologyTopologyProvider();
            multitechnologyTopologyProvider.init(logger, processor, MLMT_TOPOLOGY_IID, multitechnologyAttributesParser);
            multitechnologyTopologyProvider.setDataProvider(dataBroker);
            lProvider.add(multitechnologyTopologyProvider);
            map.put(mlmtTopologyName, lProvider);

            return map;

        } catch (final NullPointerException e) {
            logger.error("MlmtProviderFactoryImpl.HashMap NullPointerException", e);
            return null;
        }
    }
}

