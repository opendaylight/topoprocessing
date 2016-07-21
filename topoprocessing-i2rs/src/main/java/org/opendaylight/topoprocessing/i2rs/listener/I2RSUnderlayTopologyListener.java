/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.i2rs.listener;

import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matej.perina
 */
public class I2RSUnderlayTopologyListener extends UnderlayTopologyListener {

    public I2RSUnderlayTopologyListener(PingPongDataBroker dataBroker, String underlayTopologyId,
            CorrelationItemEnum correlationItem) {
        super(dataBroker, underlayTopologyId, correlationItem);
        // this needs to be done because for processing TerminationPoints we need to filter Node instead of TP
        if (CorrelationItemEnum.TerminationPoint.equals(correlationItem)) {
            this.relativeItemIdIdentifier =
                    InstanceIdentifiers.relativeItemIdIdentifier(CorrelationItemEnum.Node, I2rsModel.class);
            this.itemQName = TopologyQNames.buildItemQName(CorrelationItemEnum.Node, I2rsModel.class);
        } else {
            this.relativeItemIdIdentifier =
                    InstanceIdentifiers.relativeItemIdIdentifier(correlationItem, I2rsModel.class);
            this.itemQName = TopologyQNames.buildItemQName(correlationItem, I2rsModel.class);
        }
        this.itemIdentifier = YangInstanceIdentifier.builder(InstanceIdentifiers.I2RS_NETWORK_IDENTIFIER)
                .nodeWithKey(Network.QNAME,TopologyQNames.I2RS_NETWORK_ID_QNAME, underlayTopologyId)
                .node(itemQName).build();
    }
}
