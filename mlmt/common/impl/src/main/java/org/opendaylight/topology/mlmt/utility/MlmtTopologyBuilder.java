/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import com.google.common.base.Optional;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.rwa.rev150122.pool.input.matrix.InputSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.rwa.rev150122.pool.output.matrix.OutputSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.rwa.rev150122.internal.stage.matrix.InternalSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.rwa.rev150122.rb.pool.state.RbState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.rwa.rev150122.resource.pool.resource.pool.ResourceBlockInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.rwa.rev150122.resource.pool.resource.pool.ResourceWaveConstraints;

import org.slf4j.Logger;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MlmtTopologyBuilder {

    private Logger log;
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;

    public void init(final DataBroker dataProvider, Logger logger, MlmtOperationProcessor processor) {
        this.dataBroker = dataProvider;
        this.log = logger;
        this.processor = processor;
    }

    public void createUnderlayTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyId underlayTopologyRef) {
        log.info("MlmtTopologyBuilder.createUnderlayTopology type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " underlayTopologyRef: " + underlayTopologyRef.toString());
        final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
        final TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
        final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
        final TopologyBuilder tbuilder = new TopologyBuilder();

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
                UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
                underlayTopologyBuilder.setKey(underlayKey);
                UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
                List<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
                lUnderlayTopology.add(underlayTopology);
                tbuilder.setKey(key);
                final Topology top = tbuilder.setServerProvided(Boolean.FALSE)
                        .setTopologyTypes(topologyTypesBuilder.build())
                                .setUnderlayTopology(lUnderlayTopology).build();
                transaction.merge(type, topologyInstanceId, top);
            }
        });
    }

    public void createUnderlayTopologyList(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
                    final List<UnderlayTopology> underlayTopologyList) {
        log.info("MlmtTopologyBuilder.createUnderlayTopologyList type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " underlayTopologyList: " + underlayTopologyList.toString());
        final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(key);

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final Topology top = tbuilder.setUnderlayTopology(underlayTopologyList).build();
                transaction.merge(type, topologyInstanceId, top);
            }
        });
    }

    public void createTopologyTypes(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
                    final TopologyTypes topologyTypes) {
        log.info("MlmtTopologyBuilder.createTopologyTypes type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " topologyTypes: " + topologyTypes.toString());

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                InstanceIdentifier<TopologyTypes> target =
                        topologyInstanceId.child(TopologyTypes.class);
                transaction.merge(type, target, topologyTypes);
            }
        });
    }

    public void createNetworkTopology(final LogicalDatastoreType type) {
        log.info("MlmtTopologyBuilder.createNetworkTopology type: " + type);
        final NetworkTopologyBuilder networkTopologyBuilder = new NetworkTopologyBuilder();
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.merge(type, InstanceIdentifier.create(NetworkTopology.class),
                        networkTopologyBuilder.build());
            }

            @Override
            public boolean isCommitNow() { return true; }
        });
    }

    public void createTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId) {
        log.info("MlmtTopologyBuilder.createTopology type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString());
        final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
        final TopologyBuilder tbuilder = new TopologyBuilder();

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                tbuilder.setKey(key);
                TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
                final Topology top = tbuilder.setServerProvided(Boolean.FALSE)
                        .setUnderlayTopology(Collections.<UnderlayTopology>emptyList())
                                .setTopologyTypes(topologyTypesBuilder.build())
                                        .setLink(Collections.<Link>emptyList())
                                                .setNode(Collections.<Node>emptyList()).build();
                transaction.merge(type, topologyInstanceId, top);
            }

            @Override
            public boolean isCommitNow() { return true; }
        });
    }

    public void deleteTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId) {
        log.info("MlmtTopologyBuilder.deleteTopology type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.delete(type, topologyInstanceId);
            }

            @Override
            public boolean isCommitNow() { return true; }
        });
    }

    public void copyTopology(final LogicalDatastoreType fromType,
            final InstanceIdentifier<Topology> topologyInstanceId,
                    final LogicalDatastoreType toType) {
        try {
            log.info("MlmtTopologyBuilder.copyTopology type: " + fromType + " topologyInstanceId: "
                    + topologyInstanceId.toString() + " toType: " + toType);
            Optional<Topology> sourceTopologyObject = null;
            final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            sourceTopologyObject = rx.read(fromType, topologyInstanceId).get();
            if (sourceTopologyObject == null) {
                log.info("MlmtTopologyBuilder.copyTopology source topologyObject null");
                return;
            }
            if (sourceTopologyObject.isPresent() == false) {
                log.info("MlmtTopologyBuilder.copyTopology sourceTopologyObject not present");
                return;
            }
            final Topology sourceTopology = sourceTopologyObject.get();
            if (sourceTopology == null){
                log.info("MlmtTopologyBuilder.copyTopology dest sourceTopology is null");
                return;
            }

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    transaction.put(toType, topologyInstanceId, sourceTopology);
                }

                @Override
                public boolean isCommitNow() { return true; }
            });
        } catch (final InterruptedException e) {
            log.error("MlmtTopologyBuilder.copyTopology interrupted exception", e);
        } catch (final ExecutionException e) {
            log.error("MlmtTopologyBuilder.copyTopology execution exception", e);
        }
    }

    public void copyTopologyTypes(final LogicalDatastoreType fromType,
            final InstanceIdentifier<Topology> topologyInstanceId,
                    final LogicalDatastoreType toType) {
        try {
            log.info("MlmtTopologyBuilder.copyTopologyTypes type: " + fromType + " topologyInstanceId: "
                    + topologyInstanceId.toString() + " toType: " + toType);
            Optional<TopologyTypes> sourceTopologyTypesObject = null;
            final InstanceIdentifier<TopologyTypes> topologyTypesIid = topologyInstanceId.child(TopologyTypes.class);
            final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            sourceTopologyTypesObject = rx.read(fromType, topologyTypesIid).get();
            if (sourceTopologyTypesObject == null) {
                log.info("MlmtTopologyBuilder.copyTopologyTypes source topologyObject null");
                return;
            }
            if (sourceTopologyTypesObject.isPresent() == false) {
                log.info("MlmtTopologyBuilder.copyTopologyTypes sourceTopologyObject not present");
                return;
            }
            final TopologyTypes sourceTopologyTypes = sourceTopologyTypesObject.get();
            if (sourceTopologyTypes == null){
                log.info("MlmtTopologyBuilder.copyTopologyTypes dest sourceTopology is null");
                return;
            }

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    transaction.put(toType, topologyTypesIid, sourceTopologyTypes);
                }

                @Override
                public boolean isCommitNow() { return true; }
            });
        } catch (final InterruptedException e) {
            log.error("MlmtTopologyBuilder.copyTopologyTypes interrupted exception", e);
        } catch (final ExecutionException e) {
            log.error("MlmtTopologyBuilder.copyTopologyTypes execution exception", e);
        }
    }

    public void copyUnderlayTopology(final LogicalDatastoreType fromType,
            final InstanceIdentifier<Topology> topologyInstanceId,
                    final LogicalDatastoreType toType) {
        try {
            log.info("MlmtTopologyBuilder.copyUnderlyingTopologyTypes type: " + fromType + " topologyInstanceId: "
                    + topologyInstanceId.toString() + " toType: " + toType);
            Optional<UnderlayTopology> sourceUnderlayTopologyObject = null;
            final InstanceIdentifier<UnderlayTopology> underlayTopologyIid =
                    topologyInstanceId.child(UnderlayTopology.class);
            final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            sourceUnderlayTopologyObject = rx.read(fromType, underlayTopologyIid).get();
            if (sourceUnderlayTopologyObject == null) {
                log.info("MlmtTopologyBuilder.copyTopologyTypes source topologyObject null");
                return;
            }
            if (sourceUnderlayTopologyObject.isPresent() == false) {
                log.info("MlmtTopologyBuilder.copyUnderlayTopology sourceUnderlayTopologyObject not present");
                return;
            }
            final UnderlayTopology sourceUnderlayTopology = sourceUnderlayTopologyObject.get();
            if (sourceUnderlayTopology == null){
                log.info("MlmtTopologyBuilder.copyUnderlayTopology dest sourceUnderlayTopology is null");
                return;
            }

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    transaction.put(toType, underlayTopologyIid, sourceUnderlayTopology);
                }

                @Override
                public boolean isCommitNow() { return true; }
            });
        } catch (final InterruptedException e) {
            log.error("MlmtTopologyBuilder.copyUnderlayTopology interrupted exception", e);
        } catch (final ExecutionException e) {
            log.error("MlmtTopologyBuilder.copyUnderlayTopology execution exception", e);
        }
    }

    public void createNode(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final TopologyId nodeTopologyId,
                    final Node node) {
        log.info("MlmtTopologyBuilder.createNode type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " TopologyId: " + nodeTopologyId.toString()
                        + " nodeKey: " + node.getKey().toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NodeBuilder nbuilder = new NodeBuilder();
                final NodeKey nodeKey = node.getKey();
                final NodeId nodeId = node.getNodeId();
                nbuilder.setKey(nodeKey);
                nbuilder.setNodeId(nodeId);
                nbuilder.setTerminationPoint(Collections.<TerminationPoint>emptyList());
                SupportingNodeBuilder supportingNodeBuilder = new SupportingNodeBuilder();
                supportingNodeBuilder.setNodeRef(nodeId);
                SupportingNodeKey supportingNodeKey = new SupportingNodeKey(nodeId, new TopologyId(nodeTopologyId));
                supportingNodeBuilder.setKey(supportingNodeKey);
                List<SupportingNode> lSupporting = new ArrayList<SupportingNode>();
                lSupporting.add(supportingNodeBuilder.build());
                nbuilder.setSupportingNode(lSupporting);
                final InstanceIdentifier<Node> path = topologyInstanceId.child(Node.class, nodeKey);
                transaction.merge(type, path, nbuilder.build());
            }
        });

        List<TerminationPoint> lTp = node.getTerminationPoint();
        if (lTp != null && !lTp.isEmpty()) {
            for (TerminationPoint tp : lTp) {
                createTp(type, topologyInstanceId, node.getKey(), tp);
            }
        }
    }

    public void deleteNode(final LogicalDatastoreType type,
             final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey) {
        log.info("MlmtTopologyBuilder.deleteNode type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " TopologyId: " + topologyInstanceId.toString()
                        + " nodeKey: " + nodeKey.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<Node> nodeInstanceId = topologyInstanceId.child(Node.class, nodeKey);
                transaction.delete(type, nodeInstanceId);
            }

            @Override
            public boolean isCommitNow() { return true; }
        });
    }

    public void copyNode(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final TopologyId nodeTopologyId,
                    final Node node) {
        log.info("MlmtTopologyBuilder.copyNode type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " TopologyId: " + nodeTopologyId.toString()
                        + " nodeKey: " + node.getKey().toString());

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                NodeBuilder nbuilder = new NodeBuilder(node);
                final NodeKey nodeKey = node.getKey();
                final NodeId nodeId = node.getNodeId();
                nbuilder.setKey(nodeKey);
                nbuilder.setNodeId(nodeId);
                SupportingNodeBuilder supportingNodeBuilder = new SupportingNodeBuilder();
                supportingNodeBuilder.setNodeRef(nodeId);
                SupportingNodeKey supportingNodeKey = new SupportingNodeKey(nodeId,
                        new TopologyId(nodeTopologyId));
                supportingNodeBuilder.setKey(supportingNodeKey);
                List<SupportingNode> lSupporting = new ArrayList<SupportingNode>();
                lSupporting.add(supportingNodeBuilder.build());
                nbuilder.setSupportingNode(lSupporting);
                final InstanceIdentifier<Node> path = topologyInstanceId.child(Node.class, nodeKey);
                transaction.merge(type, path, nbuilder.build());
            }
        });
    }

    public void createTp(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey,
                    final TerminationPoint tp) {
        log.info("MlmtTopologyBuilder.createTp type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " nodeKey: " + nodeKey.toString()
                        + " terminationPointKey: " + tp.getKey().toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 final TpId tpId = tp.getTpId();
                 final TerminationPointKey tpKey = new TerminationPointKey(tpId);
                 final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                 tpBuilder.setKey(tpKey);
                 tpBuilder.setTpId(tpId);
                 List<TpId> lTpId = new ArrayList<TpId>();
                 lTpId.add(tpId);
                 tpBuilder.setTpRef(lTpId);
                 final InstanceIdentifier<TerminationPoint> instanceId = topologyInstanceId
                           .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);
                 transaction.merge(type, instanceId, tpBuilder.build());
             }
        });
    }

    public void deleteTp(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPointKey tpKey) {
        log.info("MlmtTopologyBuilder.deleteTp type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " nodeKey: " + nodeKey.toString()
                + " terminationPointKey: " + tpKey.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                InstanceIdentifier<Node> nodeInstanceId = topologyInstanceId.child(Node.class, nodeKey);
                InstanceIdentifier<TerminationPoint> tpInstanceId = 
                        nodeInstanceId.child(TerminationPoint.class, tpKey);
                transaction.delete(type, tpInstanceId);
            }

             @Override
             public boolean isCommitNow() { return true; }
        });
    }

    public void copyTp(final LogicalDatastoreType type,
           final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey,
                   final TerminationPoint tp) {
        log.info("MlmtTopologyBuilder.copyTp type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " nodeKey: " + nodeKey.toString()
                        + " terminationPointKey: " + tp.getKey().toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 final TerminationPointKey tpKey = tp.getKey();
                 final TpId tpId = tp.getTpId();
                 final TerminationPointBuilder tpBuilder = new TerminationPointBuilder(tp);
                 tpBuilder.setKey(tpKey);
                 tpBuilder.setTpId(tpId);
                 List<TpId> lTpId = new ArrayList<TpId>();
                 lTpId.add(tpId);
                 tpBuilder.setTpRef(lTpId);
                 final InstanceIdentifier<TerminationPoint> instanceId = topologyInstanceId
                           .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);
                 transaction.merge(type, instanceId, tpBuilder.build());
             }
        });
    }

    public void createLink(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Link link) {
        log.info("MlmtTopologyBuilder.createLink type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " linkKey: " + link.getKey().toString());

         processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 final LinkBuilder linkBuilder = new LinkBuilder();
                 final LinkKey linkKey = link.getKey();
                 final LinkId linkId = link.getLinkId();
                 linkBuilder.setKey(linkKey);
                 linkBuilder.setLinkId(linkId);
                 linkBuilder.setSource(link.getSource());
                 linkBuilder.setDestination(link.getDestination());
                 SupportingLinkBuilder supportingLinkBuilder = new SupportingLinkBuilder();
                 supportingLinkBuilder.setLinkRef(linkId);
                 SupportingLinkKey supportingLinkKey = new SupportingLinkKey(linkId);
                 supportingLinkBuilder.setKey(supportingLinkKey);
                 List<SupportingLink> lSupporting = new ArrayList<SupportingLink>();
                 lSupporting.add(supportingLinkBuilder.build());
                 linkBuilder.setSupportingLink(lSupporting);
                 final InstanceIdentifier<Link> instanceId = topologyInstanceId
                         .child(Link.class, linkKey);
                 transaction.merge(type, instanceId, linkBuilder.build());
             }
        });
    }

    public void deleteLink(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final LinkKey linkKey) {
        log.info("MlmtTopologyBuilder.deleteLink type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " linkKey: " + linkKey.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 InstanceIdentifier<Link> linkInstanceId = topologyInstanceId.child(Link.class, linkKey);
                 transaction.delete(type, linkInstanceId);
             }

             @Override
             public boolean isCommitNow() { return true; }
        });
    }

    public void copyLink(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Link link) {
        log.info("MlmtTopologyBuilder.copyLink type: " + type + " topologyInstanceId: "
                + topologyInstanceId.toString() + " linkKey: " + link.getKey().toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 LinkBuilder linkBuilder = new LinkBuilder(link);
                 final LinkKey linkKey = link.getKey();
                 final LinkId linkId = link.getLinkId();
                 linkBuilder.setKey(linkKey);
                 linkBuilder.setLinkId(linkId);
                 SupportingLinkBuilder supportingLinkBuilder = new SupportingLinkBuilder();
                 supportingLinkBuilder.setLinkRef(linkId);
                 SupportingLinkKey supportingLinkKey = new SupportingLinkKey(linkId);
                 supportingLinkBuilder.setKey(supportingLinkKey);
                 List<SupportingLink> lSupporting = new ArrayList<SupportingLink>();
                 lSupporting.add(supportingLinkBuilder.build());
                 linkBuilder.setSupportingLink(lSupporting);
                 InstanceIdentifier<Link> linkInstanceId = topologyInstanceId.child(Link.class, linkKey);
                 transaction.merge(type, linkInstanceId, link);
             }
        });
    }

    public void deleteGenericObject(final LogicalDatastoreType type,
            final InstanceIdentifier<?> instanceId) {
        log.info("MlmtTopologyBuilder.deleteGenericObject type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.delete(type, instanceId);
            }

            @Override
            public boolean isCommitNow() { return true; }
        });
    }

    public void createInputSet(final LogicalDatastoreType type, final InstanceIdentifier<InputSet> instanceId,
            final InputSet inputSet) {
        log.debug("MlmtTopologyBuilder.createInputSet type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.put(type, instanceId, inputSet);
            }
        });
    }

    public void createOutputSet(final LogicalDatastoreType type, final InstanceIdentifier<OutputSet> instanceId,
            final OutputSet outputSet) {
        log.debug("MlmtTopologyBuilder.createOutputSet type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.put(type, instanceId, outputSet);
            }
        });
    }

    public void createInternalSet(final LogicalDatastoreType type, final InstanceIdentifier<InternalSet> instanceId,
            final InternalSet internalSet) {
        log.debug("MlmtTopologyBuilder.createInternalSet type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 transaction.put(type, instanceId, internalSet);
             }
        });
    }

    public void createResourceWaveConstraints(final LogicalDatastoreType type,
            final InstanceIdentifier<ResourceWaveConstraints> instanceId,
                    final ResourceWaveConstraints resourceWaveConstraints) {
        log.debug("MlmtTopologyBuilder.createResourceWaveConstraints type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 transaction.put(type, instanceId, resourceWaveConstraints);
             }
        });
    }

    public void createResourceBlockInfo(final LogicalDatastoreType type,
            final InstanceIdentifier<ResourceBlockInfo> instanceId,
                    final ResourceBlockInfo resourceBlockInfo) {
        log.debug("MlmtTopologyBuilder.createResourceBlockInfo type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.put(type, instanceId, resourceBlockInfo);
            }
        });
    }

    public void createRbState(final LogicalDatastoreType type, final InstanceIdentifier<RbState> instanceId,
             final RbState rbState) {
        log.debug("MlmtTopologyBuilder.createRbState type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.put(type, instanceId, rbState);
            }
        });
    }

    public void updateInputSet(final LogicalDatastoreType type, final InstanceIdentifier<InputSet> instanceId,
            final InputSet inputSet) {
        log.debug("MlmtTopologyBuilder.updateInputSet type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.merge(type, instanceId, inputSet);
            }
        });
    }

    public void updateOutputSet(final LogicalDatastoreType type, final InstanceIdentifier<OutputSet> instanceId,
            final OutputSet outputSet) {
        log.debug("MlmtTopologyBuilder.updateOutputSet type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.merge(type, instanceId, outputSet);
            }
        });
    }

    public void updateInternalSet(final LogicalDatastoreType type, final InstanceIdentifier<InternalSet> instanceId,
            final InternalSet internalSet) {
        log.debug("MlmtTopologyBuilder.updateInternalSet type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 transaction.merge(type, instanceId, internalSet);
             }
        });
    }

    public void updateResourceWaveConstraints(final LogicalDatastoreType type,
            final InstanceIdentifier<ResourceWaveConstraints> instanceId,
                    final ResourceWaveConstraints resourceWaveConstraints) {
        log.debug("MlmtTopologyBuilder.updateResourceWaveConstraints type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 transaction.merge(type, instanceId, resourceWaveConstraints);
             }
        });
    }

    public void updateResourceBlockInfo(final LogicalDatastoreType type,
           final InstanceIdentifier<ResourceBlockInfo> instanceId,
                   final ResourceBlockInfo resourceBlockInfo) {
         log.debug("MlmtTopologyBuilder.updateResourceBlockInfo type: " + type + " topologyInstanceId: "
                 + instanceId.toString());
         processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 transaction.merge(type, instanceId, resourceBlockInfo);
             }
         });
    }

    public void updateRbState(final LogicalDatastoreType type, final InstanceIdentifier<RbState> instanceId,
            final RbState rbState) {
        log.debug("MlmtTopologyBuilder.updateRbState type: " + type + " topologyInstanceId: "
                + instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.merge(type, instanceId, rbState);
            }
        });
    }
}
