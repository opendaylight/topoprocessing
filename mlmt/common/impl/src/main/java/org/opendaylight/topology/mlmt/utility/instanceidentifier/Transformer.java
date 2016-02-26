/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility.instanceidentifier;

/**
 * A transformer is an object that transforms something in something else.
 * In particular the transform method is invoked with an object of type TItem,
 * and change it in an object of type TResult.
 *
 * @author guest
 *
 * @param <TItem> the type of the input object to be transformed
 * @param <TResult> the type of the output object, the result of the transformation
 */
public interface Transformer<TItem, TResult> {
    /**
     * Method that transform an object of type TItem in an object of type TResult.
     *
     * @param item the item top be transformed
     * @return the result of the transformation
     */
    TResult transform(TItem item);
}
