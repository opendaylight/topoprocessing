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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtOpaqueNodeAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.MtOpaqueNodeAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.BasicAttributeTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.basic.attribute.types.StringValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.opaque.attribute.rev150122.opaque.attribute.value.basic.attribute.types.StringValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.Value;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultitechnologyNodeNameHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MultitechnologyNodeNameHandler.class);

    public static void putNodeName(final DataBroker dataProvider, final MlmtOperationProcessor processor,
            final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey,
            final String nodeNameField, final String nodeName) {
        StringValueBuilder stringValueBuilder = new StringValueBuilder();
        stringValueBuilder.setStringValue(nodeName);
        MtOpaqueNodeAttributeValueBuilder mtOpaqueNodeAttributeValueBuilder =
                new MtOpaqueNodeAttributeValueBuilder();
        mtOpaqueNodeAttributeValueBuilder.setBasicAttributeTypes(stringValueBuilder.build());

        final Uri uri = new Uri(nodeNameField);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final InstanceIdentifier<Attribute> instanceAttributeId = topologyInstanceId.child(Node.class, nodeKey)
                .augmentation(MtInfoNode.class).child(Attribute.class, attributeKey);
        final ValueBuilder valueBuilder = new ValueBuilder();
        valueBuilder.addAugmentation(MtOpaqueNodeAttributeValue.class,
                mtOpaqueNodeAttributeValueBuilder.build());
        final AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setAttributeType(Controller.class);
        attributeBuilder.setValue(valueBuilder.build());
        attributeBuilder.setKey(attributeKey);

        final InstanceIdentifier<Topology> targetTopologyId = topologyInstanceId;

        try {
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final Optional<Attribute> sourceAttributeObject =
                    rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    if (sourceAttributeObject != null && sourceAttributeObject.isPresent()) {
                        transaction.put(LogicalDatastoreType.OPERATIONAL,
                                instanceAttributeId, attributeBuilder.build());
                    } else {
                        final MtInfoNodeBuilder mtInfoNodeBuilder = new MtInfoNodeBuilder();
                        final List<Attribute> listAttribute = new ArrayList<Attribute>();
                        listAttribute.add(attributeBuilder.build());
                        mtInfoNodeBuilder.setAttribute(listAttribute);
                        final InstanceIdentifier<MtInfoNode> instanceId =
                                topologyInstanceId.child(Node.class, nodeKey).augmentation(MtInfoNode.class);
                        transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId,
                                mtInfoNodeBuilder.build(), true);
                    }
                }
            });
        } catch (final InterruptedException e) {
            LOG.error("MultitechnologyNodeNameHandler.putNodeName interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MultitechnologyNodeNameHandler.putNodeName execution exception", e);
        }
    }

    public static String getNodeName(final DataBroker dataProvider, final MlmtOperationProcessor processor,
            final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey,
            final String nodeNameField) {
        final Uri uri = new Uri(nodeNameField);
        final AttributeKey attributeKey = new AttributeKey(uri);
        final InstanceIdentifier<Attribute> instanceAttributeId = topologyInstanceId.child(Node.class, nodeKey)
                .augmentation(MtInfoNode.class).child(Attribute.class, attributeKey);

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
            final MtOpaqueNodeAttributeValue mtOpaqueNodeAttributeValue =
                    value.getAugmentation(MtOpaqueNodeAttributeValue.class);
            if (mtOpaqueNodeAttributeValue == null) {
                return null;
            }
            final BasicAttributeTypes basicAttributeTypes = mtOpaqueNodeAttributeValue.getBasicAttributeTypes();
            if (basicAttributeTypes instanceof StringValue) {
                return ((StringValue)basicAttributeTypes).getStringValue();
            }
        } catch (final InterruptedException e) {
            LOG.error("MultitechnologyNodeNameHandler.getNodeName interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("MultitechnologyNodeNameHandler.getNodeName execution exception", e);
        }

        return null;
    }
}
