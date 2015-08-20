/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import com.google.common.base.Splitter;

import org.opendaylight.topoprocessing.impl.modelAdapters.ModelAdapter;
import org.opendaylight.topoprocessing.impl.modelAdapters.Inventory.InvModelAdapter;
import org.opendaylight.topoprocessing.impl.modelAdapters.NetworkTopology.NTModelAdapter;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

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
            GlobalSchemaContextHolder schemaHolder, ModelAdapter modelAdapter) {
        LOGGER.debug("Translating target-field path: " + yangPath);
        DataSchemaContextTree contextTree = schemaHolder.getContextTree();
        QName itemQName = TopologyQNames.buildItemQName(correlationItem);
        YangInstanceIdentifier itemIdentifier = null;
        itemIdentifier = createBaseIdentifier(correlationItem, modelAdapter,
                itemQName);
        DataSchemaContextNode<?> contextNode = contextTree.getChild(itemIdentifier);
        Iterable<String> pathArguments = splitYangPath(yangPath);
        Iterator<String> iterator = pathArguments.iterator();
        YangInstanceIdentifier targetIdentifier = YangInstanceIdentifier.builder().build();
        while (iterator.hasNext()) {
            String currentId = iterator.next();
            QName currentQname = parseQname(schemaHolder.getSchemaContext(), currentId);
            contextNode = contextNode.getChild(currentQname);
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

    private YangInstanceIdentifier createBaseIdentifier(CorrelationItemEnum correlationItem, ModelAdapter modelAdapter,
            QName itemQName) {
        YangInstanceIdentifier itemIdentifier;
        // if inputModel == null, than use network-topology model as default 
        if (modelAdapter instanceof InvModelAdapter) {
            itemIdentifier = YangInstanceIdentifier.builder()
                    .node(Nodes.QNAME)
                    .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME)
                    .nodeWithKey(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.QNAME,
                            TopologyQNames.INVENTORY_NODE_ID_QNAME, "")
                    .build();
        } else {
            if(modelAdapter instanceof NTModelAdapter){
                YangInstanceIdentifier.InstanceIdentifierBuilder itemIdentifierBuilder = YangInstanceIdentifier.builder()
                        .node(NetworkTopology.QNAME)
                        .node(Topology.QNAME)
                        .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, "");
                if (CorrelationItemEnum.TerminationPoint.equals(correlationItem)) {
                    itemIdentifierBuilder.node(Node.QNAME)
                        .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, "");
                }
                itemIdentifier = itemIdentifierBuilder.node(itemQName)
                        .nodeWithKey(itemQName, TopologyQNames.buildItemIdQName(correlationItem), "").build();
            } else {
                //TODO I2RS identifier
                itemIdentifier = null;
            }
        }
        return itemIdentifier;
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
