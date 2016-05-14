package org.opendaylight.topoprocessing.i2rs.adapter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;

public class I2RSModelAdapterTest {
    private I2RSModelAdapter adapter = new I2RSModelAdapter();

    @Test
    public void testCreateItemIdentifier() {
        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        YangInstanceIdentifier nodeIdentifier = adapter.buildItemIdentifier(builder, CorrelationItemEnum.Node);
        builder = YangInstanceIdentifier.builder();
        YangInstanceIdentifier tpIdentifier =
                adapter.buildItemIdentifier(builder, CorrelationItemEnum.TerminationPoint);
        assertEquals(nodeIdentifier, tpIdentifier);
        builder = YangInstanceIdentifier.builder();
        YangInstanceIdentifier linkIdentifier = adapter.buildItemIdentifier(builder, CorrelationItemEnum.Link);
        builder = YangInstanceIdentifier.builder();
        assertEquals(builder.node(Link.QNAME).build(), linkIdentifier);
    }
}
