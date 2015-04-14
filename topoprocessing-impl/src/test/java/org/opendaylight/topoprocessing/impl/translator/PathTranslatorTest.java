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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author martin.uhlir
 *
 */
//@RunWith(MockitoJUnitRunner.class)
public class PathTranslatorTest {

    private PathTranslator pathTranslator = new PathTranslator();

    @Mock
    SchemaContext mockContext;
    @Mock
    Module mockModule;
    @Mock
    DataSchemaNode mockDataSchemaNode1;
    @Mock
    DataSchemaNode mockDataSchemaNode2;

    private GlobalSchemaContextHolder contextHolder;

    //@Before
    public void startup() {
        contextHolder = new GlobalSchemaContextHolder(mockContext);
    }

    /**
     * Test case: valid, legal path is passed to translate() method.
     * Result of this method (YangInstanceIdentifier) should be equal to expected value. 
     * @throws URISyntaxException if URI is in incorrect format. Should not happen because URI in this test case
     * is a constant
     */
    //@Test
    public void testLegalPath() throws URISyntaxException {
        Mockito.when(mockContext.findModuleByName("flow-node-inventory", null)).thenReturn(mockModule);
        URI uri1 = new URI("nameSpace1");
        QName qName1 = new QName(uri1, "localName1");
        Mockito.when(mockModule.getDataChildByName("flowcapablenode")).thenReturn(mockDataSchemaNode1);
        Mockito.when(mockDataSchemaNode1.getQName()).thenReturn(qName1);
        URI uri2 = new URI("nameSpace2");
        QName qName2 = new QName(uri2, "localName2");
        Mockito.when(mockModule.getDataChildByName("ip-address")).thenReturn(mockDataSchemaNode2);
        Mockito.when(mockDataSchemaNode2.getQName()).thenReturn(qName2);

        YangInstanceIdentifier yangInstanceIdentifier =
                pathTranslator.translate("flow-node-inventory:flowcapablenode/flow-node-inventory:ip-address",
                        null, contextHolder);
        YangInstanceIdentifier expectedIdentifier =
                YangInstanceIdentifier.builder().node(qName1).node(qName2).build();
        Assert.assertTrue("Incorrect valid YangInstanceIdentifier",
                expectedIdentifier.equals(yangInstanceIdentifier));
    }

    /**
     * Test case: two colons in the path should not be accepted
     */
    //@Test(expected=IllegalArgumentException.class)
    public void testTwoColonsIllegalArgument() {
        pathTranslator.translate("bgpnode::bgp/desc:desc/ip:ip", null, contextHolder);
    }

    /**
     * Test case: the path should contain one and only one colon
     */
    //@Test(expected=IllegalArgumentException.class)
    public void testNoColonsIllegalArgument() {
        pathTranslator.translate("bgpnodebgp/desc:desc/ip:ip", null, contextHolder);
    }

    /**
     * Test case: path in the argument does not match expected format [module name]:[child name] 
     */
    //@Test(expected=IllegalArgumentException.class)
    public void testColonAtLastPosition() {
        pathTranslator.translate("bgpnode:/desc:desc/ip:ip", null, contextHolder);
    }

    /**
     * Test case: path in the argument does not match expected format [module name]:[child name] 
     */
    //@Test(expected=IllegalArgumentException.class)
    public void testColonAtFirstPosition() {
        pathTranslator.translate(":bgp/desc:desc/ip:ip", null, contextHolder);
    }

    /**
     * Test case: empty string as argument is not allowed 
     */
    //@Test(expected=IllegalArgumentException.class)
    public void testEmptyString() {
        pathTranslator.translate("", null, contextHolder);
    }

    /**
     * Test case: null as argument is not allowed 
     */
    //@Test(expected=IllegalArgumentException.class)
    public void testYangPathNull() {
        pathTranslator.translate(null, null, contextHolder);
    }

    /**
     * Test case: legal path containing only one [module name]:[child name] shall be accepted
     * @throws URISyntaxException if URI is in incorrect format. Should not happen because URI in this test case
     * is a constant
     */
    //@Test
    public void testOneArgument() throws URISyntaxException {
        Mockito.when(mockContext.findModuleByName("bgpnode", null)).thenReturn(mockModule);
        URI uri = new URI("nameSpace1");
        QName qName = new QName(uri, "localName1");
        Mockito.when(mockModule.getDataChildByName("bgp")).thenReturn(mockDataSchemaNode1);
        Mockito.when(mockDataSchemaNode1.getQName()).thenReturn(qName);

        pathTranslator.translate("bgpnode:bgp", null, contextHolder);
    }
}
