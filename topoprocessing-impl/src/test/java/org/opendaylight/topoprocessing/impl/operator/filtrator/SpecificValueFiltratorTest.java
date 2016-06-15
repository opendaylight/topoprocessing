package org.opendaylight.topoprocessing.impl.operator.filtrator;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author matus.marko
 */
public class SpecificValueFiltratorTest {

    private static final QName NUMBER_QNAME = QName.create(Node.QNAME, "number").intern();
    private static final QName STRING_QNAME = QName.create(Node.QNAME, "string").intern();
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(STRING_QNAME).build();
    private final YangInstanceIdentifier numberPath = YangInstanceIdentifier.builder().node(NUMBER_QNAME).build();

    /**
     * Tests filtering with integer values
     */
    @Test
    public void testSpecificNumberInteger() {
        SpecificValueFiltrator<Integer> filtrator = new SpecificValueFiltrator<>(5600, numberPath);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 5600));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 1600));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    /**
     * Tests filtering with short values
     */
    @Test
    public void testSpecificNumberShort() {
        SpecificValueFiltrator<Short> filtrator = new SpecificValueFiltrator<>((short) 47, numberPath);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 47));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 5));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    /**
     * Tests filtering with long values
     */
    @Test
    public void testSpecificNumberLong() {
        SpecificValueFiltrator<Long> filtrator = new SpecificValueFiltrator<>((long) 875040, numberPath);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 875040));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 955000));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    /**
     * Tests filtering with float values
     */
    @Test
    public void testSpecificNumberFloat() {
        SpecificValueFiltrator<Float> filtrator = new SpecificValueFiltrator<>((float) 46.54, numberPath);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, (float) 46.54));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, (float) 10.10));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    /**
     * Tests filtering with double values
     */
    @Test
    public void testSpecificNumberDouble() {
        SpecificValueFiltrator<Double> filtrator = new SpecificValueFiltrator<>((double) 46.54, numberPath);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 46.54));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 10.10));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }

    @Test
    public void testNotSupportedType() {
        SpecificValueFiltrator<String> filtrator = new SpecificValueFiltrator<>("abc", numberPath);
        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, new Object()));
        Assert.assertTrue("Node should not pass the filtrator", filtered1);
    }

    /**
     * Tests filtering with string values
     */
    @Test
    public void testSpecificString() {
        SpecificValueFiltrator<String> filtrator = new SpecificValueFiltrator<>("fooBar", path);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(STRING_QNAME, "fooBar"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(STRING_QNAME, "lopata"));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
    }
}
