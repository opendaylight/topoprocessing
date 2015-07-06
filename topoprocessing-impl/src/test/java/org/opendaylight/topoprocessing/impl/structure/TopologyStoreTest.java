/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
/**
 * @author martin.uhlir
 *
 */
public class TopologyStoreTest {

    private static final String TOPOLOGY_ID = "pcep-topology:1";
    private static final String NODE_ID = "pcep:1";
    private TopologyStore topologyStore;

    @Mock private NormalizedNode<?,?> mockNormalizedNode1;

    @Test
    public void testInitialization() {
        boolean aggregateInside = true;
        topologyStore = new TopologyStore(TOPOLOGY_ID , aggregateInside ,
                new HashMap<YangInstanceIdentifier, UnderlayItem>());
    }

    @Test
    public void testIsAggregateInside() {
        testInitialization();
        Assert.assertEquals(true, topologyStore.isAggregateInside());
    }

    @Test
    public void testGetId() {
        testInitialization();
        Assert.assertEquals(TOPOLOGY_ID, topologyStore.getId());
    }

    @Test
    public void testGetPhysicalNodes() {
        testInitialization();
        HashMap<YangInstanceIdentifier, UnderlayItem> physicalNodes = new HashMap<>();
        UnderlayItem physicalNode = new UnderlayItem(mockNormalizedNode1, null, TOPOLOGY_ID, NODE_ID, CorrelationItemEnum.Node);
        physicalNodes.put(createTopologyIdentifier(TOPOLOGY_ID).build(), physicalNode);

        physicalNodesEqual(physicalNodes, topologyStore.getUnderlayItems());
    }

    /**
     * @param physicalNodes
     * @param physicalNodes2
     * @return
     */
    private static boolean physicalNodesEqual(Map<YangInstanceIdentifier, UnderlayItem> expectedPhysicalNodes,
            Map<YangInstanceIdentifier, UnderlayItem> actualPhysicalNodes) {
        Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> expectedIterator = expectedPhysicalNodes.entrySet().iterator();
        Iterator<Entry<YangInstanceIdentifier, UnderlayItem>> actualIterator = actualPhysicalNodes.entrySet().iterator();
        if (expectedIterator.hasNext() && actualIterator.hasNext()) {
            Entry<YangInstanceIdentifier, UnderlayItem> expected = expectedIterator.next();
            Entry<YangInstanceIdentifier, UnderlayItem> actual = actualIterator.next();
            if (expected.getKey().equals(actual.getKey()) &&
                    expected.getValue().getItem().equals(actual.getValue().getItem()) &&
                    expected.getValue().getLeafNode().equals(actual.getValue().getLeafNode()) &&
                    expected.getValue().getTopologyId().equals(actual.getValue().getTopologyId()) &&
                    expected.getValue().getItemId().equals(actual.getValue().getItemId())) {
                return true;
            }
        }
        return false;
    }

    private static YangInstanceIdentifier.InstanceIdentifierBuilder createTopologyIdentifier(
            String underlayTopologyId) {
        InstanceIdentifierBuilder identifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, underlayTopologyId);
        return identifier;
    }
}
