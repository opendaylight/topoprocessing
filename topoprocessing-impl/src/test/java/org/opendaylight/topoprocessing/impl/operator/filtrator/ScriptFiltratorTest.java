/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.Scripting;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.ScriptingBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author michal.polkorab
 *
 */
public class ScriptFiltratorTest {

    private static final QName ROOT_QNAME = Node.QNAME;
    private static final QName IP_QNAME = QName.create(ROOT_QNAME, "ip-address").intern();
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(IP_QNAME).build();

    private TestNodeCreator creator = new TestNodeCreator();
    private ScriptFiltrator filtrator;

    /**
     * Test correct script filtration - exact match on ipv4 address
     */
    @Test
    public void test() {
        String script = "var value = node.getValue();"
                + "if (value == \"192.168.1.1\") {"
                + "    filterOut.setResult(false);"
                + "} else {"
                + "    filterOut.setResult(true);"
                + "}";
        ScriptingBuilder scriptingBuilder = new ScriptingBuilder();
        scriptingBuilder.setLanguage("javascript");
        scriptingBuilder.setScript(script);
        Scripting scripting = scriptingBuilder.build();
        filtrator = new ScriptFiltrator(scripting, path);

        NormalizedNode node = creator.createLeafNodeWithIpAddress("192.168.1.1");
        Assert.assertFalse("Node should pass the filtrator", filtrator.isFiltered(node));
        node = creator.createLeafNodeWithIpAddress("192.168.1.2");
        Assert.assertTrue("Node should not pass the filtrator", filtrator.isFiltered(node));
        node = creator.createLeafNodeWithIpAddress("");
        Assert.assertTrue("Node should not pass the filtrator", filtrator.isFiltered(node));
    }

    /**
     * Test incorrect {@link ScriptFiltrator} creation - null scripting configuration
     */
    @Test(expected=NullPointerException.class)
    public void testCreationWithNullScripting() {
        filtrator = new ScriptFiltrator(null, path);
    }

    /**
     * Test incorrect {@link ScriptFiltrator} creation - null path identifier
     */
    @Test(expected=NullPointerException.class)
    public void testCreationWithNullPathIdentifier() {
        String script = "println(\"hello\");";
        ScriptingBuilder scriptingBuilder = new ScriptingBuilder();
        scriptingBuilder.setLanguage("javascript");
        scriptingBuilder.setScript(script);
        Scripting scripting = scriptingBuilder.build();
        filtrator = new ScriptFiltrator(scripting, null);
    }

    /**
     * Test incorrect {@link ScriptFiltrator} creation - engine not found
     */
    @Test(expected=NullPointerException.class)
    public void testCreationUnknownEngine() {
        String script = "println(\"hello\");";
        ScriptingBuilder scriptingBuilder = new ScriptingBuilder();
        scriptingBuilder.setLanguage("abcTest");
        scriptingBuilder.setScript(script);
        Scripting scripting = scriptingBuilder.build();
        filtrator = new ScriptFiltrator(scripting, null);
    }
}
