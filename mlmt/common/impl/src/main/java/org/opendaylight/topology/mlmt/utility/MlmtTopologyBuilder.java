/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MlmtTopologyBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtTopologyBuilder.class);
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;

    public void init(final DataBroker dataProvider, final MlmtOperationProcessor processor) {
        this.dataBroker = dataProvider;
        this.processor = processor;
    }

    public void createUnderlayTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyId underlayTopologyRef) {
        LOG.info("MlmtTopologyBuilder.createUnderlayTopology type: {} topologyInstanceId {} underlayTopologyRef: {}",
                type, topologyInstanceId.toString(), underlayTopologyRef.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
                final UnderlayTopologyKey underlayTopologyKey = new UnderlayTopologyKey(underlayTopologyRef);
                underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
                underlayTopologyBuilder.setKey(underlayTopologyKey);
                final UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
                final InstanceIdentifier<UnderlayTopology> underlayTopologyIid = topologyInstanceId
                        .child(UnderlayTopology.class, underlayTopologyKey);
                transaction.put(type, underlayTopologyIid, underlayTopology);
            }
        });
    }

    public void createUnderlayTopologyList(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final List<UnderlayTopology> underlayTopologyList) {
        LOG.info("MlmtTopologyBuilder.createUnderlayTopologyList type {} topologyInstanceId {} underlayTopologies {}",
                type, topologyInstanceId.toString(), underlayTopologyList.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
                final TopologyBuilder tbuilder = new TopologyBuilder();
                tbuilder.setKey(key);
                tbuilder.setTopologyId(key.getTopologyId());
                final Topology top = tbuilder.setUnderlayTopology(underlayTopologyList).build();
                transaction.merge(type, topologyInstanceId, top);
            }
        });
    }

    public void createTopologyTypes(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyTypes topologyTypes) {
        LOG.info("MlmtTopologyBuilder.createTopologyTypes type {} topologyInstanceId {} topologyTypes {}",
                type, topologyInstanceId.toString(), topologyTypes.toString());

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
        LOG.info("MlmtTopologyBuilder.createNetworkTopology type {}", type);
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
        LOG.info("MlmtTopologyBuilder.createTopology type {} topologyInstanceId {} ",
                type, topologyInstanceId.toString());
        final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
        final TopologyBuilder tbuilder = new TopologyBuilder();

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                tbuilder.setKey(key);
                TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
                tbuilder.setUnderlayTopology(Collections.<UnderlayTopology>emptyList())
                        .setTopologyTypes(topologyTypesBuilder.build())
                        .setLink(Collections.<Link>emptyList())
                        .setNode(Collections.<Node>emptyList());
                if (type == LogicalDatastoreType.OPERATIONAL) {
                    tbuilder.setServerProvided(Boolean.FALSE);
                }
                final Topology top = tbuilder.build();
                transaction.merge(type, topologyInstanceId, top);
            }

            @Override
            public boolean isCommitNow() { return true; }
        });
    }

    public void deleteTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId) {
        LOG.info("MlmtTopologyBuilder.deleteTopology type {} topologyInstanceId {}",
                type, topologyInstanceId.toString());
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
            LOG.info("MlmtTopologyBuilder.copyTopology type {} topologyInstanceId {} toType {}",
                  fromType, topologyInstanceId.toString(), toType);
            final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            Optional<Topology> sourceTopologyObject = rx.read(fromType, topologyInstanceId).get();
            if (sourceTopologyObject == null) {
                LOG.info("MlmtTopologyBuilder.copyTopology source topologyObject null");
                return;
            }
            if (sourceTopologyObject.isPresent() == false) {
                LOG.info("MlmtTopologyBuilder.copyTopology sourceTopologyObject not present");
                return;
            }
            final Topology sourceTopology = sourceTopologyObject.get();
            if (sourceTopology == null) {
                LOG.info("MlmtTopologyBuilder.copyTopology dest sourceTopology is null");
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
            LOG.error("MlmtTopologyBuilder.copyTopology interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MlmtTopologyBuilder.copyTopology execution exception", e);
        }
    }

    public void copyTopologyTypes(final LogicalDatastoreType fromType,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final LogicalDatastoreType toType) {
        try {
            LOG.info("MlmtTopologyBuilder.copyTopologyTypes type {} topologyInstanceId {} toType {}",
                    fromType, topologyInstanceId.toString(), toType);
            final InstanceIdentifier<TopologyTypes> topologyTypesIid =
                    topologyInstanceId.child(TopologyTypes.class);
            final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            Optional<TopologyTypes> sourceTopologyTypesObject = rx.read(fromType, topologyTypesIid).get();
            if (sourceTopologyTypesObject == null) {
                LOG.info("MlmtTopologyBuilder.copyTopologyTypes source topologyObject null");
                return;
            }
            if (sourceTopologyTypesObject.isPresent() == false) {
                LOG.info("MlmtTopologyBuilder.copyTopologyTypes sourceTopologyObject not present");
                return;
            }
            final TopologyTypes sourceTopologyTypes = sourceTopologyTypesObject.get();
            if (sourceTopologyTypes == null) {
                LOG.info("MlmtTopologyBuilder.copyTopologyTypes dest sourceTopology is null");
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
            LOG.error("MlmtTopologyBuilder.copyTopologyTypes interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MlmtTopologyBuilder.copyTopologyTypes execution exception", e);
        }
    }

    public void copyUnderlayTopology(final LogicalDatastoreType fromType,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final LogicalDatastoreType toType) {
        try {
            LOG.info("MlmtTopologyBuilder.copyUnderlyingTopologyTypes type {} topologyInstanceId {} toType {}",
                    fromType, topologyInstanceId.toString(), toType);
            Optional<UnderlayTopology> sourceUnderlayTopologyObject = null;
            final InstanceIdentifier<UnderlayTopology> underlayTopologyIid =
                    topologyInstanceId.child(UnderlayTopology.class);
            final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            sourceUnderlayTopologyObject = rx.read(fromType, underlayTopologyIid).get();
            if (sourceUnderlayTopologyObject == null) {
                LOG.info("MlmtTopologyBuilder.copyTopologyTypes source topologyObject null");
                return;
            }
            if (sourceUnderlayTopologyObject.isPresent() == false) {
                LOG.info("MlmtTopologyBuilder.copyUnderlayTopology sourceUnderlayTopologyObject not present");
                return;
            }
            final UnderlayTopology sourceUnderlayTopology = sourceUnderlayTopologyObject.get();
            if (sourceUnderlayTopology == null) {
                LOG.info("MlmtTopologyBuilder.copyUnderlayTopology dest sourceUnderlayTopology is null");
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
            LOG.error("MlmtTopologyBuilder.copyUnderlayTopology interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MlmtTopologyBuilder.copyUnderlayTopology execution exception", e);
        }
    }

    public void createNode(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final TopologyId nodeTopologyId,
            final Node node) {
        LOG.info("MlmtTopologyBuilder.createNode type {} topologyInstanceId {} nodeTopologyId {} nodeKey {} ",
                type, topologyInstanceId.toString(), nodeTopologyId.toString(), node.getKey().toString());
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
                SupportingNodeKey supportingNodeKey =
                        new SupportingNodeKey(nodeId, new TopologyId(nodeTopologyId));
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
        LOG.info("MlmtTopologyBuilder.deleteNode type {} topologyInstanceId {} nodeKey {}",
                type, topologyInstanceId.toString(), nodeKey.toString());
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
        LOG.info("MlmtTopologyBuilder.copyNode type {} topologyInstanceId {} nodeKey {}",
                topologyInstanceId.toString(), nodeTopologyId.toString(), node.getKey().toString());

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
        LOG.info("MlmtTopologyBuilder.createTp type {} topologyInstanceId {} nodeKey {} terminationPointKey {}",
                type, topologyInstanceId.toString(), nodeKey.toString(), tp.getKey().toString());
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
        LOG.info("MlmtTopologyBuilder.deleteTp type {} topologyInstanceId {} nodeKey {} terminationPointKey {}",
                type, topologyInstanceId.toString(), nodeKey.toString(), tpKey.toString());
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
        LOG.info("MlmtTopologyBuilder.copyTp type {} topologyInstanceId {} nodeKey {} terminationPointKey {}",
                type, topologyInstanceId.toString(), nodeKey.toString(), tp.getKey().toString());
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
        LOG.info("MlmtTopologyBuilder.createLink type {} topologyInstanceId {} linkKey {}",
                type, topologyInstanceId.toString(), link.getKey().toString());

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
        LOG.info("MlmtTopologyBuilder.deleteLink type {} topologyInstanceId {} linkKey {}",
                type, topologyInstanceId.toString(), linkKey.toString());
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
        LOG.info("MlmtTopologyBuilder.copyLink type {} topologyInstanceId {} linkKey {}",
                type, topologyInstanceId.toString(), link.getKey().toString());
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
        LOG.info("MlmtTopologyBuilder.deleteGenericObject type {} topologyInstanceId {}",
                type, instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.delete(type, instanceId);
            }

            @Override
            public boolean isCommitNow() { return true; }
        });
    }

    public <T extends DataObject> void createGenericObject(final LogicalDatastoreType type,
            final InstanceIdentifier<T> instanceId, final T dataSet) {
        LOG.debug("MlmtTopologyBuilder.createGenericObject type {} topologyInstanceId {}",
                type, instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.put(type, instanceId, dataSet);
            }
        });
    }

    public <T extends DataObject> void updateGenericObject(final LogicalDatastoreType type,
            final InstanceIdentifier<T> instanceId, final T dataSet) {
        LOG.debug("MlmtTopologyBuilder.updateGenericObject type {} topologyInstanceId {}",
                type, instanceId.toString());
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                transaction.merge(type, instanceId, dataSet);
            }
        });
    }
}
