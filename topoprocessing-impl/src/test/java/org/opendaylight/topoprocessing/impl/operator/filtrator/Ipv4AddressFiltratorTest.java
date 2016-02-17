package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.topoprocessing.impl.testUtilities.TestNodeCreator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * @author matus.marko
 */
public class Ipv4AddressFiltratorTest {

    private static final String NODE_ID = "mynode:1";

    private static final QName ROOT_QNAME = Node.QNAME;
    private static final QName IP_QNAME = QName.create(ROOT_QNAME, "ip-address");
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(IP_QNAME).build();

    private TestNodeCreator creator = new TestNodeCreator();

    @Test
    public void testMask32() {
        IpPrefix ipPrefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("192.168.1.1/32"));
        Ipv4AddressFiltrator nodeIpv4Filtrator = new Ipv4AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.1.1"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.1.2"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);

        boolean filtered3 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.2.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered3);

        boolean filtered4 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.169.1.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered4);

        boolean filtered5 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("193.168.1.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered5);
    }

    @Test
    public void testMask24() {
        IpPrefix ipPrefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("192.168.1.0/24"));
        Ipv4AddressFiltrator nodeIpv4Filtrator = new Ipv4AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.1.2"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.2.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);

        boolean filtered3 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.169.1.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered3);

        boolean filtered4 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("193.168.1.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered4);
    }

    @Test
    public void testMask16() {
        IpPrefix ipPrefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("192.168.0.0/16"));
        Ipv4AddressFiltrator nodeIpv4Filtrator = new Ipv4AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.1.2"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.2.1"));
        Assert.assertFalse("Node should pass the filtrator", filtered2);

        boolean filtered3 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.169.1.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered3);

        boolean filtered4 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("193.168.1.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered4);
    }

    @Test
    public void testMask8() {
        IpPrefix ipPrefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("192.0.0.0/8"));
        Ipv4AddressFiltrator nodeIpv4Filtrator = new Ipv4AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.1.2"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.2.1"));
        Assert.assertFalse("Node should pass the filtrator", filtered2);

        boolean filtered3 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.169.1.1"));
        Assert.assertFalse("Node should pass the filtrator", filtered3);

        boolean filtered4 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("193.168.1.1"));
        Assert.assertTrue("Node should not pass the filtrator", filtered4);
    }

    @Test
    public void testMask0() {
        IpPrefix ipPrefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("0.0.0.0/0"));
        Ipv4AddressFiltrator nodeIpv4Filtrator = new Ipv4AddressFiltrator(ipPrefix, path);

        boolean filtered1 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.1.2"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.168.2.1"));
        Assert.assertFalse("Node should pass the filtrator", filtered2);

        boolean filtered3 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("192.169.1.1"));
        Assert.assertFalse("Node should pass the filtrator", filtered3);

        boolean filtered4 = nodeIpv4Filtrator.isFiltered(this.createLeafNode("193.168.1.1"));
        Assert.assertFalse("Node should pass the filtrator", filtered4);
    }

    @Test
    public void testMissingIp() {
        IpPrefix ipPrefix = new IpPrefix(Ipv4Prefix.getDefaultInstance("192.168.1.0/24"));
        Ipv4AddressFiltrator nodeIpv4Filtrator = new Ipv4AddressFiltrator(ipPrefix, path);

        boolean filtered = nodeIpv4Filtrator.isFiltered(ImmutableNodes.leafNode(ROOT_QNAME, ""));
        Assert.assertTrue("Node sholud not pass the filtrator", filtered);
    }

    protected NormalizedNode createLeafNode(String ipAddress) {
        return creator.createLeafNodeWithIpAddress(ipAddress);
    }
}
