/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Splitter;


/**
 * @author michal.polkorab
 *
 */
public class PathTranslator {

    
    private SchemaContext globalSchemaContext;

    /**
     * Translates yang path into {@link YangInstanceIdentifier}
     *
     * This method will be replaced by the DAO implementation in the next commit
     *
     * @param yangPath path to target node
     * @return {@link YangInstanceIdentifier} leading to target node
     * @throws IllegalArgumentException if yangPath is in incorrect format
     * @throws Exception 
     */
    public YangInstanceIdentifier translate(String yangPath) throws IllegalArgumentException {
        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        globalSchemaContext = GlobalSchemaContextHolder.getSchemaContext();

        Iterable<String> yangPathSplitted = Splitter.on("/").split(yangPath);
        for (String yangPathString : yangPathSplitted) {
            int index = getSeparatorIndex(yangPathString, ':');
            String moduleName = getModuleName(yangPathString, index);
            Module module = globalSchemaContext.findModuleByName(moduleName, null);
            String childName = getChildName(yangPathString, index);
            DataSchemaNode dataChildByName = module.getDataChildByName(childName);
            builder.node(dataChildByName.getQName());
        }
        return builder.build();
    }

    private static int getSeparatorIndex(String s, char separator) {
        int index = s.indexOf(separator);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid format of yang path, ':' not found in " + s);
        } else if (s.lastIndexOf(':') != index) {
            throw new IllegalArgumentException("Invalid format of yang path, only one occurence of ':' is valid in " + s);
        }
        return index;
    }

    private static String getModuleName(String s, int index) {
        return s.substring(0, index);
    }

    private static String getChildName(String yangPathString, int index) {
        return yangPathString.substring(index, yangPathString.length());
    }

}
