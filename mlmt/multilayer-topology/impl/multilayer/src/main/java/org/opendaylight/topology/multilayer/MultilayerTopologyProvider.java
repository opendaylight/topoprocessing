/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multilayer;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MlTopologyTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MultilayerTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MultilayerTopologyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.end.point.StitchingPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.DirectionalityInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.Bidirectional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.stitched.end.point.pair.StitchedHeadEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.stitched.end.point.pair.StitchedTailEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.announce.output.result.FaIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.announce.output.result.NoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.HeadEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adjacency.attributes.TailEndBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.multilayer.topology.type.MultilayerTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtInfoLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.mt.info.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultilayerTopologyProvider implements AutoCloseable, MlmtTopologyProvider,
        MultilayerTopologyService, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(MultilayerTopologyProvider.class);
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> destTopologyId;
    private MultilayerAttributesParser parser;
    private BindingTransactionChain transactionChain;
    private MultilayerForwardingAdjacency forwardingAdjacencyProvider = null;
    private Map<String, LinkBuilder> mapFaDownState;

    private synchronized void addLinkFaDownState(String faId, LinkBuilder linkBuilder) {
        mapFaDownState.put(faId, linkBuilder);
    }

    private synchronized void removeLinkFaDownState(String faId) {
        mapFaDownState.remove(faId);
    }

    private synchronized LinkBuilder getLinkFaDownState(String faId) {
        return mapFaDownState.get(faId);
    }

    public void init(MlmtOperationProcessor processor, InstanceIdentifier<Topology> destTopologyId,
            final MultilayerAttributesParser parser,
            final MultilayerForwardingAdjacency forwardingAdjacencyProvider) {
        LOG.info("MultilayerTopologyProvider.init");
        this.destTopologyId = destTopologyId;
        this.processor = processor;
        this.parser = parser;
        this.forwardingAdjacencyProvider = forwardingAdjacencyProvider;
        mapFaDownState = new HashMap<String, LinkBuilder>();
    }

    public void setDataProvider(DataBroker dataProvider) {
        LOG.info("MultilayerTopologyProvider.setDataProvider");
        this.dataProvider = dataProvider;
        this.transactionChain = dataProvider.createTransactionChain(this);
    }

    public void registerRpcImpl(final RpcProviderRegistry rpcProviderRegistry,
            InstanceIdentifier<Topology> mlmtTopologyId) {
        LOG.info("MultilayerTopologyProvider.registerRpcImpl {}", mlmtTopologyId.toString());
        RoutedRpcRegistration reg = rpcProviderRegistry.addRoutedRpcImplementation(
                MultilayerTopologyService.class, this);
        reg.registerPath(MultilayerTopologyContext.class, mlmtTopologyId);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        //NOOP
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
                Throwable cause) {
        LOG.error("Failed to export MultilayerTopologyProvider operations, Transaction {} failed cause {}",
                  transaction.getIdentifier(), cause);
        transactionChain.close();
        transactionChain = dataProvider.createTransactionChain(this);
    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        LOG.info("MultilayerTopologyProvider.onTopologyCreated");
        final InstanceIdentifier<Topology> targetTopologyId = destTopologyId;
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final MultilayerTopologyBuilder multilayerTopologyBuilder = new MultilayerTopologyBuilder();
                final MlTopologyTypeBuilder mlTopologyTypeBuilder = new MlTopologyTypeBuilder();
                mlTopologyTypeBuilder.setMultilayerTopology(multilayerTopologyBuilder.build());
                InstanceIdentifier<MlTopologyType> target = targetTopologyId.child(TopologyTypes.class)
                        .augmentation(MtTopologyType.class).child(MultitechnologyTopology.class)
                        .augmentation(MlTopologyType.class);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, target, mlTopologyTypeBuilder.build(), true);
            }
        });
    }

    @Override
    public void onNodeCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Node node) {
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey,
            final TerminationPoint tp) {
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
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

    private void createTp(WriteTransaction transaction, FaId outFaId,
            InstanceIdentifier<Topology> topologyInstanceId, NodeKey nodeKey,
                    TerminationPointBuilder tpBuilder) {
        LOG.info("MultilayerTopologyProvider.createTp");
        TerminationPointKey tpKey = tpBuilder.getKey();
        InstanceIdentifier<TerminationPoint> tpInstanceId = topologyInstanceId.child(Node.class, nodeKey)
                .child(TerminationPoint.class, tpKey);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, tpInstanceId, tpBuilder.build());

        if (forwardingAdjacencyProvider != null) {
            forwardingAdjacencyProvider.onTpCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId,
                    outFaId, nodeKey, tpKey);
        }
    }

    void createLink(WriteTransaction transaction, FaId outFaId, InstanceIdentifier<Topology> topologyInstanceId,
            LinkBuilder linkBuilder, boolean bidirFlag) {
        LOG.info("MultilayerTopologyProvider.createLink");
        LinkKey head2TailLinkKey = linkBuilder.getKey();
        InstanceIdentifier<Link> linkInstanceId = topologyInstanceId.child(Link.class, head2TailLinkKey);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, linkInstanceId, linkBuilder.build());

        LinkKey tail2HeadLinkKey = null;
        if (bidirFlag) {
            linkBuilder = parser.swapSourceDestination(linkBuilder, true);
            tail2HeadLinkKey = linkBuilder.getKey();
            linkInstanceId = topologyInstanceId.child(Link.class, tail2HeadLinkKey);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, linkInstanceId, linkBuilder.build());
            parser.swapSourceDestination(linkBuilder, false);
        }

        forwardingAdjacencyProvider.onLinkCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId,
                outFaId, head2TailLinkKey);
        if (tail2HeadLinkKey != null) {
            forwardingAdjacencyProvider.onLinkCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId,
                    outFaId, tail2HeadLinkKey);
        }
    }

    private Future<RpcResult<ForwardingAdjAnnounceOutput>> createMtLink(
            InstanceIdentifier<Topology> topologyInstanceId,
            ForwardingAdjAnnounceInput input, boolean doCreateTp) {
        try {
            WriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
            LOG.info("MultilayerTopologyProvider.createMtLink");
            DirectionalityInfo directionalityInfo = input.getDirectionalityInfo();
            boolean bidirFlag = false;
            if (directionalityInfo instanceof Bidirectional) {
                bidirFlag = true;
            }

            TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
            String topologyName = topologyKey.getTopologyId().getValue();
            String faId = parser.parseFaId(bidirFlag, topologyName);
            final FaId outFaId = new FaId(faId);

            FaEndPoint headEnd = parser.parseHeadEnd(input);
            NodeId headNodeId = parser.parseNodeId(headEnd);
            TpId headTpId = parser.parseTpId(headEnd);

            FaEndPoint tailEnd = parser.parseTailEnd(input);
            NodeId tailNodeId = parser.parseNodeId(tailEnd);
            TpId tailTpId = parser.parseTpId(tailEnd);

            if (doCreateTp) {
                TerminationPointBuilder tpBuilder = parser.parseTerminationPointBuilder(headEnd);
                NodeKey headNodeKey = new NodeKey(headNodeId);
                TerminationPointKey headTpKey = tpBuilder.getKey();

                createTp(transaction, outFaId, topologyInstanceId, headNodeKey, tpBuilder);

                TerminationPointKey tailTpKey = null;
                if (tailTpId != null) {
                    NodeKey tailNodeKey = new NodeKey(tailNodeId);
                    tpBuilder = parser.parseTerminationPointBuilder(tailEnd);
                    tailTpKey = tpBuilder.getKey();
                    createTp(transaction, outFaId, topologyInstanceId, tailNodeKey, tpBuilder);
                }
            }

            LinkBuilder linkBuilder = parser.parseLinkBuilder(input, faId);

            FaOperStatus faOperStatus = input.getOperStatus();
            if (FaOperStatus.forValue(faOperStatus.getIntValue()) == FaOperStatus.Up) {
                createLink(transaction, outFaId, topologyInstanceId, linkBuilder, bidirFlag);
            } else {
                addLinkFaDownState(faId, linkBuilder);
            }

            forwardingAdjacencyProvider.onForwardingAdjacencyCreated(LogicalDatastoreType.OPERATIONAL,
                    topologyInstanceId, outFaId, input);

            transaction.submit().checkedGet();

            FaIdBuilder faIdBuilder = new FaIdBuilder();
            faIdBuilder.setFaId(outFaId);
            StitchedHeadEndBuilder stitchedHeadEndBuilder = new StitchedHeadEndBuilder();
            stitchedHeadEndBuilder.setNode(headNodeId);
            stitchedHeadEndBuilder.setTpId(headTpId);
            StitchedTailEndBuilder stitchedTailEndBuilder = new StitchedTailEndBuilder();
            stitchedTailEndBuilder.setNode(tailNodeId);
            stitchedTailEndBuilder.setTpId(tailTpId);
            faIdBuilder.setStitchedHeadEnd(stitchedHeadEndBuilder.build());
            faIdBuilder.setStitchedTailEnd(stitchedTailEndBuilder.build());
            ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
            faAdjAnnOutputBuilder.setResult(faIdBuilder.build());

            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> success()
                    .withResult(faAdjAnnOutputBuilder.build()).build());

        } catch (final TransactionCommitFailedException e) {
            LOG.warn("MultilayerTopologyProvider.createMtLink: TransactionCommitFailedException ", e);
            transactionChain.close();
            transactionChain = dataProvider.createTransactionChain(this);
            ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
            faAdjAnnOutputBuilder.setResult(new NoneBuilder().setNone(true).build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> failed()
                    .withResult(faAdjAnnOutputBuilder.build()).build());
        } catch (final Exception e) {
            LOG.warn("MultilayerTopologyProvider.createMtLink: Exception ", e);
            transactionChain.close();
            transactionChain = dataProvider.createTransactionChain(this);
            ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
            faAdjAnnOutputBuilder.setResult(new NoneBuilder().setNone(true).build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> failed()
                    .withResult(faAdjAnnOutputBuilder.build()).build());
        }
    }

    private List<Link> readLinkList(InstanceIdentifier<Topology> topologyInstanceId) {
        try {
            Optional<Topology> topologyObject = null;
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            topologyObject = rx.read(LogicalDatastoreType.OPERATIONAL, topologyInstanceId).get();
            if (topologyObject == null) {
                return null;
            }
            if (topologyObject.isPresent() == false) {
                return null;
            }
            Topology topology = topologyObject.get();
            if (topology == null) {
                return null;
            }

            return topology.getLink();

        } catch (InterruptedException e) {
            LOG.error("MultilayerTopologyProvider.readLinkList: interrupted exception", e);
        } catch (ExecutionException e) {
            LOG.error("MultilayerTopologyProvider.readLinkList: execution exception", e);
        }
        return null;
    }

    private boolean checkLinkPresence(InstanceIdentifier<Topology> topologyInstanceId, TpId tpId,
        List<LinkId> linkToExclude) {
        boolean isInUse = false;
        List<Link> aLink = readLinkList(topologyInstanceId);
        for (Link link : aLink) {
            if (linkToExclude.contains(link.getLinkId())) {
                continue;
            }
            if (tpId.getValue().equals(link.getSource().getSourceTp().getValue())) {
                isInUse = true;
            }
            if (tpId.getValue().equals(link.getDestination().getDestTp().getValue())) {
                isInUse = true;
            }
            LOG.info("MultilayerTopologyProvider.checkLinkPresence source {}",
                    link.getSource().getSourceTp().getValue());
            LOG.info("MultilayerTopologyProvider.checkLinkPresence destination {}",
                    link.getDestination().getDestTp().getValue());
            if (isInUse) {
                break;
            }
        }
        LOG.info("MultilayerTopologyProvider.checkLinkPresence {}", isInUse);
        return isInUse;
    }

    @Override
    public Future<RpcResult<ForwardingAdjAnnounceOutput>> forwardingAdjAnnounce(ForwardingAdjAnnounceInput input) {
        LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC");

        InstanceIdentifier<?> iid = input.getNetworkTopologyRef().getValue();
        TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
        InstanceIdentifier<Topology> topologyInstanceId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, topologyKey);

        StitchingPoint headStitchingPoint = input.getHeadEnd().getStitchingPoint();
        StitchingPoint tailStitchingPoint = input.getTailEnd().getStitchingPoint();
        if (headStitchingPoint == null && tailStitchingPoint == null) {
            LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC no stitching points");
            return createMtLink(topologyInstanceId, input, true);
        }

        DirectionalityInfo directionalityInfo = input.getDirectionalityInfo();
        boolean bidirFlag = false;
        if (directionalityInfo instanceof Bidirectional) {
            bidirFlag = true;
        }
        boolean doBidir = bidirFlag;

        FaEndPoint headEnd = parser.parseHeadEnd(input);
        FaEndPoint tailEnd = parser.parseTailEnd(input);
        NodeId headNodeId = parser.parseNodeId(headEnd);
        NodeId tailNodeId = parser.parseNodeId(tailEnd);
        TpId headTpId = headStitchingPoint.getTpId();
        TpId tailTpId = tailStitchingPoint.getTpId();
        LOG.debug("MultilayerTopologyProvider.forwardingAdjAnnounce RPC headNodeId {}", headNodeId.getValue());
        LOG.debug("MultilayerTopologyProvider.forwardingAdjAnnounce RPC headTpId {}", headTpId.getValue());
        LOG.debug("MultilayerTopologyProvider.forwardingAdjAnnounce RPC tailNodeId {}", tailNodeId.getValue());
        LOG.debug("MultilayerTopologyProvider.forwardingAdjAnnounce RPC tailTpId {}", tailTpId.getValue());

        List<Link> aLink = readLinkList(topologyInstanceId);
        if (aLink == null || aLink.isEmpty()) {
            ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
            faAdjAnnOutputBuilder.setResult(new NoneBuilder().setNone(true).build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> failed()
                     .withResult(faAdjAnnOutputBuilder.build()).build());
        }

        Link leftLink = null;
        Link leftLinkReverse = null;
        if (headStitchingPoint != null) {
            LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC headStitchingPoint handling");
            /*
             * looking for "left-sided" link having headStitching point as destination tp
             * and in case of bidirectional scenario, also looking for left-sided link having
             * headStitching point as source tp
             */
            for (Link link : aLink) {
                NodeId destNodeId = link.getDestination().getDestNode();
                TpId destTpId = link.getDestination().getDestTp();
                if (headNodeId.getValue().equals(destNodeId.getValue())
                        && headTpId.getValue().equals(destTpId.getValue())) {
                    leftLink = link;
                    LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC leftLink: {}",
                            leftLink.getLinkId().toString());
                }
                if (!bidirFlag && leftLink != null) {
                    break;
                }
                if (bidirFlag) {
                    NodeId sourceNodeId = link.getSource().getSourceNode();
                    TpId sourceTpId = link.getSource().getSourceTp();
                    if (headNodeId.getValue().equals(sourceNodeId.getValue())
                            && headTpId.getValue().equals(sourceTpId.getValue())) {
                        leftLinkReverse = link;
                        LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC  leftLinkReverse: {}",
                                leftLinkReverse.getLinkId().toString());
                    }
                }
                if (bidirFlag && leftLink != null && leftLinkReverse != null) {
                    break;
                }
            }
        }
        if (bidirFlag == true && leftLinkReverse == null) {
            doBidir = false;
        }

        Link rightLink = null;
        Link rightLinkReverse = null;
        if (tailStitchingPoint != null) {
            LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC tailStitchingPoint handling");
            /*
             * looking for "right-sided" link having headStitching point as source tp
             * and in case of bidirectional, also looking for right-sided link having
             * tailStitchingPoint as destination tp
             */
            for (Link link : aLink) {
                NodeId sourceNodeId = link.getSource().getSourceNode();
                TpId sourceTpId = link.getSource().getSourceTp();
                if (tailNodeId.getValue().equals(sourceNodeId.getValue())
                        && tailTpId.getValue().equals(sourceTpId.getValue())) {
                    rightLink = link;
                    LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC rightLink {}",
                            rightLink.getLinkId().toString());
                }
                if (!bidirFlag && rightLink != null) {
                    break;
                }
                if (bidirFlag == true) {
                    NodeId destNodeId = link.getDestination().getDestNode();
                    TpId destTpId = link.getDestination().getDestTp();
                    if (tailNodeId.getValue().equals(destNodeId.getValue())
                            && tailTpId.getValue().equals(destTpId.getValue())) {
                        rightLinkReverse = link;
                        LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC rightLinkReverse: ",
                                rightLinkReverse.getLinkId().toString());
                    }
                }
                if (bidirFlag && rightLink != null && rightLinkReverse != null) {
                    break;
                }
            }
        }

        if (bidirFlag == true && rightLinkReverse == null) {
            doBidir = false;
        }

        ForwardingAdjAnnounceInputBuilder forwardingAdjAnnounceInputBuilder =
                new ForwardingAdjAnnounceInputBuilder(input);
        if (!doBidir) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters
                    .directionality.info.unidirectional.UnidirectionalBuilder unidirectionalBuilder =
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123
                    .fa.parameters.directionality.info.unidirectional.UnidirectionalBuilder();
            org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality
                    .info.UnidirectionalBuilder unidirBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight
                    .topology.multilayer.rev150123.fa.parameters.directionality.info.UnidirectionalBuilder();
            unidirBuilder.setUnidirectional(unidirectionalBuilder.build());
            forwardingAdjAnnounceInputBuilder.setDirectionalityInfo(unidirBuilder.build());
        }

        HeadEnd headEndPoint = input.getHeadEnd();
        if (leftLink != null) {
            LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce left link found");
            NodeId leftNodeId = leftLink.getSource().getSourceNode();
            TpId leftTpId = leftLink.getSource().getSourceTp();
            HeadEndBuilder headEndBuilder = new HeadEndBuilder();
            headEndBuilder.setAttribute(Collections.<Attribute>emptyList());
            headEndBuilder.setNode(leftNodeId);
            headEndBuilder.setSupportingTp(Collections.<TpId>emptyList());
            headEndBuilder.setTpId(leftTpId);
            headEndPoint = headEndBuilder.build();
        }
        forwardingAdjAnnounceInputBuilder.setHeadEnd(headEndPoint);

        NodeId rightNodeId = tailNodeId;
        TpId rightTpId = tailTpId;
        TailEnd tailEndPoint = input.getTailEnd();
        if (rightLink != null) {
            LOG.info("MultilayerTopologyProvider.forwardingAdjAnnounce right link found");
            rightNodeId = rightLink.getDestination().getDestNode();
            rightTpId = rightLink.getDestination().getDestTp();
            TailEndBuilder tailEndBuilder = new TailEndBuilder();
            tailEndBuilder.setAttribute(Collections.<Attribute>emptyList());
            tailEndBuilder.setNode(rightNodeId);
            tailEndBuilder.setSupportingTp(Collections.<TpId>emptyList());
            tailEndBuilder.setTpId(rightTpId);
            tailEndPoint = tailEndBuilder.build();
        }
        forwardingAdjAnnounceInputBuilder.setTailEnd(tailEndPoint);

        return createMtLink(topologyInstanceId, forwardingAdjAnnounceInputBuilder.build(), false);
    }

    @Override
    public Future<RpcResult<ForwardingAdjUpdateOutput>> forwardingAdjUpdate(ForwardingAdjUpdateInput input) {
        LOG.info("MultilayerTopologyProvider.forwardingAdjUpdate RPC");

        InstanceIdentifier<?> iid = input.getNetworkTopologyRef().getValue();
        TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
        InstanceIdentifier<Topology> topologyInstanceId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, topologyKey);

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output
                .result.error.ErrorBuilder iErrorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology
                .multilayer.rev150123.forwarding.adj.update.output.result.error.ErrorBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output
                .result.ErrorBuilder errorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology
                .multilayer.rev150123.forwarding.adj.update.output.result.ErrorBuilder();
        errorBuilder.setError(iErrorBuilder.build());
        ForwardingAdjUpdateOutputBuilder faAdjAnnUpdBuilder = new ForwardingAdjUpdateOutputBuilder();

        boolean checkLinkPresence = false;
        final String strFaId = input.getFaId().getValue().toString();
        LinkBuilder linkBuilder = getLinkFaDownState(strFaId);
        if (linkBuilder == null) {
            linkBuilder = parser.parseLinkBuilder(input, strFaId);
            checkLinkPresence = true;
        } else {
            List<Attribute> lAttribute = parser.parseMtInfoAttribute(input);
            if (lAttribute != null && !lAttribute.isEmpty()) {
                MtInfoLink mtInfoLink = linkBuilder.getAugmentation(MtInfoLink.class);
                final MtInfoLinkBuilder mtInfoLinkBuilder = new MtInfoLinkBuilder();
                mtInfoLinkBuilder.setAttribute(lAttribute);
                linkBuilder.addAugmentation(MtInfoLink.class, mtInfoLinkBuilder.build());
            }
        }

        InstanceIdentifier<Link> linkInstanceId = topologyInstanceId.child(Link.class, linkBuilder.getKey());

        if (checkLinkPresence) {
            try {
                Optional<Link> linkObject = null;
                final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
                linkObject = rx.read(LogicalDatastoreType.OPERATIONAL, linkInstanceId).get();
                if (linkObject == null) {
                    LOG.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: source linkObject null");
                    faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                    return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                            .withResult(faAdjAnnUpdBuilder.build()).build());
                }
                if (linkObject.isPresent() == false) {
                    LOG.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: linkObject not present");
                    faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                    return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                            .withResult(faAdjAnnUpdBuilder.build()).build());
                }

                Link faLink = linkObject.get();
                if (faLink == null) {
                    LOG.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: dest faLink with faId "
                            + strFaId + " not found");
                    faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                    return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                            .withResult(faAdjAnnUpdBuilder.build()).build());
                }
            } catch (InterruptedException e) {
                LOG.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: interrupted exception", e);
                faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                    .withResult(faAdjAnnUpdBuilder.build()).build());
            } catch (ExecutionException e) {
                LOG.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: execution exception", e);
                faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                    .withResult(faAdjAnnUpdBuilder.build()).build());
            }
        }

        if (forwardingAdjacencyProvider != null) {
            FaId faId = new FaId(strFaId);
            forwardingAdjacencyProvider.onForwardingAdjacencyUpdated(LogicalDatastoreType.OPERATIONAL,
                    topologyInstanceId, faId, input);
        }

        try {
            WriteTransaction transaction = transactionChain.newWriteOnlyTransaction();

            NodeId headNodeId = null;
            TpId headEndTpId = null;
            FaEndPoint headEnd = parser.parseHeadEnd(input);
            if (headEnd != null) {
                headNodeId = parser.parseNodeId(headEnd);
                headEndTpId = parser.parseTpId(headEnd);
                TerminationPointBuilder tpBuilder = parser.parseTerminationPointBuilder(headEnd);
                InstanceIdentifier<TerminationPoint> tpInstanceId = topologyInstanceId
                        .child(Node.class, new NodeKey(headNodeId))
                        .child(TerminationPoint.class, tpBuilder.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, tpInstanceId, tpBuilder.build());
            }

            NodeId tailNodeId = null;
            TpId tailEndTpId = null;
            FaEndPoint tailEnd = parser.parseTailEnd(input);
            if (tailEnd != null) {
                tailNodeId = parser.parseNodeId(tailEnd);
                tailEndTpId = parser.parseTpId(tailEnd);
                TerminationPointBuilder tpBuilder = parser.parseTerminationPointBuilder(tailEnd);
                InstanceIdentifier<TerminationPoint>  tpInstanceId = topologyInstanceId
                        .child(Node.class, new NodeKey(tailNodeId))
                        .child(TerminationPoint.class, tpBuilder.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, tpInstanceId, tpBuilder.build());
            }

            FaOperStatus faOperStatus = input.getOperStatus();
            DirectionalityInfo directionalityInfo = input.getDirectionalityInfo();

            if (FaOperStatus.forValue(faOperStatus.getIntValue()) == FaOperStatus.Up) {
                boolean bidirFlag = false;
                if (directionalityInfo instanceof Bidirectional) {
                    bidirFlag = true;
                }
                createLink(transaction, input.getFaId(), topologyInstanceId, linkBuilder, bidirFlag);
            } else {
                LOG.info("MultilayerTopologyProvider.forwardingAdjUpdate RPC: delete link {}",
                        linkInstanceId.toString());
                transaction.delete(LogicalDatastoreType.OPERATIONAL, linkInstanceId);
                if (directionalityInfo instanceof Bidirectional) {
                    linkBuilder = parser.swapSourceDestination(linkBuilder, true);
                    linkInstanceId = topologyInstanceId.child(Link.class, linkBuilder.getKey());
                    LOG.info("MultilayerTopologyProvider.forwardingAdjUpdate RPC: delete link {}",
                            linkInstanceId.toString());
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, linkInstanceId);
                    parser.swapSourceDestination(linkBuilder, false);
                }
            }

            transaction.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            LOG.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: TransactionCommitFailedException ", e);
            transactionChain.close();
            transactionChain = dataProvider.createTransactionChain(this);
            faAdjAnnUpdBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                    .withResult(faAdjAnnUpdBuilder.build()).build());
        } catch (final Exception e) {
            LOG.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: Exception ", e);
            transactionChain.close();
            transactionChain = dataProvider.createTransactionChain(this);
            faAdjAnnUpdBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                    .withResult(faAdjAnnUpdBuilder.build()).build());
        }

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update
                .output.result.OkBuilder okBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight
                .topology.multilayer.rev150123.forwarding.adj.update.output.result.OkBuilder();
        faAdjAnnUpdBuilder.setResult(okBuilder.build());
        return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> success()
            .withResult(faAdjAnnUpdBuilder.build()).build());
    }

    @Override
    public Future<RpcResult<ForwardingAdjWithdrawOutput>> forwardingAdjWithdraw(ForwardingAdjWithdrawInput input) {
        LOG.info("MultilayerTopologyProvider.forwardingAdjWithdraw RPC");

        InstanceIdentifier<?> iid = input.getNetworkTopologyRef().getValue();
        TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
        InstanceIdentifier<Topology> topologyInstanceId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, topologyKey);

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output
                .result.OkBuilder okBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer
                        .rev150123.forwarding.adj.withdraw.output.result.OkBuilder();
        ForwardingAdjWithdrawOutputBuilder faAdjWithdrawOutputBuilder = new ForwardingAdjWithdrawOutputBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output
                .result.error.ErrorBuilder iErrorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology
                .multilayer.rev150123.forwarding.adj.withdraw.output.result.error.ErrorBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output
                .result.ErrorBuilder errorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology
                .multilayer.rev150123.forwarding.adj.withdraw.output.result.ErrorBuilder();
        errorBuilder.setError(iErrorBuilder.build());

        List<LinkId> aLinkToExclude = new ArrayList();
        FaId faId = input.getFaId();
        DirectionalityInfo directionalityInfo = parser.parseDirection(faId);
        LinkId linkId = parser.parseLinkId(faId, false);
        aLinkToExclude.add(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        InstanceIdentifier<Link> instanceId = topologyInstanceId.child(Link.class, linkKey);
        Link faLink = null;

        try {
            Optional<Link> linkObject = null;
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            linkObject = rx.read(LogicalDatastoreType.OPERATIONAL, instanceId).get();
            if (linkObject != null && linkObject.isPresent()) {
                faLink = linkObject.get();
            }
        } catch (InterruptedException e) {
            LOG.error("MultilayerTopologyProvider.forwardingAdjWithdraw RPC: interrupted exception", e);
            faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                    .withResult(faAdjWithdrawOutputBuilder.build()).build());
        } catch (ExecutionException e) {
            LOG.error("MultilayerTopologyProvider.forwardingAdjWithdraw RPC: execution exception", e);
            faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                    .withResult(faAdjWithdrawOutputBuilder.build()).build());
        }

        LOG.info("MultilayerTopologyProvider.forwardingAdjWithdraw RPC: linkid {}", linkId.getValue().toString());

        if (forwardingAdjacencyProvider != null) {
            forwardingAdjacencyProvider.onForwardingAdjacencyDeleted(LogicalDatastoreType.OPERATIONAL,
                    topologyInstanceId, faId);
        }

        WriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceId);
        if (directionalityInfo instanceof Bidirectional) {
            linkId = parser.parseLinkId(faId, true);
            aLinkToExclude.add(linkId);
            linkKey = new LinkKey(linkId);
            instanceId = topologyInstanceId.child(Link.class, linkKey);
            transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceId);
        }

        if (faLink != null) {
            TpId tpToDelete = faLink.getSource().getSourceTp();
            TerminationPointKey tpKey = new TerminationPointKey(tpToDelete);
            NodeKey nodeKey = new NodeKey(faLink.getSource().getSourceNode());
            if (forwardingAdjacencyProvider != null) {
                forwardingAdjacencyProvider.onTpDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId,
                        nodeKey, tpKey);
            }
            tpToDelete = faLink.getDestination().getDestTp();
            tpKey = new TerminationPointKey(tpToDelete);
            nodeKey = new NodeKey(faLink.getDestination().getDestNode());
            if (forwardingAdjacencyProvider != null) {
                forwardingAdjacencyProvider.onTpDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId,
                        nodeKey, tpKey);
            }
        }

        try {
            transaction.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            LOG.warn("MultilayerTopologyProvider.forwardingAdjWithdraw RPC: TransactionCommitFailedException ", e);
            transactionChain.close();
            transactionChain = dataProvider.createTransactionChain(this);
            faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                    .withResult(faAdjWithdrawOutputBuilder.build()).build());
        }

        removeLinkFaDownState(faId.toString());

        faAdjWithdrawOutputBuilder.setResult(okBuilder.build());
        return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> success()
                .withResult(faAdjWithdrawOutputBuilder.build()).build());
    }
}
