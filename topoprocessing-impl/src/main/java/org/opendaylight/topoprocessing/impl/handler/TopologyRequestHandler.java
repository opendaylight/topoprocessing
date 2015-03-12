/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyManager;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.CorrelationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Picks up information from topology request, engages corresponding
 * listeners, aggregators.
 * @author michal.polkorab
 */
public class TopologyRequestHandler {

    /** Timeout for read transactions in seconds */
    private static final long TIMEOUT = 10;

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRequestHandler.class);

    private DOMDataBroker domDataBroker;
    private Topology topology;

    private TopologyManager manager = new TopologyManager();

    private PathTranslator translator;// = new PathTranslator();

    public TopologyRequestHandler(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    /**
     * @param topology overlay topology request
     */
    public void processNewRequest(Topology topology) {
        this.topology = topology;
        try {
            System.out.println("som v processNewRequest");
            CorrelationAugment augmentation = topology.getAugmentation(CorrelationAugment.class);
            List<Correlation> correlations = augmentation.getCorrelations().getCorrelation();
            for (Correlation correlation : correlations) {
                System.out.println("som v correlation loop");
                CorrelationType correlationType = correlation.getCorrelationType();
                EqualityCase equalityCase = (EqualityCase) correlationType;
                List<Mapping> mappings = equalityCase.getEquality().getMapping();
                for (Mapping mapping : mappings) {
                    System.out.println("Mapping loop");
                    System.out.println("pred databroker");
                    YangInstanceIdentifier pathIdentifier = translator.translate(mapping.getTargetField().getValue());
                    UnderlayTopologyListener listener = new UnderlayTopologyListener(manager, pathIdentifier);
                    YangInstanceIdentifier nodeIdentifier = YangInstanceIdentifier.builder()
                            .node(NetworkTopology.QNAME)
                            .node(Topology.QNAME)
                            .nodeWithKey(Topology.QNAME, QName.create("topology-id"), mapping.getUnderlayTopology())
                            .node(getCorrelationItemQname(correlation.getCorrelationItem()))
                            .build();
                    this.domDataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, nodeIdentifier,
                            listener, DataChangeScope.SUBTREE);
                }
            }
            System.out.println("koniec");
        } catch (Exception e) {
            System.out.println("exception" + e);
            LOG.warn("Processing new request for topology change failed.", e);
        }
    }

    
    public void setTranslator(PathTranslator translator) {
        this.translator = translator;
    }

    private QName getCorrelationItemQname(CorrelationItemEnum correlationItemEnum) throws Exception {
        QName result;
        switch (correlationItemEnum) {
            case Node:
                result = Node.QNAME;
                break;
            case Link:
                result = Link.QNAME;
                break;
            case TerminationPoint:
                result = TerminationPoint.QNAME;
                break;
            default:
                throw new Exception("Wrong Correlation Item set");
        }
        return result;
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
        // TODO - implement after discussion on how to interconnect with mlmt-observer/provider
    }
}
