package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.filtrator.Filtrator;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyFiltratorTest {

    private static final String TOPOLOGY_NAME = "mytopo:1";

    private TestNodeCreator creator = new TestNodeCreator();
    private TopologyFiltrator filtrator;
    @Mock private Filtrator mockFiltrator;
    @Mock private TopologyManager mockTopologyManager;

    private Map<YangInstanceIdentifier, UnderlayItem> threeNodesSameTopologyNodeEntry =
            new HashMap<YangInstanceIdentifier, UnderlayItem>() {{
                String nodeId = "node:1";
                put(creator.createNodeIdYiid(nodeId), new UnderlayItem(creator.createMapEntryNodeWithIpAddress(
                        nodeId, "192.168.1.1"), null, TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node));
                nodeId = "node:2";
                put(creator.createNodeIdYiid(nodeId), new UnderlayItem(creator.createMapEntryNodeWithIpAddress(
                        nodeId, "192.168.1.2"), null, TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node));
                nodeId = "node:3";
                put(creator.createNodeIdYiid(nodeId), new UnderlayItem(creator.createMapEntryNodeWithIpAddress(
                        nodeId, "192.168.1.3"), null, TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node));
            }};

    protected List<YangInstanceIdentifier> threeNodesSameTopologyNodeYiid = new ArrayList<YangInstanceIdentifier>() {{
        add(creator.createNodeIdYiid("node:1"));
        add(creator.createNodeIdYiid("node:2"));
        add(creator.createNodeIdYiid("node:3"));
    }};

    @Before
    public void setUp() {
        filtrator = new TopologyFiltrator();
        filtrator.initializeStore(TOPOLOGY_NAME, false);
        filtrator.addFilter(mockFiltrator);
        filtrator.setTopologyManager(mockTopologyManager);
    }

    @Test
    public void testProcessChanges() {
        String nodeId;
        Map<YangInstanceIdentifier, UnderlayItem> map = new HashMap<>();

        // create 3 nodes
        Mockito.when(mockFiltrator.isFiltered((UnderlayItem) Matchers.any())).thenReturn(false);
        filtrator.processCreatedChanges(threeNodesSameTopologyNodeEntry, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(3)).addOverlayItem((OverlayItem) Matchers.any());

        // update node which didn't exist before
        nodeId = "node:4";
        map.put(creator.createNodeIdYiid(nodeId), new UnderlayItem(
                creator.createMapEntryNodeWithIpAddress(nodeId, "192.168.1.4"), null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node));
        filtrator.processUpdatedChanges(map, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(4)).addOverlayItem((OverlayItem) Matchers.any());

        // update existing node
        nodeId = "node:4";
        map.clear();
        map.put(creator.createNodeIdYiid(nodeId), new UnderlayItem(
                creator.createMapEntryNodeWithIpAddress(nodeId, "192.168.1.14"), null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node));
        filtrator.processUpdatedChanges(map, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager).updateOverlayItem((OverlayItem) Matchers.any());

        // update existing node - should be filtered out
        nodeId = "node:4";
        map.clear();
        map.put(creator.createNodeIdYiid(nodeId), new UnderlayItem(
                creator.createMapEntryNodeWithIpAddress(nodeId, "192.168.1.14"), null, TOPOLOGY_NAME, nodeId,
                CorrelationItemEnum.Node));
        Mockito.when(mockFiltrator.isFiltered((UnderlayItem) Matchers.any())).thenReturn(true);
        filtrator.processUpdatedChanges(map, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(1)).removeOverlayItem((OverlayItem) Matchers.any());

        // remove 3 existing nodes
        filtrator.processRemovedChanges(threeNodesSameTopologyNodeYiid, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(4)).removeOverlayItem((OverlayItem) Matchers.any());
    }
}
