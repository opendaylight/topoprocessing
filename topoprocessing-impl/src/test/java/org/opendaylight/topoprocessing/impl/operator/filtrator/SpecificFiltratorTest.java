package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author matus.marko
 */
public class SpecificFiltratorTest {

    private static final String TOPOLOGY_ID = "mytopo:1";
    private static final String NODE_ID = "mynode:1";

    private static final QName ROOT_QNAME = Node.QNAME;
    private static final QName NUMBER_QNAME = QName.create(ROOT_QNAME, "number");
    private static final QName STRING_QNAME = QName.create(ROOT_QNAME, "string");
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(STRING_QNAME).build();
    private final YangInstanceIdentifier numberPath = YangInstanceIdentifier.builder().node(NUMBER_QNAME).build();

    /**
     * Tests filtering with integer values
     */
    @Test
    public void testSpecificNumber() {
        SpecificValueFiltrator<Integer> filtrator = new SpecificValueFiltrator<>(56, numberPath);

        boolean filtered1 = filtrator.isFiltered(new PhysicalNode(
                ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID).withChild(
                        ImmutableNodes.leafNode(NUMBER_QNAME, 56)).build(), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(new PhysicalNode(
                ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID).withChild(
                        ImmutableNodes.leafNode(NUMBER_QNAME, 16)).build(), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    /**
     * Tests filtering with string values
     */
    @Test
    public void testSpecificString() {
        SpecificValueFiltrator<String> filtrator = new SpecificValueFiltrator<>("fooBar", path);

        boolean filtered1 = filtrator.isFiltered(new PhysicalNode(
                ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID).withChild(
                        ImmutableNodes.leafNode(STRING_QNAME, "fooBar")).build(), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(new PhysicalNode(
                ImmutableNodes.mapEntryBuilder(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID).withChild(
                        ImmutableNodes.leafNode(STRING_QNAME, "lopata")).build(), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }
}
