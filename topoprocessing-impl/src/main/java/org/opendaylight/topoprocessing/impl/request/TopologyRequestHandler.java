/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.EqualityAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.operator.UnificationAggregator;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Filtration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.FiltrationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.UnificationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.filtration._case.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Picks up information from topology request, engages corresponding
 * listeners, aggregators.
 * @author michal.polkorab
 */
public class TopologyRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRequestHandler.class);
    private DOMDataBroker domDataBroker;
    private Topology topology;
    private PathTranslator translator = new PathTranslator();
    private List<ListenerRegistration<DOMDataChangeListener>> listeners = new ArrayList<>();

    private GlobalSchemaContextHolder schemaHolder;
    private RpcServices rpcServices;
    private DOMTransactionChain transactionChain;
    private TopologyWriter writer;
    private DatastoreType datastoreType;
    private Map<Class<? extends FilterBase>, FiltratorFactory> filtrators;

    /**
     * Default constructor
     * @param domDataBroker broker used for transaction operations
     * @param schemaHolder
     * @param rpcServices
     */
    public TopologyRequestHandler(DOMDataBroker domDataBroker, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices) {
        this.domDataBroker = domDataBroker;
        this.schemaHolder = schemaHolder;
        this.rpcServices = rpcServices;
    }

    /** Only for testing purposes */
    public void setTranslator(PathTranslator translator) { this.translator = translator; }

    /** Only for testing purposes */
    public void setListeners(List<ListenerRegistration<DOMDataChangeListener>> listeners) {
        this.listeners = listeners;
    }

    /** Only for testing purposes */
    public List<ListenerRegistration<DOMDataChangeListener>> getListeners() { return listeners; }

    /** Only for testing purposes */
    public DOMTransactionChain getTransactionChain() { return transactionChain; }

    /**
     * @param topology overlay topology request
     */
    public void processNewRequest(Topology topology) {
        LOG.debug("Processing overlay topology creation request");
        Preconditions.checkNotNull(topology, "Received topology can't be null");
        this.topology = topology;
        String overlayTopologyId = topology.getTopologyId().getValue();
        writer = new TopologyWriter(overlayTopologyId);
        transactionChain = domDataBroker.createTransactionChain(writer);
        writer.setTransactionChain(transactionChain);
        TopologyManager topologyManager = new TopologyManager(rpcServices, schemaHolder,
                createTopologyIdentifier(overlayTopologyId).build());
        topologyManager.setWriter(writer);
        writer.initOverlayTopology();
        try {
            LOG.debug("Processing correlation configuration");
            CorrelationAugment augmentation = topology.getAugmentation(CorrelationAugment.class);
            List<Correlation> correlations = augmentation.getCorrelations().getCorrelation();
            for (Correlation correlation : correlations) {
                TopologyOperator operator;
                if (correlation.getType().equals(Equality.class)) {
                    EqualityCase equalityCase = (EqualityCase) correlation.getCorrelationType();
                    List<Mapping> mappings = equalityCase.getEquality().getMapping();
                    operator = new EqualityAggregator();
                    operator.setTopologyManager(topologyManager);
                    iterateMappings(operator, mappings, correlation.getCorrelationItem());
                }
                else if (correlation.getType().equals(Unification.class)) {
                    UnificationCase unificationCase = (UnificationCase) correlation.getCorrelationType();
                    List<Mapping> mappings = unificationCase.getUnification().getMapping();
                    operator = new UnificationAggregator();
                    operator.setTopologyManager(topologyManager);
                    iterateMappings(operator, mappings, correlation.getCorrelationItem());
                } else if (correlation.getType().equals(Filtration.class)) {
                    FiltrationCase filtrationCase = (FiltrationCase) correlation.getCorrelationType();
                    List<Filter> filters = filtrationCase.getFiltration().getFilter();
                    operator = new TopologyFiltrator();
                    operator.setTopologyManager(topologyManager);
                    iterateFilters((TopologyFiltrator) operator, filters, correlation.getCorrelationItem());
                } else {
                    throw new IllegalStateException("Unknown correlation: " + correlation.getType());
                }
            }
            LOG.debug("Correlation configuration successfully read");
        } catch (Exception e) {
            LOG.warn("Processing new request for topology change failed.", e);
            closeOperatingResources();
            throw new IllegalStateException("Processing new request for topology change failed.", e);
        }
    }

    private void iterateFilters(TopologyFiltrator operator, List<Filter> filters, CorrelationItemEnum correlationItem) {
        for (Filter filter : filters) {
            String underlayTopologyId = filter.getUnderlayTopology();
            operator.initializeStore(underlayTopologyId, false);
            YangInstanceIdentifier pathIdentifier = translator.translate(filter.getTargetField().getValue(),
                    correlationItem, schemaHolder);
            addFiltrator(operator, filter, pathIdentifier);
            UnderlayTopologyListener listener = new UnderlayTopologyListener(domDataBroker, operator,
                    underlayTopologyId, null, correlationItem);
            InstanceIdentifierBuilder topologyIdentifier =
                    createTopologyIdentifier(underlayTopologyId);
            YangInstanceIdentifier itemIdentifier =
                    InstanceIdentifiers.buildItemIdentifier(topologyIdentifier, correlationItem);
            LOG.debug("Registering filtering underlay topology listener for topology: {}", underlayTopologyId);
            ListenerRegistration<DOMDataChangeListener> listenerRegistration;
            if (datastoreType.equals(DatastoreType.OPERATIONAL)) {
                listenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.OPERATIONAL, itemIdentifier, listener, DataChangeScope.SUBTREE);
            } else {
                listenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.CONFIGURATION, itemIdentifier, listener, DataChangeScope.SUBTREE);
            }
            listener.readExistingData(nodeIdentifier, datastoreType);
            listeners.add(listenerRegistration);
        }
    }

    private void addFiltrator(TopologyFiltrator operator, Filter filter,
            YangInstanceIdentifier pathIdentifier) {
        FiltratorFactory ff = filtrators.get(filter.getFilterType());
        Filtrator currentFiltrator = ff.createFiltrator(filter, pathIdentifier);
        operator.addFilter(currentFiltrator);
    }

    private void iterateMappings(TopologyOperator operator, List<Mapping> mappings, CorrelationItemEnum correlationItem) {
        for (Mapping mapping : mappings) {
            String underlayTopologyId = mapping.getUnderlayTopology();
            operator.initializeStore(underlayTopologyId, mapping.isAggregateInside());
            YangInstanceIdentifier pathIdentifier = translator.translate(mapping.getTargetField().getValue(),
                    correlationItem, schemaHolder);
            UnderlayTopologyListener listener = new UnderlayTopologyListener(domDataBroker, operator,
                    underlayTopologyId, pathIdentifier, correlationItem);
            InstanceIdentifierBuilder topologyIdentifier =
                    createTopologyIdentifier(underlayTopologyId);
            YangInstanceIdentifier itemIdentifier =
                    InstanceIdentifiers.buildItemIdentifier(topologyIdentifier, correlationItem);
            LOG.debug("Registering underlay topology listener for topology: {}", underlayTopologyId);
            ListenerRegistration<DOMDataChangeListener> listenerRegistration;
            if (datastoreType.equals(DatastoreType.OPERATIONAL)) {
                listenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.OPERATIONAL, itemIdentifier, listener, DataChangeScope.SUBTREE);
            } else {
                listenerRegistration = domDataBroker.registerDataChangeListener(
                        LogicalDatastoreType.CONFIGURATION, itemIdentifier, listener, DataChangeScope.SUBTREE);
            }
            listener.readExistingData(nodeIdentifier, datastoreType);
            listeners.add(listenerRegistration);
        }
    }

    private static InstanceIdentifierBuilder createTopologyIdentifier(
            String underlayTopologyId) {
        InstanceIdentifierBuilder identifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId);
        return identifier;
    }

    /**
     * @return ID of topology that is handled by this {@link TopologyRequestHandler}
     */
    public String getTopologyId() {
        return topology.getTopologyId().toString();
    }

    /**
     * Closes all registered listeners and providers
     */
    public void processDeletionRequest() {
        LOG.debug("Processing overlay topology deletion request");
        closeOperatingResources();
    }

    private void closeOperatingResources() {
        for (ListenerRegistration<DOMDataChangeListener> listener : listeners) {
            listener.close();
        }
        listeners.clear();
        writer.deleteOverlayTopology();
        if (transactionChain != null) {
            try {
                transactionChain.close();
            } catch (Exception e) {
                LOG.error("An error occurred while closing transaction chain: {}",transactionChain, e);
            }
        }
    }

    /**
     * Delegate topology types to writer
     * @param topologyTypes - taken from overlay topology request
     */
    public void delegateTopologyTypes(
            DataContainerChild<? extends PathArgument, ?> topologyTypes) {
        writer.writeTopologyTypes(topologyTypes);
    }

    /**
     * @param datastoreType
     */
    public void setDatastoreType(DatastoreType datastoreType) {
        this.datastoreType = datastoreType;
    }

    /**
     * @param filtrators
     */
    public void setFiltrators(Map<Class<? extends FilterBase>, FiltratorFactory> filtrators) {
        this.filtrators = filtrators;
    }
}
