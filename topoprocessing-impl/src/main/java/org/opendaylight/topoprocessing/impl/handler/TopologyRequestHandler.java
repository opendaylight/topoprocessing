/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
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
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
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

    public TopologyRequestHandler(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    /**
     * @param topology overlay topology request
     */
    public void processNewRequest(Topology topology) {
        this.topology = topology;
        try {
            CorrelationAugment augmentation = topology.getAugmentation(CorrelationAugment.class);
            List<Correlation> correlations = augmentation.getCorrelations().getCorrelation();
            for (Correlation correlation : correlations) {
                CorrelationType correlationType = correlation.getCorrelationType();
                EqualityCase equalityCase = (EqualityCase) correlationType;
                List<Mapping> mappings = equalityCase.getEquality().getMapping();
                for (Mapping mapping : mappings) {
                    YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder()
                            .node(NetworkTopology.QNAME)
                            .node(Topology.QNAME)
                            .nodeWithKey(Topology.QNAME, QName.create("topology-id"), mapping.getUnderlayTopology())
                            .node(getCorrelationItemQname(correlation.getCorrelationItem()))
                            .build();
                    MapNode mapNode = readData(yangInstanceIdentifier);
                    YangInstanceIdentifier pathIdentifier = new PathTranslator().
                           translate(mapping.getTargetField().getValue());
                    // TODO - register topology change listeners, create aggregators and providers
                }
            }
        } catch (Exception e) {
            LOG.warn("Processing new request for topology change failed.", e);
        }
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
     * Returns node by specified path
     * @param path Path identificator
     * @return
     */
    private MapNode readData(YangInstanceIdentifier path) throws TimeoutException {
        DOMDataReadTransaction transaction = domDataBroker.newReadOnlyTransaction();
        LogicalDatastoreType datastore = LogicalDatastoreType.OPERATIONAL;
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> listenableFuture = transaction.read(datastore, path);
        if (listenableFuture != null) {
            Optional<NormalizedNode<?, ?>> optional;
            try {
                optional = listenableFuture.get(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Problem to get data from transaction.", e);
            } catch (TimeoutException e) {
                LOG.warn("Timeout for accessing DS.config exceeded.", e);
                throw e;
            }
            if (optional != null) {
                if (optional.isPresent()) {
                    return (MapNode) optional.get();
                }
            }
        }
        return null;
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
