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
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.EqualityAggregator;
import org.opendaylight.topoprocessing.impl.operator.PreAggregationFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TerminationPointAggregator;
import org.opendaylight.topoprocessing.impl.operator.TerminationPointFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationAggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Aggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Filtration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
/**
 * Picks up information from topology request, engages corresponding
 * listeners, aggregators.
 * @author michal.polkorab
 */
public abstract class TopologyRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRequestHandler.class);
    private PingPongDataBroker pingPongDataBroker;
    private PathTranslator translator = new PathTranslator();
    private List<ListenerRegistration<DOMDataTreeChangeListener>> listeners = new ArrayList<>();
    private String topologyId;
    private GlobalSchemaContextHolder schemaHolder;
    private RpcServices rpcServices;
    private DOMTransactionChain transactionChain;
    private TopologyWriter writer;
    private DatastoreType datastoreType;
    private Map<Class<? extends FilterBase>, FiltratorFactory> filtrators;
    private Map<Model, ModelAdapter> modelAdapters;
    private Map.Entry<InstanceIdentifier<?>,DataObject> fromNormalizedNode;
    private TopologyManager topologyManager;
    private Model outputModel;

    /**
     * Default constructor
     * @param dataBroker         broker used for transaction operations
     * @param schemaHolder          provides model search
     * @param rpcServices           rpc services needed for rpc republishing
     * @param fromNormalizedNode    Normalized node with topology information
     */
    public TopologyRequestHandler(DOMDataBroker dataBroker, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices,Map.Entry<InstanceIdentifier<?>,DataObject> fromNormalizedNode) {
        if (dataBroker instanceof PingPongDataBroker) {
            this.pingPongDataBroker = (PingPongDataBroker) dataBroker;
        } else {
            throw new IllegalArgumentException("Received dom-data-broker instance is different than expected."
                    + "Expected: pingpong-broker (in 01-md-sal.xml); Actual: " + dataBroker);
        }
        this.schemaHolder = schemaHolder;
        this.rpcServices = rpcServices;
        this.fromNormalizedNode = fromNormalizedNode;
        this.topologyId = getTopologyId(fromNormalizedNode);
        writer = new TopologyWriter(topologyId);
        outputModel = getModel(fromNormalizedNode);
    }

    protected abstract Model getModel(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

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
    public void setListeners(List<ListenerRegistration<DOMDataTreeChangeListener>> listeners) {
        this.listeners = listeners;
    }

    /**
     * Only for testing purposes
     * @return UnderlayTopologyListener registrations
     */
    public List<ListenerRegistration<DOMDataTreeChangeListener>> getListeners() {
        return listeners;
    }

    /**
     * Only for testing purposes
     * @return Transaction Chain
     */
    public DOMTransactionChain getTransactionChain() {
        return transactionChain;
    }

    public void setModelAdapters(Map<Model, ModelAdapter> modelAdapters) {
        this.modelAdapters = modelAdapters;
        writer.setTranslator(modelAdapters.get(outputModel).createOverlayItemTranslator());
        transactionChain = pingPongDataBroker.createTransactionChain(writer);
        writer.setTransactionChain(transactionChain);
        topologyManager = new TopologyManager(rpcServices, schemaHolder,
                createTopologyIdentifier(topologyId).build());
        topologyManager.setWriter(writer);
        writer.initOverlayTopology();

    }

    protected abstract String getTopologyId(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

    /**
     * Process new topology request
     */
    public void processNewRequest() {
        LOG.debug("Processing overlay topology creation request");
        Correlations correlations = getCorrelations(fromNormalizedNode);
        Preconditions.checkNotNull(correlations, "Received correlations can't be null");
        try {
            LOG.debug("Processing correlation configuration");
            List<Correlation> correlationList = correlations.getCorrelation();
            for (Correlation correlation : correlationList) {
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
        TopologyFiltrator filtrator;
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        topoStoreProvider.initializeStore(underlayTopologyId, false);
        if (correlationItem.equals(CorrelationItemEnum.TerminationPoint)) {
            filtrator = new TerminationPointFiltrator(topoStoreProvider);
        } else {
            filtrator = new TopologyFiltrator(topoStoreProvider);
        }
        for (Filter filter : filtration.getFilter()) {
            YangInstanceIdentifier pathIdentifier = translator.translate(filter.getTargetField().getValue(),
            correlation.getCorrelationItem(), schemaHolder, filter.getInputModel());
            addFiltrator(filtrator, filter, pathIdentifier);
            UnderlayTopologyListener listener = modelAdapters.get(filter.getInputModel()).
                    registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                    correlationItem, datastoreType, filtrator, listeners);


            InstanceIdentifierBuilder topologyIdentifier =
                    createTopologyIdentifier(underlayTopologyId);
            YangInstanceIdentifier itemIdentifier = buildListenerIdentifier(topologyIdentifier, correlationItem);
            LOG.debug("Registering filtering underlay topology listener for topology: {}", underlayTopologyId);
            ListenerRegistration<DOMDataTreeChangeListener> listenerRegistration = null;
            if (datastoreType.equals(DatastoreType.OPERATIONAL)) {
                DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, itemIdentifier);
                listenerRegistration = pingPongDataBroker.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) listener);
            } else {
                DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, itemIdentifier);
                listenerRegistration = pingPongDataBroker.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) listener);
            }
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
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        TopologyAggregator aggregator = createAggregator(aggregation.getAggregationType(),
                correlationItem, topoStoreProvider);
        if (aggregation.getScripting() != null) {
            aggregator.initCustomAggregation(aggregation.getScripting());
        }
        for (Mapping mapping : aggregation.getMapping()) {
            String underlayTopologyId = mapping.getUnderlayTopology();
            topoStoreProvider.initializeStore(underlayTopologyId, mapping.isAggregateInside());
            YangInstanceIdentifier pathIdentifier = translator.translate(mapping.getTargetField().getValue(),
                    correlationItem, schemaHolder, mapping.getInputModel());
            if (aggregator instanceof TerminationPointAggregator) {
                ((TerminationPointAggregator) aggregator).setTargetField(pathIdentifier);
            }
            PreAggregationFiltrator filtrator= null;
            if (filtration && mapping.getApplyFilters() != null) {
                filtrator = new PreAggregationFiltrator(topoStoreProvider);
                filtrator.setTopologyAggregator(aggregator);
                for (String filterId : mapping.getApplyFilters()) {
                    Filter filter = findFilter(correlation.getFiltration().getFilter(), filterId);
                    YangInstanceIdentifier filterPath = translator.translate(filter.getTargetField().getValue(),
                            correlationItem, schemaHolder, filter.getInputModel());
                    addFiltrator(filtrator, filter, filterPath);
                }
            }
            UnderlayTopologyListener listener;
            if(filtrator == null) {
                listener = modelAdapters.get(mapping.getInputModel()).registerUnderlayTopologyListener(pingPongDataBroker,
                                underlayTopologyId, correlationItem, datastoreType, aggregator, listeners);
            } else {
                listener = modelAdapters.get(mapping.getInputModel()).registerUnderlayTopologyListener(pingPongDataBroker,
                                underlayTopologyId, correlationItem, datastoreType, filtrator, listeners);
            }
            listener.setPathIdentifier(pathIdentifier);

            InstanceIdentifierBuilder topologyIdentifier = createTopologyIdentifier(underlayTopologyId);
            YangInstanceIdentifier itemIdentifier = buildListenerIdentifier(topologyIdentifier, correlationItem);
            LOG.debug("Registering underlay topology listener for topology: {}", underlayTopologyId);
            ListenerRegistration<DOMDataTreeChangeListener> listenerRegistration = null;

            if (datastoreType.equals(DatastoreType.OPERATIONAL)) {
                DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, itemIdentifier);
                listenerRegistration = pingPongDataBroker.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) listener);
            } else {
                DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, itemIdentifier);
                listenerRegistration = pingPongDataBroker.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) listener);
            }
            listeners.add(listenerRegistration);
        }
        return aggregator;
    }

    private Filter findFilter(List<Filter> filters, String filterId) {
        for (Filter filter : filters) {
            if (filterId.equals(filter.getFilterId())) {
                return filter;
            }
        }
        return null;
    }

    private TopologyAggregator createAggregator(Class<? extends AggregationBase> type, CorrelationItemEnum item,
            TopoStoreProvider topoStoreProvider) {
        TopologyAggregator aggregator;
        if (CorrelationItemEnum.TerminationPoint.equals(item)) {
            aggregator = new TerminationPointAggregator(topoStoreProvider);
        } else if (Equality.class.equals(type)) {
            aggregator = new EqualityAggregator(topoStoreProvider);
        } else if (Unification.class.equals(type)) {
            aggregator = new UnificationAggregator(topoStoreProvider);
        } else {
            throw new IllegalArgumentException("Unsupported correlation type received: " + type);
        }
        return aggregator;
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
        return this.topologyId;
    }

    protected abstract Correlations getCorrelations(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

    /**
     * Closes all registered listeners and providers
     */
    public void processDeletionRequest() {
        LOG.debug("Processing overlay topology deletion request");
        closeOperatingResources();
    }

    private void closeOperatingResources() {
        for (ListenerRegistration<DOMDataTreeChangeListener> listener : listeners) {
            listener.close();
        }
        listeners.clear();
        writer.tearDown();
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

    /**
     * Builds item identifier (identifies item {@link MapNode})
     * @param builder starting builder (set with specific topology) that will be appended
     * with corresponding item QName
     * @param correlationItemEnum item type
     * @return item identifier (identifies item {@link MapNode})
     */
    private YangInstanceIdentifier buildListenerIdentifier(YangInstanceIdentifier.InstanceIdentifierBuilder builder,
            CorrelationItemEnum correlationItemEnum) {
        switch (correlationItemEnum) {
        case Node:
        case TerminationPoint:
            builder.node(Node.QNAME);
            break;
        case Link:
            builder.node(Link.QNAME);
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: "
                    + correlationItemEnum);
        }
        return builder.build();
    }
}
