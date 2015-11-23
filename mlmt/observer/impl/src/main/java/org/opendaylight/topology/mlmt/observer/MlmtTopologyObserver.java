/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.observer;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyBuilder;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.topology.mlmt.factory.MlmtProviderFactoryImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mlmt.topology.observer.impl.rev150122.MlmtTopologyObserverRuntimeMXBean;

public class MlmtTopologyObserver extends AbstractMlmtTopologyObserver implements AutoCloseable, MlmtTopologyObserverRuntimeMXBean {

    private MlmtOperationProcessor processor;
    private Thread thread;

    @Override
    public void init(DataBroker dataBroker, RpcProviderRegistry rpcRegistry,
            String topologyName, List<String> underlyingTopologyName) {
        LOG.info("MlmtTopologyObserver.init");
        this.dataBroker = dataBroker;
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();

        if (topologyName != null) {
            MLMT = topologyName;
        }

        mlmtTopologyId = buildTopologyIid(MLMT);
        mlmtTopologyBuilder = new MlmtTopologyBuilder();
        mlmtTopologyBuilder.init(dataBroker, LOG, processor);
        underlayTopologies = new ArrayList<String>();

        mlmtProviderFactory = new MlmtProviderFactoryImpl();
        Map<String, List<MlmtTopologyProvider>> providersMap =
                mlmtProviderFactory.createProvidersMap(rpcRegistry, dataBroker, LOG, processor, MLMT);
        mlmtProviders = providersMap.get(MLMT);

        mapConfigurationDataChangeObserver = new ArrayList<MlmtDataChangeEventListener>();
        mapOperationalDataChangeObserver = new ArrayList<MlmtDataChangeEventListener>();

        registerDataChangeEventListener(LOG, LogicalDatastoreType.CONFIGURATION, mlmtTopologyId);
    }

    @Override
    public synchronized void close() throws InterruptedException {
        LOG.info("MlmtTopologyObserver stopped.");
        closeListeners();
        if (thread != null) {
            thread.interrupt();
            thread.join();
            thread = null;
        }
    }
 }
