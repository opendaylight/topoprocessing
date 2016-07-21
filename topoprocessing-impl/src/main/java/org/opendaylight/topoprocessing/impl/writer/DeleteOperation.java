/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.writer;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Stores information that will be used for transaction configuration (.delete() case)
 * @author michal.polkorab
 */
public class DeleteOperation implements TransactionOperation {

    private final YangInstanceIdentifier identifier;

    /**
     * @param identifier points at place where data should be deleted
     */
    public DeleteOperation(YangInstanceIdentifier identifier) {
        Preconditions.checkNotNull(identifier);
        this.identifier = identifier;
    }

    @Override
    public void addOperationIntoTransaction(DOMDataWriteTransaction transaction) {
        transaction.delete(LogicalDatastoreType.OPERATIONAL, identifier);
    }

}
