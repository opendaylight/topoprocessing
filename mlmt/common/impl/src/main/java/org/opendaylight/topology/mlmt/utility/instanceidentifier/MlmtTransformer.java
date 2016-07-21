/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility.instanceidentifier;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class MlmtTransformer implements Transformer<InstanceIdentifier.PathArgument,
    InstanceIdentifier.PathArgument> {
    private String topologyName;

    public MlmtTransformer(String topologyName) {
        super();
        this.topologyName = topologyName;
    }

    @Override
    public PathArgument transform(PathArgument item) {
        if (item instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {
            InstanceIdentifier.IdentifiableItem<?, ?> idItem = (IdentifiableItem<?, ?>) item;
            if (idItem.getType().equals(Topology.class)) {
                return new InstanceIdentifier.IdentifiableItem<Topology, TopologyKey>(Topology.class,
                        new TopologyKey(new TopologyId(topologyName)));
            }
        }

        return item;
    }
}
