/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
public class PhysicalNode {

    private NormalizedNode<?, ?> node;
    private NormalizedNode<?, ?> leafNode;
    private YangInstanceIdentifier logicalIdentifier = null;

    /**
     * Constructor
     * @param node
     * @param leafNode
     */
    public PhysicalNode(NormalizedNode<?, ?> node, NormalizedNode<?, ?> leafNode) {
        this.node = node;
        this.leafNode = leafNode;
    }

    public NormalizedNode<?, ?> getNode() {
        return node;
    }

    public void setNode(NormalizedNode<?, ?> node) {
        this.node = node;
    }

    public NormalizedNode<?, ?> getLeafNode() {
        return leafNode;
    }

    public void setLeafNode(NormalizedNode<?, ?> leafNode) {
        this.leafNode = leafNode;
    }

    public YangInstanceIdentifier getLogicalIdentifier() {
        return logicalIdentifier;
    }

    public void setLogicalIdentifier(YangInstanceIdentifier logicalIdentifier) {
        this.logicalIdentifier = logicalIdentifier;
    }
}
