/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility.instanceidentifier;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

import com.google.common.base.Predicate;

public class InstanceIdentifiers {

    @SuppressWarnings("unchecked")
    public static <T extends DataObject> InstanceIdentifier<T> copy(InstanceIdentifier<T> iid) {
        List<PathArgument> ps = new LinkedList<>();
        for (PathArgument parg : iid.getPathArguments()) {
            ps.add(parg);
        }

        return (InstanceIdentifier<T>) InstanceIdentifier.<T>create(ps);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataObject> InstanceIdentifier<T> transform(
            InstanceIdentifier<T> iid, Transformer<InstanceIdentifier.PathArgument,
                    InstanceIdentifier.PathArgument> transformer) {
        if ( transformer == null ) {
            return null;
        }
        List<PathArgument> ps = new LinkedList<>();

        for (PathArgument parg : iid.getPathArguments()) {
            PathArgument newParg =transformer.transform(parg);
            if (newParg != null) {
                ps.add(newParg);
            }
        }

        return (InstanceIdentifier<T>) InstanceIdentifier.<T>create(ps);
    }

    public static <T extends DataObject> Collection<InstanceIdentifier.PathArgument>
            find(InstanceIdentifier<T> iid, Predicate<InstanceIdentifier.PathArgument> predicate) {
        if (predicate == null) {
            return null;
        }
        List<PathArgument> ps = new LinkedList<>();

        for (PathArgument parg : iid.getPathArguments()) {
            if (predicate.apply(parg)) {
                ps.add(parg);
            }
        }

        return ps;
    }

    public static <T extends DataObject> InstanceIdentifier.PathArgument
            findFirst(InstanceIdentifier<T> iid, Predicate<InstanceIdentifier.PathArgument> predicate) {
        if (predicate == null) {
            return null;
        }

        for (PathArgument parg : iid.getPathArguments()) {
            if (predicate.apply(parg)) {
                return parg;
            }
        }

        return null;
    }

    public static <T extends DataObject> InstanceIdentifier.PathArgument
            findLast(InstanceIdentifier<T> iid, Predicate<InstanceIdentifier.PathArgument> predicate) {
        if (predicate == null) {
            return null;
        }

        PathArgument result = null;
        for (PathArgument parg : iid.getPathArguments()) {
            if (predicate.apply(parg)) {
                result = parg;
            }
        }

        return result;
    }
}
