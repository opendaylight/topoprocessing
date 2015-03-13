/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PathTranslatorTest {

    private static final Logger LOG = LoggerFactory.getLogger(PathTranslatorTest.class);
    private PathTranslator pathTranslator = new PathTranslator();

    @Mock
    SchemaContext mockContext;
    @Mock
    Module mockModule;
    @Mock
    DataSchemaNode mockDataSchemaNode1;
    @Mock
    DataSchemaNode mockDataSchemaNode2;
    @Before
    public void startup() {
        GlobalSchemaContextHolder.setSchemaContext(mockContext);
    }

    @Test
    public void testYangPathNull() {
        //pathTranslator.translate(null);
    }

    @Test
    public void testLegalPath() {
        Mockito.when(mockContext.findModuleByName("flow-node-inventory", null)).thenReturn(mockModule);
        try {
            URI uri1 = new URI("nameSpace1");
            QName qName1 = new QName(uri1, "localName1");
            Mockito.when(mockModule.getDataChildByName("flowcapablenode")).thenReturn(mockDataSchemaNode1);
            Mockito.when(mockDataSchemaNode1.getQName()).thenReturn(qName1);
            URI uri2 = new URI("nameSpace2");
            QName qName2 = new QName(uri2, "localName2");
            Mockito.when(mockModule.getDataChildByName("ip-address")).thenReturn(mockDataSchemaNode2);
            Mockito.when(mockDataSchemaNode2.getQName()).thenReturn(qName2);

            YangInstanceIdentifier yangInstanceIdentifier = 
                    pathTranslator.translate("flow-node-inventory:flowcapablenode/flow-node-inventory:ip-address");
            YangInstanceIdentifier expectedIdentifier = 
                    YangInstanceIdentifier.builder().node(qName1).node(qName2).build(); 
            Assert.assertTrue("Incorrect valid YangInstanceIdentifier",
                    expectedIdentifier.equals(yangInstanceIdentifier));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testTwoColonsIllegalArgument() {
        pathTranslator.translate("bgpnode::bgp/desc:desc/ip:ip");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNoColonsIllegalArgument() {
        pathTranslator.translate("bgpnodebgp/desc:desc/ip:ip");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testColonAtLastPosition() {
        pathTranslator.translate("bgpnode:/desc:desc/ip:ip");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testColonAtFirstPosition() {
        pathTranslator.translate(":bgp/desc:desc/ip:ip");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyString() {
        pathTranslator.translate("");
    }

    @Test
    public void testOneArgument() {
        Mockito.when(mockContext.findModuleByName("bgpnode", null)).thenReturn(mockModule);
        try {
            URI uri = new URI("nameSpace1");
            QName qName = new QName(uri, "localName1");
            Mockito.when(mockModule.getDataChildByName("bgp")).thenReturn(mockDataSchemaNode1);
            Mockito.when(mockDataSchemaNode1.getQName()).thenReturn(qName);

            pathTranslator.translate("bgpnode:bgp");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
