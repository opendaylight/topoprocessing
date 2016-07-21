/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import java.util.AbstractQueue;
import java.util.Iterator;

/**
 * @author andrej.zan
 *
 */
public class IgnoreAddQueue<E> extends AbstractQueue<E> {

    /**
     * Ignore adding of element but pretends to adding was successful.
     * @param e added object
     * @return always true
     */
    @Override
    public boolean add(Object e) {
        return true;
    }

    @Override
    public boolean offer(E arg0) {
        return true;
    }

    /**
     * Always return null as if Queue is empty.
     */
    @Override
    public E peek() {
        return null;
    }

    /**
     * Always return null as if Queue is empty.
     */
    @Override
    public E poll() {
        return null;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public E next() {
                return null;
            }

            @Override
            public void remove() {
                return;
            }
        };
    }

    @Override
    public int size() {
        return 0;
    }

}
