/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.writer;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Matchers.eq;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PutOperationTest {

    private final static String TOPOLOGY_ID = "mytopo:1";
    private final YangInstanceIdentifier topologyIdentifier = YangInstanceIdentifier
            .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID).build();

    @Mock private NormalizedNode<?, ?> mockNormalizedNode;
    @Mock private DOMDataWriteTransaction mockDomDataWriteTransaction;

    @Test(expected=NullPointerException.class)
    public void testClassCreationWithNullIdentifier() {
        PutOperation putOperation = new PutOperation(null, mockNormalizedNode);
    }

    @Test(expected=NullPointerException.class)
    public void testClassCreationWithNullNode() {
        PutOperation putOperation = new PutOperation(topologyIdentifier, null);
    }

    @Test
    public void testCallingPutOperation() {
        PutOperation putOperation = new PutOperation(topologyIdentifier, mockNormalizedNode);
        putOperation.addOperationIntoTransaction(mockDomDataWriteTransaction);
        Mockito.verify(mockDomDataWriteTransaction).put(eq(LogicalDatastoreType.OPERATIONAL),
                eq(topologyIdentifier), eq(mockNormalizedNode));
    }
}
