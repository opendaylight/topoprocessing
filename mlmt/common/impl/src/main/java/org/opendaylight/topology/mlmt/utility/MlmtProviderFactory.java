/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.slf4j.Logger;

public interface MlmtProviderFactory {

    Map<String, List<MlmtTopologyProvider>> createProvidersMap(final RpcProviderRegistry session,
            final DataBroker dataBroker, final Logger logger, MlmtOperationProcessor processor, String mlmtTopologyName);

    boolean isBuildingTopologyType(TopologyTypes topologyType);
}

