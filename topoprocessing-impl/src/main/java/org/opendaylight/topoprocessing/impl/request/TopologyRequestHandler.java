/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.request;


import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.opendaylight.topoprocessing.impl.operator.LinkCalculator;
import org.opendaylight.topoprocessing.impl.operator.LinkFiltrator;
import org.opendaylight.topoprocessing.impl.operator.NodeAndTPAggregator;
import org.opendaylight.topoprocessing.impl.operator.NotificationInterConnector;
import org.opendaylight.topoprocessing.impl.operator.PreAggregationFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TerminationPointAggregator;
import org.opendaylight.topoprocessing.impl.operator.TerminationPointFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TerminationPointPreAggregationFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopoStoreProvider;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.operator.UnificationAggregator;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.RenderingOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Aggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Filtration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Rendering;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.LinkComputation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.link.computation.LinkInfo;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private Map<Class<? extends Model>, ModelAdapter> modelAdapters;
    private Map.Entry<InstanceIdentifier<?>,DataObject> fromNormalizedNode;
    private TopologyManager topologyManager;
    private Class<? extends Model> outputModel;

    /**
     * Default constructor.
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
        outputModel = getModel(fromNormalizedNode);
        writer = new TopologyWriter(topologyId, outputModel);
    }

    protected abstract Class<? extends Model> getModel(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

    /**
     * Only for testing purposes.
     * @param translator Provides translating String to Path
     */
    public void setTranslator(PathTranslator translator) {
        this.translator = translator;
    }

    /**
     * Only for testing purposes.
     * @param listeners Sets UnderlayTopologyListener registrations
     */
    public void setListeners(List<ListenerRegistration<DOMDataTreeChangeListener>> listeners) {
        this.listeners = listeners;
    }

    /**
     * Only for testing purposes.
     * @return UnderlayTopologyListener registrations
     */
    public List<ListenerRegistration<DOMDataTreeChangeListener>> getListeners() {
        return listeners;
    }

    /**
     * Only for testing purposes.
     * @return Transaction Chain
     */
    public DOMTransactionChain getTransactionChain() {
        return transactionChain;
    }

    public void setModelAdapters(Map<Class<? extends Model>, ModelAdapter> modelAdapters) {
        this.modelAdapters = modelAdapters;
        writer.setTranslator(modelAdapters.get(outputModel).createOverlayItemTranslator());
        transactionChain = pingPongDataBroker.createTransactionChain(writer);
        writer.setTransactionChain(transactionChain);
        topologyManager = new TopologyManager(rpcServices, schemaHolder,
                modelAdapters.get(outputModel).createTopologyIdentifier(topologyId).build(), outputModel);
        topologyManager.setWriter(writer);
        writer.initOverlayTopology();

    }

    protected abstract String getTopologyId(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

    /**
     * Process new topology request.
     */
    public void processNewRequest() {
        LOG.debug("Processing overlay topology creation request");
        Correlations correlations = getCorrelations(fromNormalizedNode);
        LinkComputation linkComputation = getLinkComputation(fromNormalizedNode);
        Aggregation linkAggregation = null;
        Preconditions.checkNotNull(correlations, "Received correlations can't be null");
        try {
            LOG.debug("Processing correlation configuration");
            List<Correlation> correlationList = correlations.getCorrelation();

            boolean isAggregationOfNodesAndTp = isAggregationOfNodesAndTp(correlationList) ;

            //this allow only one correlation of links in request
            for (Correlation correlation : correlationList) {
                if (FiltrationAggregation.class.equals(correlation.getType())) {
                    if (!isAggregationOfNodesAndTp) {
                        initAggregation(correlation, true);
                    }
                } else if (FiltrationOnly.class.equals(correlation.getType())) {
                    initFiltration(correlation);
                } else if (AggregationOnly.class.equals(correlation.getType())) {
                    if (correlation.getCorrelationItem() == CorrelationItemEnum.Link) {
                        linkAggregation = correlation.getAggregation();
                    } else {
                        if (!isAggregationOfNodesAndTp) {
                            initAggregation(correlation, false);
                        }
                    }
                } else if (RenderingOnly.class.equals(correlation.getType())) {
                    initRendering(correlation);
                } else {
                    throw new IllegalStateException("Filtration and Aggregation data missing: " + correlation);
                }
            }
            if (linkComputation != null) {
                initLinkComputation(linkComputation, linkAggregation);
            }
            LOG.debug("Correlation configuration successfully read");
        } catch (Exception e) {
            LOG.warn("Processing new request for topology change failed.", e);
            closeOperatingResources(0);
            throw new IllegalStateException("Processing new request for topology change failed.", e);
        }
    }

    private boolean isAggregationOfNodesAndTp(List<Correlation> correlationList) {
        Correlation nodeCorrelation = null;
        boolean nodeFiltration = false;
        Correlation tpCorrelation = null;
        boolean tpFiltration = false;

        for (Correlation correlation : correlationList) {
            if (FiltrationAggregation.class.equals(correlation.getType())
                    || AggregationOnly.class.equals(correlation.getType())) {
                if (correlation.getCorrelationItem() == CorrelationItemEnum.Node) {
                    nodeCorrelation = correlation;
                    nodeFiltration = FiltrationAggregation.class.equals(correlation.getType());
                } else if (correlation.getCorrelationItem() == CorrelationItemEnum.TerminationPoint) {
                    tpCorrelation = correlation;
                    tpFiltration = FiltrationAggregation.class.equals(correlation.getType());
                }
            }
        }
        if (nodeCorrelation == null || tpCorrelation == null) {
            return false;
        } else {
            initAggregationOfNodesAndTp(nodeCorrelation, nodeFiltration, tpCorrelation, tpFiltration);
            return true;
        }
    }

    private void initAggregationOfNodesAndTp(Correlation nodeCorrelation, boolean nodeFiltration,
            Correlation tpCorrelation, boolean tpFiltration) {

        Aggregation nodeAggregation = nodeCorrelation.getAggregation();
        Aggregation tpAggregation = tpCorrelation.getAggregation();
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        TopologyAggregator nodeAggregator = createAggregator(nodeAggregation.getAggregationType(),
                CorrelationItemEnum.Node, topoStoreProvider, null);
        TerminationPointAggregator tpAggregator = (TerminationPointAggregator) createAggregator(
                tpAggregation.getAggregationType(), CorrelationItemEnum.TerminationPoint, topoStoreProvider,
                tpAggregation.getMapping().get(0).getInputModel());

        // Termination point aggregator initialization
        if (tpAggregation.getScripting() != null) {
            tpAggregator.initCustomAggregation(tpAggregation.getScripting());
        }
        Class<? extends Model> inputModel = tpAggregation.getMapping().get(0).getInputModel();
        Map<String, Map<Integer, YangInstanceIdentifier>> targetFieldsPerTopology = new HashMap<>();
        TerminationPointPreAggregationFiltrator tpFiltrator = null;
        if (tpFiltration) {
            tpFiltrator = new TerminationPointPreAggregationFiltrator(topoStoreProvider, inputModel);
            tpFiltrator.setTopologyAggregator(tpAggregator);
        }
        for (Mapping mapping : tpAggregation.getMapping()) {
            String underlayTopologyId = mapping.getUnderlayTopology();
            // original statement - topoStoreProvider.initializeStore(underlayTopologyId, mapping.isAggregateInside());
            // aggregation inside is always enabled due to bug5190
            topoStoreProvider.initializeStore(underlayTopologyId, true);
            Map<Integer, YangInstanceIdentifier> pathIdentifier = new HashMap<>();
            for (TargetField targetField : mapping.getTargetField()) {
                pathIdentifier.put(targetField.getMatchingKey(), translator.translate(
                        targetField.getTargetFieldPath().getValue(), CorrelationItemEnum.TerminationPoint,
                        schemaHolder, inputModel));
            }
            targetFieldsPerTopology.put(underlayTopologyId, pathIdentifier);
            //((TerminationPointAggregator) aggregator).setTargetField(pathIdentifier);
            if (tpFiltration && mapping.getApplyFilters() != null) {
                for (String filterId : mapping.getApplyFilters()) {
                    Filter filter = findFilter(tpCorrelation.getFiltration().getFilter(), filterId);
                    addFiltrator(tpFiltrator, filter, CorrelationItemEnum.TerminationPoint, inputModel);
                }
            }
        }

        NodeAndTPAggregator nodeAndTpAggregator = new NodeAndTPAggregator(nodeAggregator, tpAggregator, tpFiltrator,
                tpAggregation.getMapping().get(0).getInputModel(), targetFieldsPerTopology);
        nodeAndTpAggregator.setTopologyManager(topologyManager);

        // Node aggregator initialization
        if (nodeAggregation.getScripting() != null) {
            nodeAggregator.initCustomAggregation(nodeAggregation.getScripting());
        }
        for (Mapping mapping : nodeAggregation.getMapping()) {
            inputModel = getRealInputModel(mapping.getInputModel(), CorrelationItemEnum.Node);
            String underlayTopologyId = mapping.getUnderlayTopology();
            // original statement - topoStoreProvider.initializeStore(underlayTopologyId, mapping.isAggregateInside());
            // aggregation inside is always enabled due to bug5190
            topoStoreProvider.initializeStore(underlayTopologyId, true);
            Map<Integer, YangInstanceIdentifier> pathIdentifier = new HashMap<>();
            for (TargetField targetField : mapping.getTargetField()) {
                pathIdentifier.put(targetField.getMatchingKey(), translator.translate(
                        targetField.getTargetFieldPath().getValue(), CorrelationItemEnum.Node,
                        schemaHolder, inputModel));
            }
            PreAggregationFiltrator filtrator = null;
            if (nodeFiltration && mapping.getApplyFilters() != null) {
                filtrator = new PreAggregationFiltrator(topoStoreProvider);
                filtrator.setTopologyAggregator(nodeAndTpAggregator);
                for (String filterId : mapping.getApplyFilters()) {
                    Filter filter = findFilter(nodeCorrelation.getFiltration().getFilter(), filterId);
                    addFiltrator(filtrator, filter, CorrelationItemEnum.Node, inputModel);
                }
            }
            UnderlayTopologyListener listener;
            if (filtrator == null) {
                listener = modelAdapters.get(inputModel)
                        .registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                                CorrelationItemEnum.Node, datastoreType, nodeAndTpAggregator, listeners,
                                pathIdentifier);
            } else {
                listener = modelAdapters.get(inputModel)
                        .registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                        CorrelationItemEnum.Node, datastoreType, filtrator, listeners, pathIdentifier);
            }
            LOG.debug("Registering underlay topology listener for topology: {}", underlayTopologyId);
            registerListener(listener, inputModel, underlayTopologyId, CorrelationItemEnum.Node);
        }
    }

    /**
     * @param correlation contains filtration configuration
     * @return configured {@link TopologyFiltrator}
     */
    private void initFiltration(Correlation correlation) {
        CorrelationItemEnum correlationItem = correlation.getCorrelationItem();
        Filtration filtration = correlation.getFiltration();
        Class<? extends Model> inputModel =
                getRealInputModel(filtration.getFilter().get(0).getInputModel(), correlationItem);
        String underlayTopologyId = filtration.getUnderlayTopology();
        TopologyFiltrator filtrator;
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        topoStoreProvider.initializeStore(underlayTopologyId, false);
        if (correlationItem.equals(CorrelationItemEnum.TerminationPoint)) {
            filtrator = new TerminationPointFiltrator(topoStoreProvider, inputModel);
        } else if (correlationItem.equals(CorrelationItemEnum.Link)) {
            filtrator = new LinkFiltrator(topoStoreProvider);
        } else {
            filtrator = new TopologyFiltrator(topoStoreProvider);
        }
        filtrator.setTopologyManager(topologyManager);

        Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>();
        int key = 0;
        for (Filter filter : filtration.getFilter()) {
            YangInstanceIdentifier filterPath =
                    addFiltrator(filtrator, filter, correlation.getCorrelationItem(), inputModel);
            pathIdentifiers.put(key++, filterPath);
        }

        UnderlayTopologyListener listener = modelAdapters.get(inputModel)
                .registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                        correlationItem, datastoreType, filtrator, listeners, pathIdentifiers);
        LOG.debug("Registering filtering underlay topology listener for topology: {}", underlayTopologyId);
        registerListener(listener, inputModel, underlayTopologyId, correlationItem);

        if (correlation.getCorrelationItem().equals(CorrelationItemEnum.Link)) {
            listener = modelAdapters.get(inputModel).
                    registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                            CorrelationItemEnum.Node, datastoreType, filtrator, listeners, null);
            LOG.debug("Registering secondary underlay topology listener for topology (in case of filtering on " +
                    "links): {}", underlayTopologyId);
            registerListener(listener, inputModel, underlayTopologyId, CorrelationItemEnum.Node);
        }
    }

    private YangInstanceIdentifier addFiltrator(TopologyFiltrator operator, Filter filter,
            CorrelationItemEnum correlationItem, Class<? extends Model> inputModel) {
        YangInstanceIdentifier filterPath = translator.translate(filter.getTargetField().getValue(),
                correlationItem, schemaHolder, inputModel);
        FiltratorFactory ff = filtrators.get(filter.getFilterType());
        Filtrator currentFiltrator = ff.createFiltrator(filter, filterPath);
        operator.addFilter(currentFiltrator);
        return filterPath;
    }

    private void initAggregation(Correlation correlation, boolean filtration) {
        CorrelationItemEnum correlationItem = correlation.getCorrelationItem();
        Aggregation aggregation = correlation.getAggregation();
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        TopologyAggregator aggregator;
        if (!aggregation.getMapping().isEmpty()) {
            aggregator = createAggregator(aggregation.getAggregationType(), correlationItem, topoStoreProvider,
                    getRealInputModel(aggregation.getMapping().get(0).getInputModel(), correlationItem));
        } else {
            aggregator = createAggregator(aggregation.getAggregationType(),
                    correlationItem, topoStoreProvider, null);
        }
        aggregator.setTopologyManager(topologyManager);
        if (aggregation.getScripting() != null) {
            aggregator.initCustomAggregation(aggregation.getScripting());
        }
        Class<? extends Model> inputModel;
        for (Mapping mapping : aggregation.getMapping()) {
            inputModel = getRealInputModel(mapping.getInputModel(), correlationItem);
            String underlayTopologyId = mapping.getUnderlayTopology();
            // original statement - topoStoreProvider.initializeStore(underlayTopologyId, mapping.isAggregateInside());
            // aggregation inside is always enabled due to bug5190
            topoStoreProvider.initializeStore(underlayTopologyId, true);
            Map<Integer, YangInstanceIdentifier> pathIdentifier = new HashMap<>();
            for (TargetField targetField : mapping.getTargetField()) {
                pathIdentifier.put(targetField.getMatchingKey(), translator.translate(
                        targetField.getTargetFieldPath().getValue(), correlationItem,
                        schemaHolder, inputModel));
            }
            if (aggregator instanceof TerminationPointAggregator) {
                ((TerminationPointAggregator) aggregator).setTargetField(pathIdentifier);
            }
            PreAggregationFiltrator filtrator = null;
            if (filtration && mapping.getApplyFilters() != null) {
                if (correlation.getCorrelationItem() == CorrelationItemEnum.TerminationPoint) {
                    filtrator = new TerminationPointPreAggregationFiltrator(topoStoreProvider, inputModel);
                } else {
                    filtrator = new PreAggregationFiltrator(topoStoreProvider);
                }
                filtrator.setTopologyAggregator(aggregator);
                for (String filterId : mapping.getApplyFilters()) {
                    Filter filter = findFilter(correlation.getFiltration().getFilter(), filterId);
                    addFiltrator(filtrator, filter, correlationItem, inputModel);
                }
            }
            UnderlayTopologyListener listener;
            TopologyOperator operator = filtrator == null ? aggregator : filtrator;
            listener = modelAdapters.get(inputModel)
                    .registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId, correlationItem,
                            datastoreType, operator, listeners, pathIdentifier);
            LOG.debug("Registering underlay topology listener for topology: {}", underlayTopologyId);
            registerListener(listener, inputModel, underlayTopologyId, correlationItem);
        }
    }

    private void initRendering(Correlation correlation) {
        Rendering rendering = correlation.getRendering();
        TopologyOperator operator = null;
        if (rendering != null) {
            String underlayTopologyId = rendering.getUnderlayTopology();
            UnderlayTopologyListener listener = modelAdapters.get(outputModel)
                    .registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                            correlation.getCorrelationItem(), datastoreType, operator, listeners, null);
            operator = listener.getOperator();
            if (operator instanceof NotificationInterConnector) {
                operator = ((NotificationInterConnector) operator).getOperator();
            }
            operator.setTopologyManager(topologyManager);
            LOG.debug("Registering underlay topology listener for topology: {}", underlayTopologyId);
            registerListener(listener, outputModel, underlayTopologyId, correlation.getCorrelationItem());
        } else {
            throw new IllegalStateException("Rendering data missing: " + correlation);
        }
    }

    private LinkCalculator initLinkComputation(LinkComputation linkComputation, Aggregation linkAggregation) {
        LinkCalculator calculator = null;
        List<LinkInfo> linksInformations = linkComputation.getLinkInfo();
        if (linksInformations != null && !linksInformations.isEmpty()) {
            String overlayTopologyId = linkComputation.getNodeInfo().getNodeTopology();
            calculator = new LinkCalculator(overlayTopologyId, outputModel);
            calculator.setTopologyManager(topologyManager);
            //register underlay listeners
            if (linkAggregation != null) {
                TopoStoreProvider storeProvider = new TopoStoreProvider();
                storeProvider.initializeStore(overlayTopologyId, true);
                UnificationAggregator aggregator = new UnificationAggregator(storeProvider);
                aggregator.setTopologyManager(topologyManager);
                if (linkAggregation.getScripting() != null) {
                    aggregator.initCustomAggregation(linkAggregation.getScripting());
                }
                calculator.setTopologyAggregator(aggregator);
            }
            for (LinkInfo linkInfo : linksInformations) {
                String underlayTopologyId = linkInfo.getLinkTopology();
                Map<Integer, YangInstanceIdentifier> pathIdentifiers = new HashMap<>();
                UnderlayTopologyListener listener = null;
                Class<? extends Model> inputModel =
                        getRealInputModel(linkInfo.getInputModel(), CorrelationItemEnum.Link);
                if (linkAggregation != null) {
                    for (Mapping mapping : linkAggregation.getMapping()) {
                        if (mapping.getUnderlayTopology().equals(underlayTopologyId)) {
                            for (TargetField targetField : mapping.getTargetField()) {
                                pathIdentifiers.put(targetField.getMatchingKey(), translator.translate(
                                        targetField.getTargetFieldPath().getValue(), CorrelationItemEnum.Link,
                                        schemaHolder, getRealInputModel(mapping.getInputModel(),
                                                CorrelationItemEnum.Link)));
                            }
                        }
                    }
                    listener = modelAdapters.get(inputModel)
                            .registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                                    CorrelationItemEnum.Link, datastoreType, calculator, listeners, pathIdentifiers);
                } else {
                    listener = modelAdapters.get(inputModel)
                            .registerUnderlayTopologyListener(pingPongDataBroker, underlayTopologyId,
                                    CorrelationItemEnum.Link, datastoreType, calculator, listeners, null);
                }
                LOG.debug("Registering link calculation underlay topology listener for topology: {}",
                        underlayTopologyId);
                registerListener(listener, inputModel, underlayTopologyId, CorrelationItemEnum.Link);
            }
            //register overlay listener
            UnderlayTopologyListener listener = modelAdapters.get(outputModel)
                    .registerUnderlayTopologyListener(pingPongDataBroker, topologyId,
                            CorrelationItemEnum.Node, datastoreType, calculator, listeners, null);
            InstanceIdentifierBuilder topologyIdentifier = modelAdapters.get(outputModel)
                    .createTopologyIdentifier(overlayTopologyId);
            YangInstanceIdentifier itemIdentifier = modelAdapters.get(outputModel)
                    .buildItemIdentifier(topologyIdentifier, CorrelationItemEnum.Node);
            LOG.debug("Registering link calculation overlay topology listener for topology: {}", overlayTopologyId);
            DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, itemIdentifier);
            ListenerRegistration<DOMDataTreeChangeListener> listenerRegistration =
                    pingPongDataBroker.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) listener);
            listeners.add(listenerRegistration);
        } else {
            throw new IllegalStateException("link computation data missing: " + linkComputation);
        }
        return calculator;
    }

    private void registerListener(UnderlayTopologyListener listener, Class<? extends Model> model,
            String topologyId, CorrelationItemEnum correlationItem) {
        InstanceIdentifierBuilder topologyIdentifier = modelAdapters.get(model)
                .createTopologyIdentifier(topologyId);
        YangInstanceIdentifier itemIdentifier = modelAdapters.get(model)
                .buildItemIdentifier(topologyIdentifier, correlationItem);
        DOMDataTreeIdentifier treeId = createDOMDataTreeIdentifier(itemIdentifier);
        ListenerRegistration<DOMDataTreeChangeListener> listenerRegistration =
                pingPongDataBroker.registerDataTreeChangeListener(treeId, (DOMDataTreeChangeListener) listener);
        listeners.add(listenerRegistration);
    }

    private DOMDataTreeIdentifier createDOMDataTreeIdentifier(YangInstanceIdentifier itemIdentifier) {
        return datastoreType.equals(DatastoreType.OPERATIONAL) ?
                new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, itemIdentifier) :
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, itemIdentifier);
    }

    private Filter findFilter(List<Filter> filters, String filterId) {
        for (Filter filter : filters) {
            if (filterId.equals(filter.getFilterId())) {
                return filter;
            }
        }
        return null;
    }

    private Class<? extends Model> getRealInputModel(Class<? extends Model> model, CorrelationItemEnum item) {
        // for Inventory model are links stored in NetworkTopology model
        if (item == CorrelationItemEnum.Link && model.equals(OpendaylightInventoryModel.class)) {
            return NetworkTopologyModel.class;
        } else {
            return model;
        }
    }

    private TopologyAggregator createAggregator(Class<? extends AggregationBase> type, CorrelationItemEnum item,
            TopoStoreProvider topoStoreProvider, Class<? extends Model> model) {
        TopologyAggregator aggregator;
        if (CorrelationItemEnum.TerminationPoint.equals(item)) {
            aggregator = new TerminationPointAggregator(topoStoreProvider, model);
        } else if (Equality.class.equals(type)) {
            aggregator = new EqualityAggregator(topoStoreProvider);
        } else if (Unification.class.equals(type)) {
            aggregator = new UnificationAggregator(topoStoreProvider);
        } else {
            throw new IllegalArgumentException("Unsupported correlation type received: " + type);
        }
        return aggregator;
    }

    /**
     * @return ID of topology that is handled by this {@link TopologyRequestHandler}
     */
    public String getTopologyId() {
        return this.topologyId;
    }

    protected abstract Correlations getCorrelations(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

    protected abstract LinkComputation getLinkComputation(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode);

    /**
     * Closes all registered listeners and providers.
     * @param timeOut time in ms to wait for close operation to finish, if timeOut == 0, there is no waiting
     */
    public void processDeletionRequest(int timeOut) {
        LOG.debug("Processing overlay topology deletion request");
        closeOperatingResources(timeOut);
    }

    private void closeOperatingResources(int timeOut) {
        for (ListenerRegistration<DOMDataTreeChangeListener> listener : listeners) {
            listener.close();
        }
        listeners.clear();
        writer.tearDown();
        if (timeOut > 0) {
            try {
                writer.waitForTearDownCompletion(timeOut);
            } catch (InterruptedException i) {
                LOG.debug("Captured InterruptedException in method"
                        + " closeOperatingResources in TopologyRequestHandler class.");
            }
        }
    }

    /**
     * Delegate topology types to writer.
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