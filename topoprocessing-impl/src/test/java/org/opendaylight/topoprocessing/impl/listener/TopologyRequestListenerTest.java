/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author martin.uhlir
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestListenerTest {

    @Mock DOMDataBroker dataBroker;

    MockDataChangedEvent dataChangedEvent = new MockDataChangedEvent();

    /**
     * Test that checks that Data Broker set to null is not accepted
     */
    @Test(expected=NullPointerException.class)
    public void testDataBrokerNull() {
        dataBroker = null;
        TopologyRequestListener topoRequestListener = new TopologyRequestListener(dataBroker);
    }

    /**
     * Test that empty change is valid input
     */
    @Test
    public void testDataChangeEmpty() {
        TopologyRequestListener topoRequestListener = new TopologyRequestListener(dataBroker);
        topoRequestListener.onDataChanged(dataChangedEvent);
    }

    static class MockDataChangedEvent implements AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> {

        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> created = new HashMap<>();
        Set<YangInstanceIdentifier> removed = new HashSet<>();

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
            return created;
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
            throw new UnsupportedOperationException("Not implemented by mock");
        }

        @Override
        public Set<YangInstanceIdentifier> getRemovedPaths() {
            return removed;
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getOriginalData() {
            throw new UnsupportedOperationException("Not implemented by mock");
        }

        @Override
        public NormalizedNode<?, ?> getOriginalSubtree() {
            throw new UnsupportedOperationException("Not implemented by mock");
        }

        @Override
        public NormalizedNode<?, ?> getUpdatedSubtree() {
            throw new UnsupportedOperationException("Not implemented by mock");
        }
    }
}
