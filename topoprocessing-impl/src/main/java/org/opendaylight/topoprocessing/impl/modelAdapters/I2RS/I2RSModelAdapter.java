/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.modelAdapters.I2RS;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.modelAdapters.ModelAdapter;
import org.opendaylight.topoprocessing.impl.request.TopologyRequestListener;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;

/** *
 * @author matej.perina
 */

public class I2RSModelAdapter implements ModelAdapter {

    @Override
    public UnderlayTopologyListener createUnderlayTopologyListener(DOMDataBroker domDataBroker,
            String underlayTopologyId, CorrelationItemEnum correlationItem) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TopologyRequestListener createTopologyRequestListener(DOMDataBroker dataBroker,
            BindingNormalizedNodeSerializer nodeSerializer, GlobalSchemaContextHolder schemaHolder,
            RpcServices rpcServices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OverlayItemTranslator createOverlayItemTranslator() {
        // TODO Auto-generated method stub
        return null;
    }

}
