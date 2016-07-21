/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multilayer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjacencyAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ForwardingAdjacencyTopologyTest implements MultilayerForwardingAdjacency {
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> destTopologyId;

    public void init(MlmtOperationProcessor processor, InstanceIdentifier<Topology> destTopologyId) {
        this.destTopologyId = destTopologyId;
        this.processor = processor;
    }

    public void setDataProvider(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
    }

    private void createTopologyType(final LogicalDatastoreType type,
             final InstanceIdentifier<Topology> topologyInstanceId) {
    }

    @Override
    public void onForwardingAdjacencyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final ForwardingAdjacencyAttributes faAttributes) {
    }

    @Override
    public void onForwardingAdjacencyUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final ForwardingAdjacencyAttributes faAttributes) {
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final LinkKey linkKey) {
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final NodeKey nodeKey, final TerminationPointKey tpKey) {
    }

    @Override
    public void onForwardingAdjacencyDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final FaId faId) {
    }

    @Override
    public void onLinkDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey) {
    }

    @Override
    public void onTpDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPointKey tpKey) {
    }
}
