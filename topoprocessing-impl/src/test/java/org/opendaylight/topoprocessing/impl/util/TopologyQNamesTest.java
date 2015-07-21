/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import static org.junit.Assert.*;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.junit.Test;

/**
 * @author michal.polkorab
 *
 */
public class TopologyQNamesTest {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "node-id");
    private static final QName LINK_ID_QNAME = QName.create(Link.QNAME, "link-id");
    private static final QName TP_ID_QNAME = QName.create(TerminationPoint.QNAME, "tp-id");

    /**
     * Tests item-id QNames
     */
    @Test
    public void testItemIdQNames() {
        assertEquals("Wrong QName", NODE_ID_QNAME, TopologyQNames.buildItemIdQName(CorrelationItemEnum.Node));
        assertEquals("Wrong QName", LINK_ID_QNAME, TopologyQNames.buildItemIdQName(CorrelationItemEnum.Link));
        assertEquals("Wrong QName", TP_ID_QNAME,
                TopologyQNames.buildItemIdQName(CorrelationItemEnum.TerminationPoint));
    }

    /**
     * Tests item QNames
     */
    @Test
    public void testItemQNames() {
        assertEquals("Wrong QName", Node.QNAME, TopologyQNames.buildItemQName(CorrelationItemEnum.Node));
        assertEquals("Wrong QName", Link.QNAME, TopologyQNames.buildItemQName(CorrelationItemEnum.Link));
        assertEquals("Wrong QName", TerminationPoint.QNAME,
                TopologyQNames.buildItemQName(CorrelationItemEnum.TerminationPoint));
    }
}
