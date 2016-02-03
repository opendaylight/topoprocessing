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

    private static final QName NUMBER_QNAME = QName.create(Node.QNAME, "number");
    private static final QName STRING_QNAME = QName.create(Node.QNAME, "string");
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(STRING_QNAME).build();
    private final YangInstanceIdentifier numberPath = YangInstanceIdentifier.builder().node(NUMBER_QNAME).build();

    /**
     * Tests filtering with integer values
     */
    @Test
    public void testSpecificNumber() {
        SpecificValueFiltrator<Integer> filtrator = new SpecificValueFiltrator<>(56, numberPath);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 56));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(NUMBER_QNAME, 16));
        Assert.assertTrue("Node should not pass the filtrator", filtered2);
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
