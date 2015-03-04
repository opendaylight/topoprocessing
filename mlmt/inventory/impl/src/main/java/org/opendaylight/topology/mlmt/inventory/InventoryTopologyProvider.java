/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.inventory;

import com.google.common.base.Optional;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;

public class InventoryTopologyProvider implements AutoCloseable, MlmtTopologyProvider {
    private Logger log;
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> destTopologyId;
    private InventoryAttributesParser parser;

    public void init(final Logger logger, MlmtOperationProcessor processor,
        final InstanceIdentifier<Topology> destTopologyId, final InventoryAttributesParser parser) {
        logger.info("InventoryTopologyProvider.init");
        this.log = logger;
        this.destTopologyId = destTopologyId;
        this.processor = processor;
        this.parser = parser;
    }

    public void setDataProvider(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public void close() {

    }

    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {}

    @Override
    public void onNodeCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        handleNodeAttributes(type, topologyInstanceId, node);
    }

    public void onTpCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPoint tp) {}

    public void onLinkCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {}

    public void onTopologyUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {}

    public void onNodeUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {}

    public void onTpUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPoint tp) {}

    public void onLinkUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {}

    public void onTopologyDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId) {}

    public void onNodeDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey) {}

    public void onTpDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPointKey tpKey) {}

    public void onLinkDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey){}

    private void handleNodeAttributes(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        log.info("InventoryTopologyProvider.onNodeCreated");
        NodeRef nodeRefAttributes = parser.parseInventoryNodeAttributes(node);
        if (nodeRefAttributes == null) {
            return;
        }

        setNodeRefNodeAttributes(destTopologyId, nodeRefAttributes, node.getKey());
    }

    private void setNodeRefNodeAttributes(
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeRef nodeRef,
            final NodeKey nodeKey) {
        try {
            log.info("InventoryTopologyProvider.setNodeRefNodeAttributes");
            final InstanceIdentifier<Topology> targetTopologyId = topologyInstanceId;
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final InstanceIdentifier<InventoryNode> instanceInvNodeId =
                    targetTopologyId.child(Node.class, nodeKey).augmentation(InventoryNode.class);
            final Optional<InventoryNode> sourceAttributeObject =
                    rx.read(LogicalDatastoreType.OPERATIONAL, instanceInvNodeId).get();

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    final InventoryNodeBuilder inventoryNodeBuilder = new InventoryNodeBuilder();
                    inventoryNodeBuilder.setInventoryNodeRef(nodeRef);
                    if (sourceAttributeObject != null && sourceAttributeObject.isPresent() && sourceAttributeObject.get() != null) {
                        transaction.put(LogicalDatastoreType.OPERATIONAL, instanceInvNodeId, inventoryNodeBuilder.build());
                    } else {
                        transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceInvNodeId, inventoryNodeBuilder.build());
                    }
                }
            });
         } catch (final InterruptedException e) {
             log.error("onNodeCreated interrupted exception", e);
         } catch (final ExecutionException e) {
             log.error("onNodeCreated execution exception", e);
         }
    }
}

