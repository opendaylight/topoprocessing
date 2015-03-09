/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.listener;

import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * Listens on {@link SchemaContext} update and updates
 * existing / stored {@link SchemaContext}
 * 
 * @author michal.polkorab
 */
public class GlobalSchemaContextListener implements SchemaContextListener {

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        GlobalSchemaContextHolder.setSchemaContext(context);
    }

}