package org.opendaylight.topoprocessing.impl.writer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class DeleteOperationTest {

    private static final YangInstanceIdentifier IDENTIFIER = YangInstanceIdentifier.of(Node.QNAME);
    @Mock private DOMDataWriteTransaction transaction;

    @Test
    public void testAddOperationIntoTransaction() {
        DeleteOperation deleteOperation = new DeleteOperation(IDENTIFIER);
        deleteOperation.addOperationIntoTransaction(transaction);
        Mockito.verify(transaction).delete(LogicalDatastoreType.OPERATIONAL, IDENTIFIER);
    }
}