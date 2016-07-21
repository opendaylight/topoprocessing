/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multitechnology;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.Controller;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtOpaqueLinkAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtOpaqueLinkAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.BasicAttributeTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.basic.attribute.types.StringValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.basic.attribute.types.StringValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.Value;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultitechnologyLinkNameHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MultitechnologyLinkNameHandler.class);

    public static void putLinkName(final DataBroker dataProvider, final MlmtOperationProcessor processor,
            final InstanceIdentifier<Topology> topologyInstanceId, final LinkKey linkKey,
            final String linkNameField, final String linkName) {
        StringValueBuilder stringValueBuilder = new StringValueBuilder();
        stringValueBuilder.setStringValue(linkName);
        MtOpaqueLinkAttributeValueBuilder mtOpaqueLinkAttributeValueBuilder =
                new MtOpaqueLinkAttributeValueBuilder();
        mtOpaqueLinkAttributeValueBuilder.setBasicAttributeTypes(stringValueBuilder.build());

        final Uri uri = new Uri(linkNameField);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final InstanceIdentifier<Attribute> instanceAttributeId = topologyInstanceId.child(Link.class, linkKey)
                .augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);
        final ValueBuilder valueBuilder = new ValueBuilder();
        valueBuilder.addAugmentation(MtOpaqueLinkAttributeValue.class, mtOpaqueLinkAttributeValueBuilder.build());
        final AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setAttributeType(Controller.class);
        attributeBuilder.setValue(valueBuilder.build());
        attributeBuilder.setKey(attributeKey);

        try {
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final Optional<Attribute> sourceAttributeObject =
                     rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    if (sourceAttributeObject != null && sourceAttributeObject.isPresent()) {
                        transaction.put(LogicalDatastoreType.OPERATIONAL, instanceAttributeId,
                                attributeBuilder.build());
                    } else {
                        final MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
                        final List<Attribute> listAttribute = new ArrayList<Attribute>();
                        listAttribute.add(attributeBuilder.build());
                        mtInfoLinkBuilder.setAttribute(listAttribute);
                        final InstanceIdentifier<MtInfoLink> instanceId =
                                topologyInstanceId.child(Link.class, linkKey).augmentation(MtInfoLink.class);
                        transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId,
                                mtInfoLinkBuilder.build(), true);
                    }
                }
            });
        } catch (final InterruptedException e) {
            LOG.error("MultitechnologyLinkNameHandler.putLinkName interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MultitechnologyLinkNameHandler.putLinkName execution exception", e);
        }
    }

    public static String getLinkName(final DataBroker dataProvider, final MlmtOperationProcessor processor,
            final InstanceIdentifier<Topology> topologyInstanceId, final LinkKey linkKey,
            final String linkNameField) {
        final Uri uri = new Uri(linkNameField);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final InstanceIdentifier<Attribute> instanceAttributeId = topologyInstanceId.child(Link.class, linkKey)
                .augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);

        final InstanceIdentifier<Topology> targetTopologyId = topologyInstanceId;

        try {
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final Optional<Attribute> sourceAttributeObject =
                    rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();
            if (sourceAttributeObject == null || !sourceAttributeObject.isPresent()) {
                return null;
            }
            final Value value = sourceAttributeObject.get().getValue();
            if (value == null) {
                return null;
            }
            final MtOpaqueLinkAttributeValue mtOpaqueLinkAttributeValue =
                    value.getAugmentation(MtOpaqueLinkAttributeValue.class);
            if (mtOpaqueLinkAttributeValue == null) {
                return null;
            }
            final BasicAttributeTypes basicAttributeTypes =
                    mtOpaqueLinkAttributeValue.getBasicAttributeTypes();
            if (basicAttributeTypes instanceof StringValue) {
                return ((StringValue)basicAttributeTypes).getStringValue();
            }
        } catch (final InterruptedException e) {
            LOG.error("MultitechnologyLinNameHandler.getLinkName interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MultitechnologyLinNameHandler.getLinkName execution exception", e);
        }

        return null;
    }
}
