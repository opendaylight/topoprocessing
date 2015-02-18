/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

public class MlmtTopologyUpdateOnNode extends MlmtAbstractTopologyUpdate {

    private Node node;

    public MlmtTopologyUpdateOnNode(LogicalDatastoreType storeType, InstanceIdentifier<Topology> topologyId, Node node) {
        super(MlmtTopologyUpdateType.NODE, storeType, topologyId);
        this.node = node;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public NodeKey getNodeKey() {
        return node.getKey();
    }

    @Override
    public TerminationPoint getTerminationPoint() {
        return null;
    }

    @Override
    public Link getLink() {
        return null;
    }
}

