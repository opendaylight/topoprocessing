/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.nt.listener;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matej.perina
 *
 */
public class NTUnderlayTopologyListener extends UnderlayTopologyListener {

    public NTUnderlayTopologyListener(DOMDataBroker domDataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        super(domDataBroker, underlayTopologyId, correlationItem);
        // this needs to be done because for processing TerminationPoints we need to filter Node instead of TP
        if (CorrelationItemEnum.TerminationPoint.equals(correlationItem)) {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node,
                    NetworkTopologyModel.class);
            this.itemQName = TopologyQNames.buildItemQName(CorrelationItemEnum.Node, NetworkTopologyModel.class);
        } else {
            this.relativeItemIdIdentifier = InstanceIdentifiers.relativeItemIdIdentifier(correlationItem,
                    NetworkTopologyModel.class);
            this.itemQName = TopologyQNames.buildItemQName(correlationItem, NetworkTopologyModel.class);
        }
        this.itemIdentifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId)
                .node(itemQName).build();
    }

}
