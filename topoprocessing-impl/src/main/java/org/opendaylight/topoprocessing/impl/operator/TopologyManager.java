/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.operator;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.IdentifierGenerator;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.uhlir
 *
 */
public class TopologyManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyManager.class);

    private IdentifierGenerator idGenerator = new IdentifierGenerator();
    private List<OverlayItemWrapper> nodeWrappers = new ArrayList<>();
    private List<OverlayItemWrapper> linkWrappers = new ArrayList<>();
    private TopologyWriter writer;
    private RpcRepublisher republisher;

    /**
     * Default constructor
     * @param writer writes data into the operational datastore
     * @param republisher republishes rpcs
     */
    public TopologyManager(TopologyWriter writer, RpcRepublisher republisher) {
        this.writer = writer;
        this.republisher = republisher;
    }

    /**
     * For testing purpose only
     * @return All overlayItem wrappers
     */
    public List<OverlayItemWrapper> getNodeWrappers(){
        return nodeWrappers;
    }

    /**
     * Adds new overlay item into
     * - existing overlay item wrapper
     * - new overlay item wrapper
     * @param newOverlayItem - OverlayItem which shall be put into wrapper
     */
    public void addOverlayItem(OverlayItem newOverlayItem) {
        if (newOverlayItem != null && !newOverlayItem.getUnderlayItems().isEmpty()) {
            for (UnderlayItem newUnderlayItem : newOverlayItem.getUnderlayItems()) {
                for (OverlayItemWrapper wrapper : getWrappersList(newOverlayItem.getCorrelationItem())) {
                    for (OverlayItem overlayItemFromWrapper : wrapper.getOverlayItems()) {
                        for (UnderlayItem underlayItemFromWrapper : overlayItemFromWrapper.getUnderlayItems()) {
                            if (underlayItemFromWrapper.getItemId().equals(newUnderlayItem.getItemId())) {
                                // update existing wrapper
                                wrapper.addOverlayItem(newOverlayItem);
                                writer.writeItem(wrapper, newOverlayItem.getCorrelationItem());
                                registerOverlayRpcs(wrapper, newOverlayItem);
                                return;
                            }
                        }
                    }
                }
            }
            // create new overlay item wrapper with unique id and add the overlay item into it
            String wrapperId = idGenerator.getNextIdentifier(newOverlayItem.getCorrelationItem());
            OverlayItemWrapper newWrapper = new OverlayItemWrapper(wrapperId, newOverlayItem);
            getWrappersList(newOverlayItem.getCorrelationItem()).add(newWrapper);
            writer.writeItem(newWrapper, newOverlayItem.getCorrelationItem());
            registerOverlayRpcs(newWrapper, newOverlayItem);
        }
    }

    /**
     * @param overlayItemIdentifier OverlayItem with new changes to update
     */
    public void updateOverlayItem(OverlayItem overlayItemIdentifier) {
        for (OverlayItemWrapper wrapper : getWrappersList(overlayItemIdentifier.getCorrelationItem())) {
            for (OverlayItem overlayItem : wrapper.getOverlayItems()) {
                if (overlayItem.equals(overlayItemIdentifier)) {
                    writer.writeItem(wrapper, overlayItemIdentifier.getCorrelationItem());
                    registerOverlayRpcs(wrapper, overlayItem);
                }
            }
        }
    }

    /**
     * @param overlayItemIdentifier OverlayItem to remove
     */
    public void removeOverlayItem(OverlayItem overlayItemIdentifier) {
        OverlayItemWrapper foundWrapper = null;
        for (OverlayItemWrapper wrapper : getWrappersList(overlayItemIdentifier.getCorrelationItem())) {
            if (wrapper.getOverlayItems().remove(overlayItemIdentifier)) {
                foundWrapper = wrapper;
                break;
            }
        }
        if (foundWrapper != null) {
            if (foundWrapper.getOverlayItems().size() == 0) {
                // remove overlay item wrapper as well
                writer.deleteItem(foundWrapper, overlayItemIdentifier.getCorrelationItem());
                getWrappersList(overlayItemIdentifier.getCorrelationItem()).remove(foundWrapper);
            } else {
                writer.writeItem(foundWrapper, overlayItemIdentifier.getCorrelationItem());
            }
        }
    }

    /**
     * Calls {@link RpcRepublisher} to republish present rpcs
     * @param wrapper contains id for republished rpc
     * @param overlayItem contains UnderlayItems with RPCs
     */
    private void registerOverlayRpcs(OverlayItemWrapper wrapper, OverlayItem overlayItem) {
        LOGGER.trace("Registering overlay RPCs");
        republisher.registerRpcs(wrapper, overlayItem);
    }

    private List<OverlayItemWrapper> getWrappersList(CorrelationItemEnum correlationItem) {
        List<OverlayItemWrapper> resultList = null;
        switch (correlationItem) {
        case Node:
        case TerminationPoint:
            resultList = nodeWrappers; 
            break;
        case Link:
            resultList = linkWrappers;
            break;
        default:
            throw new IllegalArgumentException("Wrong Correlation item set: " + correlationItem);
        }
        return resultList;
    }
}