/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;

/**
 * @author matus.marko
 */
public interface Filtrator {

    /**
     * Filters {@link PhysicalNode}
     * @param node {@link PhysicalNode} to be filtered
     * @return true if node was filtered out false otherwise
     */
    boolean isFiltered(PhysicalNode node);
}
