/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.NetworkTopology;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author matej.perina
 *
 */
public class NTUnderlayTopologyListener extends UnderlayTopologyListener{

    public NTUnderlayTopologyListener(DOMDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        super(domDataBroker, underlayTopologyId, correlationItem);
    }

    @Override
    protected Map<YangInstanceIdentifier, NormalizedNode<?, ?>> listToMap(Iterator<NormalizedNode<?, ?>> nodes,
            String underlayTopologyId) {
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> map = Maps.uniqueIndex(nodes,
                new Function<NormalizedNode<?, ?>, YangInstanceIdentifier>() {
            @Nullable
            @Override
            public YangInstanceIdentifier apply(NormalizedNode<?, ?> node) {
                return YangInstanceIdentifier
                        .builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                        .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId)
                        .node(Node.QNAME)
                        .node(node.getIdentifier())
                        .build();
            }
        });
        return map;
    }

    public void registerUnderlayTopologyListener(TopologyOperator operator){
        this.setOperator(operator);
    };
}
