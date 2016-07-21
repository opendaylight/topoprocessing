/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.translator;

import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @author martin.uhlir
 *
 */
public interface LinkTranslator {

    /**
     * Converts OverlayItemWrapper object containing links to datastore link object.
     * @param wrapper OverlayItemWrapper object containing link OverlayItems
     * @return {@link Link} in datastore format
     */
    public NormalizedNode<?, ?> translate(OverlayItemWrapper wrapper);

}
