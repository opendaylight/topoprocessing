package org.opendaylight.topoprocessing.impl.operator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.*;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class AggregationMapTest {

    private static final int YIIDS_COUNT = 6;
    private AggregationMap aggregationMap;
    private List<YangInstanceIdentifier> yiids = new ArrayList<>();

    @Mock
    LogicalNode pn0, pn1, pn2, pn3, pn4, pn5;

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < YIIDS_COUNT; i++) {
            YangInstanceIdentifier yiid = YangInstanceIdentifier.builder()
                    .node(NetworkTopology.QNAME)
                    .node(Topology.QNAME)
                    .nodeWithKey(Topology.QNAME, QName.create("topology-id"), "openflow:" + i)
                    .node(Node.QNAME)
                    .build();
            yiids.add(yiid);
        }

        Map<YangInstanceIdentifier, LogicalNode> originalMap = new HashMap<>();
        originalMap.put(yiids.get(0), pn0);
        originalMap.put(yiids.get(1), pn1);
        originalMap.put(yiids.get(2), pn2);

        aggregationMap = new AggregationMap(originalMap);
    }

    @Test
    public void testCreated() throws Exception {
        aggregationMap.put(yiids.get(3), pn3);
        aggregationMap.put(yiids.get(4), pn4);
        aggregationMap.put(yiids.get(5), pn5);

        Map data1 = aggregationMap.getCreatedData();
        Assert.assertEquals(3, data1.size());
        Map data2 = aggregationMap.getCreatedData();
        Assert.assertEquals(0, data2.size());
    }

    @Test
    public void testUpdated() throws Exception {
        aggregationMap.put(yiids.get(0), pn3);
        aggregationMap.put(yiids.get(1), pn4);
        aggregationMap.put(yiids.get(2), pn5);

        Map data1 = aggregationMap.getUpdatedData();
        Assert.assertEquals(3, data1.size());
        Map data2 = aggregationMap.getUpdatedData();
        Assert.assertEquals(0, data2.size());
    }

    @Test
    public void testDeleted() throws Exception {
        aggregationMap.remove(yiids.get(2));
        aggregationMap.remove(yiids.get(1));
        aggregationMap.remove(yiids.get(0));

        Set data1 = aggregationMap.getRemovedData();
        Assert.assertEquals(3, data1.size());
        Set data2 = aggregationMap.getRemovedData();
        Assert.assertEquals(0, data2.size());
    }
}
