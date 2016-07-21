/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.util;

import java.util.Arrays;
import java.util.Collection;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;

/**
 * @author samuel.kontris
 *
 */
public class TopologyBuilder {

    public static MapEntryNode createTerminationPointMapEntry(Class<? extends Model> model, String tpId) {
        if (model.equals(NetworkTopologyModel.class)) {
            return ImmutableNodes.mapEntry(TerminationPoint.QNAME, TopologyQNames.NETWORK_TP_ID_QNAME, tpId);
        } else if (model.equals(I2rsModel.class)) {
            return ImmutableNodes.mapEntry(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                    .topology.rev150608.network.node.TerminationPoint.QNAME, TopologyQNames.I2RS_TP_ID_QNAME, tpId);
        } else {
            return null;
        }
    }

    public static MapNode createTerminationPointMapNode(Class<? extends Model> model,
            Collection<MapEntryNode> terminationPoints) {
        if (model.equals(NetworkTopologyModel.class)) {
            return ImmutableNodes.mapNodeBuilder(TerminationPoint.QNAME).withValue(terminationPoints).build();
        } else if (model.equals(I2rsModel.class)) {
            return ImmutableNodes.mapNodeBuilder(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                    .topology.rev150608.network.node.TerminationPoint.QNAME).withValue(terminationPoints).build();
        }
        return null;
    }

    public static MapNode createTerminationPointMapNode(Class<? extends Model> model,
            MapEntryNode... terminationPoints) {
        return createTerminationPointMapNode(model, Arrays.asList(terminationPoints));
    }

    public static MapEntryNode createNodeWithTerminationPoints(Class<? extends Model> model, String nodeId,
            MapNode terminationPoints) {

        MapEntryNode node;
        if (model.equals(NetworkTopologyModel.class)) {
            node = ImmutableNodes.mapEntry(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, nodeId);
        } else if (model.equals(I2rsModel.class)) {
            node = ImmutableNodes.mapEntry(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                    .rev150608.network.Node.QNAME, TopologyQNames.I2RS_NODE_ID_QNAME, nodeId);
        } else {
            return null;
        }

        return ImmutableMapEntryNodeBuilder.create(node).withChild(terminationPoints).build();
    }
}
