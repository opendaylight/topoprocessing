/*
 * Copyright (c)2014 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.multilayer;

import com.google.common.base.Optional;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyOperation;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.multitechnology.topology.type.MultitechnologyTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.rev150122.MtTopologyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MultilayerTopologyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjAnnounceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjUpdateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.ForwardingAdjWithdrawOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.FaEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.end.point.StitchingPoint;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.DirectionalityInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.fa.parameters.directionality.info.Bidirectional;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MultilayerTopologyContext;

import com.google.common.util.concurrent.Futures;

public class MultilayerTopologyProvider implements MultilayerTopologyProviderRuntimeMXBean,
        AutoCloseable, MlmtTopologyProvider, MultilayerTopologyService, TransactionChainListener {
    private Logger log;
    private DataBroker dataProvider;
    private MlmtOperationProcessor processor;
    private InstanceIdentifier<Topology> destTopologyId;
    private MultilayerAttributesParser parser;
    private BindingTransactionChain transactionChain;

    public void init(final Logger logger, MlmtOperationProcessor processor, InstanceIdentifier<Topology> destTopologyId,
            final MultilayerAttributesParser parser) {
        logger.info("MultilayerTopologyProvider.init");
        this.log = logger;
        this.destTopologyId = destTopologyId;
        this.processor = processor;
        this.parser = parser;
    }

    public void setDataProvider(DataBroker dataProvider) {
        log.info("MultilayerTopologyProvider.setDataProvider");
        this.dataProvider = dataProvider;
        this.transactionChain = dataProvider.createTransactionChain(this);
    }

    public void registerRpcImpl(final RpcProviderRegistry rpcProviderRegistry, InstanceIdentifier<Topology> mlmtTopologyId) {
        log.info("MultilayerTopologyProvider.registerRpcImpl " + mlmtTopologyId.toString());
        RoutedRpcRegistration reg = rpcProviderRegistry.addRoutedRpcImplementation(MultilayerTopologyService.class, this);
        reg.registerPath(MultilayerTopologyContext.class, mlmtTopologyId);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        //NOOP
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction, Throwable cause) {
        log.error("Failed to export MultilayerTopologyProvider operations, Transaction {} failed.", transaction.getIdentifier(), cause);
        transactionChain.close();
        transactionChain = dataProvider.createTransactionChain(this);
    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        log.info("MultilayerTopologyProvider.onTopologyCreated");
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
                              final InstanceIdentifier<Topology> topologyInstanceId,
                              final Node node) {
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type,
                            final InstanceIdentifier<Topology> topologyInstanceId,
                            final NodeKey nodeKey,
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
        log.info("MultilayerTopologyProvider stopped.");
    }

    private Future<RpcResult<ForwardingAdjAnnounceOutput>> createMtLink(ForwardingAdjAnnounceInput input) {
        log.info("MultilayerTopologyProvider.createMtLink");
        FaEndPoint headEnd = parser.parseHeadEnd(input);
        NodeId headNodeId = parser.parseNodeId(headEnd);
        TpId headEndTpId = parser.parseTpId(headEnd);
        TerminationPointBuilder tpBuilder = parser.parseTerminationPointBuilder(headEnd);
        InstanceIdentifier<TerminationPoint> tpInstanceId = destTopologyId
                .child(Node.class, new NodeKey(headNodeId))
                .child(TerminationPoint.class, tpBuilder.getKey());
        WriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.OPERATIONAL, tpInstanceId, tpBuilder.build());

        FaEndPoint tailEnd = parser.parseTailEnd(input);
        NodeId tailNodeId = parser.parseNodeId(tailEnd);
        TpId tailEndTpId = parser.parseTpId(tailEnd);
        if (tailEndTpId != null) {
            tpBuilder = parser.parseTerminationPointBuilder(tailEnd);
            tpInstanceId = destTopologyId.child(Node.class, new NodeKey(tailNodeId))
                     .child(TerminationPoint.class, tpBuilder.getKey());
            transaction.merge(LogicalDatastoreType.OPERATIONAL, tpInstanceId, tpBuilder.build());
        }

        DirectionalityInfo directionalityInfo = input.getDirectionalityInfo();
        boolean bidirFlag = false;
        if (directionalityInfo instanceof Bidirectional) {
            bidirFlag = true;
        }

        InstanceIdentifier<?> iid = input.getNetworkTopologyRef().getValue();
        TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
        String topologyName = topologyKey.getTopologyId().getValue();

        String faId = parser.parseFaId(bidirFlag, false, topologyName);
        final FaId outFaId = new FaId(faId);
        LinkBuilder linkBuilder = parser.parseLinkBuilder(input, faId);
        InstanceIdentifier<Link> linkInstanceId = destTopologyId.child(Link.class, linkBuilder.getKey());
        transaction.merge(LogicalDatastoreType.OPERATIONAL, linkInstanceId, linkBuilder.build());
        if (bidirFlag) {
            linkBuilder = parser.swapSourceDestination(linkBuilder);
            linkInstanceId = destTopologyId.child(Link.class, linkBuilder.getKey());
            transaction.merge(LogicalDatastoreType.OPERATIONAL, linkInstanceId, linkBuilder.build());
        }

        try {
            transaction.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            log.warn("MultilayerTopologyProvider.createMtLink: TransactionCommitFailedException ", e);
            transactionChain.close();
            transactionChain = dataProvider.createTransactionChain(this);
            ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
            faAdjAnnOutputBuilder.setResult(new NoneBuilder().setNone(true).build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> failed()
                    .withResult(faAdjAnnOutputBuilder.build()).build());
        }

        ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
        faAdjAnnOutputBuilder.setResult(new FaIdBuilder().setFaId(outFaId).build());
        return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> success()
                .withResult(faAdjAnnOutputBuilder.build()).build());
     }

     @Override
     public Future<RpcResult<ForwardingAdjAnnounceOutput>> forwardingAdjAnnounce(ForwardingAdjAnnounceInput input) {
        log.info("MultilayerTopologyProvider.forwardingAdjAnnounce RPC");
        StitchingPoint headStitchingPoint = input.getHeadEnd().getStitchingPoint();
        StitchingPoint tailStitchingPoint = input.getTailEnd().getStitchingPoint();
        if (headStitchingPoint == null && tailStitchingPoint == null) {
            return createMtLink(input);
        }

        ForwardingAdjAnnounceOutputBuilder faAdjAnnOutputBuilder = new ForwardingAdjAnnounceOutputBuilder();
        faAdjAnnOutputBuilder.setResult(new NoneBuilder().setNone(true).build());
        return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjAnnounceOutput> failed()
                 .withResult(faAdjAnnOutputBuilder.build()).build());
     }

    @Override
    public Future<RpcResult<ForwardingAdjUpdateOutput>> forwardingAdjUpdate(ForwardingAdjUpdateInput input) {
        log.info("MultilayerTopologyProvider.forwardingAdjUpdate RPC");

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.error.ErrorBuilder
                iErrorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.error.ErrorBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.ErrorBuilder
                errorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.update.output.result.ErrorBuilder();
        errorBuilder.setError(iErrorBuilder.build());
        ForwardingAdjUpdateOutputBuilder faAdjAnnUpdBuilder = new ForwardingAdjUpdateOutputBuilder();

        final String faId = input.getFaId().getValue().toString();
        LinkBuilder linkBuilder = parser.parseLinkBuilder(input, faId);
        InstanceIdentifier<Link> linkInstanceId = destTopologyId.child(Link.class, linkBuilder.getKey());

        try {
            Optional<Link> linkObject = null;
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            linkObject = rx.read(LogicalDatastoreType.OPERATIONAL, linkInstanceId).get();
            if (linkObject == null) {
                log.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: source linkObject null\n");
                faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                        .withResult(faAdjAnnUpdBuilder.build()).build());
            }
            if (linkObject.isPresent() == false) {
                log.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: linkObject not present\n");
                faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                        .withResult(faAdjAnnUpdBuilder.build()).build());
            }
            Link faLink = linkObject.get();
            if (faLink == null){
                log.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: dest faLink with faId " +
                        faId + " not found");
                faAdjAnnUpdBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                        .withResult(faAdjAnnUpdBuilder.build()).build());
            }
        } catch (InterruptedException e) {
            log.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: interrupted exception", e);
            faAdjAnnUpdBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                    .withResult(faAdjAnnUpdBuilder.build()).build());
        } catch (ExecutionException e) {
            log.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: execution exception", e);
            faAdjAnnUpdBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                    .withResult(faAdjAnnUpdBuilder.build()).build());
        }

        FaEndPoint headEnd = parser.parseHeadEnd(input);
        NodeId headNodeId = parser.parseNodeId(headEnd);
        TpId headEndTpId = parser.parseTpId(headEnd);
        TerminationPointBuilder tpBuilder = parser.parseTerminationPointBuilder(headEnd);
        InstanceIdentifier<TerminationPoint> tpInstanceId = destTopologyId
                .child(Node.class, new NodeKey(headNodeId))
                .child(TerminationPoint.class, tpBuilder.getKey());
        WriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.OPERATIONAL, tpInstanceId, tpBuilder.build());

        FaEndPoint tailEnd = parser.parseTailEnd(input);
        NodeId tailNodeId = parser.parseNodeId(tailEnd);
        TpId tailEndTpId = parser.parseTpId(tailEnd);
        tpBuilder = parser.parseTerminationPointBuilder(tailEnd);
        tpInstanceId = destTopologyId.child(Node.class, new NodeKey(tailNodeId))
                .child(TerminationPoint.class, tpBuilder.getKey());
        transaction.merge(LogicalDatastoreType.OPERATIONAL, tpInstanceId, tpBuilder.build());

        transaction.merge(LogicalDatastoreType.OPERATIONAL, linkInstanceId, linkBuilder.build());

        DirectionalityInfo directionalityInfo = input.getDirectionalityInfo();
        if (directionalityInfo instanceof Bidirectional) {
            linkBuilder = parser.swapSourceDestination(linkBuilder);
            linkInstanceId = destTopologyId.child(Link.class, linkBuilder.getKey());
            transaction.merge(LogicalDatastoreType.OPERATIONAL, linkInstanceId, linkBuilder.build());
        }

        try {
            transaction.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
             log.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: TransactionCommitFailedException ", e);
             transactionChain.close();
             transactionChain = dataProvider.createTransactionChain(this);
             faAdjAnnUpdBuilder.setResult(errorBuilder.build());
             return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> failed()
                     .withResult(faAdjAnnUpdBuilder.build()).build());
        }

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.OkBuilder
               okBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.OkBuilder();
        ForwardingAdjWithdrawOutputBuilder faAdjWithdrawOutputBuilder = new ForwardingAdjWithdrawOutputBuilder();
        faAdjWithdrawOutputBuilder.setResult(okBuilder.build());
        return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjUpdateOutput> success()
                .withResult(faAdjAnnUpdBuilder.build()).build());
    }

    @Override
    public Future<RpcResult<ForwardingAdjWithdrawOutput>> forwardingAdjWithdraw(ForwardingAdjWithdrawInput input)  {
        log.info("MultilayerTopologyProvider.forwardingAdjWithdraw RPC");

        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.OkBuilder
               okBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.OkBuilder();
        ForwardingAdjWithdrawOutputBuilder faAdjWithdrawOutputBuilder = new ForwardingAdjWithdrawOutputBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.error.ErrorBuilder
                iErrorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.error.ErrorBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.ErrorBuilder
                errorBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.forwarding.adj.withdraw.output.result.ErrorBuilder();
        errorBuilder.setError(iErrorBuilder.build());

        FaId faId = input.getFaId();
        DirectionalityInfo directionalityInfo = parser.parseDirection(faId);
        LinkId linkId = new LinkId(faId.getValue());
        LinkKey linkKey = new LinkKey(linkId);
        InstanceIdentifier<Link> instanceId = destTopologyId.child(Link.class, linkKey);
        Link faLink = null;

        try {
            Optional<Link> linkObject = null;
            final ReadOnlyTransaction rx = dataProvider.newReadOnlyTransaction();
            linkObject = rx.read(LogicalDatastoreType.OPERATIONAL, instanceId).get();
            if (linkObject == null) {
                log.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: source linkObject null\n");
                faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                        .withResult(faAdjWithdrawOutputBuilder.build()).build());
            }
            if (linkObject.isPresent() == false) {
                log.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: linkObject not present\n");
                faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                        .withResult(faAdjWithdrawOutputBuilder.build()).build());
            }
            faLink = linkObject.get();
            if (faLink == null){
                log.warn("MultilayerTopologyProvider.forwardingAdjUpdate RPC: dest faLink with faId " +
                        faId + " not found");
                faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                        .withResult(faAdjWithdrawOutputBuilder.build()).build());
            }
        } catch (InterruptedException e) {
            log.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: interrupted exception", e);
            faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                    .withResult(faAdjWithdrawOutputBuilder.build()).build());
        } catch (ExecutionException e) {
            log.error("MultilayerTopologyProvider.forwardingAdjUpdate RPC: execution exception", e);
            faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
            return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                    .withResult(faAdjWithdrawOutputBuilder.build()).build());
        }

        log.info("MultilayerTopologyProvider.forwardingAdjWithdraw RPC: linkid " + linkId.getValue().toString());

        WriteTransaction transaction = transactionChain.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceId);

        if (directionalityInfo instanceof Bidirectional) {
            String strFaId = parser.parseFaId(faId, true, true);
            if (strFaId != null) {
                linkId = new LinkId(strFaId);
                linkKey = new LinkKey(linkId);
                instanceId = destTopologyId.child(Link.class, linkKey);
                transaction.delete(LogicalDatastoreType.OPERATIONAL, instanceId);
            }
        }

        TerminationPointKey tpKey = new TerminationPointKey(faLink.getSource().getSourceTp());
        NodeKey nodeKey = new NodeKey(faLink.getSource().getSourceNode());
        InstanceIdentifier<TerminationPoint> tpInstanceId = destTopologyId.child(Node.class, nodeKey)
                .child(TerminationPoint.class, tpKey);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, tpInstanceId);

        tpKey = new TerminationPointKey(faLink.getDestination().getDestTp());
        nodeKey = new NodeKey(faLink.getDestination().getDestNode());
        tpInstanceId = destTopologyId.child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, tpInstanceId);

        try {
            transaction.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            log.warn("MultilayerTopologyProvider.forwardingAdjWithdraw RPC: TransactionCommitFailedException ", e);
            transactionChain.close();
            transactionChain = dataProvider.createTransactionChain(this);
            faAdjWithdrawOutputBuilder.setResult(errorBuilder.build());
                    return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> failed()
                            .withResult(faAdjWithdrawOutputBuilder.build()).build());
        }

        faAdjWithdrawOutputBuilder.setResult(okBuilder.build());
                return Futures.immediateFuture(RpcResultBuilder.<ForwardingAdjWithdrawOutput> success()
                        .withResult(faAdjWithdrawOutputBuilder.build()).build());
    }
}
