/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

/**
 * @author michal.polkorab
 *
 */
public class EqualityAggregator extends TopologyAggregator {

    private static final int UNDERLAY_ITEMS_IN_OVERLAY_ITEM = 2;
    private static final boolean WRAP_SINGLE_UNDERLAY_ITEM = false;

    @Override
    protected int getMinUnderlayItems() {
        return UNDERLAY_ITEMS_IN_OVERLAY_ITEM;
    }

    @Override
    protected boolean wrapSingleItem() {
        return WRAP_SINGLE_UNDERLAY_ITEM;
    }

}
