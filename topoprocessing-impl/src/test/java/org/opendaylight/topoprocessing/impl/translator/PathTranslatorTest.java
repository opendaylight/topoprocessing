/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PathTranslatorTest {

    private PathTranslator pathTranslator = new PathTranslator();
    private SchemaContext schemaContext;

    @Mock
    private GlobalSchemaContextHolder mockSchemaHolder;

    private Class<? extends Model> NTmodel = NetworkTopologyModel.class;
    private Class<? extends Model> I2RSmodel = I2rsModel.class;
    private Class<? extends Model> INVmodel = OpendaylightInventoryModel.class;

    private static SchemaContext createTestContext() throws IOException, YangSyntaxErrorException,
    ReactorException {
        List<String> modules = new ArrayList<String>();
        modules.add("/ietf-inet-types.yang");
        modules.add("/network-topology.yang");
        modules.add("/l3-unicast-igp-topology.yang");
        modules.add("/network-topology-pcep.yang");
        modules.add("/ietf-network.yang");
        modules.add("/i2rs-topology.yang");
        modules.add("/yang-ext.yang");
        modules.add("/opendaylight-inventory.yang");
        modules.add("/opendaylight-l2-types.yang");
        modules.add("/ietf-yang-types.yang");
        modules.add("/opendaylight-topology.yang");
        modules.add("/opendaylight-meter-types.yang");
        modules.add("/opendaylight-queue-types.yang");
        modules.add("/opendaylight-port-types.yang");
        modules.add("/opendaylight-match-types.yang");
        modules.add("/opendaylight-action-types.yang");
        modules.add("/opendaylight-group-types.yang");
        modules.add("/opendaylight-flow-types.yang");
        modules.add("/opendaylight-table-types.yang");
        modules.add("/flow-node-inventory.yang");
        return parseYangSources(modules);
    }

    @Before
    public void startup() throws URISyntaxException, ParseException, IOException, YangSyntaxErrorException,
    ReactorException {
        schemaContext = createTestContext();
        DataSchemaContextTree contextTree = DataSchemaContextTree.from(schemaContext);
        Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(schemaContext);
        Mockito.when(mockSchemaHolder.getContextTree()).thenReturn(contextTree);
    }

    /**
     * Test case: valid, legal path is passed to translate() method. Result of
     * this method (YangInstanceIdentifier) should be equal to expected value.
     *
     * @throws URISyntaxException
     *             if URI is in incorrect format. Should not happen because URI
     *             in this test case is a constant
     * @throws ParseException ParseException
     */
    @Test
    public void testLegalPath() throws URISyntaxException, ParseException {
        Set<QName> childNames = new HashSet<>();
        QName qName = new QName(new URI("urn:opendaylight:params:xml:ns:yang:topology:pcep?revision=2013-10-24"),
                "path-computation-client");
        childNames.add(qName);
        AugmentationIdentifier augmentationIdentifier = new AugmentationIdentifier(childNames);
        YangInstanceIdentifier translate = pathTranslator.translate("network-topology-pcep:path-computation-client",
                CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
        YangInstanceIdentifier expectedIdentifier = YangInstanceIdentifier.builder().node(augmentationIdentifier)
                .node(qName).build();
        System.out.println(expectedIdentifier.toString());
        Assert.assertEquals("Incorrect valid YangInstanceIdentifier", expectedIdentifier.toString(),
                translate.toString());
    }

    @Test
    public void testLegalPathNtTp() throws URISyntaxException, ParseException {
        Set<QName> childNames = new HashSet<>();
        QName qName = new QName(new URI("urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology?revision=2013-07-12"),
                "igp-termination-point-attributes");
        childNames.add(qName);
        AugmentationIdentifier augmentationIdentifier = new AugmentationIdentifier(childNames);
        YangInstanceIdentifier translate =
                pathTranslator.translate("l3-unicast-igp-topology:igp-termination-point-attributes",
                CorrelationItemEnum.TerminationPoint, mockSchemaHolder, NTmodel);
        YangInstanceIdentifier expectedIdentifier = YangInstanceIdentifier.builder().node(augmentationIdentifier)
                .node(qName).build();
        System.out.println(expectedIdentifier.toString());
        Assert.assertEquals("Incorrect valid YangInstanceIdentifier", expectedIdentifier.toString(),
                translate.toString());
    }

    @Test
    public void testLegalPathInventoryTp() throws URISyntaxException, ParseException {
        Set<QName> childNames = new HashSet<>();
        QName qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "port-number");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "hardware-address");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "configuration");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "advertised-features");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "name");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "queue");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "current-feature");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "supported");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "peer-features");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "current-speed");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "maximum-speed");
        childNames.add(qName);

        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "state");
        childNames.add(qName);
        AugmentationIdentifier augmentationIdentifier = new AugmentationIdentifier(childNames);
        YangInstanceIdentifier translate = pathTranslator.translate("flow-node-inventory:state",
                CorrelationItemEnum.TerminationPoint, mockSchemaHolder, INVmodel);
        YangInstanceIdentifier expectedIdentifier = YangInstanceIdentifier.builder().node(augmentationIdentifier)
                .node(qName).build();

        String[] expectedSplit = expectedIdentifier.getPathArguments().get(0).toString().split(",");
        String[] translatedSplit = translate.getPathArguments().get(0).toString().split(",");
        Set<String> expectedNames = new HashSet<>();
        Set<String> translatedNames = new HashSet<>();
        for (int i = 0; i < expectedSplit.length; i++) {
            expectedNames.add(expectedSplit[i].substring(expectedSplit[i].indexOf(')')).replaceAll("[^a-z]", ""));
            translatedNames.add(translatedSplit[i].substring(translatedSplit[i].indexOf(')')).replaceAll("[^a-z]", ""));
        }
        Assert.assertEquals(expectedNames, translatedNames);
    }

    @Test
    public void testLegalPathInventory() throws URISyntaxException, ParseException {
        Set<QName> childNames = new HashSet<>();
        QName qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "manufacturer");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "hardware");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "supported-actions");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "software");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "serial-number");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "description");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "table");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "meter");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "group");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "supported-match-types");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "supported-instructions");
        childNames.add(qName);
        qName = new QName(new URI("urn:opendaylight:flow:inventory?revision=2013-08-19"), "switch-features");
        childNames.add(qName);
        AugmentationIdentifier augmentationIdentifier = new AugmentationIdentifier(childNames);
        YangInstanceIdentifier translate = pathTranslator.translate("flow-node-inventory:switch-features",
                CorrelationItemEnum.Node, mockSchemaHolder, INVmodel);
        YangInstanceIdentifier expectedIdentifier = YangInstanceIdentifier.builder().node(augmentationIdentifier)
                .node(qName).build();

        String[] expectedSplit = expectedIdentifier.getPathArguments().get(0).toString().split(",");
        String[] translatedSplit = translate.getPathArguments().get(0).toString().split(",");
        Set<String> expectedNames = new HashSet<>();
        Set<String> translatedNames = new HashSet<>();
        for (int i = 0; i < expectedSplit.length; i++) {
            expectedNames.add(expectedSplit[i].substring(expectedSplit[i].indexOf(')')).replaceAll("[^a-z]", ""));
            translatedNames.add(translatedSplit[i].substring(translatedSplit[i].indexOf(')')).replaceAll("[^a-z]", ""));
        }
        Assert.assertEquals(expectedNames, translatedNames);
    }

    /**
     * Test case: two colons in the path should not be accepted
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTwoColonsIllegalArgument() {
        pathTranslator.translate("network-topology-pcep::path-computation-client/network-topology-pcep:ip-address",
                CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: the path should contain one and only one colon in one path
     * argument
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNoColonsIllegalArgument() {
        pathTranslator.translate("network-topology-pceppath-computation-client/network-topology-pcep:ip-address",
                CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: path in the argument does not match expected format [module
     * name]:[child name]
     */
    @Test(expected = IllegalArgumentException.class)
    public void testColonAtLastPosition() {
        pathTranslator.translate("network-topology-pcep:/network-topology-pcep:ip-address", CorrelationItemEnum.Node,
                mockSchemaHolder, NTmodel);

    }

    /**
     * Test case: path in the argument does not match expected format [module
     * name]:[child name]
     */
    @Test(expected = IllegalArgumentException.class)
    public void testColonAtFirstPosition() {
        pathTranslator.translate(":path-computation-client/network-topology-pcep:ip-address", CorrelationItemEnum.Node,
                mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: empty string as argument is not allowed
     */
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyString() {
        pathTranslator.translate("", CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: null as argument is not allowed
     */
    @Test(expected = IllegalArgumentException.class)
    public void testYangPathNull() {
        pathTranslator.translate(null, CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPathBeginingWithSlash() {
        pathTranslator.translate("/network-topology-pcep:ip-address", CorrelationItemEnum.Node, mockSchemaHolder,
                NTmodel);
    }

    /**
     * Test case: module does not exist
     *
     * @throws URISyntaxException URISyntaxException
     * @throws ParseException ParseException
     * @throws IOException IOException
     * @throws YangSyntaxErrorException YangSyntaxErrorException
     * @throws ReactorException ReactorException
     */
    @Test(expected = IllegalStateException.class)
    public void testUnexistingModuleName()
            throws URISyntaxException, ParseException, IOException, YangSyntaxErrorException, ReactorException {

        List<String> modules = new ArrayList<String>();
        modules.add("/ietf-inet-types.yang");
        modules.add("/network-topology.yang");

        SchemaContext schemaContext2 = parseYangSources(modules);
        Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(schemaContext2);
        testLegalPath();
    }

    public static SchemaContext parseYangSources(List<String> input) throws SourceException, ReactorException,
    FileNotFoundException{

        StatementStreamSource[] sources = new StatementStreamSource[input.size()];

        for(int i = 0; i < input.size(); i++){
            sources[i] = new YangStatementSourceImpl(PathTranslatorTest.class.getResourceAsStream(input.get(i)));
        }

        CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        reactor.addSources(sources);

        return reactor.buildEffective();
    }
}
