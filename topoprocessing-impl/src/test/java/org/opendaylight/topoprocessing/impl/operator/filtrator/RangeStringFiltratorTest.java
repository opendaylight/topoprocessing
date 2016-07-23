/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
public class RangeStringFiltratorTest {

    private static final QName ROOT_QNAME = Node.QNAME;
    private static final QName STRING_QNAME = QName.create(ROOT_QNAME, "string").intern();
    private final YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(STRING_QNAME).build();

    @Test
    public void test() {
        RangeStringFiltrator filtrator = new RangeStringFiltrator("cccc", "hhhh", path);

        boolean filtered1 = filtrator.isFiltered(ImmutableNodes.leafNode(STRING_QNAME, "cdef"));
        Assert.assertFalse("Node should pass the filtrator", filtered1);

        boolean filtered2 = filtrator.isFiltered(ImmutableNodes.leafNode(STRING_QNAME, "cccc"));
        Assert.assertFalse("Node should pass the filtrator", filtered2);

        boolean filtered3 = filtrator.isFiltered(ImmutableNodes.leafNode(STRING_QNAME, "hhhh"));
        Assert.assertFalse("Node should pass the filtrator", filtered3);

        boolean filtered4 = filtrator.isFiltered(ImmutableNodes.leafNode(STRING_QNAME, "aaaa"));
        Assert.assertTrue("Node should not pass the filtrator", filtered4);

        boolean filtered5 = filtrator.isFiltered(ImmutableNodes.leafNode(STRING_QNAME, "kkkk"));
        Assert.assertTrue("Node should not pass the filtrator", filtered5);
    }
}
