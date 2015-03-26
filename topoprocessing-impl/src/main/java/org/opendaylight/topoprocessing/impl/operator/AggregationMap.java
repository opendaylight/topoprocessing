/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.impl.structure.LogicalNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.*;

/**
 * @author matus.marko
 */
public class AggregationMap extends HashMap<YangInstanceIdentifier, LogicalNode> {

    private Map<YangInstanceIdentifier, LogicalNode> createdData = new HashMap<>();
    private Map<YangInstanceIdentifier, LogicalNode> updatedData = new HashMap<>();
    private List<YangInstanceIdentifier> removedData = new ArrayList<>();

    public AggregationMap() {
    }

    public AggregationMap(Map<? extends YangInstanceIdentifier, ? extends LogicalNode> m) {
        super(m);
    }

    /**
     * Mark entry as created
     * @param key      key
     * @param value    value
     */
    public void markCreated(YangInstanceIdentifier key, LogicalNode value) {
        createdData.put(key, value);
    }

    /**
     * Mark entry as updated
     * @param key key
     * @param value value
     */
    public void markUpdated(YangInstanceIdentifier key, LogicalNode value) {
        updatedData.put(key, value);
    }

    /**
     * Mark entry as deleted
     * @param key key
     */
    public void markDeleted(YangInstanceIdentifier key) {
        removedData.add(key);
    }

    @Override
    public LogicalNode put(YangInstanceIdentifier key, LogicalNode value) {
        LogicalNode obj = super.put(key, value);
        if (null == obj) {
            markCreated(key, value);
        } else {
            markUpdated(key, value);
        }
        return obj;
    }

    @Override
    public LogicalNode remove(Object key) {
        markDeleted((YangInstanceIdentifier) key);
        return super.remove(key);
    }

    /**
     * Return map with created data
     * @return map
     */
    public Map<YangInstanceIdentifier, LogicalNode> getCreatedData() {
        Map<YangInstanceIdentifier, LogicalNode> map = new HashMap<>(createdData);
        createdData.clear();
        return map;
    }

    /**
     * Return map with updated data
     * @return map
     */
    public Map<YangInstanceIdentifier, LogicalNode> getUpdatedData() {
        Map<YangInstanceIdentifier, LogicalNode> map = new HashMap<>(updatedData);
        updatedData.clear();
        return map;
    }

    /**
     * Return list with removed data
     * @return list
     */
    public List<YangInstanceIdentifier> getRemovedData() {
        List<YangInstanceIdentifier> list = new ArrayList<>(removedData);
        removedData.clear();
        return list;
    }
}
