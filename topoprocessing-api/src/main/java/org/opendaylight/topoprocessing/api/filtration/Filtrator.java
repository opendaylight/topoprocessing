/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.api.filtration;


/**
 * @author matus.marko
 */
public interface Filtrator {

    /**
     * Filters {@link UnderlayItem}
     * @param node {@link UnderlayItem} to be filtered
     * @return true if node was filtered out false otherwise
     */
    boolean isFiltered(UnderlayItem node);
}
