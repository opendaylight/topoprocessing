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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.EqualityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.network.topology.topology.correlations.correlation.correlation.type.equality._case.Equality;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Sets up all needed structures for correct Equality correlation processing
 * @author michal.polkorab
 */
public class EqualityCorrelationHandler {

    private PathTranslator pathTranslator;

    /**
     * @param pathTranslator
     */
    public EqualityCorrelationHandler(PathTranslator pathTranslator) {
        this.pathTranslator = pathTranslator;
    }

    /**
     * @param equalityCase
     */
    public void handle(EqualityCase equalityCase) {
        Equality equality = equalityCase.getEquality();
        List<LeafPath> leaves = equality.getLeaf();
        for (LeafPath leafPath : leaves) {
            String path = leafPath.getValue();
            YangInstanceIdentifier identifier = pathTranslator.translate(path);
        }
        // pass InstanceIdentifiers to EqualityAggregator / EqualityProvider
    }

}
