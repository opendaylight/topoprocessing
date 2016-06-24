/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multitechnology;

import com.google.common.base.Optional;

import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.ArrayList;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.Value;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.basic.attribute.types.StringValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtOpaqueLinkAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtOpaqueLinkAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.Controller;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.basic.attribute.types.StringValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.BasicAttributeTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.mlmt.utility.MlmtInfoOpaqueAttrId;
import org.opendaylight.topology.mlmt.utility.MlmtInfoCorrelationField;

public class MultitechnologyLinkCorrelationFieldHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MultitechnologyLinkCorrelationFieldHandler.class);

    public static void putCorrelationField(final DataBroker dataProvider, final MlmtOperationProcessor processor,
            final InstanceIdentifier<Topology> topologyInstanceId, final LinkKey linkKey,
            final String correlationField, final String correlatedValue) {
        StringValueBuilder stringValueBuilder = new StringValueBuilder();
        stringValueBuilder.setStringValue(
                correlationField.concat(MlmtInfoCorrelationField.MTINFO_ATTR_CORRELATION_FIELD_SEP)
                .concat(correlatedValue));
        MtOpaqueLinkAttributeValueBuilder mtOpaqueLinkAttributeValueBuilder =
                new MtOpaqueLinkAttributeValueBuilder();
        mtOpaqueLinkAttributeValueBuilder.setBasicAttributeTypes(stringValueBuilder.build());

        final Uri uri = new Uri(MlmtInfoOpaqueAttrId.MTINFO_OPAQUE_ATTR_ID_CORRELATION_FIELD);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final ValueBuilder valueBuilder = new ValueBuilder();
        valueBuilder.addAugmentation(MtOpaqueLinkAttributeValue.class, mtOpaqueLinkAttributeValueBuilder.build());
        final AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setAttributeType(Controller.class);
        attributeBuilder.setValue(valueBuilder.build());
        attributeBuilder.setKey(attributeKey);

        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                List<Attribute> listAttribute = new ArrayList<Attribute>();
                listAttribute.add(attributeBuilder.build());
                MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
                mtInfoLinkBuilder.setAttribute(listAttribute);
                final LinkBuilder linkBuilder = new LinkBuilder();
                linkBuilder.setKey(linkKey);
                linkBuilder.setLinkId(linkKey.getLinkId());
                linkBuilder.addAugmentation(MtInfoLink.class, mtInfoLinkBuilder.build());
                final InstanceIdentifier<Link> instanceId = topologyInstanceId.child(Link.class, linkKey);
                transaction.merge(LogicalDatastoreType.CONFIGURATION, instanceId, linkBuilder.build(), true);
            }
        });
    }

    public static String getCorrelationField(final DataBroker dataProvider, final MlmtOperationProcessor processor,
            final InstanceIdentifier<Topology> topologyInstanceId, final LinkKey linkKey) {
        final Uri uri = new Uri(MlmtInfoOpaqueAttrId.MTINFO_OPAQUE_ATTR_ID_CORRELATION_FIELD);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final InstanceIdentifier<Attribute> instanceAttributeId = topologyInstanceId.child(Link.class, linkKey)
                .augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);

        try {
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final Optional<Attribute> sourceAttributeObject =
                    rx.read(LogicalDatastoreType.CONFIGURATION, instanceAttributeId).get();
            if (sourceAttributeObject == null || !sourceAttributeObject.isPresent()) {
                return null;
            }
            final Value value = sourceAttributeObject.get().getValue();
            final MtOpaqueLinkAttributeValue mtOpaqueLinkAttributeValue =
                    value.getAugmentation(MtOpaqueLinkAttributeValue.class);
            if (mtOpaqueLinkAttributeValue != null) {
                final BasicAttributeTypes basicAttributeTypes =
                        mtOpaqueLinkAttributeValue.getBasicAttributeTypes();
                if (basicAttributeTypes instanceof StringValue) {
                    return ((StringValue)basicAttributeTypes).getStringValue();
                }
            }
        } catch (final InterruptedException e) {
            LOG.error("MultitechnologyLinkCorrelationFieldHandler.getCorrelationField interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MultitechnologyLinkCorrelationFieldHandler.getCorrelationField execution exception", e);
        }

        return null;
    }
}
