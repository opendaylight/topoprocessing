package org.opendaylight.topoprocessing.impl.operator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.operator.filtrator.AbstractFiltrator;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 *
 * @author martin.dindoffer
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PreAggregationFiltratorTest {

    private static final String TOPOLOGY_ID = "mytopo:1";
    private static final TestNodeCreator TEST_NODE_CREATOR = new TestNodeCreator();
    private static final QName IP_ADDRESS_QNAME = QName.create(Node.QNAME, "ip-address").intern();
    private static final YangInstanceIdentifier PATH_IDENTIFIER = YangInstanceIdentifier.builder()
            .node(IP_ADDRESS_QNAME).build();

    private PreAggregationFiltrator filtrator;
    @Mock private AbstractFiltrator mockFilter;
    @Mock private TopologyAggregator mockAggregator;

    @Mock private TopoStoreProvider tsProviderMock;
    @Mock private TopologyStore topoStoreMock;
    @Mock private ConcurrentMap<YangInstanceIdentifier, UnderlayItem> underlayItemsMock;
    @Mock private UnderlayItem underlayItemMock;

    @Before
    public void setUp() {
        Mockito.when(tsProviderMock.getTopologyStore(TOPOLOGY_ID)).thenReturn(topoStoreMock);
        Mockito.when(topoStoreMock.getUnderlayItems()).thenReturn(underlayItemsMock);
        Mockito.when(mockFilter.getPathIdentifier()).thenReturn(PATH_IDENTIFIER);

        filtrator = new PreAggregationFiltrator(tsProviderMock);
        filtrator.addFilter(mockFilter);
        filtrator.setTopologyAggregator(mockAggregator);
    }

    @Test
    public void testProcessCreatedChanges() {
        Mockito.when(mockFilter.isFiltered((NormalizedNode) Matchers.any())).thenReturn(false);

        Map<Integer, NormalizedNode<?, ?>> leafNodesMap = new HashMap<>();
        String nodeId = "node:1";
        leafNodesMap.put(0, TEST_NODE_CREATOR.createLeafNodeWithIpAddress("192.168.1.1"));
        YangInstanceIdentifier yiid = TEST_NODE_CREATOR.createNodeIdYiid(nodeId);
        UnderlayItem item = new UnderlayItem(null, leafNodesMap, TOPOLOGY_ID, nodeId, CorrelationItemEnum.Node);
        filtrator.processCreatedChanges(yiid, item, TOPOLOGY_ID);
        Mockito.verify(mockAggregator).processCreatedChanges(yiid, item, TOPOLOGY_ID);
    }

    @Test
    public void testProcessUpdatedChanges() {
        Mockito.when(mockFilter.isFiltered((NormalizedNode) Matchers.any())).thenReturn(false);
        Map<Integer, NormalizedNode<?, ?>> leafNodesMap = new HashMap<>();

        // test updating a new node
        String nodeId = "node:1";
        leafNodesMap.put(0, TEST_NODE_CREATOR.createLeafNodeWithIpAddress("192.168.1.4"));
        YangInstanceIdentifier yiid = TEST_NODE_CREATOR.createNodeIdYiid(nodeId);
        UnderlayItem item = new UnderlayItem(null, leafNodesMap, TOPOLOGY_ID, nodeId, CorrelationItemEnum.Node);
        filtrator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);

        Mockito.verify(mockAggregator).processCreatedChanges(yiid, item, TOPOLOGY_ID);

        // test updating a preexisting node
        Mockito.when(underlayItemsMock.get((YangInstanceIdentifier) Matchers.any())).thenReturn(underlayItemMock);
        leafNodesMap.put(0, TEST_NODE_CREATOR.createLeafNodeWithIpAddress("192.168.1.5"));
        yiid = TEST_NODE_CREATOR.createNodeIdYiid(nodeId);
        item = new UnderlayItem(null, leafNodesMap, TOPOLOGY_ID, nodeId, CorrelationItemEnum.Node);
        filtrator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);

        Mockito.verify(mockAggregator).processUpdatedChanges(yiid, item, TOPOLOGY_ID);

        // test updating a preexisting node expecting filtration to occur
        leafNodesMap.put(0, TEST_NODE_CREATOR.createLeafNodeWithIpAddress("192.168.1.5"));
        yiid = TEST_NODE_CREATOR.createNodeIdYiid(nodeId);
        item = new UnderlayItem(null, leafNodesMap, TOPOLOGY_ID, nodeId, CorrelationItemEnum.Node);
        Mockito.when(mockFilter.isFiltered((NormalizedNode) Matchers.any())).thenReturn(true);
        filtrator.processUpdatedChanges(yiid, item, TOPOLOGY_ID);

        Mockito.verify(mockAggregator).processRemovedChanges(yiid, TOPOLOGY_ID);
    }

    @Test
    public void testProcessRemovedChanges() {
        String nodeId = "node:1";
        YangInstanceIdentifier yiid = TEST_NODE_CREATOR.createNodeIdYiid(nodeId);
        filtrator.processRemovedChanges(yiid, TOPOLOGY_ID);
        Mockito.verify(mockAggregator).processRemovedChanges(yiid, TOPOLOGY_ID);
    }

    /**
     * Shouldn't be able to set a TopologyManager
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testSetTopologyManager() {
        TopologyManager tm = Mockito.mock(TopologyManager.class);
        filtrator.setTopologyManager(tm);
    }
}
