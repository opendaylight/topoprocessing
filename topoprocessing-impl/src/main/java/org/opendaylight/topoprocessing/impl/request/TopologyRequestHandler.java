/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationBase;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationAggregation;
import org.opendaylight.topoprocessing.impl.operator.PreAggregationFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.Filtration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.Aggregation;
import com.google.common.base.Preconditions;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Picks up information from topology request, engages corresponding
 * listeners, aggregators.
 * @author michal.polkorab
 */
public class TopologyRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRequestHandler.class);
    private static final int CLOSE_RESOURCES_SLEEP_TIME = 500;
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
     * @param schemaHolder  provides model search
     * @param rpcServices   rpc services needed for rpc republishing
     */
    public TopologyRequestHandler(DOMDataBroker domDataBroker, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices) {
        this.domDataBroker = domDataBroker;
        this.schemaHolder = schemaHolder;
        this.rpcServices = rpcServices;
    }

    /**
     * Only for testing purposes
     * @param translator Provides translating String to Path
     */
    public void setTranslator(PathTranslator translator) {
        this.translator = translator;
    }

    /**
     * Only for testing purposes
     * @param listeners Sets UnderlayTopologyListener registrations
     */
    public void setListeners(List<ListenerRegistration<DOMDataChangeListener>> listeners) {
        this.listeners = listeners;
    }

    /**
     * Only for testing purposes
     * @return UnderlayTopologyListener registrations
     */
    public List<ListenerRegistration<DOMDataChangeListener>> getListeners() {
        return listeners;
    }

    /**
     * Only for testing purposes
     * @return Transaction Chain
     */
    public DOMTransactionChain getTransactionChain() {
        return transactionChain;
    }

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
                TopologyOperator operator = null;
                if (FiltrationAggregation.class.equals(correlation.getType())) {
                    operator = initAggregation(correlation, true);
                } else if (FiltrationOnly.class.equals(correlation.getType())) {
                    operator = initFiltration(correlation);
                } else if (AggregationOnly.class.equals(correlation.getType())) {
                    operator = initAggregation(correlation, false);
                } else {
                    throw new IllegalStateException("Filtration and Aggregation data missing: " + correlation);
                }
                operator.setTopologyManager(topologyManager);
            }
            LOG.debug("Correlation configuration successfully read");
        } catch (Exception e) {
            LOG.warn("Processing new request for topology change failed.", e);
            closeOperatingResources();
            throw new IllegalStateException("Processing new request for topology change failed.", e);
        }
    }

    /**
     * @param correlation contains filtration configuration
     * @return configured {@link TopologyFiltrator}
     */
    private TopologyFiltrator initFiltration(Correlation correlation) {
        CorrelationItemEnum correlationItem = correlation.getCorrelationItem();
        Filtration filtration = correlation.getFiltration();
        String underlayTopologyId = filtration.getUnderlayTopology();
        TopologyFiltrator filtrator = new TopologyFiltrator();
        for (Filter filter : filtration.getFilter()) {
            filtrator.initializeStore(underlayTopologyId, false);
            YangInstanceIdentifier pathIdentifier = translator.translate(filter.getTargetField().getValue(),
                    correlation.getCorrelationItem(), schemaHolder);
            addFiltrator(filtrator, filter, pathIdentifier);
            UnderlayTopologyListener listener = new UnderlayTopologyListener(domDataBroker, filtrator,
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
            listener.readExistingData(itemIdentifier, datastoreType);
            listeners.add(listenerRegistration);
        }
        return filtrator;
    }

    private void addFiltrator(TopologyFiltrator operator, Filter filter,
            YangInstanceIdentifier pathIdentifier) {
        FiltratorFactory ff = filtrators.get(filter.getFilterType());
        Filtrator currentFiltrator = ff.createFiltrator(filter, pathIdentifier);
        operator.addFilter(currentFiltrator);
    }

    private TopologyAggregator initAggregation(Correlation correlation, boolean filtration) {
        CorrelationItemEnum correlationItem = correlation.getCorrelationItem();
        Aggregation aggregation = correlation.getAggregation();
        TopologyAggregator aggregator = createAggregator(aggregation.getAggregationType());
        if (aggregation.getScripting() != null) {
            aggregator.initCustomAggregation(aggregation.getScripting());
        }
        for (Mapping mapping : aggregation.getMapping()) {
            String underlayTopologyId = mapping.getUnderlayTopology();
            aggregator.initializeStore(underlayTopologyId, mapping.isAggregateInside());
            YangInstanceIdentifier pathIdentifier = translator.translate(mapping.getTargetField().getValue(),
                    correlationItem, schemaHolder);
            UnderlayTopologyListener listener = null;
            if (filtration && mapping.getApplyFilters() != null) {
                PreAggregationFiltrator filtrator = new PreAggregationFiltrator();
                filtrator.initializeStore(underlayTopologyId, false);
                filtrator.setTopologyAggregator(aggregator);
                for (String filterId : mapping.getApplyFilters()) {
                    Filter filter = findFilter(correlation.getFiltration().getFilter(), filterId);
                    YangInstanceIdentifier filterPath = translator.translate(filter.getTargetField().getValue(),
                            correlationItem, schemaHolder);
                    addFiltrator(filtrator, filter, filterPath);
                }
                listener = new UnderlayTopologyListener(domDataBroker, filtrator, underlayTopologyId,
                        pathIdentifier, correlationItem);
            } else {
                listener = new UnderlayTopologyListener(domDataBroker, aggregator, underlayTopologyId,
                        pathIdentifier, correlationItem);
            }
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
            listener.readExistingData(itemIdentifier, datastoreType);
            listeners.add(listenerRegistration);
        }
        return aggregator;
    }

/**
 * @param filters 
 * @param filterId
 * @return
 */
private Filter findFilter(List<Filter> filters, String filterId) {
    for (Filter filter : filters) {
        if (filterId.equals(filter.getFilterId())) {
            return filter;
        }
    }
    return null;
}

    private TopologyAggregator createAggregator(Class<? extends AggregationBase> type) {
        if (Equality.class.equals(type)) {
            return new EqualityAggregator();
        } else if (Unification.class.equals(type)) {
            return new UnificationAggregator();
        } else {
            throw new IllegalArgumentException("Unsupported correlation type received: " + type);
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
        try {
            Thread.sleep(CLOSE_RESOURCES_SLEEP_TIME);
        } catch (InterruptedException e) {
            LOG.error("An error occurred while Thread.sleep was called: {}", e);
        }
        if (transactionChain != null) {
            try {
                transactionChain.close();
            } catch (Exception e) {
                LOG.error("An error occurred while closing transaction chain: {}", transactionChain, e);
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
     * @param datastoreType configures whether to use CONFIGURATION or OPERATIONAL datastore
     */
    public void setDatastoreType(DatastoreType datastoreType) {
        this.datastoreType = datastoreType;
    }

    /**
     * @param filtrators sets default and registered filtrators
     */
    public void setFiltrators(Map<Class<? extends FilterBase>, FiltratorFactory> filtrators) {
        this.filtrators = filtrators;
    }
}
