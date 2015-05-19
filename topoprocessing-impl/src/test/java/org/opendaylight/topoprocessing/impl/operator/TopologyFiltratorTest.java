package org.opendaylight.topoprocessing.impl.operator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.Map;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyFiltratorTest extends TestNodeCreator {

    private static final String TOPOLOGY_NAME = "mytopo:1";

    private TopologyFiltrator filtrator;
    @Mock private NodeIpFiltrator mockFiltrator;
    @Mock private TopologyManager mockTopologyManager;

    @Before
    public void setUp() throws Exception {
        filtrator = new TopologyFiltrator();
        filtrator.initializeStore(TOPOLOGY_NAME);
        filtrator.addFilter(mockFiltrator);
        filtrator.setTopologyManager(mockTopologyManager);
    }

    @Test
    public void testProcessChanges() {
        // create 3 nodes
        Mockito.when(mockFiltrator.isFiltered((PhysicalNode) Matchers.any())).thenReturn(false);
        filtrator.processCreatedChanges(this.createEntryThreeNodesSameTopology(), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(3)).addLogicalNode((LogicalNode) Matchers.any());

        // update node which previously didn't exist
        Map<YangInstanceIdentifier, PhysicalNode> update1 = this.createEntry(TOPOLOGY_NAME, "node:4", "192.168.1.4");
        filtrator.processUpdatedChanges(update1, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(4)).addLogicalNode((LogicalNode) Matchers.any());

        // update existing node
        Map<YangInstanceIdentifier, PhysicalNode> update2 = this.createEntry(TOPOLOGY_NAME, "node:4", "192.168.1.14");
        filtrator.processUpdatedChanges(update2, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager).updateLogicalNode((LogicalNode) Matchers.any());

        // update existing node - should be filtered out
        Mockito.when(mockFiltrator.isFiltered((PhysicalNode) Matchers.any())).thenReturn(true);
        Map<YangInstanceIdentifier, PhysicalNode> update3 = this.createEntry(TOPOLOGY_NAME, "node:4", "10.0.0.14");
        filtrator.processUpdatedChanges(update3, TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(1)).removeLogicalNode((LogicalNode) Matchers.any());

        // remove 3 existing nodes
        filtrator.processRemovedChanges(this.createYiidThreeNodesSameTopology(), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(4)).removeLogicalNode((LogicalNode) Matchers.any());

        // create node - missing ip-address value
        Mockito.reset(mockTopologyManager);
        Mockito.when(mockFiltrator.isFiltered((PhysicalNode) Matchers.any())).thenReturn(false);
        filtrator.processCreatedChanges(this.createEntry(TOPOLOGY_NAME, "node:6"), TOPOLOGY_NAME);
        Mockito.verify(mockTopologyManager, Mockito.times(0)).addLogicalNode((LogicalNode) Matchers.any());
        Mockito.verify(mockTopologyManager, Mockito.times(0)).removeLogicalNode((LogicalNode) Matchers.any());
        Mockito.verify(mockTopologyManager, Mockito.times(0)).updateLogicalNode((LogicalNode) Matchers.any());
    }
}
