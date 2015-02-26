/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.multilayer;

import java.util.Iterator;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
//import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
//import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
//import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;

public class ForwardingAdjacencyTopologyProvider implements
            AutoCloseable, MlmtTopologyProvider {
    private static Logger LOG;
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> DEST_TOPOLOGY_IID;

    public void init(final Logger logger, MlmtOperationProcessor theProcessor, InstanceIdentifier<Topology> destTopologyId) {
       LOG = logger;
       DEST_TOPOLOGY_IID = destTopologyId;
       processor = theProcessor;
    }

    public void setDataProvider(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
                                  final InstanceIdentifier<Topology> topologyInstanceId,
                                  final Topology topology) {
        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;

    }

    @Override
    public void onNodeCreated(final LogicalDatastoreType type,
                              final InstanceIdentifier<Topology> topologyInstanceId,
                              final Node node) {
        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type,
                            final InstanceIdentifier<Topology> topologyInstanceId,
                            final NodeKey nodeKey,
                            final TerminationPoint tp) {
       final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type,
                             final InstanceIdentifier<Topology> topologyInstanceId,
                             final Link link) {
        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
    }


    @Override
    public void onTopologyUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
    }

    @Override
    public void onNodeUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
    }

    @Override
    public void onTpUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPoint tp) {
    }

    @Override
    public void onLinkUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
    }

  @Override
       public void onTopologyDeleted(final LogicalDatastoreType type,
               final InstanceIdentifier<Topology> topologyInstanceId) {}

    @Override
    public void onNodeDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey) {

    }

    @Override
    public void onTpDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPointKey tpKey) {

    }

    @Override
    public void onLinkDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey) {

    }

    @Override
    public synchronized void close() throws InterruptedException {
        LOG.info("MultilayerTopologyProvider stopped.");
    }

     private void dumpMap(Map<InstanceIdentifier<?>, DataObject> map){
       Iterator<InstanceIdentifier<?>> iter = map.keySet().iterator();
       while(iter.hasNext()){
         InstanceIdentifier<?> iid = iter.next();
         LOG.info("\nKey = " + iid );
         LOG.info("\nValue = " + map.get(iid) + "\n");
       }
     }


}
