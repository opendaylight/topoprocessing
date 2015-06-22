/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.api.filtration;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author martin.uhlir
 *
 */
public interface UnderlayItem {

    /**
     * @return underlay topology item
     */
    public NormalizedNode<?, ?> getNode();

}
