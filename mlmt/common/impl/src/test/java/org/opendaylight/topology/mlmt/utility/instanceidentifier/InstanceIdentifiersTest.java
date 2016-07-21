/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility.instanceidentifier;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class InstanceIdentifiersTest {

    private InstanceIdentifier<Topology> topoIid;

    @Before
    public void setUp() {
        topoIid = InstanceIdentifier.create(NetworkTopology.class).child(
                Topology.class, new TopologyKey(new TopologyId("mlmt:1")));
    }
    @Test
    public void testCopy() {
        InstanceIdentifier<Topology> copyOfTopoIid = InstanceIdentifiers.copy(topoIid);
        Assert.assertEquals(topoIid, copyOfTopoIid);
    }

    @Test
    public void testTransform() {
        InstanceIdentifier<Topology> transformedIid1 =
                InstanceIdentifiers.transform(topoIid, new Identity());
        Assert.assertEquals(topoIid, transformedIid1);

        InstanceIdentifier<Topology> transformedIid2 = InstanceIdentifiers.transform(
                topoIid, new MlmtTransformer("mlmt:2"));
        Assert.assertNotEquals(topoIid, transformedIid2);
        KeyedInstanceIdentifier<Topology, TopologyKey> topoMlmt2Iid =
                InstanceIdentifier.create(NetworkTopology.class).child(
                        Topology.class, new TopologyKey(new TopologyId("mlmt:2")));
        Assert.assertNotNull(transformedIid2.getPathArguments().iterator().next());
        Assert.assertEquals(topoMlmt2Iid, transformedIid2);
    }

    @Test
    public void testFind() {
        Collection<PathArgument> result = InstanceIdentifiers.find(
                topoIid, new ItemOfClass<Topology>().cls(Topology.class));
        Assert.assertEquals(1, result.size());
        PathArgument parg = result.iterator().next();
        if (parg instanceof InstanceIdentifier.IdentifiableItem<?,?>) {
            InstanceIdentifier.IdentifiableItem<?, ?> i = (IdentifiableItem<?, ?>) parg;
            Assert.assertTrue(i.getKey() instanceof TopologyKey);
            TopologyKey topologyKey = (TopologyKey)i.getKey();
            Assert.assertEquals("mlmt:1", topologyKey.getTopologyId().getValue());
        }
        result = InstanceIdentifiers.find(topoIid, new ItemWithKey<Topology, TopologyKey>()
                .cls(Topology.class).key(new TopologyKey(new TopologyId("mlmt:1"))));
        Assert.assertEquals(1, result.size());
        parg = result.iterator().next();
        if (parg instanceof InstanceIdentifier.IdentifiableItem<?,?>) {
            InstanceIdentifier.IdentifiableItem<?, ?> i = (IdentifiableItem<?, ?>) parg;
            Assert.assertTrue(i.getKey() instanceof TopologyKey);
            TopologyKey topologyKey = (TopologyKey)i.getKey();
            Assert.assertEquals("mlmt:1", topologyKey.getTopologyId().getValue());
        }
    }

    @Test
    public void testFindFirst() {
        PathArgument parg = InstanceIdentifiers.findFirst(topoIid,
                new ItemOfClass<Topology>().cls(Topology.class));
        if (parg instanceof InstanceIdentifier.IdentifiableItem<?,?>) {
            InstanceIdentifier.IdentifiableItem<?, ?> i = (IdentifiableItem<?, ?>) parg;
            Assert.assertTrue(i.getKey() instanceof TopologyKey);
            TopologyKey topologyKey = (TopologyKey)i.getKey();
            Assert.assertEquals("mlmt:1", topologyKey.getTopologyId().getValue());
        }
    }

    @Test
    public void testFindLast() {
        PathArgument parg = InstanceIdentifiers.findLast(topoIid,
                new ItemOfClass<Topology>().cls(Topology.class));
        if (parg instanceof InstanceIdentifier.IdentifiableItem<?,?>) {
            InstanceIdentifier.IdentifiableItem<?, ?> i = (IdentifiableItem<?, ?>) parg;
            Assert.assertTrue(i.getKey() instanceof TopologyKey);
            TopologyKey topologyKey = (TopologyKey)i.getKey();
            Assert.assertEquals("mlmt:1", topologyKey.getTopologyId().getValue());
        }
    }
}
