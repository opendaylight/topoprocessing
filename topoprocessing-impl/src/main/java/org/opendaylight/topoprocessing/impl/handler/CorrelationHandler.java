/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.handler;

import java.util.List;

import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;

/**
 * Differentiates received correlations and engages correct {@link CorrelationHandler}
 * @author michal.polkorab
 */
public class CorrelationHandler {

    private PathTranslator pathTranslator;

    /**
     * @param globalContext
     */
    public CorrelationHandler() {
        pathTranslator = new PathTranslator();
    }

    /**
     * Translates received correlation and delegates functionality to specific correlation handler,
     * which engages / initializes needed objects
     * @param augmentation defined correlation
     */
    public void translate(CorrelationAugment augmentation) {
        List<Correlation> correlations = augmentation.getCorrelations().getCorrelation();
        for (Correlation correlation : correlations) {
            if (correlation.getName().equals(Equality.class)) {
                EqualityCorrelationHandler equalityHandler = new EqualityCorrelationHandler(pathTranslator);
                equalityHandler.handle((EqualityCase) correlation.getCorrelationType());
                
            }
        }
    }
}
