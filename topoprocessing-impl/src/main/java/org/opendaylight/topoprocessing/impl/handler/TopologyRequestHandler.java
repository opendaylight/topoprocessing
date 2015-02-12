/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import java.util.List;

import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

/**
 * Picks up information from topology request, engages corresponding
 * listeners, aggregators.
 * @author michal.polkorab
 */
public class TopologyRequestHandler {

    private PathTranslator pathTranslator = new PathTranslator();
    private String topologyId;

    /**
     * @param topology overlay topology request
     */
    public void processNewRequest(Topology topology) {
        topologyId = topology.getTopologyId().getValue();
        // TODO - read topology request data and execute proper action
        //      - register topology change listeners, create aggregators and providers
        //      - implement after discussion on how to interconnect with mlmt-observer/provider
        translateCorrelation(topology.getAugmentation(CorrelationAugment.class));
    }

    /**
     * Translates received correlation and delegates functionality to specific correlation handler,
     * which engages / initializes needed objects
     * @param augmentation defined correlation
     */
    private void translateCorrelation(CorrelationAugment augmentation) {
        List<Correlation> correlations = augmentation.getCorrelations().getCorrelation();
        for (Correlation correlation : correlations) {
            if (correlation.getName().equals(Equality.class)) {
                EqualityCorrelationHandler equalityHandler = new EqualityCorrelationHandler(pathTranslator);
                equalityHandler.handle((EqualityCase) correlation.getCorrelationType());
            }
        }
    }

    /**
     * @return ID of topology that is handled by this {@link TopologyRequestHandler}
     */
    public String getTopologyId() {
        return topologyId;
    }

    /**
     * Closes all registered listeners and providers
     */
    public void processDeletionRequest() {
        // TODO - implement after discussion on how to interconnect with mlmt-observer/provider
    }
}
