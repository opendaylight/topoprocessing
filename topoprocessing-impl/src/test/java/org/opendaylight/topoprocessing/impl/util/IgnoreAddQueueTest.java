/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.AbstractQueue;

import org.junit.Test;

public class IgnoreAddQueueTest {
    @Test
    public void test() {
        AbstractQueue<Integer> queue = new IgnoreAddQueue<>();
        assertEquals(queue.add(0), true);
        assertEquals(queue.offer(0), true);
        assertEquals(queue.size(), 0);
        assertNull(queue.peek());
        assertNull(queue.poll());
        assertNull(queue.iterator().next());
        assertEquals(queue.iterator().hasNext(), false);
    }
}
