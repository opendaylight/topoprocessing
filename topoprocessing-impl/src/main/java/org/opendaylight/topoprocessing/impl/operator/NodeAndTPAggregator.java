/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;

import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author samuel.kontris
 *
 */
public class NodeAndTPAggregator implements TopologyOperator {

    private static final Logger LOG = LoggerFactory.getLogger(NodeAndTPAggregator.class);
    private TopologyOperator nodeOperator;
    private TopologyOperator tpOperator;
    private TopologyManager manager;
    private Class<? extends Model> inputModel;

    private OverlayItemWrapper wrapper;
    private String topologyId;
    private YangInstanceIdentifier itemIdentifier;

    private Manager nodeManager = new Manager() {

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            nodeCreatedOverlayItem(newOverlayItem);
        }

        @Override
        public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
            // TODO Auto-generated method stub

        }
    };

    private Manager tpManager = new Manager() {

        @Override
        public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
        }

        @Override
        public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
        }

        @Override
        public void addOverlayItem(OverlayItem newOverlayItem) {
            // TODO Auto-generated method stub

        }
    };


    public NodeAndTPAggregator(TopologyOperator nodeOperator, TopologyOperator tpOperator, Class<? extends Model> inputModel) {
        this.nodeOperator = nodeOperator;
        this.tpOperator = tpOperator;
        nodeOperator.setTopologyManager(nodeManager);
        tpOperator.setTopologyManager(tpManager);
        this.inputModel = inputModel;
    }

    @Override
    public void processCreatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem createdItem,
                    String topologyId) {
        this.itemIdentifier = itemIdentifier;
        this.topologyId = topologyId;
        nodeOperator.processCreatedChanges(itemIdentifier, createdItem, topologyId);
    }

    @Override
    public void processUpdatedChanges(YangInstanceIdentifier itemIdentifier, UnderlayItem updatedItem,
                    String topologyId) {
        this.itemIdentifier = itemIdentifier;
        this.topologyId = topologyId;
        nodeOperator.processUpdatedChanges(itemIdentifier, updatedItem, topologyId);
    }

    @Override
    public void processRemovedChanges(YangInstanceIdentifier itemIdentifier, String topologyId) {
        this.itemIdentifier = itemIdentifier;
        this.topologyId = topologyId;
        nodeOperator.processRemovedChanges(itemIdentifier, topologyId);
    }

    @Override
    public void setTopologyManager(Manager manager) {
        if(manager instanceof TopologyManager) {
            this.manager = (TopologyManager) manager;
        } else {
            LOG.warn("Received manager should be instance of " + TopologyManager.class);
        }
    }

    private UnderlayItem createUnderlayNodeWithAllTPs(OverlayItemWrapper wrapper) {
        //UnderlayItem underlayItem = new UnderlayItem(item, leafNodes, topologyId, "itemID", CorrelationItemEnum.Node);
        return null;
    }

    private void nodeCreatedOverlayItem(OverlayItem newOverlayItem) {
        wrapper = manager.findOrCreateWrapper(newOverlayItem);
        UnderlayItem underlayItem = createUnderlayNodeWithAllTPs(wrapper);
        // this will add termination points to wrapper - it calls addOverlayItem in tpManager
        tpOperator.processCreatedChanges(itemIdentifier, underlayItem, topologyId);
        manager.writeWrapper(wrapper, newOverlayItem.getCorrelationItem());
    }

}
