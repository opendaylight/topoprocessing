/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.util;

import java.util.PriorityQueue;

/**
 * @author andrej.zan
 *
 */
public class IgnoreAddQueue<E> extends PriorityQueue<E> {

    /**
     *
     */
    private static final long serialVersionUID = -7786956237270525070L;

    @Override
    public boolean add(Object e) {
        return true;
    }

}
