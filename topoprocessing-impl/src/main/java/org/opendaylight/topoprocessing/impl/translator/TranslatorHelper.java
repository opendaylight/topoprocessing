/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.translator;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

/**
 * @author samuel.kontris
 *
 */
public final class TranslatorHelper {

    private TranslatorHelper(){
    }

    public static String createTpRefNT(String topologyId, String nodeId, String tpId) {
        StringBuilder tpRefValue = new StringBuilder();
        tpRefValue.append('/').append(NetworkTopology.QNAME.getLocalName()).append(':')
                .append(NetworkTopology.QNAME.getLocalName()).append('/')
                .append(Topology.QNAME.getLocalName()).append('/')
                .append(topologyId).append('/')
                .append(Node.QNAME.getLocalName()).append('/')
                .append(nodeId).append('/')
                .append(TerminationPoint.QNAME.getLocalName()).append('/')
                .append(tpId);
        return tpRefValue.toString();
    }
}
