/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

<<<<<<< Updated upstream
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
=======
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.opendaylight.topoprocessing.impl.structure.*;
import org.opendaylight.topoprocessing.impl.translator.LogicalNodeToNodeTranslator;
>>>>>>> Stashed changes
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NodeIpFiltration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Unification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.mapping.grouping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.NodeIpFiltrationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.UnificationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.node.ip.filtration._case.node.ip.filtration.Filter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * @author matus.marko
 */
public class TopologyManager {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyManager.class);

    private TopologyOperator aggregator = null;
    private TopologyFiltrator filtrator = null;
    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private List<TopologyStore> topologyStores = new ArrayList<>();
    private TopologyWriter topologyWriter;
<<<<<<< Updated upstream
=======
    private LogicalNodeToNodeTranslator converter = new LogicalNodeToNodeTranslator();
>>>>>>> Stashed changes

    /**
     * Process created changes
     * @param createdEntries
     * @param topologyId
     */
    public void processCreatedChanges(Map<YangInstanceIdentifier, PhysicalNode> createdEntries, final String topologyId) {
        Preconditions.checkNotNull(aggregator, "Operator needs to be initialized.");
        LOG.debug("Processing createdChanges");
<<<<<<< Updated upstream
        aggregator.processCreatedChanges(createdEntries, topologyId);
=======
        AggregationMap map = aggregator.processCreatedChanges(createdEntries, topologyId);
        Map<YangInstanceIdentifier, LogicalNode> createdData = map.getCreatedData();
        if (0 < createdData.size()) {
            Map.Entry<YangInstanceIdentifier, LogicalNode> entry = createdData.entrySet().iterator().next();
            LogicalNodeWrapper wrapper = new LogicalNodeWrapper("node1", entry.getValue());
            Map<YangInstanceIdentifier, NormalizedNode<?, ?>> dataToCreate = new HashMap<>();
            dataToCreate.put(entry.getKey(), converter.convert(wrapper));
            topologyWriter.writeCreatedData(dataToCreate);
        }
>>>>>>> Stashed changes
        LOG.debug("CreatedChanges processed");
    }

    /**
     * Process removed changes
     * @param identifiers which were removed in this change
     * @param topologyId id of topology on which this method was called
     */
    public void processRemovedChanges(List<YangInstanceIdentifier> identifiers, final String topologyId) {
        Preconditions.checkNotNull(aggregator, "Operator needs to be initialized.");
        LOG.debug("Processing removedChanges");
<<<<<<< Updated upstream
        aggregator.processRemovedChanges(identifiers, topologyId);
=======
//        AggregationMap map = aggregator.processRemovedChanges(identifiers, topologyId);
//        topologyWriter.writeCreatedData(converter.convert(map.getCreatedData()));
//        topologyWriter.updateData(converter.convert(map.getUpdatedData()));
//        topologyWriter.deleteData(map.getRemovedData());
>>>>>>> Stashed changes
        LOG.debug("RemovedChanges processed");
    }

    /**
     * Process updated changes
     * @param updatedEntries
     * @param topologyId
     */
    public void processUpdatedChanges(Map<YangInstanceIdentifier, PhysicalNode> updatedEntries,
            String topologyId) {
        Preconditions.checkNotNull(aggregator, "Operator needs to be initialized.");
        LOG.debug("Processing updatedChanges");
<<<<<<< Updated upstream
        aggregator.processUpdatedChanges(updatedEntries, topologyId);
=======
//        AggregationMap map = aggregator.processUpdatedChanges(updatedEntries, topologyId);
//        topologyWriter.writeCreatedData(converter.convert(map.getCreatedData()));
//        topologyWriter.updateData(converter.convert(map.getUpdatedData()));
//        topologyWriter.deleteData(map.getRemovedData());
>>>>>>> Stashed changes
        LOG.debug("UpdatedChanges processed");
    }

    /**
     * Set correlation and initialize stores
     * @param correlation Correlation
     */
    public void initializeStructures(Correlation correlation) {
        //TODO Pipelining
        if (correlation.getName().equals(Equality.class)) {
            EqualityCase typeCase = (EqualityCase) correlation.getCorrelationType();
            List<Mapping> mappings = typeCase.getEquality().getMapping();
            iterateMappings(mappings);
            aggregator = new TopologyAggregator(correlation.getCorrelationItem(), topologyStores, idGenerator);
        }
        else if (correlation.getName().equals(Unification.class)) {
            UnificationCase typeCase = (UnificationCase) correlation.getCorrelationType();
            List<Mapping> mappings = typeCase.getUnification().getMapping();
            iterateMappings(mappings);
            //TODO Unification Operator
        }
        else if (correlation.getName().equals(NodeIpFiltration.class)) {
            NodeIpFiltrationCase typeCase = (NodeIpFiltrationCase) correlation.getCorrelationType();
            List<Filter> filters = typeCase.getNodeIpFiltration().getFilter();
            for (Filter filter : filters) {
            }
            filtrator = new TopologyFiltrator(correlation);
        }
    }

    private void iterateMappings(List<Mapping> mappings) {
        if (mappings != null) {
            for (Mapping mapping : mappings) {
                initializeStore(mapping.getUnderlayTopology());
            }
        }
    }

    private void initializeStore(String underlayTopologyId) {
        for (TopologyStore topologyStore : topologyStores) {
            if (underlayTopologyId.equals(topologyStore.getId())) {
                return;
            }
        }
        topologyStores.add(new TopologyStore(underlayTopologyId,
                new HashMap<YangInstanceIdentifier, PhysicalNode>()));
    }

    /**
     * @return the topologyStores
     */
    public List<TopologyStore> getTopologyStores() {
        return topologyStores;
    }

    /**
     * @param topologyWriter writes data into operational datastore
     */
    public void set(TopologyWriter topologyWriter) {
        this.topologyWriter = topologyWriter;
    }
}
