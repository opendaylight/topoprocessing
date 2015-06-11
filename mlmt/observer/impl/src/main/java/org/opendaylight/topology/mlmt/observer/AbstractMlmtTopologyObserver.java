/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.observer;

import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyBuilder;
import org.opendaylight.topology.mlmt.utility.MlmtProviderFactory;
import org.opendaylight.topology.mlmt.utility.MlmtConsequentAction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMlmtTopologyObserver implements DataChangeListener, MlmtTopologyProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractMlmtTopologyObserver.class);
    protected InstanceIdentifier<Topology> mlmtTopologyId;
    protected ListenerRegistration<DataChangeListener> listenerRegistration;
    protected DataBroker dataBroker;
    protected MlmtTopologyBuilder mlmtTopologyBuilder;
    protected MlmtProviderFactory mlmtProviderFactory;
    protected List<MlmtTopologyProvider> mlmtProviders;

    protected List<String> underlayTopologies;
    protected static final String MLMT = "mlmt:1";

    public enum MlmtDataChangeEventType {
        CREATED,
        UPDATED,
        DELETED;

        @Override
        public  String toString() {
            if (this == CREATED) {
                return ("Created");
            } else if (this == UPDATED) {
                return ("Updated");
            } else if (this == DELETED) {
                return ("Deleted");
            }
            return null;
        }
    }

    public abstract void init(DataBroker dataBroker, RpcProviderRegistry rpcRegistry);

    protected InstanceIdentifier<Topology> buildTopologyIid(final String topologyName) {
        TopologyId tid = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
    }

    private void buildTopology(final LogicalDatastoreType type, final InstanceIdentifier<Topology> buildTopologyId,
            Topology topology) {
        List<Node> lNode = topology.getNode();
        List<Link> lLink = topology.getLink();
        if (lNode != null && !lNode.isEmpty()) {
            for (Node node : lNode) {
                mlmtTopologyBuilder.createNode(LogicalDatastoreType.OPERATIONAL, buildTopologyId,
                        topology.getTopologyId(), node);
                for (MlmtTopologyProvider provider : mlmtProviders) {
                   provider.onNodeCreated(type, buildTopologyId, node);
                }
                List<TerminationPoint> lTp = node.getTerminationPoint();
                if (lTp != null && !lTp.isEmpty()) {
                    for (TerminationPoint tp : lTp) {
                        for (MlmtTopologyProvider provider : mlmtProviders) {
                            provider.onTpCreated(type, buildTopologyId, node.getKey(), tp);
                        }
                    }
                }
            }
        }
        if (lLink != null && !lLink.isEmpty()) {
            for (Link link : lLink) {
                mlmtTopologyBuilder.createLink(LogicalDatastoreType.OPERATIONAL, buildTopologyId, link);
                   for (MlmtTopologyProvider provider : mlmtProviders) {
                       provider.onLinkCreated(type, buildTopologyId, link);
                }
            }
        }
    }

    private void copyTopology(final LogicalDatastoreType type, final InstanceIdentifier<Topology> copyTopologyId,
            Topology topology) {
        List<Node> lNode = topology.getNode();
        List<Link> lLink = topology.getLink();
        if (lNode != null && !lNode.isEmpty()) {
            for (Node node : lNode) {
                mlmtTopologyBuilder.copyNode(LogicalDatastoreType.OPERATIONAL, copyTopologyId,
                        topology.getTopologyId(), node);
                List<TerminationPoint> lTp = node.getTerminationPoint();
                if (lTp != null && !lTp.isEmpty()) {
                    for (TerminationPoint tp : lTp) {
                        mlmtTopologyBuilder.copyTp(LogicalDatastoreType.OPERATIONAL, copyTopologyId,
                                node.getKey(), tp);
                    }
                }
            }
        }
        if (lLink != null && !lLink.isEmpty()) {
            for (Link link : lLink) {
                mlmtTopologyBuilder.copyLink(LogicalDatastoreType.OPERATIONAL, copyTopologyId, link);
            }
        }
    }

    protected void onObservedTopologyCreated(final LogicalDatastoreType type,
        final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver.onObservedTopologyCreated topologyInstanceId " + topologyInstanceId.toString());
        MlmtConsequentAction mlmtConsequentAction = MlmtConsequentAction.BUILD;
        if (topology.getTopologyTypes() != null) {
            mlmtConsequentAction = mlmtProviderFactory.consequentAction(topology.getTopologyTypes());
        }
        if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
             buildTopology(type, mlmtTopologyId, topology);
        } else if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
             copyTopology(type, mlmtTopologyId, topology);
        }
    }

    private void resyncMlmt(String underlayTopologyName) {
        TopologyId topologyId = new TopologyId(underlayTopologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        InstanceIdentifier<Topology> topologyInstanceId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, key);

        try {
            ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            Optional<Topology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, topologyInstanceId).get();
            if (optional.isPresent()) {
                Topology rxTopology = optional.get();
                if (rxTopology != null) {
                    onObservedTopologyCreated(LogicalDatastoreType.CONFIGURATION, topologyInstanceId, rxTopology);
                }
            }
            optional = rTx.read(LogicalDatastoreType.OPERATIONAL, topologyInstanceId).get();
            if (optional.isPresent()) {
                Topology rxTopology = optional.get();
                if (rxTopology != null) {
                    onObservedTopologyCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, rxTopology);
                }
            }
        } catch (InterruptedException e) {
          LOG.error("MlmtTopologyObserver.getMlmtConsequentAction interrupted exception", e);
        } catch (ExecutionException e) {
          LOG.error("MlmtTopologyObserver.getMlmtConsequentAction execution exception", e);
        }
    }

    protected void onMlmtTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver::onMlmtTopologyCreated topologyInstanceId " + topologyInstanceId.toString());

        mlmtTopologyBuilder.copyTopology(LogicalDatastoreType.CONFIGURATION, topologyInstanceId,
                LogicalDatastoreType.OPERATIONAL);

        List<UnderlayTopology> lUnderlay = topology.getUnderlayTopology();
        if (lUnderlay != null) {
            for (UnderlayTopology underlayTopology : lUnderlay) {
                TopologyId underlayTopologyId = underlayTopology.getTopologyRef();
                String pattern = "(?<=topology-id=\')(.*)(?=\'])";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(underlayTopologyId.getValue().toString());
                if (!m.find()) {
                    continue;
                }
                String topologyName = new String(m.group(1));
                LOG.debug("MlmtTopologyObserver.onMlmtTopologyCreated underlay topology name = " + topologyName);
                underlayTopologies.add(topologyName);
                TopologyId topologyId = new TopologyId(topologyName);
                TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
                InstanceIdentifier<Topology> topologyIid = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
                listenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        topologyIid, this, DataBroker.DataChangeScope.SUBTREE);
                listenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                        topologyIid, this, DataBroker.DataChangeScope.SUBTREE);
            }
        }
    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver.onTopologyCreated topologyInstanceId: " + topologyInstanceId.toString());
        if (topologyInstanceId.equals(mlmtTopologyId)) {
            onMlmtTopologyCreated(type, topologyInstanceId, topology);
        }
        String observedTopologyName = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class).getTopologyId().getValue();
        if (underlayTopologies.contains(observedTopologyName)) {
            onObservedTopologyCreated(type, topologyInstanceId, topology);
        }
    }

    protected MlmtConsequentAction getMlmtConsequentAction (final InstanceIdentifier<Topology> topologyInstanceId) {
        MlmtConsequentAction mlmtConsequentAction = MlmtConsequentAction.BUILD;
        try {
            ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            Optional<Topology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, topologyInstanceId).get();
            if (optional.isPresent()) {
                Topology rxTopology = optional.get();
                if (rxTopology != null & rxTopology.getTopologyTypes() != null) {
                    mlmtConsequentAction = mlmtProviderFactory.consequentAction(rxTopology.getTopologyTypes());
                }
            }
            else {
                optional = rTx.read(LogicalDatastoreType.OPERATIONAL, topologyInstanceId).get();
                if (optional.isPresent()) {
                    Topology rxTopology = optional.get();
                    if (rxTopology != null & rxTopology.getTopologyTypes() != null) {
                        mlmtConsequentAction = mlmtProviderFactory.consequentAction(rxTopology.getTopologyTypes());
                    }
                }
            }
        } catch (InterruptedException e) {
          LOG.error("MlmtTopologyObserver.getMlmtConsequentAction interrupted exception", e);
        } catch (ExecutionException e) {
          LOG.error("MlmtTopologyObserver.getMlmtConsequentAction execution exception", e);
        }

        return mlmtConsequentAction;
    }

    @Override
    public void onNodeCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        LOG.info("MlmtTopologyObserver.onNodeCreated topologyInstanceId: " + topologyInstanceId.toString() +
                " nodeKey: " + node.getKey().toString());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
            mlmtTopologyBuilder.copyNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                    topologyKey.getTopologyId(), node);
            return;
        }
        else if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
            TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
            mlmtTopologyBuilder.createNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                    topologyKey.getTopologyId(), node);
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onNodeCreated(type, topologyInstanceId, node);
        }
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPoint tp) {
        LOG.info("MlmtTopologyObserver.onTpCreated topologyInstanceId: " + topologyInstanceId.toString() +
                " nodeKey: " + nodeKey.toString() + " terminationPointId: " + tp.getTpId());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            mlmtTopologyBuilder.copyTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tp);
            return;
        }
        else if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
            mlmtTopologyBuilder.createTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tp);
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpCreated(type, topologyInstanceId, nodeKey, tp);
        }
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        LOG.info("MlmtTopologyObserver.onLinkCreated topologyInstanceId: " + topologyInstanceId.toString() +
                " linkId: " + link.getLinkId());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            mlmtTopologyBuilder.copyLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, link);
            return;
        }
        else if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
            mlmtTopologyBuilder.createLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, link);
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkCreated(type, topologyInstanceId, link);
        }
    }

    @Override
    public void onTopologyUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        LOG.info("MlmtTopologyObserver.onTopologyUpdated topologyInstanceId: " + topologyInstanceId.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyUpdated(type, topologyInstanceId, topology);
        }
    }

    @Override
    public void onNodeUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
         LOG.info("MlmtTopologyObserver.onNodeUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                 " nodeKey: " + node.getKey().toString());
         MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
         if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
             TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
             mlmtTopologyBuilder.copyNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                     topologyKey.getTopologyId(), node);
             return;
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onNodeUpdated(type, topologyInstanceId, node);
        }
    }

    @Override
    public void onTpUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPoint tp) {
        LOG.info("MlmtTopologyObserver.onTpUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                " nodeKey: " + nodeKey.toString() + " terminationPointKey " + tp.getKey());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            mlmtTopologyBuilder.copyTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tp);
            return;
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpUpdated(type, topologyInstanceId, nodeKey, tp);
        }
    }

    @Override
    public void onLinkUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        LOG.info("MlmtTopologyObserver.onLinkUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                " linkKey: " + link.getKey().toString());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            mlmtTopologyBuilder.copyLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, link);
            return;
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkUpdated(type, topologyInstanceId, link);
        }
    }

    @Override
    public void onTopologyDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId) {
        LOG.info("MlmtTopologyObserver.onTopologyDeleted topologyInstanceId: " + topologyInstanceId.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyDeleted(type, topologyInstanceId);
        }
        mlmtTopologyBuilder.deleteTopology(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId);
        mlmtTopologyBuilder.copyTopology(LogicalDatastoreType.CONFIGURATION, mlmtTopologyId, LogicalDatastoreType.OPERATIONAL);
    }

    @Override
    public void onNodeDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey) {
         LOG.info("MlmtTopologyObserver.onNodeDeleted topologyInstanceId: " + topologyInstanceId.toString() +
                 " nodeKey: " + nodeKey.toString());
         for (MlmtTopologyProvider provider : mlmtProviders) {
             provider.onNodeDeleted(type, topologyInstanceId, nodeKey);
         }
         mlmtTopologyBuilder.deleteNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey);
    }

    @Override
    public void onTpDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPointKey tpKey) {
         LOG.info("MlmtTopologyObserver.onTpDeleted topologyInstanceId: " + topologyInstanceId.toString() +
                 " nodeKey: " + nodeKey.toString() + " tpKey: " + tpKey.toString());
         for (MlmtTopologyProvider provider : mlmtProviders) {
             provider.onTpDeleted(type, topologyInstanceId, nodeKey, tpKey);
         }
         mlmtTopologyBuilder.deleteTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tpKey);
    }

    @Override
    public void onLinkDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey) {
        LOG.info("MlmtTopologyObserver.onLinkDeleted topologyInstanceId: " + topologyInstanceId.toString() +
                " linkKey: " + linkKey.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkDeleted(type, topologyInstanceId, linkKey);
        }
        mlmtTopologyBuilder.deleteLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, linkKey);
    }

    protected void dumpMap(final Map<InstanceIdentifier<?>, DataObject> map, MlmtDataChangeEventType type){
        LOG.debug("---" + type.toString() + "---");
        Iterator<InstanceIdentifier<?>> iter = map.keySet().iterator();
        while (iter.hasNext()){
            try {
                InstanceIdentifier<?> iid = iter.next();
                LOG.debug("Key: " + iid);
                DataObject d = Preconditions.checkNotNull(map.get(iid));
                if (d != null && d instanceof java.io.Serializable) {
                    LOG.debug("Value: " + d.toString());
                }
            } catch (final IllegalArgumentException e ) {
               LOG.error("MlmtTopologyObserver.dumpMap: IllegalArgumentException", e);
            } catch (final java.lang.UnsupportedOperationException e ) {
               LOG.error("MlmtTopologyObserver.dumpMap: UnsupportedOperationException", e);
            } catch (final IllegalStateException e ) {
               LOG.error("MlmtTopologyObserver.dumpMap: IllegalStateException", e);
            }
         }
         LOG.debug("-------------");
     }

    /*
     * create and update events
     */
    protected void handleData(final Map<InstanceIdentifier<?>, DataObject> c , final MlmtDataChangeEventType type) {
        Iterator<InstanceIdentifier<?>> iter = c.keySet().iterator();
        String changeType = type.toString();
        while (iter.hasNext()) {
            InstanceIdentifier<?> iid = iter.next();
            TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
            String topologyName = topologyKey.getTopologyId().getValue();
            LOG.info("MlmtTopologyObserver.handleData " + changeType + " topologyName " + topologyName);
            if (iid.getTargetType().equals(Topology.class)) {
                Topology topology = (Topology)c.get(iid);
                InstanceIdentifier<Topology> topologyInstanceId =
                        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                if (underlayTopologies.contains(topologyName)) {
                    LOG.info("MlmtTopologyObserver.handleData " + changeType + " Topology Id " + MLMT);
                    onTopologyCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, topology);
                }
                if (topologyName.equals(MLMT)) {
                    LOG.info("MlmtTopologyObserver.handleData " + changeType + " Topology Id " + MLMT);
                    onTopologyCreated(LogicalDatastoreType.CONFIGURATION, mlmtTopologyId, topology);
                }
            }
            else if (iid.getTargetType().equals(Node.class)) {
                LOG.info("MlmtTopologyObserver.onDataChanged " + changeType +
                        " Node.class Topology Key " + topologyKey.toString());
                if (underlayTopologies.contains(topologyName)) {
                    InstanceIdentifier<Topology> topologyInstanceId =
                            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                    final Node node = (Node)c.get(iid);
                    final NodeId nodeId = node.getNodeId();
                    LOG.info("MlmtTopologyObserver.handleData Node class changeType " + changeType +
                            " Node Id " +  nodeId.toString());
                    onNodeCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, node);
                } // end if
            } // end if target == Node.class
            else if (iid.getTargetType().equals(TerminationPoint.class)) {
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                LOG.info("MlmtTopologyObserver.handleData TerminationPoint class changeType " + changeType +
                       " Topology Key " + topologyKey.toString() +
                       " Node Key " + nodeKey.toString());
                if (underlayTopologies.contains(topologyName)) {
                     InstanceIdentifier<Topology> topologyInstanceId =
                             InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                     final TerminationPoint tp = (TerminationPoint)c.get(iid);
                     final TpId tpId = tp.getTpId();
                     LOG.info("MlmtTopologyObserver.onDataChanged Node Id " + changeType +
                             " Node.class Node Id " +  tpId.toString());
                     onTpCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, nodeKey, tp);
                } // end underlayTopologies
            } // end if target == TerminationPoint.class
            else if (iid.getTargetType().equals(Link.class)) {
                LinkKey linkKey = iid.firstKeyOf(Link.class, LinkKey.class);
                LOG.info("MlmtTopologyObserver.handleData Link.class changeType" + changeType +
                        " Topology Key " + topologyKey.toString() +
                        " Link Key " + linkKey.toString());
                if (underlayTopologies.contains(topologyName)) {
                    InstanceIdentifier<Topology> topologyInstanceId =
                            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                    final Link link = (Link)c.get(iid);
                    final LinkId linkId = link.getLinkId();
                    LOG.debug("MlmtTopologyObserver.handleData Link.class type " + type.toString() +
                            " linkId " + linkId.toString());
                    onLinkCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, link);
                 } // end if underlayTopologies
            } // end if target == Link.class
        } // end while
    }

    /*
     * delete events
     */
    protected void handleData(final Set<InstanceIdentifier<?>> c) {
        Iterator<InstanceIdentifier<?>> iter = c.iterator();
        while (iter.hasNext()) {
            InstanceIdentifier<?> iid = iter.next();
            TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
            LOG.info("Removed Key " + iid.toString());
            if (iid.getTargetType().equals(Topology.class)) {
                LOG.info("MlmtTopologyObserver removed Key Topology.class");
                InstanceIdentifier<Topology> topologyInstanceId =
                        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                onTopologyDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId);
            }
            else if (iid.getTargetType().equals(Node.class)) {
                LOG.info("MlmtTopologyObserver removed Key Node.class");
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                InstanceIdentifier<Topology> topologyInstanceId =
                        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                onNodeDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, nodeKey);
            }
            else if (iid.getTargetType().equals(TerminationPoint.class)) {
                LOG.info("MlmtTopologyObserver removed Key Tp.class");
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                TerminationPointKey tpKey = iid.firstKeyOf(TerminationPoint.class, TerminationPointKey.class);
                InstanceIdentifier<Topology> topologyInstanceId =
                        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                onTpDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, nodeKey, tpKey);
            }
            else if (iid.getTargetType().equals(Link.class)) {
                LOG.info("MlmtTopologyObserver removed Key Link.class");
                InstanceIdentifier<Topology> topologyInstanceId =
                        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                LinkKey linkKey = iid.firstKeyOf(Link.class, LinkKey.class);
                onLinkDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, linkKey);
            }
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.info("MlmtTopologyObserver.onDataChanged");
        Preconditions.checkNotNull(change);
        Map<InstanceIdentifier<?>, DataObject> c = change.getCreatedData();
        if (c != null) {
            this.dumpMap(c, MlmtDataChangeEventType.CREATED);
            handleData(c, MlmtDataChangeEventType.CREATED);
        }
        c = change.getUpdatedData();
        if (c != null) {
            this.dumpMap(c, MlmtDataChangeEventType.UPDATED);
            handleData(c, MlmtDataChangeEventType.UPDATED);
        }
        Set<InstanceIdentifier<?>> r = change.getRemovedPaths();
        if (r != null) {
            handleData(r);
        }
    }
}
