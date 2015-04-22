/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author michal.polkorab
 *
 */
public class InstanceIdentifiers {

    /** Network-topology Topology identifier*/
    public static final YangInstanceIdentifier TOPOLOGY_IDENTIFIER =
            YangInstanceIdentifier.of(NetworkTopology.QNAME).node(Topology.QNAME);

            private InstanceIdentifiers() {
        throw new UnsupportedOperationException("InstanceIdentifiers can't be instantiated.");
    }
}
