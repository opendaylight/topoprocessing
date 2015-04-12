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
import java.util.ArrayList;
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
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
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
import org.opendaylight.topology.mlmt.factory.MlmtProviderFactoryImpl;
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
    protected static final String mlmt = "mlmt:1";

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

    abstract public void init(DataBroker dataBroker, RpcProviderRegistry rpcRegistry);

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
                TopologyKey topologyKey = buildTopologyId.firstKeyOf(Topology.class, TopologyKey.class);
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

    protected void onObservedTopologyCreated(final LogicalDatastoreType type,
        final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver.onObservedTopologyCreated topologyInstanceId " + topologyInstanceId.toString());
        boolean isBuildingTopologyType = mlmtProviderFactory.isBuildingTopologyType(topology.getTopologyTypes());
        if (isBuildingTopologyType) {
            buildTopology(type, mlmtTopologyId, topology);
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyCreated(type, topologyInstanceId, topology);
        }
    }

    protected void onMlmtTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver::onMlmtTopologyCreated topologyInstanceId " + topologyInstanceId.toString());
        List<UnderlayTopology> lUnderlay = topology.getUnderlayTopology();
        if (lUnderlay != null) {
            for (UnderlayTopology underlayTopology : lUnderlay) {
                TopologyId underlayTopologyId = underlayTopology.getTopologyRef();
                String pattern = "(?<=topology-id=\')(.*)(?=\'])";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(underlayTopologyId.getValue().toString());
                if (m.find() == false) {
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
                mlmtTopologyBuilder.createUnderlayTopology(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, underlayTopologyId);
            }
        }

        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyCreated(type, topologyInstanceId, topology);
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

    @Override
    public void onNodeCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
        boolean isBuildingTopologyType = true;

        try {
            LOG.info("MlmtTopologyObserver.onNodeCreated topologyInstanceId: " + topologyInstanceId.toString() +
                    " nodeId: " + node.getNodeId());
            ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            Optional<Topology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, topologyInstanceId).get();
            if (optional.isPresent()) {
                Topology rxTopology = optional.get();
                if (rxTopology != null) {
                    isBuildingTopologyType = mlmtProviderFactory.isBuildingTopologyType(rxTopology.getTopologyTypes());
                }
            }
        } catch (InterruptedException e) {
          LOG.error("MlmtTopologyObserver.onNodeCreated interrupted exception", e);
        } catch (ExecutionException e) {
          LOG.error("MlmtTopologyObserver.onNodeCreated execution exception", e);
        }

        if (isBuildingTopologyType) {
            TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
            mlmtTopologyBuilder.createNode(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, topologyKey.getTopologyId(), node);
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
        mlmtTopologyBuilder.createTp(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, nodeKey, tp);

        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpCreated(type, topologyInstanceId, nodeKey, tp);
        }
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        LOG.info("MlmtTopologyObserver.onLinkCreated topologyInstanceId: " + topologyInstanceId.toString() +
                " linkId: " + link.getLinkId());
        mlmtTopologyBuilder.createLink(LogicalDatastoreType.OPERATIONAL, mlmtTopologyId, link);

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
         for (MlmtTopologyProvider provider : mlmtProviders) {
             provider.onNodeUpdated(type, topologyInstanceId, node);
         }
    }

    @Override
    public void onTpUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPoint tp) {
        LOG.info("MlmtTopologyObserver.onTpUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                " nodeKey: " + nodeKey.toString() + " terminationPointKey " + tp.getKey());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpUpdated(type, topologyInstanceId, nodeKey, tp);
        }
    }

    @Override
    public void onLinkUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        LOG.info("MlmtTopologyObserver.onLinkUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                " linkKey: " + link.getKey().toString());
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
        try {
            LOG.debug("---" + type.toString() + "---");
            Iterator<InstanceIdentifier<?>> iter = map.keySet().iterator();
            while (iter.hasNext()){
                InstanceIdentifier<?> iid = iter.next();
                LOG.debug("Key: " + iid);
                DataObject d = Preconditions.checkNotNull(map.get(iid));
                if (d != null) {
                    LOG.debug("Value: " + d.toString());
                }
            }
            LOG.debug("-------------");
        } catch (final IllegalArgumentException e ) {
            LOG.error("MlmtTopologyObserver.dumpMap: IllegalArgumentException", e);
        }
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
                    LOG.info("MlmtTopologyObserver.handleData " + changeType + " Topology Id " + mlmt);
                    onTopologyCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, topology);
                }
                if (topologyName.equals(mlmt)) {
                    LOG.info("MlmtTopologyObserver.handleData " + changeType + " Topology Id " + mlmt);
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
         Map<InstanceIdentifier<?>, DataObject> c = change.getCreatedData();
         this.dumpMap(c, MlmtDataChangeEventType.CREATED);
         if (c != null) {
             handleData(c, MlmtDataChangeEventType.CREATED);
         }
         c = change.getUpdatedData();
         this.dumpMap(c, MlmtDataChangeEventType.UPDATED);
         if (c != null) {
             handleData(c, MlmtDataChangeEventType.UPDATED);
         }
         Set<InstanceIdentifier<?>> r = change.getRemovedPaths();
         if (r != null) {
             handleData(r);
         }
    }
}
