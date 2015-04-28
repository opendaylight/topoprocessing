/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.NodeIpFiltrationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.UnificationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.node.ip.filtration._case.node.ip.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Picks up information from topology request, engages corresponding
 * listeners, aggregators.
 * @author michal.polkorab
 */
public class TopologyRequestHandler {

    private DOMDataBroker domDataBroker;
    private Topology topology;
    private TopologyManager manager = new TopologyManager();
    private PathTranslator translator = new PathTranslator();
    private List<ListenerRegistration<DOMDataChangeListener>> listeners = new ArrayList<>();
    private List<TopologyStore> topologyStores = new ArrayList<>();

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRequestHandler.class);
    private GlobalSchemaContextHolder schemaHolder;

    /**
     * Default constructor
     * @param domDataBroker broker used for transaction operations
     * @param schemaHolder
     */
    public TopologyRequestHandler(DOMDataBroker domDataBroker, GlobalSchemaContextHolder schemaHolder) {
        this.domDataBroker = domDataBroker;
        this.schemaHolder = schemaHolder;
    }

    /** Only for testing purposes */
    public void setManager(TopologyManager manager) {
        this.manager = manager;
    }

    /** Only for testing purposes */
    public void setTranslator(PathTranslator translator) { this.translator = translator; }

    /** Only for testing purposes */
    public void setListeners(List<ListenerRegistration<DOMDataChangeListener>> listeners) {
        this.listeners = listeners;
    }

    /** Only for testing purposes */
    public List<ListenerRegistration<DOMDataChangeListener>> getListeners() { return listeners; }

    /**
     * @param topology overlay topology request
     */
    public void processNewRequest(Topology topology) {
        LOG.debug("Processing overlay topology creation request");
        Preconditions.checkNotNull(topology, "Received topology can't be null");
        this.topology = topology;
        try {
            LOG.debug("Processing correlation configuration");
            CorrelationAugment augmentation = topology.getAugmentation(CorrelationAugment.class);
            List<Correlation> correlations = augmentation.getCorrelations().getCorrelation();
            for (Correlation correlation : correlations) {
//                manager.initializeStructures(correlation);
                if (correlation.getName().equals(Equality.class)) {
                    EqualityCase equalityCase = (EqualityCase) correlation.getCorrelationType();
                    List<Mapping> mappings = equalityCase.getEquality().getMapping();
                    iterateMappings(mappings, correlation.getCorrelationItem());
                }
                else if (correlation.getName().equals(Unification.class)) {
                    UnificationCase unificationCase = (UnificationCase) correlation.getCorrelationType();
                    List<Mapping> mappings = unificationCase.getUnification().getMapping();
                    iterateMappings(mappings, correlation.getCorrelationItem());
                }
                else if (correlation.getName().equals(NodeIpFiltration.class)) {
                    NodeIpFiltrationCase nodeIpFiltrationCase = (NodeIpFiltrationCase) correlation.getCorrelationType();
                    List<Filter> filters = nodeIpFiltrationCase.getNodeIpFiltration().getFilter();
                    iterateFilters(filters, correlation.getCorrelationItem());
                } else {
                    throw new IllegalStateException("Unknown correlation: " + correlation.getName());
                }
            }
            LOG.debug("Correlation configuration successfully read");
            TopologyWriter writer = new TopologyWriter(domDataBroker, topology.getTopologyId().getValue());
            manager.set(writer);
            writer.initOverlayTopology();
        } catch (Exception e) {
            LOG.warn("Processing new request for topology change failed.", e);
        }
    }

    private void iterateFilters(List<Filter> filters, CorrelationItemEnum correlationItem) {

        for (Filter filter : filters) {
            String underlayTopologyId = filter.getUnderlayTopology();
            YangInstanceIdentifier pathIdentifier = translator.translate(filter.getTargetField().getValue(),
                    correlationItem, schemaHolder);
            TopologyFiltrator filtrator = new TopologyFiltrator(filter);
            filtrator.setNext(manager);
            UnderlayTopologyListener listener = new UnderlayTopologyListener(filtrator,
                    underlayTopologyId, pathIdentifier);
            YangInstanceIdentifier.InstanceIdentifierBuilder topologyIdentifier =
                    createTopologyIdentifier(underlayTopologyId);
            YangInstanceIdentifier nodeIdentifier = buildNodeIdentifier(topologyIdentifier, correlationItem);
            LOG.debug("Registering filtering underlay topology listener for topology: "
                    + underlayTopologyId);
            ListenerRegistration<DOMDataChangeListener> listenerRegistration =
                    domDataBroker.registerDataChangeListener(
                            LogicalDatastoreType.OPERATIONAL, nodeIdentifier, listener, DataChangeScope.SUBTREE);
            LOG.debug("Filtering Underlay topology listener for topology: " + underlayTopologyId
                    + " has been successfully registered");
            listeners.add(listenerRegistration);
        }
    }

    private void iterateMappings(List<Mapping> mappings, CorrelationItemEnum correlationItem) {
        for (Mapping mapping : mappings) {
            String underlayTopologyId = mapping.getUnderlayTopology();
            YangInstanceIdentifier pathIdentifier = translator.translate(mapping.getTargetField().getValue(),
                    correlationItem, schemaHolder);
            TopologyAggregator aggregator = new TopologyAggregator(correlationItem, topologyStores, null);
            aggregator.setNext(manager);
            UnderlayTopologyListener listener = new UnderlayTopologyListener(aggregator,
                    underlayTopologyId, pathIdentifier);
            YangInstanceIdentifier.InstanceIdentifierBuilder topologyIdentifier =
                    createTopologyIdentifier(underlayTopologyId);
            YangInstanceIdentifier nodeIdentifier = buildNodeIdentifier(topologyIdentifier, correlationItem);
            LOG.debug("Registering underlay topology listener for topology: "
                    + underlayTopologyId);
            ListenerRegistration<DOMDataChangeListener> listenerRegistration =
                    domDataBroker.registerDataChangeListener(
                            LogicalDatastoreType.OPERATIONAL, nodeIdentifier, listener, DataChangeScope.SUBTREE);
            LOG.debug("Underlay topology listener for topology: " + underlayTopologyId
                    + " has been successfully registered");
            listeners.add(listenerRegistration);
        }
    }

    private static YangInstanceIdentifier.InstanceIdentifierBuilder createTopologyIdentifier(
            String underlayTopologyId) {
        InstanceIdentifierBuilder identifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.topologyIdQName, underlayTopologyId);
        return identifier;
    }

    private static YangInstanceIdentifier buildNodeIdentifier(
            YangInstanceIdentifier.InstanceIdentifierBuilder builder, CorrelationItemEnum correlationItemEnum)
                    throws IllegalStateException {
        switch (correlationItemEnum) {
            case Node:
                builder.node(Node.QNAME);
                break;
            case Link:
                builder.node(Link.QNAME);
                break;
            case TerminationPoint:
                builder.node(Node.QNAME);
                builder.node(TerminationPoint.QNAME);
                break;
            default:
                throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItemEnum);
        }
        return builder.build();
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
        for (ListenerRegistration<DOMDataChangeListener> listener : listeners) {
            listener.close();
        }
    }
}
