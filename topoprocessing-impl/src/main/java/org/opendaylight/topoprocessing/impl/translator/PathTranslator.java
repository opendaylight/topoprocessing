/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import java.util.Iterator;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;


/**
 * @author michal.polkorab
 *
 */
public class PathTranslator {

    /**
     * Translates yang path into {@link YangInstanceIdentifier}
     *
     * This method will be replaced by the DAO implementation in the next commit
     *
     * @param yangPath path to target node
     * @return {@link YangInstanceIdentifier} leading to target node
     */
    public YangInstanceIdentifier translate(String yangPath) {

        return null;
    }
}
