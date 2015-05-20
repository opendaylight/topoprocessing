/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.writer;

import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;

/**
 * @author michal.polkorab
 *
 */
public interface TransactionOperation {

    /**
     * Adds this operation into provided transaction (.put(), .delete())
     * @param transaction {@link DOMDataWriteTransaction} to be filled with operation
     */
    public void addOperationIntoTransaction(DOMDataWriteTransaction transaction);

}
