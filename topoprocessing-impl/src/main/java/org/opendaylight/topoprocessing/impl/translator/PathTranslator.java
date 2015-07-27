/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import com.google.common.base.Splitter;
import java.util.Iterator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.uhlir
 *
 */
public class PathTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathTranslator.class);

    /**
     * Translates yang path into {@link YangInstanceIdentifier}
     *
     * @param yangPath path to target item
     * @param correlationItem   Type of Correlation Item
     * @param schemaHolder      Provides access to SchemaContext
     * @param inputModel specifies which base path will be used for yangPath lookup
     * @return {@link YangInstanceIdentifier} leading to target item
     * @throws IllegalArgumentException if yangPath is in incorrect format
     * @throws IllegalStateException if required module is not loaded
     */
    public YangInstanceIdentifier translate(String yangPath, CorrelationItemEnum correlationItem,
            GlobalSchemaContextHolder schemaHolder, Model inputModel) {
        LOGGER.debug("Translating target-field path: " + yangPath);
        DataSchemaContextTree contextTree = schemaHolder.getContextTree();
        QName itemQName = TopologyQNames.buildItemQName(correlationItem);
        YangInstanceIdentifier itemIdentifier = null;
        if (Model.NetworkTopology.equals(inputModel)) {
            itemIdentifier = YangInstanceIdentifier.builder()
                    .node(NetworkTopology.QNAME)
                    .node(Topology.QNAME)
                    .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, "")
                    .node(itemQName)
                    .nodeWithKey(itemQName,
                            TopologyQNames.buildItemIdQName(correlationItem), "")
                    .build();
        } else {
            itemIdentifier = YangInstanceIdentifier.builder()
                    .node(Nodes.QNAME)
                    .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME)
                    .nodeWithKey(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME,
                            QName.create("urn:opendaylight:inventory", "2013-08-19", "id"), "")
                    .build();
        }
        DataSchemaContextNode<?> contextNode = contextTree.getChild(itemIdentifier);
        LOGGER.debug("prvy contextNode: " + contextNode);
        Iterable<String> pathArguments = splitYangPath(yangPath);
        Iterator<String> iterator = pathArguments.iterator();
        YangInstanceIdentifier targetIdentifier = YangInstanceIdentifier.builder().build();
        while (iterator.hasNext()) {
            String currentId = iterator.next();
            LOGGER.debug("currentId: " + currentId);
            QName currentQname = parseQname(schemaHolder.getSchemaContext(), currentId);
            LOGGER.debug("currentQname: " + currentQname);
            contextNode = contextNode.getChild(currentQname);
            LOGGER.debug("contextNode: " + contextNode);
            while (contextNode.isMixin()) {
                targetIdentifier = YangInstanceIdentifier.create(targetIdentifier.getPathArguments())
                        .node(contextNode.getIdentifier());
                contextNode = contextNode.getChild(currentQname);
            }
            targetIdentifier = YangInstanceIdentifier.create(targetIdentifier.getPathArguments())
                    .node(contextNode.getIdentifier());
        }
        LOGGER.debug("Target-field identifier: " + targetIdentifier);
        return targetIdentifier;
    }

    private static QName parseQname(SchemaContext context, String pathArgument) {
        int index = getSeparatorIndex(pathArgument, ':');
        String moduleName = getModuleName(pathArgument, index);
        Module module = context.findModuleByName(moduleName, null);
        if (null == module) {
            throw new IllegalStateException("Couldn't find specified module: " + moduleName
                    + ". Check if all necessary modules are loaded");
        }
        String childName = getChildName(pathArgument, index + 1);
        return QName.create(module.getNamespace(), module.getRevision(), childName);
    }

    private static Iterable<String> splitYangPath(String yangPath) {
        if (yangPath == null) {
            throw new IllegalArgumentException("YangPath cannot be null");
        }
        return Splitter.on("/").split(yangPath);
    }

    private static int getSeparatorIndex(String pathArgument, char separator) {
        int index = pathArgument.indexOf(separator);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid format of yang path, ':' not found in '" + pathArgument + "'");
        } else if (pathArgument.lastIndexOf(separator) != index) {
            throw new IllegalArgumentException("Invalid format of yang path, only one occurence of ':'"
                    + " is valid in " + pathArgument);
        } else if (index == pathArgument.length() - 1 || index == 0) {
            throw new IllegalArgumentException("Invalid format of yang path, "
                    + "format [module name]:[child name] expected in " + pathArgument);
        }
        return index;
    }

    private static String getModuleName(String pathArgument, int index) {
        return pathArgument.substring(0, index);
    }

    private static String getChildName(String pathArgument, int index) {
        return pathArgument.substring(index, pathArgument.length());
    }
}
