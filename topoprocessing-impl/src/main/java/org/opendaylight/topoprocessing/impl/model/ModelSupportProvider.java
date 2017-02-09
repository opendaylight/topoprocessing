/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;

public enum ModelSupportProvider {
    MODEL_PROVIDER;

    private final Map<Class<? extends Model>, ModelAdapter> modelAdapters = new ConcurrentHashMap<>();

    public void registerModelAdapter(Class<? extends Model> model, ModelAdapter modelAdapter) {
        modelAdapters.put(model, modelAdapter);
    }

    public ModelAdapter getModelAdapter(Class<? extends Model> model) {
        return modelAdapters.get(model);
    }
}
