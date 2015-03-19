/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.CorrelationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


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
    private ArrayList<ListenerRegistration<DOMDataChangeListener>> listeners = new ArrayList<>();

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRequestHandler.class);

    /**
     * Default constructor
     * @param domDataBroker broker used for transaction operations
     */
    public TopologyRequestHandler(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    /** Only for testing purposes */
    public void setManager(TopologyManager manager) {
        this.manager = manager;
    }

    /** Only for testing purposes */
    public void setTranslator(PathTranslator translator) { this.translator = translator; }

    /** Only for testing purposes */
    public void setListeners(ArrayList<ListenerRegistration<DOMDataChangeListener>> listeners) {
        this.listeners = listeners;
    }

    /** Only for testing purposes */
    public ArrayList<ListenerRegistration<DOMDataChangeListener>> getListeners() { return listeners; }

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
                manager.initializeStructure(correlation);
                CorrelationType correlationType = correlation.getCorrelationType();
                EqualityCase equalityCase = (EqualityCase) correlationType;
                List<Mapping> mappings = equalityCase.getEquality().getMapping();
                for (Mapping mapping : mappings) {
                    String underlayTopologyId = mapping.getUnderlayTopology();
                    YangInstanceIdentifier pathIdentifier = translator.translate(mapping.getTargetField().getValue());
                    UnderlayTopologyListener listener = new UnderlayTopologyListener(manager,
                            underlayTopologyId, pathIdentifier);
                    YangInstanceIdentifier.InstanceIdentifierBuilder nodeIdentifierBuilder = YangInstanceIdentifier.builder()
                            .node(NetworkTopology.QNAME)
                            .node(Topology.QNAME)
                            .nodeWithKey(Topology.QNAME, QName.create("topology-id"), underlayTopologyId);
                    YangInstanceIdentifier nodeIdentifier = buildNodeIdentifier(nodeIdentifierBuilder, correlation.getCorrelationItem());
                    LOG.debug("Registering underlay topology listener for topology: "
                            + underlayTopologyId);
                    ListenerRegistration<DOMDataChangeListener> listenerRegistration =
                            this.domDataBroker.registerDataChangeListener(
                                    LogicalDatastoreType.OPERATIONAL, nodeIdentifier, listener, DataChangeScope.SUBTREE);
                    LOG.debug("Underlay topology listener for topology: " + underlayTopologyId
                            + " has been successfully registered");
                    listeners.add(listenerRegistration);
                }
            }
            LOG.debug("Correlation configuration successfully read");
        } catch (Exception e) {
            LOG.warn("Processing new request for topology change failed.", e);
        }
    }

    private YangInstanceIdentifier buildNodeIdentifier(YangInstanceIdentifier.InstanceIdentifierBuilder builder,
                                                       CorrelationItemEnum correlationItemEnum) throws Exception {
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
                throw new Exception("Wrong Correlation Item set");
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
