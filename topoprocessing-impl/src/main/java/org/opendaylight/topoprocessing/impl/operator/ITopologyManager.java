/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;

/**
 * @author samuel.kontris
 *
 */
public interface ITopologyManager {

    /**
     * @param newOverlayItem - OverlayItem which shall be put into wrapper
     */
    public void addOverlayItem(OverlayItem newOverlayItem);

    /**
     * @param overlayItemIdentifier OverlayItem with new changes to update
     */
    public void updateOverlayItem(OverlayItem overlayItemIdentifier);

    /**
     * @param overlayItemIdentifier OverlayItem to remove
     */
    public void removeOverlayItem(OverlayItem overlayItemIdentifier);
}
