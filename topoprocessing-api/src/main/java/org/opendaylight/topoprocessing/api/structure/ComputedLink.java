/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.api.structure;

import java.util.HashMap;
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

    /**
     * @param link              Link of Underlay topology
     * @param srcNode           denoting source
     * @param dstNode           denoting destination
     * @param topologyId        identifier of {@link Topology}
     * @param linkId            identifier of the {@link Link}
     * @param correlationItem   {@link Link}
     */
    public ComputedLink(NormalizedNode<?, ?> link,
            NormalizedNode<?, ?> srcNode, NormalizedNode<?, ?> dstNode, String topologyId, String linkId,
            CorrelationItemEnum correlationItem) {
        super(link, null, topologyId, linkId, correlationItem);
        this.dstNode = dstNode;
        setSrcNode(srcNode);
    }

    /**
     * @return the srcNode
     */
    public NormalizedNode<?, ?> getSrcNode() {
        return getLeafNode().get(0);
    }

    /**
     * @return the dstNode
     */
    public NormalizedNode<?, ?> getDstNode() {
        return dstNode;
    }

    /**
     * @param srcNode the srcNode to set
     */
    public void setSrcNode(NormalizedNode<?, ?> srcNode) {
        Map<Integer, NormalizedNode<?, ?>> srcNodeMap = new HashMap<>(1);
        srcNodeMap.put(0, srcNode);
        setLeafNode(srcNodeMap);
    }

    /**
     * @param dstNode the dstNode to set
     */
    public void setDstNode(NormalizedNode<?, ?> dstNode) {
        this.dstNode = dstNode;
    }

}
