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
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
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

    /**
     * Translates yang path into {@link YangInstanceIdentifier}
     *
     * @param yangPath path to target node
     * @param correlationItem 
     * @param schemaHolder 
     * @return {@link YangInstanceIdentifier} leading to target node
     * @throws IllegalArgumentException if yangPath is in incorrect format
     */
    public YangInstanceIdentifier translate(String yangPath, CorrelationItemEnum correlationItem,
            GlobalSchemaContextHolder schemaHolder) throws IllegalArgumentException {
        LOGGER.debug("Translating target-field path: " + yangPath);
        SchemaContext globalSchemaContext = schemaHolder.getSchemaContext();
        DataSchemaContextTree contextTree = new GlobalSchemaContextHolder(globalSchemaContext).getContextTree();
        YangInstanceIdentifier nodeIdentifier = YangInstanceIdentifier.builder()
                .node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TopologyQNames.topologyIdQName, "")
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, TopologyQNames.networkNodeIdQName, "")
                .build();
        DataSchemaContextNode<?> context = contextTree.getChild(nodeIdentifier);
        Iterable<String> pathArguments = splitYangPath(yangPath);
        Iterator<String> iterator = pathArguments.iterator();
        YangInstanceIdentifier targetIdentifier = YangInstanceIdentifier.builder().build();
        while (iterator.hasNext()) {
            String currentId = iterator.next();
            QName currentQname = parseQname(schemaHolder.getSchemaContext(), currentId);
            context = context.getChild(currentQname);
            while (context.isMixin()) {
                PathArgument identifier = context.getIdentifier();
                targetIdentifier = YangInstanceIdentifier.create(targetIdentifier.getPathArguments())
                        .node(identifier);
                context = context.getChild(currentQname);
            }
            targetIdentifier = YangInstanceIdentifier.create(targetIdentifier.getPathArguments())
                    .node(context.getIdentifier());
        }
        LOGGER.debug("Target-field identifier: " + targetIdentifier);
        return targetIdentifier;
    }

    private QName parseQname(SchemaContext context, String pathArgument) {
        int index = getSeparatorIndex(pathArgument, ':');
        String moduleName = getModuleName(pathArgument, index);
        Module module = context.findModuleByName(moduleName, null);
        String childName = getChildName(pathArgument, index + 1);
        return QName.create(module.getNamespace(), module.getRevision(), childName);
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

    private static String getModuleName(String s, int index) {
        return s.substring(0, index);
    }

    private static String getChildName(String s, int index) {
        return s.substring(index, s.length());
    }
}
