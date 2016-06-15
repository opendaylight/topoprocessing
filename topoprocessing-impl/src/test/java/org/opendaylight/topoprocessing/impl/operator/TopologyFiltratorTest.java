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
import org.opendaylight.topoprocessing.impl.operator.filtrator.AbstractFiltrator;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyFiltratorTest {

    private static final String TOPOLOGY_NAME = "mytopo:1";
    private static final QName UNNUMBERED_QNAME =
        QName.create(TerminationPoint.QNAME, "unnumbered").intern();


    private TestNodeCreator creator = new TestNodeCreator();
    private TopologyFiltrator filtrator;
    @Mock private AbstractFiltrator mockFiltrator;
    @Mock private TopologyManager mockTopologyManager;
    private YangInstanceIdentifier pathIdentifier = YangInstanceIdentifier.builder().node(UNNUMBERED_QNAME).build();


    protected List<YangInstanceIdentifier> threeNodesSameTopologyNodeYiid = new ArrayList<YangInstanceIdentifier>() {{
        add(creator.createNodeIdYiid("node:1"));
        add(creator.createNodeIdYiid("node:2"));
        add(creator.createNodeIdYiid("node:3"));
    }};

    @Before
    public void setUp() {
        TopoStoreProvider topoStoreProvider = new TopoStoreProvider();
        filtrator = new TopologyFiltrator(topoStoreProvider);
        topoStoreProvider.initializeStore(TOPOLOGY_NAME, false);
        filtrator.addFilter(mockFiltrator);
        filtrator.setTopologyManager(mockTopologyManager);
    }

    @Test
    public void testProcessChanges() {
        Map<Integer, NormalizedNode<?,?>> map = new HashMap<>();

        // create 3 nodes
        Mockito.when(mockFiltrator.isFiltered((NormalizedNode) Matchers.any())).thenReturn(false);
        Mockito.when(mockFiltrator.getPathIdentifier()).thenReturn(pathIdentifier);


        String nodeId = "node:1";
        map.put(0, creator.createLeafNodeWithIpAddress("192.168.1.1"));
        filtrator.processCreatedChanges(creator.createNodeIdYiid(nodeId), new UnderlayItem(null, map,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);
        nodeId = "node:2";
        map.put(0, creator.createLeafNodeWithIpAddress("192.168.1.2"));
        filtrator.processCreatedChanges(creator.createNodeIdYiid(nodeId), new UnderlayItem(null, map,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);
        nodeId = "node:3";
        map.put(0, creator.createLeafNodeWithIpAddress("192.168.1.3"));
        filtrator.processCreatedChanges(creator.createNodeIdYiid(nodeId), new UnderlayItem(null, map,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);

        Mockito.verify(mockTopologyManager, Mockito.times(3)).addOverlayItem((OverlayItem) Matchers.any());

        // update node which didn't exist before
        nodeId = "node:4";
        map.put(0, creator.createLeafNodeWithIpAddress("192.168.1.4"));
        filtrator.processUpdatedChanges(creator.createNodeIdYiid(nodeId), new UnderlayItem(null, map,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(4)).addOverlayItem((OverlayItem) Matchers.any());

        // update existing node
        nodeId = "node:4";
        map.put(0, creator.createLeafNodeWithIpAddress("192.168.1.14"));
        filtrator.processUpdatedChanges(creator.createNodeIdYiid(nodeId), new UnderlayItem(null, map,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager).updateOverlayItem((OverlayItem) Matchers.any());

        // update existing node - should be filtered out
        nodeId = "node:4";
        map.put(0, creator.createLeafNodeWithIpAddress("192.168.1.14"));
        filtrator.processUpdatedChanges(creator.createNodeIdYiid(nodeId), new UnderlayItem(null, map,
                TOPOLOGY_NAME, nodeId, CorrelationItemEnum.Node), TOPOLOGY_NAME);
        Mockito.when(mockFiltrator.isFiltered((NormalizedNode) Matchers.any())).thenReturn(true);
//        Mockito.verify(mockTopologyManager, Mockito.times(1)).removeOverlayItem((OverlayItem) Matchers.any());
        Mockito.verify(mockTopologyManager, Mockito.times(0)).removeOverlayItem((OverlayItem) Matchers.any());

        // remove 3 existing nodes
        filtrator.processRemovedChanges(creator.createNodeIdYiid("node:1"), TOPOLOGY_NAME);
        filtrator.processRemovedChanges(creator.createNodeIdYiid("node:2"), TOPOLOGY_NAME);
        filtrator.processRemovedChanges(creator.createNodeIdYiid("node:3"), TOPOLOGY_NAME);

//        Mockito.verify(mockTopologyManager, Mockito.times(4)).removeOverlayItem((OverlayItem) Matchers.any());
        Mockito.verify(mockTopologyManager, Mockito.times(3)).removeOverlayItem((OverlayItem) Matchers.any());
    }
}
