/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.linkComputation.translator;

import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.translator.NodeTranslator;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matej.perina
 *
 */

public class LCNodeTranslator  implements NodeTranslator{

    @Override
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper) {
        // There will be no node translations in link-computation module
        return null;
    }
}
