/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.multitechnology;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.TedNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.TedLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.attribute.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.AttributeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.NativeL3IgpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.NativeTed;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedNodeAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedNodeAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedLinkAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtTedLinkAttributeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.ted.rev150122.MtLinkMetricAttributeValueBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.impl.rev150122.MultitechnologyTopologyProviderRuntimeMXBean;

public class MultitechnologyTopologyProvider implements MultitechnologyTopologyProviderRuntimeMXBean,
            AutoCloseable, MlmtTopologyProvider {
    private static Logger LOG;
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> DEST_TOPOLOGY_IID;
    private MultitechnologyAttributesParser parser;

    public void init(final Logger logger, MlmtOperationProcessor processor,
            final InstanceIdentifier<Topology> destTopologyId, final MultitechnologyAttributesParser parser) {
        try {
            logger.info("MultitechnologyTopologyProvider.init");
            this.LOG = logger;
            this.DEST_TOPOLOGY_IID = destTopologyId;
            this.processor = processor;
            this.parser = parser;
        } catch (final NullPointerException e) {
             LOG.error("MultitechnologyTopologyProvider.init null pointer exception", e);
        }
    }

    public void setDataProvider(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public void close() {

    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        LOG.info("MultitechnologyTopologyProvider.onTopologyCreated");
        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    try {
                        final MultitechnologyTopologyBuilder multitechnologyTopologyBuilder = new MultitechnologyTopologyBuilder();
                        final MtTopologyTypeBuilder mtTopologyTypeBuilder = new MtTopologyTypeBuilder();
                        mtTopologyTypeBuilder.setMultitechnologyTopology(multitechnologyTopologyBuilder.build());
                        InstanceIdentifier<MtTopologyType> target = DEST_TOPOLOGY_IID.child(TopologyTypes.class).
                               augmentation(MtTopologyType.class);
                        MtTopologyType top = mtTopologyTypeBuilder.build();
                        transaction.merge(LogicalDatastoreType.OPERATIONAL, target, top, true);
                    } catch (final NullPointerException e) {
                        LOG.error("MultitechnologyTopologyProvider.createTopology null pointer exception", e);
                    }
                }
            });
    }

    @Override
    public void onNodeCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        handleNodeAttributes(type, topologyInstanceId, node);
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPoint tp) {
        LOG.info("MultitechnologyTopologyProvider.onTpCreated");
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        handleLinkAttributes(type, topologyInstanceId, link);
    }

    @Override
    public void onTopologyUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
    }

    @Override
    public void onNodeUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        handleNodeAttributes(type, topologyInstanceId, node);
    }

    @Override
    public void onTpUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPoint tp) {
    }

    @Override
    public void onLinkUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        handleLinkAttributes(type, topologyInstanceId, link);
    }

    @Override
    public void onTopologyDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId) {

    }

    @Override
    public void onNodeDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey) {

    }

    @Override
    public void onTpDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey,
            final TerminationPointKey tpKey) {

    }

    @Override
    public void onLinkDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey) {

    }

    private void handleNodeAttributes(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        try {
            LOG.info("MultitechnologyTopologyProvider.onNodeCreated");
            TedNodeAttributes tedNodeAttributes = parser.parseTedNodeAttributes(node);
            if (tedNodeAttributes == null)
                return;

            setNativeMtNodeAttributes(type, topologyInstanceId, tedNodeAttributes, node.getKey());
        } catch (final NullPointerException e) {
             LOG.error("MultitechnologyTopologyProvider.onNodeCreated null pointer exception", e);
        }
    }

    private void handleLinkAttributes(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        try {
            LOG.info("MultitechnologyTopologyProvider.onLinkCreated");
            Long metric = parser.parseLinkMetric(link);
            if (metric != null)
                setNativeMtLinkMetricAttribute(type, topologyInstanceId, metric, link.getKey());

            TedLinkAttributes tedLinkAttributes = parser.parseTedLinkAttributes(link);
            if (tedLinkAttributes == null)
                return;

            setNativeMtLinkTedAttribute(type, topologyInstanceId, tedLinkAttributes, link.getKey());
        } catch (final NullPointerException e) {
             LOG.error("MultitechnologyTopologyProvider.onLinkCreated null pointer exception", e);
        }
    }

    private void setNativeMtLinkMetricAttribute(
            final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Long metric,
            final LinkKey linkKey) {
        try {
            LOG.info("MultitechnologyTopologyProvider.setNativeMtLinkMetricAttribute");
            final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
            final String path = "native-l3-igp-metric:1";
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final Uri uri = new Uri(path);
            final AttributeKey attributeKey = new AttributeKey(uri);
            final InstanceIdentifier<Attribute> instanceAttributeId = targetTopologyId.child(Link.class, linkKey).
                augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);
            final Optional<Attribute> sourceAttributeObject =
                rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    try {
                        final MtLinkMetricAttributeValueBuilder mtLinkMetricAVBuilder = new MtLinkMetricAttributeValueBuilder();
                        mtLinkMetricAVBuilder.setMetric(metric);
                        final ValueBuilder valueBuilder = new ValueBuilder();
                        valueBuilder.addAugmentation(MtLinkMetricAttributeValue.class, mtLinkMetricAVBuilder.build());
                        final AttributeBuilder attributeBuilder = new AttributeBuilder();
                        attributeBuilder.setAttributeType(NativeL3IgpMetric.class);
                        attributeBuilder.setValue(valueBuilder.build());
                        attributeBuilder.setId(uri);
                        attributeBuilder.setKey(attributeKey);
                        if (sourceAttributeObject != null && sourceAttributeObject.isPresent() && sourceAttributeObject.get() != null) {
                            transaction.put(LogicalDatastoreType.OPERATIONAL, instanceAttributeId, attributeBuilder.build());
                        } else {
                            final MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
                            final ArrayList<Attribute> la = new ArrayList<Attribute>();
                            la.add(attributeBuilder.build());
                            mtInfoLinkBuilder.setAttribute(la);
                            final InstanceIdentifier<MtInfoLink> instanceId = targetTopologyId.child(Link.class, linkKey).
                                augmentation(MtInfoLink.class);
                            transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId, mtInfoLinkBuilder.build(), true);
                        }
                    } catch (final NullPointerException e) {
                       LOG.error("MultitechnologyTopologyProvider.createTopology null pointer exception", e);
                    }
                }
            });
        } catch (final InterruptedException e) {
            LOG.error("onNodeCreated interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("onNodeCreated execution exception", e);
        }
    }

    private void setNativeMtLinkTedAttribute(
            final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TedLinkAttributes ted,
            final LinkKey linkKey) {
        try {
            LOG.info("MultitechnologyTopologyProvider.setNativeMtLinkTedAttribute");
            final String path = "native-ted:1";
            final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final Uri uri = new Uri(path);
            final AttributeKey attributeKey = new AttributeKey(uri);
            final InstanceIdentifier<Attribute> instanceAttributeId = targetTopologyId.child(Link.class, linkKey).
                augmentation(MtInfoLink.class).child(Attribute.class, attributeKey);
            final Optional<Attribute> sourceAttributeObject =
                rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    try {
                        final MtTedLinkAttributeValueBuilder mtTedLAVBuilder = new MtTedLinkAttributeValueBuilder();
                        if (ted.getMaxLinkBandwidth() != null)
                            mtTedLAVBuilder.setMaxLinkBandwidth(ted.getMaxLinkBandwidth());
                        if (ted.getMaxResvLinkBandwidth() != null)
                            mtTedLAVBuilder.setMaxResvLinkBandwidth(ted.getMaxResvLinkBandwidth());
                        if (ted.getUnreservedBandwidth() != null)
                            mtTedLAVBuilder.setUnreservedBandwidth(ted.getUnreservedBandwidth());
                        if (ted.getColor() != null)
                            mtTedLAVBuilder.setColor(ted.getColor());
                        if (ted.getSrlg() != null)
                            mtTedLAVBuilder.setSrlg(ted.getSrlg());
                        if (ted.getTeDefaultMetric() != null)
                            mtTedLAVBuilder.setTeDefaultMetric(ted.getTeDefaultMetric());

                        final ValueBuilder valueBuilder = new ValueBuilder();
                        valueBuilder.addAugmentation(MtTedLinkAttributeValue.class, mtTedLAVBuilder.build());
                        final AttributeBuilder attributeBuilder = new AttributeBuilder();
                        attributeBuilder.setAttributeType(NativeTed.class);
                        attributeBuilder.setValue(valueBuilder.build());
                        attributeBuilder.setId(uri);
                        attributeBuilder.setKey(attributeKey);

                        if (sourceAttributeObject != null && sourceAttributeObject.isPresent() && sourceAttributeObject.get() != null) {
                            transaction.put(LogicalDatastoreType.OPERATIONAL, instanceAttributeId, attributeBuilder.build());
                        } else {
                            final MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
                            final ArrayList<Attribute> la = new ArrayList<Attribute>();
                            la.add(attributeBuilder.build());
                            mtInfoLinkBuilder.setAttribute(la);
                            final InstanceIdentifier<MtInfoLink> instanceId = targetTopologyId.child(Link.class, linkKey).
                                augmentation(MtInfoLink.class);
                            transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId, mtInfoLinkBuilder.build(), true);
                        }
                    } catch (final NullPointerException e) {
                        LOG.error("MultitechnologyTopologyProvider.setNativeMtLinkTedAttribute null pointer exception", e);
                    }
                }
            });
        } catch (final InterruptedException e) {
            LOG.error("onNodeCreated interrupted exception", e);
        } catch (final ExecutionException e) {
            LOG.error("onNodeCreated execution exception", e);
        }
    }

    private void setNativeMtNodeAttributes(
            final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final TedNodeAttributes ted,
            final NodeKey nodeKey) {
        try {
            LOG.info("MultitechnologyTopologyProvider.setNativeMtNodeAttributes");
            final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
            final String path = "native-ted:1";
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            final Uri uri = new Uri(path);
            final AttributeKey attributeKey = new AttributeKey(uri);
            final InstanceIdentifier<Attribute> instanceAttributeId = targetTopologyId.child(Node.class, nodeKey).
                    augmentation(MtInfoNode.class).child(Attribute.class, attributeKey);
            final Optional<Attribute> sourceAttributeObject =
                    rx.read(LogicalDatastoreType.OPERATIONAL, instanceAttributeId).get();

            processor.enqueueOperation(new MlmtTopologyOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction transaction) {
                    try {
                        final MtTedNodeAttributeValueBuilder tedNodeAttrValueBuilder = new MtTedNodeAttributeValueBuilder();
                        if (ted.getTeRouterIdIpv4() != null)
                            tedNodeAttrValueBuilder.setTeRouterIdIpv4(ted.getTeRouterIdIpv4());
                        if (ted.getTeRouterIdIpv6() != null)
                            tedNodeAttrValueBuilder.setTeRouterIdIpv6(ted.getTeRouterIdIpv6());
                        if (ted.getIpv4LocalAddress() != null)
                            tedNodeAttrValueBuilder.setIpv4LocalAddress(ted.getIpv4LocalAddress());
                        if (ted.getIpv6LocalAddress() != null)
                            tedNodeAttrValueBuilder.setIpv6LocalAddress(ted.getIpv6LocalAddress());
                        if (ted.getPccCapabilities() != null)
                            tedNodeAttrValueBuilder.setPccCapabilities(ted.getPccCapabilities());

                        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
                        final ValueBuilder valueBuilder = new ValueBuilder();
                        valueBuilder.addAugmentation(MtTedNodeAttributeValue.class, tedNodeAttrValueBuilder.build());
                        final AttributeBuilder attributeBuilder = new AttributeBuilder();
                        attributeBuilder.setAttributeType(NativeTed.class);
                        attributeBuilder.setValue(valueBuilder.build());
                        attributeBuilder.setKey(attributeKey);

                        if (sourceAttributeObject != null && sourceAttributeObject.isPresent() && sourceAttributeObject.get() != null) {
                            transaction.put(LogicalDatastoreType.OPERATIONAL, instanceAttributeId, attributeBuilder.build());
                        } else {
                            final MtInfoNodeBuilder mtInfoNodeBuilder = new MtInfoNodeBuilder();
                            final ArrayList<Attribute> la = new ArrayList<Attribute>();
                            la.add(attributeBuilder.build());
                            mtInfoNodeBuilder.setAttribute(la);
                            final InstanceIdentifier<MtInfoNode> instanceId = targetTopologyId.child(Node.class, nodeKey).
                                    augmentation(MtInfoNode.class);
                            transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId, mtInfoNodeBuilder.build(), true);
                        }
                    } catch (final NullPointerException e) {
                        LOG.error("MultitechnologyTopologyProvider.setNativeMtNodeAttributes null pointer exception", e);
                    }
                }
            });
         } catch (final InterruptedException e) {
             LOG.error("onNodeCreated interrupted exception", e);
         } catch (final ExecutionException e) {
             LOG.error("onNodeCreated execution exception", e);
         }
    }
}
