/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.topology.mlmt.factory.MlmtProviderFactoryImpl;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyBuilder;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MlmtTopologyObserver extends AbstractMlmtTopologyObserver implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtTopologyObserver.class);
    private MlmtOperationProcessor processor;
    private Thread thread;

    @Override
    public void init(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistry,
            final String topologyName, final List<String> underlyingTopologyName) {
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
        mlmtTopologyBuilder.init(dataBroker, processor);
        underlayTopologies = new ArrayList<String>();

        mlmtProviderFactory = new MlmtProviderFactoryImpl();
        Map<String, List<MlmtTopologyProvider>> providersMap =
                mlmtProviderFactory.createProvidersMap(rpcRegistry, dataBroker, processor, MLMT);
        mlmtProviders = providersMap.get(MLMT);

        mapConfigurationDataChangeObserver = new ArrayList<MlmtDataChangeEventListener>();
        mapOperationalDataChangeObserver = new ArrayList<MlmtDataChangeEventListener>();
        registerDataChangeEventListener(LogicalDatastoreType.CONFIGURATION, mlmtTopologyId);

        if (!checkNetworkTopology(LogicalDatastoreType.CONFIGURATION)) {
            mlmtTopologyBuilder.createNetworkTopology(LogicalDatastoreType.CONFIGURATION);
        }
        if (!checkNetworkTopology(LogicalDatastoreType.OPERATIONAL)) {
            mlmtTopologyBuilder.createNetworkTopology(LogicalDatastoreType.OPERATIONAL);
        }
        mlmtTopologyBuilder.createTopology(LogicalDatastoreType.CONFIGURATION, mlmtTopologyId);

        TopologyTypesBuilder topologyTypesBuilder = mlmtProviderFactory.configTopologyTypes();
        if (topologyTypesBuilder != null) {
            mlmtTopologyBuilder.createTopologyTypes(LogicalDatastoreType.CONFIGURATION,
                    mlmtTopologyId, topologyTypesBuilder.build());
        }

        if (underlyingTopologyName == null || underlyingTopologyName.isEmpty()) {
            return;
        }
        for (String name : underlyingTopologyName) {
            final String toporef = "/network-topology/topology[topology-id='topologyname']";
            final String path = toporef.replace("topologyname", name);
            mlmtTopologyBuilder.createUnderlayTopology(LogicalDatastoreType.CONFIGURATION,
                    mlmtTopologyId, new TopologyId(path));
        }
    }

    public void startup() {
        final String topologyName = "mlmt:1";
        init(dataBroker, rpcRegistry, topologyName, null);
    }

    @Override
    public void close() throws InterruptedException {
        LOG.info("MlmtTopologyObserver stopped.");
        closeListeners();
        if (thread != null) {
            thread.interrupt();
            thread.join();
            thread = null;
        }
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public RpcProviderRegistry getRpcRegistry() {
        return rpcRegistry;
    }

    public void setRpcRegistry(RpcProviderRegistry rpcRegistry) {
        this.rpcRegistry = rpcRegistry;
    }

    public void setNotificationService(NotificationPublishService notificationService) {
        this.notificationService = notificationService;
    }

    public NotificationPublishService getNotificationService() {
        return notificationService;
    }
}
