/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author matus.marko
 */
public class AggregationMap extends HashMap {

    private List createdData = new ArrayList();
    private List updatedData = new ArrayList();
    private List deletedData = new ArrayList();

    /**
     * Mark entry as created
     * @param key
     */
    public void markCreated(Object key) {
        this.createdData.add(key);
    }

    /**
     * Mark entry as updated
     * @param key
     */
    public void markUpdated(Object key) {
        this.updatedData.add(key);
    }

    /**
     * Mark entry as deleted
     * @param key
     */
    public void markDeleted(Object key) {
        this.deletedData.add(key);
    }

    @Override
    public Object put(Object key, Object value) {
        Object obj = super.put(key, value);
        if (null == obj) {
            this.markCreated(key);
        } else {
            this.markUpdated(key);
        }
        return obj;
    }

    @Override
    public Object remove(Object key) {
        this.markDeleted(key);
        return super.remove(key);
    }

    /**
     * Return map with created data
     * @return
     */
    public HashMap getCreatedData() {
        HashMap map = new HashMap();
        for (Object key : this.createdData) {
            map.put(key, this.get(key));
        }
        this.createdData.clear();
        return map;
    }

    /**
     * Return map with updated data
     * @return
     */
    public HashMap getUpdatedData() {
        HashMap map = new HashMap();
        for (Object key : this.updatedData) {
            map.put(key, this.get(key));
        }
        this.updatedData.clear();
        return map;
    }

    /**
     * Return list with removed data
     * @return
     */
    public List getRemovedData() {
        return this.deletedData;
    }
}
