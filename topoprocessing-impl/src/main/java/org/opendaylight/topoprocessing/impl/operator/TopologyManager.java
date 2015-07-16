/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.topoprocessing.impl.rpc.OverlayRpcImplementation;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.LogicalNodeWrapper;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.uhlir
 *
 */
public class TopologyManager implements DOMRpcAvailabilityListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyManager.class);

    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private List<LogicalNodeWrapper> wrappers = new ArrayList<>();
    private TopologyWriter writer;
    private RpcServices rpcServices;
    private Collection<DOMRpcIdentifier> availableRpcs;
    private YangInstanceIdentifier nodeIdentifier;
    private GlobalSchemaContextHolder schemaHolder;

    /**
     * @param rpcServices           used for rpc republishing
     * @param schemaHolder          access to SchemaContext
     * @param topologyIdentifier    topology identifier
     */
    public TopologyManager(RpcServices rpcServices, GlobalSchemaContextHolder schemaHolder,
            YangInstanceIdentifier topologyIdentifier) {
        this.rpcServices = rpcServices;
        this.schemaHolder = schemaHolder;
        this.nodeIdentifier = topologyIdentifier.node(Node.QNAME);
        availableRpcs = new HashSet<>();
        this.rpcServices.getRpcService().registerRpcListener(this);
    }

    /**
     * For testing purpose only
     * @return All logicalNode wrappers
     */
    public List<LogicalNodeWrapper> getWrappers() {
        return wrappers;
    }

    /**
     * Adds new Logical node into
     * - existing Logical node wrapper
     * - new Logical node wrapper
     * @param newLogicalNode - logical node which shall be put into wrapper
     */
    public void addLogicalNode(LogicalNode newLogicalNode) {
        if (newLogicalNode != null && !newLogicalNode.getPhysicalNodes().isEmpty()) {
            for (PhysicalNode newPhysicalNode : newLogicalNode.getPhysicalNodes()) {
                for (LogicalNodeWrapper wrapper : wrappers) {
                    for (LogicalNode logicalNodeFromWrapper : wrapper.getLogicalNodes()) {
                        for (PhysicalNode physicalNodeFromWrapper : logicalNodeFromWrapper.getPhysicalNodes()) {
                            if (physicalNodeFromWrapper.getNodeId().equals(newPhysicalNode.getNodeId())) {
                                // update existing wrapper
                                wrapper.addLogicalNode(newLogicalNode);
                                writer.writeNode(wrapper);
                                registerOverlayRpcs(wrapper, newLogicalNode);
                                return;
                            }
                        }
                    }
                }
            }
            // create new Logical node wrapper with unique id and add the logical node into it
            String wrapperId = idGenerator.getNextIdentifier(CorrelationItemEnum.Node);
            LogicalNodeWrapper newWrapper = new LogicalNodeWrapper(wrapperId, newLogicalNode);
            wrappers.add(newWrapper);
            writer.writeNode(newWrapper);
            registerOverlayRpcs(newWrapper, newLogicalNode);
        }
    }

    /**
     * @param logicalIdentifier Logical Node with new changes to update
     */
    public void updateLogicalNode(LogicalNode logicalIdentifier) {
        for (LogicalNodeWrapper wrapper : wrappers) {
            for (LogicalNode logicalNode : wrapper.getLogicalNodes()) {
                if (logicalNode.equals(logicalIdentifier)) {
                    writer.writeNode(wrapper);
                    registerOverlayRpcs(wrapper, logicalNode);
                }
            }
        }
    }

    /**
     * @param logicalIdentifier Logical Node to remove
     */
    public void removeLogicalNode(LogicalNode logicalIdentifier) {
        LogicalNodeWrapper foundWrapper = null;
        for (LogicalNodeWrapper wrapper : wrappers) {
            if (wrapper.getLogicalNodes().contains(logicalIdentifier)) {
                wrapper.getLogicalNodes().remove(logicalIdentifier);
                foundWrapper = wrapper;
                break;
            }
        }
        if (foundWrapper != null) {
            if (foundWrapper.getLogicalNodes().size() == 0) {
                // remove logical node wrapper as well
                writer.deleteNode(foundWrapper);
                wrappers.remove(foundWrapper);
            } else {
                writer.writeNode(foundWrapper);
            }
        }
    }

    /**
     * @param writer writes into the operational datastore
     */
    public void setWriter(TopologyWriter writer) {
        this.writer = writer;
    }

    @Override
    public void onRpcAvailable(Collection<DOMRpcIdentifier> rpcs) {
        availableRpcs.addAll(rpcs);
    }

    @Override
    public void onRpcUnavailable(Collection<DOMRpcIdentifier> rpcs) {
        availableRpcs.removeAll(rpcs);
    }

    /**
     * Gathers RPCs for all {@link PhysicalNode}s present in the {@link LogicalNode} and registers them under
     * {@link LogicalNodeWrapper} Id
     * @param wrapper wraps LogicalNode and contains id for republished rpc
     * @param logicalNode Which contains Physical Nodes with RPCs
     */
    private void registerOverlayRpcs(LogicalNodeWrapper wrapper, LogicalNode logicalNode) {
        LOGGER.trace("Registering overlay RPCs");
        YangInstanceIdentifier contextIdentifier = YangInstanceIdentifier.builder(nodeIdentifier)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, wrapper.getNodeId()).build();
        YangInstanceIdentifier nodeEntryIdentifier;
        for (PhysicalNode node : logicalNode.getPhysicalNodes()) {
            List<DOMRpcIdentifier> underlayRpcs = new ArrayList<>();
            nodeEntryIdentifier = YangInstanceIdentifier.builder(nodeIdentifier)
                    .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, node.getNodeId()).build();
            for (Iterator<DOMRpcIdentifier> iterator = availableRpcs.iterator(); iterator.hasNext();) {
                DOMRpcIdentifier rpcIdentifier = iterator.next();
                if (rpcIdentifier.getContextReference().equals(nodeEntryIdentifier)) {
                    underlayRpcs.add(rpcIdentifier);
                }
            }
            OverlayRpcImplementation overlayImplementation =
                    new OverlayRpcImplementation(rpcServices.getRpcService(), schemaHolder.getSchemaContext(),
                            nodeEntryIdentifier);
            Set<DOMRpcIdentifier> overlayRpcIdentifiers = new HashSet<>();
            for (DOMRpcIdentifier underlayRpcIdentifier : underlayRpcs) {
                overlayRpcIdentifiers.add(DOMRpcIdentifier.create(underlayRpcIdentifier.getType(), contextIdentifier));
            }
            if (!overlayRpcIdentifiers.isEmpty()) {
                rpcServices.getRpcProviderService()
                    .registerRpcImplementation(overlayImplementation, overlayRpcIdentifiers);
            }
        }
    }
}
