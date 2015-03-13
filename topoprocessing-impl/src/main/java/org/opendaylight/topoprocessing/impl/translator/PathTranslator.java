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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 import com.google.common.base.Splitter;


/**
 * @author martin.uhlir
 *
 */
public class PathTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathTranslator.class);

    private SchemaContext globalSchemaContext;

    /**
     * Translates yang path into {@link YangInstanceIdentifier}
     *
     * @param yangPath path to target node
     * @return {@link YangInstanceIdentifier} leading to target node
     * @throws IllegalArgumentException if yangPath is in incorrect format
     */
    public YangInstanceIdentifier translate(String yangPath) throws IllegalArgumentException {
        LOGGER.debug("Translating target-item path: " + yangPath);
        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        globalSchemaContext = GlobalSchemaContextHolder.getSchemaContext();
        Iterable<String> pathArguments = splitYangPath(yangPath);
        for (String pathArgument : pathArguments) {
            int index = getSeparatorIndex(pathArgument, ':');
            String moduleName = getModuleName(pathArgument, index);
            Module module = globalSchemaContext.findModuleByName(moduleName, null);
            String childName = getChildName(pathArgument, index + 1);
            DataSchemaNode dataChildByName = module.getDataChildByName(childName);
            QName qName = dataChildByName.getQName();
            builder.node(qName);
        }
        return builder.build();
    }

    private Iterable<String> splitYangPath(String yangPath) {
        if (yangPath == null) {
            throw new IllegalArgumentException("YangPath cannot be null");
        }
        return Splitter.on("/").split(yangPath);
    }

    private static int getSeparatorIndex(String s, char separator) {
        int index = s.indexOf(separator);
         if (index == -1) {
            throw new IllegalArgumentException("Invalid format of yang path, ':' not found in " + s);
        } else if (s.lastIndexOf(separator) != index) {
            throw new IllegalArgumentException("Invalid format of yang path, only one occurence of ':'"
                    + " is valid in " + s);
        } else if (index == s.length() - 1 || index == 0) {
            throw new IllegalArgumentException("Invalid format of yang path, "
                    + "format [module name]:[child name] expected in " + s);
        }
        return index;
    }

    private static String getModuleName(String s, int index) {
        return s.substring(0, index);
    }

    private static String getChildName(String s, int index) {
        return s.substring(index, s.length());
    }
}
