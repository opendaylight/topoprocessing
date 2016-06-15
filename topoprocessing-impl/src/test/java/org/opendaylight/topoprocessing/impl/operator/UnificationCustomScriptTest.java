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

    private static final QName ROOT_QNAME = QName.create("foo", "2014-03-13", "bar").intern();
    private static final QName LIST_IP_QNAME = QName.create(ROOT_QNAME, "ip").intern();
    private static final QName LEAF_IP_QNAME = QName.create(ROOT_QNAME, "ip-id").intern();
    private static final QName QNAME_LEAF_IP = QName.create(ROOT_QNAME, "ip").intern();

    private static final String TOPO1 = "topo1";
    private static final String TOPO2 = "topo2";

    private TopologyAggregator aggregator;
    private YangInstanceIdentifier leafYiid21, leafYiid22, leafYiid23;
    private CorrelationItemEnum nodeItem = CorrelationItemEnum.Node;
    private TopoStoreProvider topoStoreProvider;

    @Mock
    private NormalizedNode<?,?> mockNormalizedNode1, mockNormalizedNode2;
    @Mock private TopologyManager mockManager;

    /**
     * Sets aggregator up
     */
    @Before
    public void setUp() {
        // initialize and set up topology stores
        topoStoreProvider = new TopoStoreProvider();
        aggregator = new UnificationAggregator(topoStoreProvider);
        topoStoreProvider.initializeStore(TOPO1, false);
        topoStoreProvider.initializeStore(TOPO2, false);
        String script = "if (originalItem.getLeafNodes().get(java.lang.Integer.valueOf('0')).getValue() ==="
                + "        newItem.getLeafNodes().get(java.lang.Integer.valueOf('0')).getValue()) {"
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
        Map<Integer, NormalizedNode<?, ?>> targetFields1 = new HashMap<>(1);
        targetFields1.put(0, leafNode21);
        UnderlayItem UnderlayItem1 = new UnderlayItem(mockNormalizedNode1, targetFields1, TOPO1, "21", nodeItem);
        Map<Integer, NormalizedNode<?, ?>> targetFields2 = new HashMap<>(1);
        targetFields2.put(0, leafNode22);
        UnderlayItem UnderlayItem2 = new UnderlayItem(mockNormalizedNode1, targetFields2, TOPO1, "22", nodeItem);
        aggregator.processCreatedChanges(leafYiid21, UnderlayItem1, TOPO1);
        aggregator.processCreatedChanges(leafYiid22, UnderlayItem2, TOPO1);

        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        // checks that two nodes have been correctly added into topology TOPO2
        Assert.assertEquals(0, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());

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
        Map<Integer, NormalizedNode<?, ?>> targetFields3 = new HashMap<>(1);
        targetFields3.put(0, leafNode23);
        UnderlayItem underlayItem3 = new UnderlayItem(mockNormalizedNode1, targetFields3, TOPO2, "23", nodeItem);
        aggregator.processCreatedChanges(leafYiid23, underlayItem3, TOPO2);

        Assert.assertEquals(2, aggregator.getTopoStoreProvider().getTopologyStore(TOPO1).getUnderlayItems().size());
        Assert.assertEquals(1, aggregator.getTopoStoreProvider().getTopologyStore(TOPO2).getUnderlayItems().size());

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
        aggregator = new UnificationAggregator(topoStoreProvider);
        String script = "println(\"hello\")";
        ScriptingBuilder scriptingBuilder = new ScriptingBuilder();
        scriptingBuilder.setLanguage("abcTest");
        scriptingBuilder.setScript(script);
        Scripting scripting = scriptingBuilder.build();
        aggregator.initCustomAggregation(scripting);
    }
}
