/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.Scripting;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.ScriptingBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

/**
 * @author michal.polkorab
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class UnificationCustomScriptTest {

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar");
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip");
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id");
    private static final QName QNAME_LEAF_IP = QName.create(ROOT_QNAME, "ip");

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";

    private TopologyAggregator aggregator;
    private YangInstanceIdentifier leafYiid21, leafYiid22, leafYiid23;
    private CorrelationItemEnum nodeItem = CorrelationItemEnum.Node;

    @Mock
    private NormalizedNode<?,?> mockNormalizedNode1, mockNormalizedNode2;
    @Mock private TopologyManager mockManager;

    /**
     * Sets aggregator up
     */
    @Before
    public void setUp() {
        // initialize and set up topology stores
        aggregator = new UnificationAggregator();
        aggregator.initializeStore(TOPO1, false);
        aggregator.initializeStore(TOPO2, false);
        String script = "if (originalItem.getLeafNode().getValue() === newItem.getLeafNode().getValue()) {"
                + "    aggregable.setResult(true);"
                + "} else {"
                + "    aggregable.setResult(false);"
                + "}";
        ScriptingBuilder scriptingBuilder = new ScriptingBuilder();
        scriptingBuilder.setLanguage("javascript");
        scriptingBuilder.setScript(script);
        Scripting scripting = scriptingBuilder.build();
        aggregator.initCustomAggregation(scripting);
        aggregator.setTopologyManager(mockManager);
    }

    /**
     * Simple test for script aggregation
     * @throws Exception
     */
    @Test
    public void testProcessCreatedChanges() throws Exception {
        // change 1
        TestNodeCreator testNodeCreator = new TestNodeCreator();
        leafYiid21 = testNodeCreator.createNodeIdYiid("21");
        LeafNode<String> leafNode21 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.1");

        leafYiid22 = testNodeCreator.createNodeIdYiid("22");
        LeafNode<String> leafNode22 = ImmutableNodes.leafNode(QNAME_LEAF_IP, "192.168.1.3");
        UnderlayItem UnderlayItem1 = new UnderlayItem(mockNormalizedNode1, leafNode21, TOPO1, "21", nodeItem);
        UnderlayItem UnderlayItem2 = new UnderlayItem(mockNormalizedNode1, leafNode22, TOPO1, "22", nodeItem);
        Map<YangInstanceIdentifier, UnderlayItem> UnderlayItems1 = new HashMap<>();
        UnderlayItems1.put(leafYiid21, UnderlayItem1);
        UnderlayItems1.put(leafYiid22, UnderlayItem2);

        aggregator.processCreatedChanges(UnderlayItems1, TOPO1);

        Assert.assertEquals(2, aggregator.getTopologyStore(TOPO1).getUnderlayItems().size());
        // checks that two nodes have been correctly added into topology TOPO2
        Assert.assertEquals(0, aggregator.getTopologyStore(TOPO2).getUnderlayItems().size());

        // addLogicalNode method has been called twice
        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).updateOverlayItem((OverlayItem) Mockito.any());

        // change 2
        leafYiid23 = YangInstanceIdentifier.builder()
                .nodeWithKey(LIST_IP_QNAME, LEAF_IP_QNAME, "23").build();
        LeafNode<Object> leafNode23 = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QNAME_LEAF_IP))
                .withValue("192.168.1.1").build();
        UnderlayItem UnderlayItem3 = new UnderlayItem(mockNormalizedNode1, leafNode23, TOPO2, "23", nodeItem);
        Map<YangInstanceIdentifier, UnderlayItem> UnderlayItems2 = new HashMap<>();
        UnderlayItems2.put(leafYiid23, UnderlayItem3);

        aggregator.processCreatedChanges(UnderlayItems2, TOPO2);

        Assert.assertEquals(2, aggregator.getTopologyStore(TOPO1).getUnderlayItems().size());
        Assert.assertEquals(1, aggregator.getTopologyStore(TOPO2).getUnderlayItems().size());

        Mockito.verify(mockManager, Mockito.times(2)).addOverlayItem((OverlayItem) Mockito.any());
        Mockito.verify(mockManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Mockito.any());
        // updateLogicalNode method has been called once
        Mockito.verify(mockManager, Mockito.times(1)).updateOverlayItem((OverlayItem) Mockito.any());
    }

    /**
     * Throws an exception when ScriptEngine was not found
     */
    @Test(expected=NullPointerException.class)
    public void testIncorrectCreation() {
        aggregator = new UnificationAggregator();
        String script = "println(\"hello\")";
        ScriptingBuilder scriptingBuilder = new ScriptingBuilder();
        scriptingBuilder.setLanguage("abcTest");
        scriptingBuilder.setScript(script);
        Scripting scripting = scriptingBuilder.build();
        aggregator.initCustomAggregation(scripting);
    }
}
