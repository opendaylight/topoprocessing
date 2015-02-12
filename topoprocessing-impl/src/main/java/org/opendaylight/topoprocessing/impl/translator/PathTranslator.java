/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import java.util.Iterator;

import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
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
     * @param yangPath path to target node
     * @return {@link YangInstanceIdentifier} leading to target node
     */
    public YangInstanceIdentifier translate(String yangPath) {
        if (yangPath == null) {
            throw new IllegalArgumentException("Received path is null");
        }
        Iterator<String> pathIterator = Splitter.onPattern("/").omitEmptyStrings()
                .split(yangPath).iterator();
        if (pathIterator.hasNext() == false) {
            throw new IllegalArgumentException("Path can't be empty. Received path: " + yangPath);
        }
        String startModule = pathIterator.next();
        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        SchemaContext schemaContext = GlobalSchemaContextHolder.getSchemaContext();
        final Module readModule = schemaContext.findModuleByName(startModule, null);
        YangInstanceIdentifier identifier = collectPathArguments(builder, pathIterator, readModule, schemaContext);
        if (identifier == null) {
            throw new IllegalStateException("Unable to create identifier for given path: " + yangPath);
        }
        return identifier;
    }

    private YangInstanceIdentifier collectPathArguments(final InstanceIdentifierBuilder builder,
            final Iterator<String> pathIterator, final DataNodeContainer parentNode,
            SchemaContext schemaContext) {
        if (parentNode == null) {
            return null;
        }
        if (pathIterator.hasNext() == false) {
            return builder.build();
        }
        final String head = pathIterator.next();
        final String nodeName = toNodeName(head);
        final String moduleName = toModuleName(head);
        DataSchemaNode targetNode = null;
        if (!Strings.isNullOrEmpty(moduleName)) {
            Module module = schemaContext.findModuleByName(moduleName, null);
            if (module == null) {
                throw new IllegalStateException("Specified module: " + moduleName + " was not found.");
            }
            targetNode = parentNode.getDataChildByName(nodeName);
            if (targetNode == null) {
                throw new IllegalStateException("Specified node: " + nodeName + " was not found.");
            }
        } else {
            throw new IllegalStateException("Specified module name is null or empty.");
        }
        builder.node(targetNode.getQName());
        if ((targetNode instanceof DataNodeContainer)) {
            return collectPathArguments(builder, pathIterator, ((DataNodeContainer) targetNode), schemaContext);
        }
        return builder.build();
    }

    private static String toNodeName(final String str) {
        final int idx = str.indexOf(':');
        if (idx == -1) {
            return str;
        }
        // Make sure there is only one occurrence
        if (str.indexOf(':', idx + 1) != -1) {
            return str;
        }
        return str.substring(idx + 1);
    }

    private static String toModuleName(final String str) {
        final int idx = str.indexOf(':');
        if (idx == -1) {
            return null;
        }
        // Make sure there is only one occurrence
        if (str.indexOf(':', idx + 1) != -1) {
            return null;
        }
        return str.substring(0, idx);
    }

}
