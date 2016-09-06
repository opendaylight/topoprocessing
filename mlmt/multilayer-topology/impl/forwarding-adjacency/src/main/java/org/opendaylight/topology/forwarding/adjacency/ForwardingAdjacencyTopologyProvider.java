/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.forwarding.adjacency;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.multilayer.MultilayerForwardingAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.FaTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.FaTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.FaTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.MlLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.MlLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.MlTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.MlTerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.forwarding.adjacency.topology.type.ForwardingAdjacencyTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.ml.link.attributes.SupportingFa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.ml.link.attributes.SupportingFaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.ml.link.attributes.SupportingFaKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.network.topology.topology.ForwardingAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.network.topology.topology.ForwardingAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123.network.topology.topology.ForwardingAdjacencyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjacencyAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.multilayer.topology.type.MultilayerTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardingAdjacencyTopologyProvider implements AutoCloseable, MultilayerForwardingAdjacency {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardingAdjacencyTopologyProvider.class);
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> destTopologyId;

    public void init(MlmtOperationProcessor processor, InstanceIdentifier<Topology> destTopologyId) {
        LOG.info("ForwardingAdjacencyTopologyProvider.init");
        this.destTopologyId = destTopologyId;
        this.processor = processor;
    }

    public void setDataProvider(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
    }

    private void createTopologyType(final LogicalDatastoreType type,
             final InstanceIdentifier<Topology> topologyInstanceId) {
        LOG.info("ForwardingAdjacencyTopologyProvider.createTopologyType");
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final ForwardingAdjacencyTopologyBuilder forwardingAdjacencyTopologyBuilder =
                        new ForwardingAdjacencyTopologyBuilder();
                final FaTopologyTypeBuilder faTopologyTypeBuilder = new FaTopologyTypeBuilder();
                faTopologyTypeBuilder.setForwardingAdjacencyTopology(forwardingAdjacencyTopologyBuilder.build());

                InstanceIdentifier<FaTopologyType> faTopologyTypeIid = topologyInstanceId.child(TopologyTypes.class)
                        .augmentation(MtTopologyType.class).child(MultitechnologyTopology.class)
                        .augmentation(MlTopologyType.class).child(MultilayerTopology.class)
                        .augmentation(FaTopologyType.class);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, faTopologyTypeIid,
                        faTopologyTypeBuilder.build(), true);
            }
        });
    }

    @Override
    public synchronized void close() throws InterruptedException {
        LOG.info("ForwardingAdjacencyTopologyProvider stopped.");
    }

    public void handleForwardingAdjacencyAttributes(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final ForwardingAdjacencyAttributes faAttributes) {
        final ForwardingAdjacencyBuilder forwardingAdjacencyBuilder = new ForwardingAdjacencyBuilder(faAttributes);
        forwardingAdjacencyBuilder.setFaId(faId);
        final ForwardingAdjacencyKey faKey = new ForwardingAdjacencyKey(faId);
        forwardingAdjacencyBuilder.setKey(faKey);

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<ForwardingAdjacency> instanceId =
                        topologyInstanceId.augmentation(FaTopology.class)
                       .child(ForwardingAdjacency.class, faKey);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId,
                        forwardingAdjacencyBuilder.build(), true);
            }
        });
    }

    @Override
    public void onForwardingAdjacencyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final ForwardingAdjacencyAttributes faAttributes) {
        LOG.info("ForwardingAdjacencyTopologyProvider.onForwardingAdjacencyCreated");
        createTopologyType(type, destTopologyId);
        handleForwardingAdjacencyAttributes(type, topologyInstanceId, faId, faAttributes);
    }

    @Override
    public void onForwardingAdjacencyUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final ForwardingAdjacencyAttributes faAttributes) {
        LOG.info("ForwardingAdjacencyTopologyProvider.onForwardingAdjacencyUpdated");
        handleForwardingAdjacencyAttributes(type, topologyInstanceId, faId, faAttributes);
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final LinkKey linkKey) {
        final SupportingFaBuilder supportingFaBuilder = new SupportingFaBuilder();
        supportingFaBuilder.setFa(faId);
        SupportingFaKey supportingFaKey = new SupportingFaKey(faId);
        supportingFaBuilder.setKey(supportingFaKey);
        final MlLinkBuilder mlLinkBuilder = new MlLinkBuilder();
        List<SupportingFa> lSupportingFa = new ArrayList<SupportingFa>();
        lSupportingFa.add(supportingFaBuilder.build());
        mlLinkBuilder.setSupportingFa(lSupportingFa);

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<MlLink> instanceId = topologyInstanceId.child(Link.class, linkKey)
                        .augmentation(MlLink.class);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId, mlLinkBuilder.build(), true);
            }
        });
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId, final NodeKey nodeKey, final TerminationPointKey tpKey) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.rev150123
                .ml.tp.attributes.SupportingFaBuilder supportingFaBuilder = new org.opendaylight.yang.gen.v1.urn
                        .opendaylight.topology.forwarding.adjacency.rev150123.ml.tp.attributes.SupportingFaBuilder();
        supportingFaBuilder.setFa(faId);
        final MlTerminationPointBuilder mlTpBuilder = new MlTerminationPointBuilder();
        mlTpBuilder.setSupportingFa(supportingFaBuilder.build());

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<MlTerminationPoint> instanceId =
                        topologyInstanceId.child(Node.class, nodeKey)
                        .child(TerminationPoint.class, tpKey).augmentation(MlTerminationPoint.class);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId, mlTpBuilder.build(), true);
            }
        });
    }

    @Override
    public void onForwardingAdjacencyDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final FaId faId) {
        final ForwardingAdjacencyKey faKey = new ForwardingAdjacencyKey(faId);

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<ForwardingAdjacency> instanceId =
                        topologyInstanceId.augmentation(FaTopology.class)
                        .child(ForwardingAdjacency.class, faKey);
                transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceId);
            }
        });
    }

    @Override
    public void onLinkDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey) {
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<MlLink> instanceId = topologyInstanceId.child(Link.class, linkKey)
                        .augmentation(MlLink.class);
                transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceId);
            }
        });
    }

    @Override
    public void onTpDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
              final NodeKey nodeKey, final TerminationPointKey tpKey) {
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<MlTerminationPoint> instanceId =
                        topologyInstanceId.child(Node.class, nodeKey)
                        .child(TerminationPoint.class, tpKey).augmentation(MlTerminationPoint.class);
                transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceId);
            }
        });
    }
}
