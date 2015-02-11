/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import java.util.Collections;
import java.util.ArrayList;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.slf4j.Logger;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MlmtTopologyBuilder {

    private Logger LOG;
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;

    public void init(final String name, final DataBroker dataProvider, Logger logger, MlmtOperationProcessor theProcessor) {
        dataBroker = dataProvider;
        LOG = logger;
        processor = theProcessor;
    }

    public void createUnderlayTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyId underlayTopologyRef) {
        try {
            LOG.info("MlmtTopologyBuilder.createUnderlayTopology");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
                    underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
                    UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
                    underlayTopologyBuilder.setKey(underlayKey);
                    UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
                    ArrayList<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
                    lUnderlayTopology.add(underlayTopology);
                    final TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
                    final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
                    final TopologyBuilder tbuilder = new TopologyBuilder();
                    tbuilder.setKey(key);
                    final Topology top = tbuilder.setServerProvided(Boolean.FALSE)
                            .setTopologyTypes(topologyTypesBuilder.build())
                            .setUnderlayTopology(lUnderlayTopology).build();
                    transaction.merge(type, topologyInstanceId, top);
               }
          });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder.createUnderlayTopology null pointer exception", e);
        }
    }

    public void createTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyId underlayTopologyRef) {
        try {
           LOG.info("MlmtTopologyBuilder.createTopology");
           processor.enqueueOperation(new MlmtTopologyOperation() {
               @Override
               public void applyOperation(ReadWriteTransaction transaction) {
                  final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
                  underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
                  UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
                  underlayTopologyBuilder.setKey(underlayKey);
                  UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
                  ArrayList<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
                   lUnderlayTopology.add(underlayTopology);
                  final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
                  final TopologyBuilder tbuilder = new TopologyBuilder();
                  tbuilder.setKey(key);
                  TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
                  final Topology top = tbuilder.setServerProvided(Boolean.FALSE)
                          .setUnderlayTopology(Collections.<UnderlayTopology>emptyList())
                          .setTopologyTypes(topologyTypesBuilder.build())
                          .setLink(Collections.<Link>emptyList())
                          .setNode(Collections.<Node>emptyList()).build();
                  transaction.merge(type, topologyInstanceId, top);
                }
            });
        } catch (final NullPointerException e) {
           LOG.error("MlmtTopologyBuilder.createTopology null pointer exception", e);
        }
    }

     public void deleteTopology(final LogicalDatastoreType type,
             final InstanceIdentifier<Topology> topologyInstanceId) {
        try {
            LOG.info("MlmtTopologyBuilder::deleteTopology");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    transaction.delete(type, topologyInstanceId);
                }
            });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder::deleteTopology null pointer exception", e);
        }
    }

    public void copyTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        try {
            LOG.info("MlmtTopologyBuilder.copyTopology");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final TopologyKey key = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
                    final TopologyBuilder tbuilder = new TopologyBuilder(topology);
                    tbuilder.setKey(key);
                    transaction.merge(type, topologyInstanceId, tbuilder.build());
                }
            });
        } catch (final NullPointerException e) {
          LOG.error("MlmtTopologyBuilder.copyTopology null pointer exception", e);
        }
    }

    public void createNode(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        try {
            LOG.info("MlmtTopologyBuilder.createNode");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final NodeBuilder nbuilder = new NodeBuilder();
                    final NodeKey nodeKey = node.getKey();
                    nbuilder.setKey(nodeKey);
                    nbuilder.setNodeId(node.getNodeId());
                    nbuilder.setTerminationPoint(Collections.<TerminationPoint>emptyList());
                    nbuilder.setSupportingNode(Collections.<SupportingNode>emptyList());
                    final InstanceIdentifier<Node> path = topologyInstanceId.child(Node.class, nodeKey);
                    transaction.merge(type, path, nbuilder.build());
                 }
              });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder.createNode null pointer exception", e);
        }
    }

    public void copyNode(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        try {
            LOG.info("MlmtTopologyBuilder.copyNode");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final NodeId nodeId = node.getNodeId();
                    final NodeKey nodeKey = new NodeKey(nodeId);
                    final NodeBuilder nbuilder = new NodeBuilder(node);
                    nbuilder.setKey(nodeKey);
                    nbuilder.setNodeId(nodeId);
                    final InstanceIdentifier<Node> instanceId = topologyInstanceId.child(Node.class, nodeKey);
                    transaction.merge(type, instanceId, nbuilder.build());
                }
            });
        } catch (final NullPointerException e) {
          LOG.error("MlmtTopologyBuilder.copyNode null pointer exception", e);
      }
    }

    public void deleteNode(final LogicalDatastoreType type,
             final InstanceIdentifier<Node> nodeInstanceId) {
        try {
            LOG.info("MlmtTopologyBuilder.deleteNode");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    transaction.delete(type, nodeInstanceId);
                }
            });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder.deleteNode null pointer exception", e);
        }
    }

    public void createTp(final LogicalDatastoreType type,
           final InstanceIdentifier<Topology> topologyInstanceId,
           final NodeKey nodeKey,
           final TerminationPoint tp) {
        try {
            LOG.info("MlmtTopologyBuilder.createTp");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final TpId tpId = tp.getTpId();
                    final TerminationPointKey tpKey = new TerminationPointKey(tpId);
                    final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                    tpBuilder.setKey(tpKey);
                    tpBuilder.setTpId(tpId);
                    final InstanceIdentifier<TerminationPoint> instanceId = topologyInstanceId
                           .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);
                    transaction.merge(type, instanceId, tpBuilder.build());
                }
            });
        } catch (final NullPointerException e) {
          LOG.error("MlmtTopologyBuilder.createTp null pointer exception", e);
        }
    }

    public void copyTp(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPoint tp) {
        LOG.info("MlmtTopologyBuilder.copyTp");
        try {
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final TpId tpId = tp.getTpId();
                    final TerminationPointKey tpKey = new TerminationPointKey(tpId);
                    final TerminationPointBuilder tpBuilder = new TerminationPointBuilder(tp);
                    tpBuilder.setKey(tpKey);
                    tpBuilder.setTpId(tpId);
                    final InstanceIdentifier<TerminationPoint> instanceId = topologyInstanceId
                           .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);
                    transaction.merge(type, instanceId, tpBuilder.build());
                }
            });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder.copyTp null pointer exception", e);
        }
    }

    public void deleteTp(final LogicalDatastoreType type,
            final InstanceIdentifier<TerminationPoint> tpInstanceId) {

        LOG.info("MlmtTopologyBuilder.deleteTp");
        try {
            processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    transaction.delete(type, tpInstanceId);
                }
            });
        } catch (NullPointerException e) {
           LOG.error("MlmtTopologyBuilder.deleteTp null pointer exception", e);
        }
    }

    public void createLink(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        try {
            LOG.info("MlmtTopologyBuilder.createLink");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final LinkBuilder linkBuilder = new LinkBuilder();
                    final LinkKey linkKey = link.getKey();
                    linkBuilder.setKey(linkKey);
                    linkBuilder.setLinkId(link.getLinkId());
                    linkBuilder.setSource(link.getSource());
                    linkBuilder.setDestination(link.getDestination());
                    linkBuilder.setSupportingLink(Collections.<SupportingLink>emptyList());
                    final InstanceIdentifier<Link> instanceId = topologyInstanceId
                            .child(Link.class, linkKey);
                    transaction.merge(type, instanceId, linkBuilder.build());
                }
            });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder.createLink null pointer exception", e);
        }
    }

     public void copyLink(final LogicalDatastoreType type,
             final InstanceIdentifier<Topology> topologyInstanceId,
             final Link link) {
         LOG.info("MlmtTopologyBuilder.copyTp");
         try {
             processor.enqueueOperation(new MlmtTopologyOperation() {
                 @Override
                 public void applyOperation(ReadWriteTransaction transaction) {
                     final LinkId linkId = link.getLinkId();
                     final LinkKey linkKey = new LinkKey(linkId);
                     final LinkBuilder linkBuilder = new LinkBuilder(link);
                     linkBuilder.setKey(linkKey);
                     linkBuilder.setLinkId(linkId);
                     final InstanceIdentifier<Link> instanceId = topologyInstanceId
                         .child(Link.class, linkKey);
                     transaction.merge(type, instanceId, linkBuilder.build());
                 }
             });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder.copyTp null pointer exception", e);
        }
    }

    public void deleteLink(final LogicalDatastoreType type,
            final InstanceIdentifier<Link> linkInstanceId) {
        try {
            LOG.info("MlmtTopologyBuilder.deleteLink");
            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    transaction.delete(type, linkInstanceId);
                }
            });
        } catch (final NullPointerException e) {
            LOG.error("MlmtTopologyBuilder.deleteLink null pointer exception", e);
        }
    }
}
