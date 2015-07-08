/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.structure;

import org.opendaylight.topoprocessing.impl.operator.filtrator.ScriptFiltrator;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;

/**
 * This class is used for receiving the value from scripts.
 * Scripts are used only in {@link ScriptFiltrator} and in {@link TopologyAggregator}
 * - both occurrences need only boolean expression to be returned.
 * @author michal.polkorab
 */
public class ScriptResult {

    private boolean result;

    /**
     * @return the result
     */
    public boolean getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(boolean result) {
        this.result = result;
    }

}
