/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.api.structure;

import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author martin.uhlir
 *
 */
public class ComputedLink extends UnderlayItem {

    private NormalizedNode<?, ?> dstNode;
    private NormalizedNode<?, ?> srcNode;

    /**
     * @param link              Link of Underlay topology
     * @param leafNodes         denoting target fields
     * @param srcNode           denoting source
     * @param dstNode           denoting destination
     * @param topologyId        identifier of {@link Topology}
     * @param linkId            identifier of the {@link Link}
     * @param correlationItem   {@link Link}
     */
    public ComputedLink(NormalizedNode<?, ?> link, Map<Integer, NormalizedNode<?, ?>> leafNodes,
            NormalizedNode<?, ?> srcNode, NormalizedNode<?, ?> dstNode, String topologyId, String linkId,
            CorrelationItemEnum correlationItem) {
        super(link, leafNodes, topologyId, linkId, correlationItem);
        this.srcNode = srcNode;
        this.dstNode = dstNode;
    }

    /**
     * @return the srcNode
     */
    public NormalizedNode<?, ?> getSrcNode() {
        return srcNode;
    }

    /**
     * @param srcNode the srcNode to set
     */
    public void setSrcNode(NormalizedNode<?, ?> srcNode) {
        this.srcNode = srcNode;
    }

    /**
     * @return the dstNode
     */
    public NormalizedNode<?, ?> getDstNode() {
        return dstNode;
    }

    /**
     * @param dstNode the dstNode to set
     */
    public void setDstNode(NormalizedNode<?, ?> dstNode) {
        this.dstNode = dstNode;
    }

}
