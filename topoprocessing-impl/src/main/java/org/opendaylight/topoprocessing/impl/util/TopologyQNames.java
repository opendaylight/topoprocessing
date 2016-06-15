/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * @author michal.polkorab
 *
 */
public final class TopologyQNames {

    public static final QName I2RS_NODE_QNAME =
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.rev150608.network.Node.QNAME;
    /** I2RS model network-id QName */
    public static final QName I2RS_NETWORK_ID_QNAME = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.rev150608.Network.QNAME, "network-id").intern();
    /** I2RS model supporting-node network-ref */
    public static final QName I2RS_NETWORK_REF = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.rev150608.network.node.SupportingNode.QNAME, "network-ref").intern();
    /** I2RS model supporting-node node-ref */
    public static final QName I2RS_NODE_REF = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.rev150608.network.node.SupportingNode.QNAME, "node-ref").intern();
    public static final QName I2RS_NODE_ID_QNAME = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.rev150608.network.Node.QNAME, "node-id").intern();
    /** I2RS model supporting-link link-ref QName */
    public static final QName I2RS_LINK_REF = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.link.SupportingLink.QNAME , "link-ref").intern();
    /** I2RS model supporting-link network-ref QName */
    public static final QName I2RS_LINK_NETWORK_REF = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.link.SupportingLink.QNAME , "network-ref").intern();
    /** I2RS model link-id QName */
    public static final QName I2RS_LINK_ID_QNAME = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.Link.QNAME , "link-id").intern();
    /** I2RS model tp-id QName */
    public static final QName I2RS_TP_ID_QNAME = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.node.TerminationPoint.QNAME , "tp-id").intern();
    /** I2RS model supporting-termination-point tp-ref QName */
    public static final QName I2RS_TP_REF = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.node.termination.point.SupportingTerminationPoint.QNAME,
            "tp-ref").intern();
    /** I2RS model supporting-termination-point network-ref QName */
    public static final QName I2RS_TP_NETWORK_REF = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.node.termination.point.SupportingTerminationPoint.QNAME,
            "network-ref").intern();
    /** I2RS model supporting-termination-point node-ref QName */
    public static final QName I2RS_TP_NODE_REF = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.node.termination.point.SupportingTerminationPoint.QNAME,
            "node-ref").intern();
    /**I2RS source-node QName**/
    public static final QName I2RS_LINK_SOURCE_NODE_QNAME = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.link.Source.QNAME, "source-node").intern();
    /**I2RS dest-node QName**/
    public static final QName I2RS_LINK_DEST_NODE_QNAME = QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .network.topology.rev150608.network.link.Destination.QNAME, "dest-node").intern();

    /** Network-topology topology-id QName */
    public static final QName TOPOLOGY_ID_QNAME =
        QName.create(Topology.QNAME, "topology-id").intern();
    /** Network-topology node-id QName */
    public static final QName NETWORK_NODE_ID_QNAME = QName.create(Node.QNAME, "node-id").intern();
    /** Network-topology link-id QName */
    public static final QName NETWORK_LINK_ID_QNAME = QName.create(Link.QNAME, "link-id").intern();
    /** Network-topology tp-id QName */
    public static final QName NETWORK_TP_ID_QNAME =
        QName.create(TerminationPoint.QNAME, "tp-id").intern();
    /** Network-topology supporting-node topology-ref */
    public static final QName TOPOLOGY_REF =
        QName.create(SupportingNode.QNAME, "topology-ref").intern();
    /** Network-topology supporting-node node-ref */
    public static final QName NODE_REF = QName.create(SupportingNode.QNAME, "node-ref").intern();
    /** Network-topology link-ref QName */
    public static final QName LINK_REF = QName.create(Link.QNAME, "link-ref").intern();
    /** Network-topology termination-point tp-ref */
    public static final QName TP_REF = QName.create(TerminationPoint.QNAME, "tp-ref").intern();

    /**Inventory QName */
    public static final QName NODE_CONNECTOR_ID_QNAME =
        QName.create(NodeConnector.QNAME, "id").intern();
    /** Opendaylight-inventory node-id QName */
    public static final QName INVENTORY_NODE_ID_QNAME =
        QName.create("urn:opendaylight:inventory", "2013-08-19", "id").intern();
    public static final QName INVENTORY_NODE_CONNECTOR_REF_QNAME = QName
        .create("urn:opendaylight:model:topology:inventory", "2013-10-30",
            "inventory-node-connector-ref").intern();
    public static final QName INVENTORY_NODE_REF_QNAME = QName
        .create("urn:opendaylight:model:topology:inventory", "2013-10-30", "inventory-node-ref")
        .intern();
    public static final QName INVENTORY_NODE_QNAME =
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME;

    /**Correlation augment QName**/
    public static final QName TOPOLOGY_CORRELATION_AUGMENT =
        QName.create("urn:opendaylight:topology:correlation", "2015-01-21", "correlations")
            .intern();
    /**Link computation augment QName**/
    public static final QName LINK_COMPUTATION_AUGMENT =
        QName.create("urn:opendaylight:link:computation", "2015-08-24", "link-computation")
            .intern();
    /**Network topology source-node QName**/
    public static final QName LINK_SOURCE_NODE_QNAME =
        QName.create(Source.QNAME, "source-node").intern();
    /**Network topology source-tp QName**/
    public static final QName LINK_SOURCE_TP_QNAME =
        QName.create(Source.QNAME, "source-tp").intern();
    /**Network topology dest-node QName**/
    public static final QName LINK_DEST_NODE_QNAME =
        QName.create(Destination.QNAME, "dest-node").intern();
    /**Network topology dest-tp QName**/
    public static final QName LINK_DEST_TP_QNAME =
        QName.create(Destination.QNAME, "dest-tp").intern();

    private TopologyQNames() {
        throw new UnsupportedOperationException("TopologyQNames can't be instantiated.");
    }

    /**
     * Returns corresponding item QName
     * @param correlationItem item type
     * @return corresponding item QName
     */
    public static QName buildItemQName(CorrelationItemEnum correlationItem, Class<? extends Model> model) {
        QName itemQName;
        if(!model.equals(I2rsModel.class)) {
            switch (correlationItem) {
            case Node:
                itemQName = Node.QNAME;
                break;
            case Link:
                itemQName = Link.QNAME;
                break;
            case TerminationPoint:
                itemQName = TerminationPoint.QNAME;
                break;
            default:
                throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
            }
        } else {
            switch (correlationItem) {
            case Node:
                itemQName = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                        .network.rev150608.network.Node.QNAME;
                break;
            case Link:
                itemQName = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                        .network.topology.rev150608.network.Link.QNAME;
                break;
            case TerminationPoint:
                itemQName = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                        .network.topology.rev150608.network.node.TerminationPoint.QNAME;
                break;
            default:
                throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
            }
        }
        return itemQName;
    }

    /**
     * Returns corresponding item id QName
     * @param correlationItem item type
     * @return corresponding item id QName
     */
    public static QName buildItemIdQName(CorrelationItemEnum correlationItem, Class<? extends Model> model) {
        QName itemIdQName;
        switch (correlationItem) {
        case Node:
            itemIdQName = ! model.equals(I2rsModel.class) ? NETWORK_NODE_ID_QNAME : I2RS_NODE_ID_QNAME;
            break;
        case Link:
            itemIdQName = ! model.equals(I2rsModel.class) ? NETWORK_LINK_ID_QNAME : I2RS_LINK_ID_QNAME;
            break;
        case TerminationPoint:
            itemIdQName = ! model.equals(I2rsModel.class) ? NETWORK_TP_ID_QNAME : I2RS_TP_ID_QNAME;
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
        }
        return itemIdQName;
    }
}
