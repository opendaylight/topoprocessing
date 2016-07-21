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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class ItemOfClass<T> implements Predicate<InstanceIdentifier.PathArgument> {

    private Class<T> cls;

    public Class<T> getCls() {
        return cls;
    }

    public void setCls(Class<T> cls) {
        this.cls = cls;
    }

    public ItemOfClass<T> cls(Class<T> cls) {
        this.cls = cls;
        return this;
    }

    @Override
    public boolean apply(PathArgument input) {
        return input.getType().equals(cls);
    }
}
