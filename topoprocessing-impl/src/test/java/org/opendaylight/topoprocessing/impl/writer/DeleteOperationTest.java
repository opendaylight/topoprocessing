/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.writer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DeleteOperationTest {

    private final static String TOPOLOGY_ID = "mytopo:1";
    private final YangInstanceIdentifier topologyIdentifier = YangInstanceIdentifier
            .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY_ID).build();

    @Mock private DOMDataWriteTransaction mockDomDataWriteTransaction;

    @Test(expected=NullPointerException.class)
    public void testClassCreationWithNullIdentifier() {
        DeleteOperation deleteOperation = new DeleteOperation(null);
    }

    @Test
    public void testCallingDeleteOperation() {
        DeleteOperation deleteOperation = new DeleteOperation(topologyIdentifier);
        deleteOperation.addOperationIntoTransaction(mockDomDataWriteTransaction);
        Mockito.verify(mockDomDataWriteTransaction).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
                Mockito.eq(topologyIdentifier));
    }
}
