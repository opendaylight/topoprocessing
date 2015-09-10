/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.linkComputation;

import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author martin.uhlir
 *
 */
public class ComputedLink extends UnderlayItem {

    private NormalizedNode<?, ?> leafNode2;

    /**
     * @param link              Link of Underlay topology
     * @param leafNode          denoting source
     * @param leafNode2         denoting destination
     * @param topologyId        identifier of {@link Topology}
     * @param linkId            identifier of the {@link Link}
     * @param correlationItem   {@link Link}
     */
    public ComputedLink(NormalizedNode<?, ?> link,
            NormalizedNode<?, ?> leafNode, NormalizedNode<?, ?> leafNode2, String topologyId, String linkId,
            CorrelationItemEnum correlationItem) {
        super(link, leafNode, topologyId, linkId, correlationItem);
        this.setLeafNode2(leafNode2);
    }

    /**
     * @return the leafNode2
     */
    public NormalizedNode<?, ?> getLeafNode2() {
        return leafNode2;
    }

    /**
     * @param leafNode2 the leafNode2 to set
     */
    public void setLeafNode2(NormalizedNode<?, ?> leafNode2) {
        this.leafNode2 = leafNode2;
    }

}
