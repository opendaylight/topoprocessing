/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import java.util.List;

import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
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
    public YangInstanceIdentifier translate(String yangPath, CorrelationItemEnum correlationItem) throws IllegalArgumentException {
        LOGGER.error("Translating target-item path: " + yangPath);
        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        globalSchemaContext = GlobalSchemaContextHolder.getSchemaContext();
        ListSchemaNode node = null;
        SchemaPath path = null;
        if (correlationItem.equals(CorrelationItemEnum.Node)) {
            LOGGER.error("MODULE NAME: opendaylight-inventory");
            Module module = globalSchemaContext.findModuleByName("opendaylight-inventory", null);
            LOGGER.error("MODULE: " + module);
            LOGGER.error("CHILDS: " + module.getChildNodes());
            DataSchemaNode nodes = module.getDataChildByName("nodes");
            LOGGER.error("NODES CHILDS: " + ((ContainerSchemaNode) nodes).getChildNodes());
            node = ((ListSchemaNode)((ContainerSchemaNode) nodes).getDataChildByName("node"));
            LOGGER.error("NODE CHILDS: " + node.getChildNodes());
            LOGGER.error("UNKNOWN SCHEMA NODES: " + node.getUnknownSchemaNodes());
            LOGGER.error("NODE AUGMENTATIONS: " + node.getAvailableAugmentations());
            LOGGER.error("NODE FLOWCAPABLE? : " + node.getDataChildByName("flow-capable-node"));
//            DataSchemaNode ipAddressNode = node.getDataChildByName("ip-address");
            
        }
        Iterable<String> pathArguments = splitYangPath(yangPath);
        for (String pathArgument : pathArguments) {
//            LOGGER.error("IP ADDRESS: " + ipAddressNode);
//            LeafSchemaNode leafnode = (LeafSchemaNode) ipAddressNode;
//            LOGGER.error("IP ADDRESS SCHEMA PATH: " + leafnode.getPath());
            
            int index = getSeparatorIndex(pathArgument, ':');
            String moduleName = getModuleName(pathArgument, index);
            LOGGER.error("MODULE NAME: " + moduleName);
            Module module = globalSchemaContext.findModuleByName(moduleName, null);
            LOGGER.error("MODULE: " + module);
            String childName = getChildName(pathArgument, index + 1);
            LOGGER.error("CHILDNAME: " + childName);
            DataSchemaNode byName = node.getDataChildByName(childName);
            path = byName.getPath();
            
//            LOGGER.error("CHILDS: " + module.getChildNodes());
//            DataSchemaNode dataChildByName = module.getDataChildByName(childName);
//            LOGGER.error("DATACHILDBYNAME: " + dataChildByName);
//            QName qName = dataChildByName.getQName();
//            LOGGER.error("QNAME: " + qName);
//            builder.node(qName);
        }
        Iterable<QName> pathFromRoot = path.getPathFromRoot();
        for (QName qName : pathFromRoot) {
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
