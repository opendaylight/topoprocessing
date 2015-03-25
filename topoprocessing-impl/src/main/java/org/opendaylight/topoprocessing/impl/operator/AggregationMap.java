/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import java.util.*;

/**
 * @author matus.marko
 */
public class AggregationMap<K, V> extends HashMap<K, V> {

    private Map<K, V> createdData = new HashMap<>();
    private Map<K, V> updatedData = new HashMap<>();
    private List<Object> removedData = new ArrayList<>();

    /**
     * Mark entry as created
     * @param key      key
     * @param value    value
     */
    public void markCreated(K key, V value) {
        createdData.put(key, value);
    }

    /**
     * Mark entry as updated
     * @param key key
     * @param value value
     */
    public void markUpdated(K key, V value) {
        updatedData.put(key, value);
    }

    /**
     * Mark entry as deleted
     * @param key key
     */
    public void markDeleted(Object key) {
        removedData.add(key);
    }

    @Override
    public V put(K key, V value) {
        V obj = super.put(key, value);
        if (null == obj) {
            markCreated(key, value);
        } else {
            markUpdated(key, value);
        }
        return obj;
    }

    @Override
    public V remove(Object key) {
        markDeleted(key);
        return super.remove(key);
    }

    /**
     * Return map with created data
     * @return map
     */
    public Map<K, V> getCreatedData() {
        Map<K, V> map = Collections.unmodifiableMap(createdData);
        createdData.clear();
        return map;
    }

    /**
     * Return map with updated data
     * @return map
     */
    public Map<K, V> getUpdatedData() {
        Map<K, V> map = Collections.unmodifiableMap(updatedData);
        updatedData.clear();
        return map;
    }

    /**
     * Return list with removed data
     * @return list
     */
    public List<Object> getRemovedData() {
        List<Object> list = Collections.unmodifiableList(removedData);
        removedData.clear();
        return list;
    }
}
