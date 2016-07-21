/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.observer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.topology.mlmt.observer.AbstractMlmtTopologyObserver.MlmtDataChangeEventType;
import org.opendaylight.topology.mlmt.utility.MlmtConsequentAction;
import org.opendaylight.topology.mlmt.utility.MlmtDataChangeObserver;
import org.opendaylight.topology.mlmt.utility.MlmtProviderFactory;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyBuilder;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMlmtTopologyObserver implements MlmtDataChangeObserver, MlmtTopologyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMlmtTopologyObserver.class);
    protected InstanceIdentifier<Topology> mlmtTopologyId;
    protected DataBroker dataBroker;
    protected MlmtTopologyBuilder mlmtTopologyBuilder;
    protected MlmtProviderFactory mlmtProviderFactory;
    protected List<MlmtTopologyProvider> mlmtProviders;
    protected List<MlmtDataChangeEventListener> mapConfigurationDataChangeObserver;
    protected List<MlmtDataChangeEventListener> mapOperationalDataChangeObserver;

    protected List<String> underlayTopologies;
    protected static String MLMT = "mlmt:1";

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

    public abstract void init(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistry,
            final String topologyName, final List<String> underlyingTopologyName);

    protected void registerDataChangeEventListener(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyIid) {
        MlmtDataChangeEventListener dataChangeObserver = new MlmtDataChangeEventListener();
        dataChangeObserver.init(type, dataBroker, topologyIid);
        dataChangeObserver.registerObserver(this);
        if (type == LogicalDatastoreType.CONFIGURATION) {
            mapConfigurationDataChangeObserver.add(dataChangeObserver);
        } else {
            mapOperationalDataChangeObserver.add(dataChangeObserver);
        }
    }
    protected InstanceIdentifier<Topology> buildTopologyIid(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
    }

    protected boolean checkNetworkTopology(final LogicalDatastoreType type) {
        try {
            final ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            final Optional<NetworkTopology> sourceObject = rx.read(type,
                    InstanceIdentifier.create(NetworkTopology.class)).get();
            if (sourceObject == null) {
                LOG.debug("AbstractMlmtTopologyObserver:checkNetworkTopology sourceObject is null");
                return false;
            }
            if (!sourceObject.isPresent()) {
                LOG.debug("AbstractMlmtTopologyObserver:checkNetworkTopology sourceObject is not present");
                return false;
            }

            return true;
        } catch (final InterruptedException e) {
            LOG.error("AbstractMlmtTopologyObserver:checkNetworkTopology interrupted exception", e);
            return false;
        } catch (final ExecutionException e) {
            LOG.error("AbstractMlmtTopologyObserver:checkNetworkTopology execution exception", e);
            return false;
        }
    }

    private void buildTopology(final LogicalDatastoreType type, final InstanceIdentifier<Topology> buildTopologyId,
            final Topology topology) {
        TopologyKey topologyKey = buildTopologyId.firstKeyOf(Topology.class, TopologyKey.class);
        String sTopologyName = topologyKey.getTopologyId().getValue();

        List<Node> lNode = topology.getNode();
        List<Link> lLink = topology.getLink();
        if (lNode != null && !lNode.isEmpty()) {
            for (Node node : lNode) {
                mlmtTopologyBuilder.createNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                        topology.getTopologyId(), node);
                for (MlmtTopologyProvider provider : mlmtProviders) {
                    provider.onNodeCreated(type, mlmtTopologyId, node);
                }
                List<TerminationPoint> lTp = node.getTerminationPoint();
                if (lTp != null && !lTp.isEmpty()) {
                    for (TerminationPoint tp : lTp) {
                        mlmtTopologyBuilder.createTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                                node.getKey(), tp);
                        for (MlmtTopologyProvider provider : mlmtProviders) {
                            provider.onTpCreated(type, mlmtTopologyId, node.getKey(), tp);
                        }
                    }
                }
            }
        }
        if (lLink != null && !lLink.isEmpty()) {
            for (Link link : lLink) {
                mlmtTopologyBuilder.createLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, link);
                for (MlmtTopologyProvider provider : mlmtProviders) {
                    provider.onLinkCreated(type, mlmtTopologyId, link);
                }
            }
        }
    }

    private void copyTopology(final LogicalDatastoreType type, final InstanceIdentifier<Topology> copyTopologyId,
            final Topology topology) {
        final TopologyKey topologyKey = copyTopologyId.firstKeyOf(Topology.class, TopologyKey.class);
        final String sTopologyName = topologyKey.getTopologyId().getValue();

        List<Node> lNode = topology.getNode();
        List<Link> lLink = topology.getLink();
        if (lNode != null && !lNode.isEmpty()) {
            for (Node node : lNode) {
                mlmtTopologyBuilder.copyNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                        topology.getTopologyId(), node);
                List<TerminationPoint> lTp = node.getTerminationPoint();
                if (lTp != null && !lTp.isEmpty()) {
                    for (TerminationPoint tp : lTp) {
                        mlmtTopologyBuilder.copyTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
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
        LOG.debug("MlmtTopologyObserver.onObservedTopologyCreated topologyInstanceId {}",
                topologyInstanceId.toString());
        MlmtConsequentAction mlmtConsequentAction = MlmtConsequentAction.BUILD;
        if (topology.getTopologyTypes() != null) {
            mlmtConsequentAction = mlmtProviderFactory.consequentAction(topology.getTopologyTypes());
        }
        if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
            buildTopology(type, topologyInstanceId, topology);
        } else if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            copyTopology(type, topologyInstanceId, topology);
            return;
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyCreated(type, topologyInstanceId, topology);
        }
    }

    private void handleMlmtTopology(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        if (topology.getTopologyTypes() != null) {
            mlmtTopologyBuilder.createTopologyTypes(LogicalDatastoreType.OPERATIONAL, topologyInstanceId,
                    topology.getTopologyTypes());
        }
        if (topology.getUnderlayTopology() != null) {
            mlmtTopologyBuilder.createUnderlayTopologyList(LogicalDatastoreType.OPERATIONAL, topologyInstanceId,
                    topology.getUnderlayTopology());
        }

        List<UnderlayTopology> lUnderlay = topology.getUnderlayTopology();
        if (lUnderlay != null && !lUnderlay.isEmpty()) {
            for (UnderlayTopology underlayTopology : lUnderlay) {
                TopologyId underlayTopologyId = underlayTopology.getTopologyRef();
                String pattern = "(?<=topology-id=\')(.*)(?=\'])";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(underlayTopologyId.getValue());
                if (!m.find()) {
                    continue;
                }
                String topologyName = new String(m.group(1));
                if (!underlayTopologies.contains(topologyName)) {
                    LOG.debug("MlmtTopologyObserver.onMlmtTopologyCreated underlay topology name {}",
                            topologyName);
                    underlayTopologies.add(topologyName);
                    TopologyId topologyId = new TopologyId(topologyName);
                    TopologyKey key = new TopologyKey(Preconditions.checkNotNull(topologyId));
                    InstanceIdentifier<Topology> topologyIid = InstanceIdentifier.create(NetworkTopology.class)
                            .child(Topology.class, key);
                    registerDataChangeEventListener(LogicalDatastoreType.CONFIGURATION, topologyIid);
                    registerDataChangeEventListener(LogicalDatastoreType.OPERATIONAL, topologyIid);
                }
            }
        }
    }

    protected void onMlmtTopologyCreated(final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        LOG.info("MlmtTopologyObserver::onMlmtTopologyCreated topologyInstanceId {}",
                topologyInstanceId.toString());
        mlmtTopologyBuilder.createTopology(LogicalDatastoreType.OPERATIONAL, topologyInstanceId);
        handleMlmtTopology(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, topology);
    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver.onTopologyCreated topologyInstanceId: {}",
                topologyInstanceId.toString());
        if (topologyInstanceId.equals(mlmtTopologyId)) {
            onMlmtTopologyCreated(topologyInstanceId, topology);
            return;
        }
        String observedTopologyName = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class)
                .getTopologyId().getValue();
        if (underlayTopologies.contains(observedTopologyName)) {
            onObservedTopologyCreated(type, topologyInstanceId, topology);
        }
    }

    protected MlmtConsequentAction getMlmtConsequentAction (final InstanceIdentifier<Topology> topologyInstanceId) {
        MlmtConsequentAction mlmtConsequentAction = MlmtConsequentAction.BUILD;
        InstanceIdentifier<TopologyTypes> topologyTypesIid = topologyInstanceId.child(TopologyTypes.class);
        TopologyTypes rxTopologyTypes = null;
        try {
            ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            Optional<TopologyTypes> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, topologyTypesIid).get();
            if (optional.isPresent()) {
                rxTopologyTypes = optional.get();
                if (rxTopologyTypes != null) {
                    LOG.info("MlmtTopologyObserver.getMlmtConsequentAction: found configured topology {}",
                            rxTopologyTypes.toString());
                    mlmtConsequentAction = mlmtProviderFactory.consequentAction(rxTopologyTypes);
                }
            } else {
                optional = rTx.read(LogicalDatastoreType.OPERATIONAL, topologyTypesIid).get();
                if (optional.isPresent()) {
                    rxTopologyTypes = optional.get();
                    if (rxTopologyTypes != null) {
                        LOG.info("MlmtTopologyObserver.getMlmtConsequentAction: found operational topology: {}",
                                rxTopologyTypes.toString());
                        mlmtConsequentAction = mlmtProviderFactory.consequentAction(rxTopologyTypes);
                    }
                }
            }
        } catch (InterruptedException e) {
            LOG.error("MlmtTopologyObserver.getMlmtConsequentAction interrupted exception", e);
        } catch (ExecutionException e) {
            LOG.error("MlmtTopologyObserver.getMlmtConsequentAction execution exception", e);
        }
        LOG.info("MlmtTopologyObserver.getMlmtConsequentAction {}", mlmtConsequentAction.toString());
        return mlmtConsequentAction;
    }

    @Override
    public void onNodeCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Node node) {
        LOG.info("MlmtTopologyObserver.onNodeCreated topologyInstanceId {} nodeKey {}",
                topologyInstanceId.toString(), node.getKey().toString());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
            mlmtTopologyBuilder.copyNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                    topologyKey.getTopologyId(), node);
            return;
        } else if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
            TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
            mlmtTopologyBuilder.createNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId,
                    topologyKey.getTopologyId(), node);
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onNodeCreated(type, topologyInstanceId, node);
        }
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey,
            final TerminationPoint tp) {
        LOG.info("MlmtTopologyObserver.onTpCreated topologyInstanceId {} nodeKey {} terminationPointId {}",
                topologyInstanceId.toString(), nodeKey.toString(), tp.getTpId());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            mlmtTopologyBuilder.copyTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tp);
            return;
        } else if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
            mlmtTopologyBuilder.createTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tp);
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpCreated(type, topologyInstanceId, nodeKey, tp);
        }
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Link link) {
        LOG.info("MlmtTopologyObserver.onLinkCreated topologyInstanceId {} linkId {}",
                topologyInstanceId.toString(), link.getLinkId());
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction == MlmtConsequentAction.COPY) {
            mlmtTopologyBuilder.copyLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, link);
            return;
        } else if (mlmtConsequentAction == MlmtConsequentAction.BUILD) {
            mlmtTopologyBuilder.createLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, link);
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkCreated(type, topologyInstanceId, link);
        }
    }

    protected void onMlmtTopologyUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver::onMlmtTopologyUpdated topologyInstanceId {}",
                topologyInstanceId.toString());
        handleMlmtTopology(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, topology);
    }

    @Override
    public void onTopologyUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver.onTopologyUpdated topologyInstanceId {}",
                topologyInstanceId.toString());
        if (topologyInstanceId.equals(mlmtTopologyId)) {
            onMlmtTopologyUpdated(type, topologyInstanceId, topology);
            return;
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyUpdated(type, topologyInstanceId, topology);
        }
    }

    @Override
    public void onNodeUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Node node) {
        LOG.info("MlmtTopologyObserver.onNodeUpdated topologyInstanceId {} nodeKey {}",
                topologyInstanceId.toString(), node.getKey().toString());
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
        LOG.info("MlmtTopologyObserver.onTpUpdated topologyInstanceId {} nodeKey {} terminationPointKey {}",
                topologyInstanceId.toString(), nodeKey.toString(), tp.getKey());
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
    public void onLinkUpdated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Link link) {
        LOG.info("MlmtTopologyObserver.onLinkUpdated topologyInstanceId {} linkKey {}",
                topologyInstanceId.toString(), link.getKey().toString());
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
    public void onTopologyDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId) {
        LOG.info("MlmtTopologyObserver.onTopologyDeleted topologyInstanceId {}",
                topologyInstanceId.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyDeleted(type, topologyInstanceId);
        }
        if (topologyInstanceId.equals(mlmtTopologyId)) {
            mlmtTopologyBuilder.deleteTopology(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId);
            mlmtTopologyBuilder.copyTopology(LogicalDatastoreType.CONFIGURATION, mlmtTopologyId,
                    LogicalDatastoreType.OPERATIONAL);
        }
    }

    @Override
    public void onNodeDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final NodeKey nodeKey) {
        LOG.info("MlmtTopologyObserver.onNodeDeleted topologyInstanceId {} nodeKey {}",
                topologyInstanceId.toString(), nodeKey.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onNodeDeleted(type, topologyInstanceId, nodeKey);
        }
        MlmtConsequentAction mlmtConsequentAction = getMlmtConsequentAction(topologyInstanceId);
        if (mlmtConsequentAction != MlmtConsequentAction.CORRELATE) {
            mlmtTopologyBuilder.deleteNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey);
        }
    }

    @Override
    public void onTpDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPointKey tpKey) {
        LOG.info("MlmtTopologyObserver.onTpDeleted topologyInstanceId {} nodeKey {} tpKey {}",
                topologyInstanceId.toString(), nodeKey.toString(), tpKey.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpDeleted(type, topologyInstanceId, nodeKey, tpKey);
        }
        mlmtTopologyBuilder.deleteTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tpKey);
    }

    @Override
    public void onLinkDeleted(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final LinkKey linkKey) {
        LOG.info("MlmtTopologyObserver.onLinkDeleted topologyInstanceId {} linkKey {}",
                topologyInstanceId.toString(), linkKey.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkDeleted(type, topologyInstanceId, linkKey);
        }
        mlmtTopologyBuilder.deleteLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, linkKey);
    }

    protected void dumpMap(final Map<InstanceIdentifier<?>, DataObject> map, final MlmtDataChangeEventType type) {
        Iterator<InstanceIdentifier<?>> iter = map.keySet().iterator();
        while (iter.hasNext()){
            try {
                InstanceIdentifier<?> iid = iter.next();
                LOG.debug("Key: {}", iid);
                DataObject dataObject = Preconditions.checkNotNull(map.get(iid));
                if (dataObject != null && dataObject instanceof java.io.Serializable) {
                    LOG.debug("Value: {}", dataObject.toString());
                }
            } catch (final IllegalArgumentException e) {
                LOG.error("MlmtTopologyObserver.dumpMap: IllegalArgumentException", e);
            } catch (final java.lang.UnsupportedOperationException e) {
                LOG.error("MlmtTopologyObserver.dumpMap: UnsupportedOperationException", e);
            } catch (final IllegalStateException e) {
                LOG.error("MlmtTopologyObserver.dumpMap: IllegalStateException", e);
            }
        }
    }

    protected synchronized void handleCreatedData(final LogicalDatastoreType storageType,
            final Map<InstanceIdentifier<?>, DataObject> mapIidObject, final MlmtDataChangeEventType type) {
        Iterator<InstanceIdentifier<?>> iter = mapIidObject.keySet().iterator();
        String changeType = type.toString();
        while (iter.hasNext()) {
            final InstanceIdentifier<?> iid = iter.next();
            final TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
            final String topologyName = topologyKey.getTopologyId().getValue();
            final InstanceIdentifier<Topology> topologyInstanceId =
                    InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
            if (iid.getTargetType().equals(Topology.class)
                    && (underlayTopologies.contains(topologyName) || topologyName.equals(MLMT))) {
                final Topology topology = (Topology)mapIidObject.get(iid);
                LOG.info("MlmtTopologyObserver.handleCreatedData storageType {} Topology.class topologyId {}",
                        storageType, topology.getTopologyId());
                onTopologyCreated(storageType, topologyInstanceId, topology);
            } else if (iid.getTargetType().equals(Node.class) && underlayTopologies.contains(topologyName)) {
                final Node node = (Node)mapIidObject.get(iid);
                final NodeId nodeId = node.getNodeId();
                LOG.info("MlmtTopologyObserver.handleCreatedData storageType {} Node class nodeId {}",
                        storageType, nodeId.toString());
                onNodeCreated(storageType, topologyInstanceId, node);
            } else if (iid.getTargetType().equals(TerminationPoint.class)
                    && underlayTopologies.contains(topologyName)) {
                final NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                final TerminationPoint tp = (TerminationPoint)mapIidObject.get(iid);
                final TpId tpId = tp.getTpId();
                LOG.info("MlmtTopologyObserver.handleCreatedData storageType {} TerminationPoint.class tpId {}",
                        storageType, tpId.toString());
                onTpCreated(storageType, topologyInstanceId, nodeKey, tp);
            } else if (iid.getTargetType().equals(Link.class) && underlayTopologies.contains(topologyName)) {
                final Link link = (Link)mapIidObject.get(iid);
                final LinkId linkId = link.getLinkId();
                LOG.info("MlmtTopologyObserver.handleCreatedData storageType {} Link.class linkId {}",
                        storageType, linkId.toString());
                onLinkCreated(storageType, topologyInstanceId, link);
            }
        }
    }

    protected synchronized void handleUpdatedData(final LogicalDatastoreType storageType,
            final Map<InstanceIdentifier<?>, DataObject> mapIidObject, final MlmtDataChangeEventType type) {
        String changeType = type.toString();

        Iterator<InstanceIdentifier<?>> iter = mapIidObject.keySet().iterator();
        while (iter.hasNext()) {
            final InstanceIdentifier<?> iid = iter.next();
            final TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
            final String topologyName = topologyKey.getTopologyId().getValue();
            final InstanceIdentifier<Topology> topologyInstanceId =
                    InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
            if (iid.getTargetType().equals(TerminationPoint.class) && underlayTopologies.contains(topologyName)) {
                final NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                final TerminationPoint tp = (TerminationPoint)mapIidObject.get(iid);
                final TpId tpId = tp.getTpId();
                LOG.info("MlmtTopologyObserver.handleUpdatedData storageType {} TerminationPoint.class tpId {}",
                        storageType, tpId.toString());
                onTpCreated(storageType, topologyInstanceId, nodeKey, tp);
            } else if (iid.getTargetType().equals(Node.class) && underlayTopologies.contains(topologyName)) {
                final Node node = (Node)mapIidObject.get(iid);
                final NodeId nodeId = node.getNodeId();
                LOG.info("MlmtTopologyObserver.handleUpdatedData storageType {} Node.class nodeId {}",
                        changeType, nodeId.toString());
                onNodeCreated(storageType, topologyInstanceId, node);
            } else if (iid.getTargetType().equals(Link.class) && underlayTopologies.contains(topologyName)) {
                final Link link = (Link)mapIidObject.get(iid);
                final LinkId linkId = link.getLinkId();
                LOG.info("MlmtTopologyObserver.handleUpdatedData storageType {} Link.class linkId {}",
                        storageType, linkId.toString());
                onLinkCreated(storageType, topologyInstanceId, link);
            } else if (iid.getTargetType().equals(Topology.class) && underlayTopologies.contains(topologyName)) {
                final Topology topology = (Topology)mapIidObject.get(iid);
                if (underlayTopologies.contains(topologyName)) {
                    LOG.info("MlmtTopologyObserver.handleUpdatedData storageType {} Topology.class topologyName {}",
                            storageType, topologyName);
                    onTopologyCreated(storageType, topologyInstanceId, topology);
                } else if (topologyName.equals(MLMT)) {
                    LOG.info("MlmtTopologyObserver.handleUpdatedData storageType {} Topology.class topologyName {}",
                            storageType, topologyName);
                    onTopologyCreated(LogicalDatastoreType.CONFIGURATION, mlmtTopologyId, topology);
                }
            }
        }
    }

    protected synchronized void handleRemovedData(final LogicalDatastoreType storageType,
            final Set<InstanceIdentifier<?>> iidSet) {
        Iterator<InstanceIdentifier<?>> iter = iidSet.iterator();
        while (iter.hasNext()) {
            final InstanceIdentifier<?> iid = iter.next();
            final TopologyKey topologyKey = iid.firstKeyOf(Topology.class, TopologyKey.class);
            final String topologyName = topologyKey.getTopologyId().getValue();
            final InstanceIdentifier<Topology> topologyInstanceId =
                    InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
            if (iid.getTargetType().equals(Link.class)) {
                LOG.info("MlmtTopologyObserver.handleRemovedData Link.class topologyName {} key {}",
                        topologyName, iid.toString());
                LinkKey linkKey = iid.firstKeyOf(Link.class, LinkKey.class);
                onLinkDeleted(storageType, topologyInstanceId, linkKey);
            } else if (iid.getTargetType().equals(TerminationPoint.class)) {
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                TerminationPointKey tpKey = iid.firstKeyOf(TerminationPoint.class, TerminationPointKey.class);
                LOG.info("MlmtTopologyObserver.handleRemovedData TerminationPoint.class topologyName {} key {}",
                        topologyName, iid.toString());
                onTpDeleted(storageType, topologyInstanceId, nodeKey, tpKey);
            } else if (iid.getTargetType().equals(Node.class)) {
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                LOG.info("MlmtTopologyObserver.handleRemovedData Node.class topologyName {} key {} ",
                        topologyName, iid.toString());
                onNodeDeleted(storageType, topologyInstanceId, nodeKey);
            } else if (iid.getTargetType().equals(Topology.class)) {
                LOG.info("MlmtTopologyObserver.handleRemovedData Topology.class topologyName {} key {}",
                        topologyName, iid.toString());
                onTopologyDeleted(storageType, topologyInstanceId);
            }
        }
    }

    @Override
    public void onDataChanged(LogicalDatastoreType type,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        synchronized (this) {
            try {
                LOG.info("MlmtTopologyObserver.onDataChanged");
                Preconditions.checkNotNull(change);

                Set<InstanceIdentifier<?>> removePathsSet = change.getRemovedPaths();
                if (removePathsSet != null) {
                    handleRemovedData(type, removePathsSet);
                }

                Map<InstanceIdentifier<?>, DataObject> mapIidObject = change.getCreatedData();
                if (mapIidObject != null) {
                    this.dumpMap(mapIidObject, MlmtDataChangeEventType.CREATED);
                    handleCreatedData(type, mapIidObject, MlmtDataChangeEventType.CREATED);
                }

                mapIidObject = change.getUpdatedData();
                if (mapIidObject != null) {
                    this.dumpMap(mapIidObject, MlmtDataChangeEventType.UPDATED);
                    handleUpdatedData(type, mapIidObject, MlmtDataChangeEventType.UPDATED);
                }
            } catch (final Exception e) {
                LOG.error("AbstractMlmtTopologyObserver.onDataChanged: ", e);
            }
        }
    }

    @Override
    public void closeListeners() {
        if (mapConfigurationDataChangeObserver != null
                && !mapConfigurationDataChangeObserver.isEmpty()) {
            for (MlmtDataChangeEventListener listener : mapConfigurationDataChangeObserver) {
                listener.close();
            }
        }
        if (mapOperationalDataChangeObserver != null
                && !mapOperationalDataChangeObserver.isEmpty()) {
            for (MlmtDataChangeEventListener listener : mapOperationalDataChangeObserver) {
                listener.close();
            }
        }
    }
}
