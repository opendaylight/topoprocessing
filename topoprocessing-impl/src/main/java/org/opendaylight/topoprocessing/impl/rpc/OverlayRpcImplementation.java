/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.rpc;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.spi.ForwardingDOMRpcImplementation;

/**
 * @author michal.polkorab
 *
 */
public class OverlayRpcImplementation extends ForwardingDOMRpcImplementation {

    private UnderlayRpcImplementation underlayImplementation;

    /**
     * Default constructor
     * @param underlayImplementation 
     */
    public OverlayRpcImplementation(UnderlayRpcImplementation underlayImplementation) {
        this.underlayImplementation = underlayImplementation;
    }

    @Override
    protected DOMRpcImplementation delegate() {
        return underlayImplementation;
    }

}
