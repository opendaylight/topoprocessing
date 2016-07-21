/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * @author michal.polkorab
 *
 */
public class TopologyQNamesTest {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "node-id").intern();
    private static final QName LINK_ID_QNAME = QName.create(Link.QNAME, "link-id").intern();
    private static final QName TP_ID_QNAME = QName.create(TerminationPoint.QNAME, "tp-id").intern();

    private static final QName I2RS_NODE_QNAME = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.rev150608.network.Node.QNAME;
    private static final QName I2RS_LINK_QNAME = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev150608.network.Link.QNAME;
    private static final QName I2RS_TP_QNAME = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev150608.network.node.TerminationPoint.QNAME;

    private static final QName I2RS_NODE_ID_QNAME = QName.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
            .yang.ietf.network.rev150608.network.Node.QNAME, "node-id").intern();
    private static final QName I2RS_LINK_ID_QNAME = QName.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
            .yang.ietf.network.topology.rev150608.network.Link.QNAME , "link-id").intern();
    private static final QName I2RS_TP_ID_QNAME = QName.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev150608.network.node.TerminationPoint.QNAME , "tp-id").intern();

    /**
     * Tests item-id QNames.
     */
    @Test
    public void testItemIdQNames() {
        // NETWORK TOPOLOGY MODEL
        assertEquals("Wrong QName", NODE_ID_QNAME,
                TopologyQNames.buildItemIdQName(CorrelationItemEnum.Node, NetworkTopologyModel.class));
        assertEquals("Wrong QName", LINK_ID_QNAME,
                TopologyQNames.buildItemIdQName(CorrelationItemEnum.Link, NetworkTopologyModel.class));
        assertEquals("Wrong QName", TP_ID_QNAME,
                TopologyQNames.buildItemIdQName(CorrelationItemEnum.TerminationPoint, NetworkTopologyModel.class));
        // I2RS MODEL
        assertEquals("Wrong QName", I2RS_NODE_ID_QNAME,
                TopologyQNames.buildItemIdQName(CorrelationItemEnum.Node, I2rsModel.class));
        assertEquals("Wrong QName", I2RS_LINK_ID_QNAME,
                TopologyQNames.buildItemIdQName(CorrelationItemEnum.Link, I2rsModel.class));
        assertEquals("Wrong QName", I2RS_TP_ID_QNAME,
                TopologyQNames.buildItemIdQName(CorrelationItemEnum.TerminationPoint, I2rsModel.class));
    }

    /**
     * Tests item QNames.
     */
    @Test
    public void testItemQNames() {
        // NETWORK TOPOLOGY MODEL
        assertEquals("Wrong QName", Node.QNAME,
                TopologyQNames.buildItemQName(CorrelationItemEnum.Node, NetworkTopologyModel.class));
        assertEquals("Wrong QName", Link.QNAME,
                TopologyQNames.buildItemQName(CorrelationItemEnum.Link, NetworkTopologyModel.class));
        assertEquals("Wrong QName", TerminationPoint.QNAME,
                TopologyQNames.buildItemQName(CorrelationItemEnum.TerminationPoint, NetworkTopologyModel.class));
        // I2RS MODEL
        assertEquals("Wrong QName", I2RS_NODE_QNAME,
                TopologyQNames.buildItemQName(CorrelationItemEnum.Node, I2rsModel.class));
        assertEquals("Wrong QName", I2RS_LINK_QNAME,
                TopologyQNames.buildItemQName(CorrelationItemEnum.Link, I2rsModel.class));
        assertEquals("Wrong QName", I2RS_TP_QNAME,
                TopologyQNames.buildItemQName(CorrelationItemEnum.TerminationPoint, I2rsModel.class));
    }
}
