/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.topoprocessing.impl.structure.TopologyStore;

/**
 * @author martin.uhlir
 *
 */
public class TopoStoreProviderTest {

    private static final String TOPOLOGY1_ID = "topo1";
    private static final String TOPOLOGY2_ID = "topo2";
    private static final String EMPTY_STRING = "";
    private TopoStoreProviderImpl provider;

    private class TopoStoreProviderImpl extends TopoStoreProvider {
        public TopoStoreProviderImpl() {
        }
    }

    @Before
    public void setUp() {
         provider =  new TopoStoreProviderImpl();
    }

    @Test
    public void testInitializeStore1() {
        provider.initializeStore(TOPOLOGY1_ID, false);
        Assert.assertEquals(1, provider.getTopologyStores().size());
    }

    @Test
    public void testInitializeStoreTwice() {
        provider.initializeStore(TOPOLOGY1_ID, false);
        provider.initializeStore(TOPOLOGY1_ID, false);
        Assert.assertEquals(1, provider.getTopologyStores().size());
    }

    @Test
    public void testInitializeTwoStores() {
        provider.initializeStore(TOPOLOGY1_ID, false);
        provider.initializeStore(TOPOLOGY2_ID, false);
        Assert.assertEquals(2, provider.getTopologyStores().size());
        Assert.assertEquals(TOPOLOGY1_ID, provider.getTopologyStore(TOPOLOGY1_ID).getId());
        Assert.assertEquals(TOPOLOGY2_ID, provider.getTopologyStore(TOPOLOGY2_ID).getId());
    }

    @Test
    public void testInitializeTwoStoresWithAggregateInsideTrue() {
        provider.initializeStore(TOPOLOGY1_ID, true);
        provider.initializeStore(TOPOLOGY2_ID, true);
        Assert.assertEquals(2, provider.getTopologyStores().size());
        Assert.assertEquals(TOPOLOGY1_ID, provider.getTopologyStore(TOPOLOGY1_ID).getId());
        Assert.assertEquals(TOPOLOGY2_ID, provider.getTopologyStore(TOPOLOGY2_ID).getId());
        Assert.assertEquals(true, provider.getTopologyStore(TOPOLOGY1_ID).isAggregateInside());
        Assert.assertEquals(true, provider.getTopologyStore(TOPOLOGY2_ID).isAggregateInside());
    }

    @Test(expected=IllegalStateException.class)
    public void testInitializeStoreWithNull() {
        provider.initializeStore(null, true);
        Assert.assertEquals(0, provider.getTopologyStores().size());
    }

    @Test (expected=IllegalStateException.class)
    public void testInitializeStoreWithEmptyString() {
        provider.initializeStore(EMPTY_STRING, true);
        Assert.assertEquals(0, provider.getTopologyStores().size());
    }

    @Test
    public void testGetTopologyStore1() {
        provider.initializeStore(TOPOLOGY1_ID, false);
        TopologyStore topologyStore1 = provider.getTopologyStore(TOPOLOGY1_ID);
        Assert.assertEquals(TOPOLOGY1_ID, topologyStore1.getId());
    }

    @Test
    public void testGetUnexistingTopologyStore() {
        provider.initializeStore(TOPOLOGY1_ID, false);
        TopologyStore topologyStore2 = provider.getTopologyStore(TOPOLOGY2_ID);
        Assert.assertEquals(null, topologyStore2);
    }
}
