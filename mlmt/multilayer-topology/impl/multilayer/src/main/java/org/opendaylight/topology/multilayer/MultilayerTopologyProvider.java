/*
 * Copyright (c)2014 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.multilayer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MultilayerTopologyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.end.point.StitchingPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.announcement.context.SupportingResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.multilayer.topology.type.MultilayerTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.impl.rev150123.MultilayerTopologyProviderRuntimeMXBean;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.announce.output.result.FaIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.announce.output.result.NoneBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;

import com.google.common.util.concurrent.Futures;

public class MultilayerTopologyProvider implements MultilayerTopologyProviderRuntimeMXBean,
            AutoCloseable, MlmtTopologyProvider, MultilayerTopologyService {
    private static Logger LOG;
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> DEST_TOPOLOGY_IID;

    public void init(final Logger logger, MlmtOperationProcessor theProcessor, InstanceIdentifier<Topology> destTopologyId) {
        LOG = logger;
        DEST_TOPOLOGY_IID = destTopologyId;
        processor = theProcessor;
    }

    public void setDataProvider(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        LOG.info("MultilayerTopologyProvider.onTopologyCreated");
        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final MultilayerTopologyBuilder multilayerTopologyBuilder = new MultilayerTopologyBuilder();
                final MlTopologyTypeBuilder mlTopologyTypeBuilder = new MlTopologyTypeBuilder();
                mlTopologyTypeBuilder.setMultilayerTopology(multilayerTopologyBuilder.build());
                InstanceIdentifier<MlTopologyType> target = DEST_TOPOLOGY_IID.child(TopologyTypes.class)
                        .augmentation(MtTopologyType.class).child(MultitechnologyTopology.class)
                        .augmentation(MlTopologyType.class);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, target, mlTopologyTypeBuilder.build(), true);
            }
        });
    }

    @Override
    public void onNodeCreated(final LogicalDatastoreType type,
                              final InstanceIdentifier<Topology> topologyInstanceId,
                              final Node node) {
        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type,
                            final InstanceIdentifier<Topology> topologyInstanceId,
                            final NodeKey nodeKey,
                            final TerminationPoint tp) {
       final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
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
    }

  @Override
       public void onTopologyDeleted(final LogicalDatastoreType type,
               final InstanceIdentifier<Topology> topologyInstanceId) {}

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

    @Override
    public synchronized void close() throws InterruptedException {
        LOG.info("MultilayerTopologyProvider stopped.");
    }

     private void dumpMap(Map<InstanceIdentifier<?>, DataObject> map){
       Iterator<InstanceIdentifier<?>> iter = map.keySet().iterator();
       while(iter.hasNext()){
         InstanceIdentifier<?> iid = iter.next();
         LOG.info("Key: " + iid );
         LOG.info("Value: " + map.get(iid));
       }
     }

    private Future<RpcResult<ForwardingAdjAnnounceOutput>> createMtLink(final HeadEnd headEnd, final TailEnd tailEnd, final MtInfo mtInfo) {
            LOG.info("MultilayerTopologyProvider.createMtLink");
            final InstanceIdentifier<Topology> targetTopologyId = DEST_TOPOLOGY_IID;
            final NodeId headNodeId = headEnd.getNode();
            final List<TpId> lHeadTpId = headEnd.getSupportingTp();
            final TpId headTpId = headEnd.getTpId();
            final NodeId tailNodeId = tailEnd.getNode();
            final List<TpId> lTailTpId = tailEnd.getSupportingTp();
            final TpId tailTpId = tailEnd.getTpId();
            final SourceBuilder sourceBuilder = new SourceBuilder();
            sourceBuilder.setSourceNode(headNodeId).setSourceTp(headTpId);
            final DestinationBuilder destinationBuilder = new DestinationBuilder();
            destinationBuilder.setDestNode(headNodeId).setDestTp(headTpId);
            final LinkBuilder linkBuilder = new LinkBuilder();
            final LinkId linkId = new LinkId(headNodeId.toString());
            final LinkKey linkKey = new LinkKey(linkId);
            linkBuilder.setSource(sourceBuilder.build()).setDestination(destinationBuilder.build())
                    .setKey(linkKey).setLinkId(linkId)
                    .setSupportingLink(Collections.<SupportingLink>emptyList());
            final MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
            mtInfoLinkBuilder.setAttribute(mtInfo.getAttribute());
            linkBuilder.addAugmentation(MtInfoLink.class, mtInfoLinkBuilder.build());
            final InstanceIdentifier<Link> instanceId = targetTopologyId.child(Link.class, linkKey);

           WriteTransaction transaction = dataProvider.newWriteOnlyTransaction();
           transaction.merge(LogicalDatastoreType.OPERATIONAL, instanceId, linkBuilder.build());
           try {
                   transaction.submit().checkedGet();
           }  catch (final Exception e) {
               LOG.warn("transaction", e);

               ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
               faAdjAnnOutputBuilder.setResult(new NoneBuilder().setNone(true).build());
               return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> failed()
                       .withResult(faAdjAnnOutputBuilder.build()).build());
           }

           ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
           faAdjAnnOutputBuilder.setResult(new FaIdBuilder().setFaId(new FaId(headNodeId.toString())).build());
           return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> success()
                        .withResult(faAdjAnnOutputBuilder.build()).build());
     }

     @Override
     public Future<RpcResult<ForwardingAdjAnnounceOutput>> forwardingAdjAnnounce(ForwardingAdjAnnounceInput input) {

         StitchingPoint headStitchingPoint = input.getHeadEnd().getStitchingPoint();
         StitchingPoint tailStitchingPoint = input.getTailEnd().getStitchingPoint();
//         Uri uriAc = announcementContext.getId();
         List<SupportingResource> lSrAc = input.getAnnouncementContext().getSupportingResource();

         return createMtLink(input.getHeadEnd(), input.getTailEnd(), input);
     }

     @Override
     public Future<RpcResult<ForwardingAdjUpdateOutput>> forwardingAdjUpdate(ForwardingAdjUpdateInput input) {

         org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.error.ErrorBuilder
                 iErrorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.error.ErrorBuilder();

         org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.ErrorBuilder
                 errorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.ErrorBuilder();
         errorBuilder.setError(iErrorBuilder.build());

         ForwardingAdjUpdateOutputBuilder faAdjAnnUpdBuilder = new ForwardingAdjUpdateOutputBuilder();
         faAdjAnnUpdBuilder.setResult(errorBuilder.build());
         return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                       .withResult(faAdjAnnUpdBuilder.build()).build());
     }

     @Override
     public Future<RpcResult<ForwardingAdjWithdrawOutput>> forwardingAdjWithdraw(ForwardingAdjWithdrawInput input)  {

         org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.error.ErrorBuilder
                 iErrorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.error.ErrorBuilder();

         org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.ErrorBuilder
                 errorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.ErrorBuilder();
         errorBuilder.setError(iErrorBuilder.build());

         ForwardingAdjWithdrawOutputBuilder faAdjWithdrawOutputBuilder = new ForwardingAdjWithdrawOutputBuilder();
         faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
                 return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                         .withResult(faAdjWithdrawOutputBuilder.build()).build());
     }
}
