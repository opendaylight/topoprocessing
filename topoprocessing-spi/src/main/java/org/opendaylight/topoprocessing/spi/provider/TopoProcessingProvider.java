/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.spi.provider;

import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;

/**
 * @author michal.polkorab
 *
 */
public interface TopoProcessingProvider extends AutoCloseable {

    /**
     * Starts Topology Processing Framework
     */
    void startup();

    /**
     * Registers user defined filtrator.
     * @param filterType - key under which the filtrator will be registered, represented by modeled identity
     * @param filtratorFactory - factory dealing with filtering functionality
     */
    void registerFiltratorFactory(Class<? extends FilterBase> filterType, FiltratorFactory filtratorFactory);

    /**
     * Unregister user defined filtrator.
     * @param filterType - key of filtrator to be unregistered - represented by modeled identity
     */
    void unregisterFiltratorFactory(Class<? extends FilterBase> filterType);

    /**
     * Registers specific ModelAdaper.
     * @param model - defines topology model
     */
    void registerModelAdapter(Class<? extends Model> model, Object modelAdapter);
}
