/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility.instanceidentifier;

import com.google.common.base.Predicate;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class ItemWithKey<T,K> implements Predicate<InstanceIdentifier.PathArgument> {

    private Class<T> cls;
    private K key;

    public Class<T> getCls() {
        return cls;
    }

    public void setCls(Class<T> cls) {
        this.cls = cls;
    }

    public ItemWithKey<T, K> cls(Class<T> cls) {
        this.cls = cls;
        return this;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public ItemWithKey<T, K> key(K key) {
        this.key = key;
        return this;
    }

    @Override
    public boolean apply(PathArgument input) {
        if (cls != null) {
            if (!cls.equals(input.getType())) {
                return false;
            }
        }
        if (key != null) {
            if (input instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {
                InstanceIdentifier.IdentifiableItem<?, ?> idItem = (IdentifiableItem<?, ?>) input;
                if (key.equals(idItem.getKey())) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }
}
