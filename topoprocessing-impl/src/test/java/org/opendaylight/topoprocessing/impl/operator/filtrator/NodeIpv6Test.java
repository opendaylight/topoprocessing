package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.topoprocessing.impl.structure.PhysicalNode;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @author matus.marko
 */
public class NodeIpv6Test {

    private static final String TOPOLOGY_ID = "mytopo:1";
    private static final String NODE_ID = "mynode:1";

    private static final QName ROOT_QNAME = Node.QNAME;
    private static final QName IP_QNAME = QName.create(ROOT_QNAME, "ip-address");
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(IP_QNAME).build();
    private TestNodeCreator creator = new TestNodeCreator();

    @Test
    public void test1() {
        YangInstanceIdentifier pathIdentifier;
        IpPrefix ipPrefix = new IpPrefix(Ipv6Prefix.getDefaultInstance("0123:4567:89ab:cdef:0:0:0:0/64"));
        NodeIpv6 nodeIpv6 = new NodeIpv6(ipPrefix, path);

        boolean filtered1 = nodeIpv6.isFiltered(
                new PhysicalNode(creator.createMapEntryNodeWithIpAddress(NODE_ID,
                        "0123:4567:89ab:cdef:0:0:0:0"), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv6.isFiltered(
                new PhysicalNode(creator.createMapEntryNodeWithIpAddress(NODE_ID,
                        "0123:4567:89ab:cdef:0123:4567:89ab:cdef"), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertFalse("Node should pass the filtrator", filtered2);

        boolean filtered3 = nodeIpv6.isFiltered(
                new PhysicalNode(creator.createMapEntryNodeWithIpAddress(NODE_ID,
                        "0123:4567:89ab:cdef:ffff:ffff:ffff:ffff"), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertFalse("Node should pass the filtrator", filtered3);

        boolean filtered4 = nodeIpv6.isFiltered(
                new PhysicalNode(creator.createMapEntryNodeWithIpAddress(NODE_ID,
                        "0123:4567:89ac:cdef:0123:4567:89ab:cdef"), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertTrue("Node should not pass the filtrator", filtered4);

        boolean filtered5 = nodeIpv6.isFiltered(
                new PhysicalNode(creator.createMapEntryNodeWithIpAddress(NODE_ID,
                        "0123:4568:89ab:cdef:0123:4567:89ab:cdef"), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertTrue("Node should not pass the filtrator", filtered5);

        boolean filtered6 = nodeIpv6.isFiltered(
                new PhysicalNode(creator.createMapEntryNodeWithIpAddress(NODE_ID,
                        "0124:4567:89ab:cdef:0123:4567:89ab:cdef"), null, TOPOLOGY_ID, NODE_ID));
        Assert.assertTrue("Node should not pass the filtrator", filtered6);
    }
}