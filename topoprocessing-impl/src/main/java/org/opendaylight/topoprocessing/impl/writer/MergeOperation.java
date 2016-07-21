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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Stores information that will be used for transaction configuration (.merge() case)
 * @author matej.perina
 */
public class MergeOperation implements TransactionOperation {

    private final YangInstanceIdentifier identifier;
    private final NormalizedNode<?, ?> node;

    /**
     * @param identifier points at place where data should be updated/merged
     * @param node {@link NormalizedNode} to be updated/merged
     */
    public MergeOperation(YangInstanceIdentifier identifier, NormalizedNode<?, ?> node) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(node);
        this.identifier = identifier;
        this.node = node;
    }

    @Override
    public void addOperationIntoTransaction(DOMDataWriteTransaction transaction) {
        transaction.merge(LogicalDatastoreType.OPERATIONAL, identifier, node);
    }

}
