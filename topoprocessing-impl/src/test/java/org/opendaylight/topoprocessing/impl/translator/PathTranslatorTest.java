/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import java.io.IOException;
import java.io.InputStream;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.io.ByteSource;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PathTranslatorTest {

    private PathTranslator pathTranslator = new PathTranslator();
    private SchemaContext schemaContext;

    @Mock private GlobalSchemaContextHolder mockSchemaHolder;

    private Class<? extends Model> NTmodel = NetworkTopologyModel.class;

    private static SchemaContext createTestContext() throws IOException, YangSyntaxErrorException {
        final YangParserImpl parser = new YangParserImpl();
        List<String> modules = new ArrayList<String>();
        modules.add("/ietf-inet-types.yang");
        modules.add("/network-topology.yang");
        modules.add("/network-topology-pcep.yang");
        return parser.parseSources(Collections2.transform(modules, new Function<String, ByteSource>() {
            @Override
            public ByteSource apply(final String input) {
                return new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return PathTranslatorTest.class.getResourceAsStream(input);
                    }
                };
            }
        }));
      }


    @Before
    public void startup() throws URISyntaxException, ParseException, IOException, YangSyntaxErrorException {
        schemaContext = createTestContext();
        DataSchemaContextTree contextTree = DataSchemaContextTree.from(schemaContext);
        Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(schemaContext);
        Mockito.when(mockSchemaHolder.getContextTree()).thenReturn(contextTree);
    }

    /**
     * Test case: valid, legal path is passed to translate() method.
     * Result of this method (YangInstanceIdentifier) should be equal to expected value.
     * @throws URISyntaxException if URI is in incorrect format. Should not happen because URI in this test case
     * is a constant
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
        YangInstanceIdentifier expectedIdentifier =
                YangInstanceIdentifier.builder().node(augmentationIdentifier).node(qName).build();
        Assert.assertEquals("Incorrect valid YangInstanceIdentifier",
                expectedIdentifier.toString(), translate.toString());
    }

    /**
     * Test case: two colons in the path should not be accepted
     */
    @Test(expected=IllegalArgumentException.class)
    public void testTwoColonsIllegalArgument() {
        pathTranslator.translate("network-topology-pcep::path-computation-client/network-topology-pcep:ip-address",
                CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: the path should contain one and only one colon in one path argument
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNoColonsIllegalArgument() {
        pathTranslator.translate("network-topology-pceppath-computation-client/network-topology-pcep:ip-address",
                CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: path in the argument does not match expected format [module name]:[child name]
     */
    @Test(expected=IllegalArgumentException.class)
    public void testColonAtLastPosition() {
        pathTranslator.translate("network-topology-pcep:/network-topology-pcep:ip-address",
                CorrelationItemEnum.Node, mockSchemaHolder,NTmodel);

    }

    /**
     * Test case: path in the argument does not match expected format [module name]:[child name]
     */
    @Test(expected=IllegalArgumentException.class)
    public void testColonAtFirstPosition() {
        pathTranslator.translate(":path-computation-client/network-topology-pcep:ip-address",
                CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: empty string as argument is not allowed
     */
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyString() {
        pathTranslator.translate("", CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: null as argument is not allowed
     */
    @Test(expected=IllegalArgumentException.class)
    public void testYangPathNull() {
        pathTranslator.translate(null, CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPathBeginingWithSlash() {
        pathTranslator.translate("/network-topology-pcep:ip-address",
                CorrelationItemEnum.Node, mockSchemaHolder, NTmodel);
    }

    /**
     * Test case: module does not exist
     *
     * @throws URISyntaxException URISyntaxException
     * @throws ParseException ParseException
     * @throws IOException IOException
     * @throws YangSyntaxErrorException YangSyntaxErrorException
     */
    @Test(expected=IllegalStateException.class)
    public void testUnexistingModuleName()
            throws URISyntaxException, ParseException, IOException, YangSyntaxErrorException {
        final YangParserImpl parser = new YangParserImpl();
        List<String> modules = new ArrayList<String>();
        modules.add("/ietf-inet-types.yang");
        modules.add("/network-topology.yang");
        SchemaContext schemaContext2 = parser.parseSources(Collections2.transform(modules,
                new Function<String, ByteSource>() {
            @Override
            public ByteSource apply(final String input) {
                return new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return PathTranslatorTest.class.getResourceAsStream(input);
                    }
                };
            }
        }));
        Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(schemaContext2);
        testLegalPath();
    }
}
