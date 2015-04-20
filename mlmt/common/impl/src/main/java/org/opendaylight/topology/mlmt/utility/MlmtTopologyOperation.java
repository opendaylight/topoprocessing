/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

/**
 * Internal interface for submitted operations. Implementations of this
 * interface are enqueued and batched into data store transactions.
 */
public abstract class MlmtTopologyOperation {
    /**
     * Execute the operation on top of the transaction.
     *
     * @param transaction Datastore transaction
     */
    abstract public void applyOperation(ReadWriteTransaction transaction);

    public boolean isCommitNow() { return false; }
}
