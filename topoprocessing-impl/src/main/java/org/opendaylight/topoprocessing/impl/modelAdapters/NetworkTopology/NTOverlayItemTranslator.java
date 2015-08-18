/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.NetworkTopology;

import org.opendaylight.topoprocessing.impl.modelAdapters.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/** *
 * @author matej.perina
 */
public class NTOverlayItemTranslator extends OverlayItemTranslator{

    @Override
    protected NormalizedNode<?, ?> translateNode(OverlayItemWrapper wrapper) {
        return new NTNodeTranslator().translate(wrapper);
    }

    @Override
    protected NormalizedNode<?, ?> translateLink(OverlayItemWrapper wrapper) {
        return new NTLinkTranslator().translate(wrapper);
    }

}
