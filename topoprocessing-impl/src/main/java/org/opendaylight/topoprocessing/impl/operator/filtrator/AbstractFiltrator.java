/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import com.google.common.base.Preconditions;

import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author michal.vrsansky
 */
public abstract class AbstractFiltrator implements Filtrator {
    private final YangInstanceIdentifier pathIdentifier;

    /**
     * Stores PathIdentifier for later use.
     * If PathIdentifier is null, throws {@link NullPointerException}
     * @param pathIdentifier PathIdentifier identifying target field which on this filtrator operate
     * @throws NullPointerException if the path identifier is null
     */
    public AbstractFiltrator(YangInstanceIdentifier pathIdentifier) {
        Preconditions.checkNotNull(pathIdentifier);
        this.pathIdentifier = pathIdentifier;
    }

    /**
     * Returns stored PathIdentifier identifying target field which on this filtrator operate.
     * If PathIdentifier is null, throws {@link NullPointerException}
     * @return stored PathIdentifier
     * @throws NullPointerException if the path identifier is null
     */
    public YangInstanceIdentifier getPathIdentifier() {
        Preconditions.checkNotNull(pathIdentifier);
        return pathIdentifier;
    }

}
