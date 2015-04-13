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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
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
     * @param correlationItem 
     * @return {@link YangInstanceIdentifier} leading to target node
     * @throws IllegalArgumentException if yangPath is in incorrect format
     */
    public YangInstanceIdentifier translate(String yangPath, CorrelationItemEnum correlationItem)
            throws IllegalArgumentException {
        LOGGER.debug("Translating target-field path: " + yangPath);
        globalSchemaContext = GlobalSchemaContextHolder.getSchemaContext();
        ListSchemaNode parent = null;
        SchemaPath path = null;
        if (CorrelationItemEnum.Node.equals(correlationItem)) {
            Module module = globalSchemaContext.findModuleByName("opendaylight-inventory", null);
            ContainerSchemaNode nodes = (ContainerSchemaNode) module.getDataChildByName("nodes");
            parent = (ListSchemaNode) nodes.getDataChildByName("node");
        }
        Iterable<String> pathArguments = splitYangPath(yangPath);
        Iterator<String> iterator = pathArguments.iterator();
        if (iterator.hasNext()) {
            path = parsePath(parent, iterator);
        } else {
            throw new IllegalArgumentException("Target-field can't be empty : " + pathArguments);
        }
        Iterable<QName> pathFromRoot = path.getPathFromRoot();
        LOGGER.debug("Translated target-field path: " + pathFromRoot);
        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        for (QName qName : pathFromRoot) {
            builder.node(qName);
        }
        return builder.build();
    }

    private SchemaPath parsePath(SchemaNode parent, Iterator<String> iterator) {
        SchemaPath schemaPath = null;
        DataSchemaNode child = null;
        String pathArgument = iterator.next();
        int index = getSeparatorIndex(pathArgument, ':');
        String childName = getChildName(pathArgument, index + 1);
        if (parent instanceof ContainerSchemaNode) {
            child = ((ContainerSchemaNode) parent).getDataChildByName(childName);
        } else if (parent instanceof ChoiceCaseNode) {
            child = ((ChoiceCaseNode) parent).getDataChildByName(childName);
        } else if  (parent instanceof ChoiceSchemaNode) {
            child = ((ChoiceSchemaNode) parent).getCaseNodeByName(childName);
        } else if  (parent instanceof ListSchemaNode) {
            child = ((ListSchemaNode) parent).getDataChildByName(childName);
        } else if  (parent instanceof LeafSchemaNode) {
            schemaPath = ((LeafSchemaNode) parent).getPath();
        }
        if (iterator.hasNext()) {
            schemaPath = parsePath(child, iterator);
        } else {
            schemaPath = child.getPath();
        }
        return schemaPath;
    }

    private static Iterable<String> splitYangPath(String yangPath) {
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

    private static String getChildName(String s, int index) {
        return s.substring(index, s.length());
    }
}
