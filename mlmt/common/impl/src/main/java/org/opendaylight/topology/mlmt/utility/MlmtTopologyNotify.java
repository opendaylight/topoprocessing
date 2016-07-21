/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MlmtTopologyNotify implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtTopologyNotify.class);
    private final BlockingQueue<MlmtTopologyUpdate> notifyQ;
    private static final int NOTIFY_Q_LENGTH_DEFAULT = 54;
    private static final int THREAD_SLEEP = 100;
    private MlmtTopologyUpdate entry;
    private MlmtTopologyUpdateListener listener;
    private volatile boolean finishing = false;

    public MlmtTopologyNotify(final MlmtTopologyUpdateListener listener) {
        this(listener, NOTIFY_Q_LENGTH_DEFAULT);
    }

    public MlmtTopologyNotify(final MlmtTopologyUpdateListener listener, final int queueLength) {
        this.listener = listener;
        this.notifyQ = new LinkedBlockingQueue<MlmtTopologyUpdate>(queueLength);
    }

    public void add(MlmtTopologyUpdate update) {
        this.notifyQ.add(update);
    }

    @Override
    public void run() {
        while (!finishing) {
            try {
                // First we block waiting for an element to get in
                entry = notifyQ.take();
                listener.update(entry);
                // Lets sleep for sometime to allow aggregation of event
                Thread.sleep(THREAD_SLEEP);
            } catch (final InterruptedException e) {
                LOG.error("MlmtTopologyNotify Thread interrupted", e);
                finishing = true;
            } catch (final Exception e) {
                LOG.error("MlmtTopologyNotify Thread exception", e);
            }
        }
        cleanNotifyQueue();
    }

    private void cleanNotifyQueue() {
        while (!notifyQ.isEmpty()) {
            notifyQ.poll();
        }
    }
}
