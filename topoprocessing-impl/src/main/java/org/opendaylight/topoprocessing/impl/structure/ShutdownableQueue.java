/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.Future;
import org.opendaylight.topoprocessing.impl.writer.TransactionOperation;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * {@link ConcurrentLinkedQueue} extended with option to stop further processing
 * and to delegate the stopped event information
 * @author michal.polkorab
 */
public class ShutdownableQueue extends ConcurrentLinkedQueue<TransactionOperation> {

    private static final long serialVersionUID = -5016560105452768389L;
    private boolean acceptNewEntries = true;
    private SettableFuture<Boolean> shutdownFuture;

    /**
     * Prepares this queue for shutdown - meaning that no new {@link TransactionOperation}
     * will be added into the queue
     * @return shutdown future - completed when all already present {@link TransactionOperation}s
     *  are written
     */
    public Future<Boolean> signalShutdown() {
        acceptNewEntries = false;
        shutdownFuture = SettableFuture.create();
        return shutdownFuture;
    }

    @Override
    public boolean add(TransactionOperation e) {
        if (acceptNewEntries) {
            return super.add(e);
        }
        return false;
    }

    /**
     * Checks if this queue can be shut down - the shutdown flag is set and there are
     * no pending {@link TransactionOperation}s left
     * @return true if this queue can be shut down
     */
    public boolean canShutdown() {
        return (! acceptNewEntries) && isEmpty();
    }

    /**
     * Finally shuts down this queue
     */
    public void shutdown() {
        shutdownFuture.set(true);
    }
}
