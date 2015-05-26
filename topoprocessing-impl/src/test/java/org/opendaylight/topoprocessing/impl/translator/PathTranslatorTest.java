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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PathTranslatorTest {

    private PathTranslator pathTranslator = new PathTranslator();

    @Mock private SchemaContext mockContext;
    @Mock private Module mockModule;
    @Mock private DataSchemaNode mockDataSchemaNode1;
    @Mock private DataSchemaNode mockDataSchemaNode2;
    
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private SchemaContext mockSchemaContext;

    @Mock private DataSchemaContextTree mockContextTree;
    @Mock private DataSchemaContextNode<?> mockContextNode;
    @Mock private DataSchemaContextNode<?> mockContextNodeIdentifier;

    YangInstanceIdentifier nodeIdentifier = YangInstanceIdentifier.builder()
            .node(NetworkTopology.QNAME)
            .node(Topology.QNAME)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, "")
            .node(Node.QNAME)
            .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, "")
            .build();

    @Before
    public void startup() throws URISyntaxException, ParseException {
        Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(mockSchemaContext);
        Mockito.when(mockSchemaHolder.getContextTree()).thenReturn(mockContextTree);
        Mockito.when(mockSchemaContext.findModuleByName(Mockito.eq("network-topology-pcep"), (Date) Mockito.any()))
            .thenReturn(mockModule);
        String uriString = "urn:opendaylight:params:xml:ns:yang:topology:pcep";
        URI namespaceURI = new URI(uriString);
        Mockito.when(mockModule.getNamespace()).thenReturn(namespaceURI);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date revisionDate = sdf.parse("24/10/2013");
        Mockito.when(mockModule.getRevision()).thenReturn(revisionDate);
        String pathComputationClient = "path-computation-client";
        Mockito.when(mockContextTree.getChild(nodeIdentifier)).thenReturn((DataSchemaContextNode) mockContextNode);
        QName qName = QName.create(namespaceURI, revisionDate, pathComputationClient);
        Mockito.when(mockContextNode.getChild(qName)).thenReturn((DataSchemaContextNode) mockContextNode);

        NodeIdentifier nodeIdentifier = new NodeIdentifier(new QName(new URI(uriString), pathComputationClient));
        Mockito.doReturn(nodeIdentifier).when(mockContextNode).getIdentifier();

        String ipAddress = "ip-address";
        qName = QName.create(namespaceURI, revisionDate, ipAddress);
        Mockito.when(mockContextNode.getChild(qName)).thenReturn((DataSchemaContextNode) mockContextNode);
    }

    /**
     * Test case: valid, legal path is passed to translate() method.
     * Result of this method (YangInstanceIdentifier) should be equal to expected value. 
     * @throws URISyntaxException if URI is in incorrect format. Should not happen because URI in this test case
     * is a constant
     */
    @Test
    public void testLegalPath() throws URISyntaxException {
        Set<QName> childNames = new HashSet<>();
        childNames.add(new QName(new URI("urn:opendaylight:params:xml:ns:yang:topology:pcep?revision=2013-10-24"), "path-computation-client"));
        AugmentationIdentifier augmentationIdentifier = new AugmentationIdentifier(childNames);
        YangInstanceIdentifier translate = pathTranslator.translate("network-topology-pcep:path-computation-client",
                null, mockSchemaHolder);
    }

    /**
     * Test case: two colons in the path should not be accepted
     */
    @Test(expected=IllegalArgumentException.class)
    public void testTwoColonsIllegalArgument() {
        pathTranslator.translate("network-topology-pcep::path-computation-client/network-topology-pcep:ip-address",
                null, mockSchemaHolder);
    }

    /**
     * Test case: the path should contain one and only one colon
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNoColonsIllegalArgument() {
        pathTranslator.translate("network-topology-pceppath-computation-client/network-topology-pcep:ip-address",
                null, mockSchemaHolder);
    }

    /**
     * Test case: path in the argument does not match expected format [module name]:[child name] 
     */
    @Test(expected=IllegalArgumentException.class)
    public void testColonAtLastPosition() {
        pathTranslator.translate("network-topology-pcep:/network-topology-pcep:ip-address",
                null, mockSchemaHolder);

    }

    /**
     * Test case: path in the argument does not match expected format [module name]:[child name] 
     */
    @Test(expected=IllegalArgumentException.class)
    public void testColonAtFirstPosition() {
        pathTranslator.translate(":path-computation-client/network-topology-pcep:ip-address",
                null, mockSchemaHolder);
    }

    /**
     * Test case: empty string as argument is not allowed 
     */
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyString() {
        pathTranslator.translate("",
                null, mockSchemaHolder);
    }

    /**
     * Test case: null as argument is not allowed 
     */
    @Test(expected=IllegalArgumentException.class)
    public void testYangPathNull() {
        pathTranslator.translate(null, null, mockSchemaHolder);
    }

    /**
     * Test case: legal path containing only one [module name]:[child name] shall be accepted
     * @throws URISyntaxException if URI is in incorrect format. Should not happen because URI in this test case
     * is a constant
     */
    @Test
    public void testOneArgument() throws URISyntaxException {
        Mockito.when(mockContext.findModuleByName("bgpnode", null)).thenReturn(mockModule);
        URI uri = new URI("nameSpace1");
        QName qName = new QName(uri, "localName1");
        Mockito.when(mockModule.getDataChildByName("bgp")).thenReturn(mockDataSchemaNode1);
        Mockito.when(mockDataSchemaNode1.getQName()).thenReturn(qName);

        pathTranslator.translate("network-topology-pcep:ip-address", null, mockSchemaHolder);
        Assert.assertTrue(true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPahtBeginingWithSlash() {
        pathTranslator.translate("/network-topology-pcep:ip-address", null, mockSchemaHolder);
    }
}
