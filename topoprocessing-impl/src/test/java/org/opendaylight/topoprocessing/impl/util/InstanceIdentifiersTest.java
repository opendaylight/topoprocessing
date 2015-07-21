/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.junit.Assert;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author michal.polkorab
 *
 */
public class InstanceIdentifiersTest {

    /**
     * Tests buildRelativeItemIdIdentifier(...)
     */
    @Test
    public void testRelativeItemIdIdentifier() {
        Assert.assertEquals("Wrong identifiers", YangInstanceIdentifier.of(TopologyQNames.NETWORK_NODE_ID_QNAME),
                        InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node));
        Assert.assertEquals("Wrong identifiers", YangInstanceIdentifier.of(TopologyQNames.NETWORK_LINK_ID_QNAME),
                        InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Link));
        Assert.assertEquals("Wrong identifiers", YangInstanceIdentifier.of(TopologyQNames.NETWORK_TP_ID_QNAME),
                        InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.TerminationPoint));
    }

}
