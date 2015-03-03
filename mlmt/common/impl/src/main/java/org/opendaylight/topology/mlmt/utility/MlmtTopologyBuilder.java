/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import java.util.Collections;
import java.util.List;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
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
import org.slf4j.Logger;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MlmtTopologyBuilder {

    private Logger log;
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;

    public void init(final String name, final DataBroker dataProvider, Logger logger, MlmtOperationProcessor processor) {
        this.dataBroker = dataProvider;
        this.log = logger;
        this.processor = processor;
    }

    public void createUnderlayTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyId underlayTopologyRef) {
        log.info("MlmtTopologyBuilder.createUnderlayTopology");
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
                underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
                UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
                underlayTopologyBuilder.setKey(underlayKey);
                UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
                List<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
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
    }

    public void createTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyId underlayTopologyRef) {
        log.info("MlmtTopologyBuilder.createTopology");
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
                underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
                UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
                underlayTopologyBuilder.setKey(underlayKey);
                UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
                List<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
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
     }

     public void deleteTopology(final LogicalDatastoreType type,
             final InstanceIdentifier<Topology> topologyInstanceId) {
         log.info("MlmtTopologyBuilder::deleteTopology");
         processor.enqueueOperation(new MlmtTopologyOperation() {
              @Override
              public void applyOperation(ReadWriteTransaction transaction) {
                  transaction.delete(type, topologyInstanceId);
              }
         });
    }

    public void createNode(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TopologyId nodeTopologyId,
            final Node node) {
        log.info("MlmtTopologyBuilder.createNode");
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NodeBuilder nbuilder = new NodeBuilder();
                final NodeKey nodeKey = node.getKey();
                nbuilder.setKey(nodeKey);
                nbuilder.setNodeId(node.getNodeId());
                nbuilder.setTerminationPoint(Collections.<TerminationPoint>emptyList());
                SupportingNodeBuilder supportingNodeBuilder = new SupportingNodeBuilder();
                supportingNodeBuilder.setNodeRef(node.getNodeId());
                SupportingNodeKey supportingNodeKey = new SupportingNodeKey(node.getNodeId(), new TopologyId(nodeTopologyId));
                supportingNodeBuilder.setKey(supportingNodeKey);
                List<SupportingNode> lSupporting = new ArrayList<SupportingNode>();
                lSupporting.add(supportingNodeBuilder.build());
                nbuilder.setSupportingNode(lSupporting);
                final InstanceIdentifier<Node> path = topologyInstanceId.child(Node.class, nodeKey);
                transaction.merge(type, path, nbuilder.build());
            }
        });
    }

    public void deleteNode(final LogicalDatastoreType type,
             final InstanceIdentifier<Topology> topologyInstanceId,
             final NodeKey nodeKey) {
        log.info("MlmtTopologyBuilder.deleteNode");
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final InstanceIdentifier<Node> nodeInstanceId = topologyInstanceId.child(Node.class, nodeKey);
                transaction.delete(type, nodeInstanceId);
            }
        });
    }

    public void createTp(final LogicalDatastoreType type,
           final InstanceIdentifier<Topology> topologyInstanceId,
           final NodeKey nodeKey,
           final TerminationPoint tp) {
        log.info("MlmtTopologyBuilder.createTp");
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
        log.info("MlmtTopologyBuilder.deleteTp");
        processor.enqueueOperation(new MlmtTopologyOperation() {
        @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 InstanceIdentifier<Node> nodeInstanceId = topologyInstanceId.child(Node.class, nodeKey);
                 InstanceIdentifier<TerminationPoint> tpInstanceId = nodeInstanceId.child(TerminationPoint.class, tpKey);
                 transaction.delete(type, tpInstanceId);
             }
        });
    }

    public void createLink(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
         log.info("MlmtTopologyBuilder.createLink");
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
            final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey) {
        log.info("MlmtTopologyBuilder.deleteLink");
        processor.enqueueOperation(new MlmtTopologyOperation() {
             @Override
             public void applyOperation(ReadWriteTransaction transaction) {
                 InstanceIdentifier<Link> linkInstanceId = topologyInstanceId.child(Link.class, linkKey);
                 transaction.delete(type, linkInstanceId);
             }
        });
    }
}
