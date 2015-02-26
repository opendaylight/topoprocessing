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

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
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
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.mlmt.utility.MlmtTopologyBuilder;
import org.opendaylight.topology.mlmt.factory.MlmtProviderFactoryImpl;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mlmt.topology.observer.impl.rev150122.MlmtTopologyObserverRuntimeMXBean;

public class MlmtTopologyObserver extends AbstractBindingAwareProvider implements AutoCloseable, DataChangeListener, MlmtTopologyObserverRuntimeMXBean,
        MlmtTopologyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtTopologyObserver.class);
    private InstanceIdentifier<Topology> MLMT_TOPOLOGY_IID;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;
    private MlmtTopologyBuilder mlmtTopologyBuilder;
    private List<MlmtTopologyProvider> mlmtProviders;
    private List<String> underlayTopologies;
    private static final String mlmt = "mlmt:1";
    private Thread thread;

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

    /**
     * Gets called on start of a bundle.
     *
     * @param session
     */
    @Override
    public synchronized void onSessionInitiated(final ProviderContext session) {
        LOG.info("MlmtTopologyObserver.onSessionInitiated");
        dataBroker = session.getSALService(DataBroker.class);
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();

        MLMT_TOPOLOGY_IID = buildTopologyIid(mlmt);
        mlmtTopologyBuilder = new MlmtTopologyBuilder();
        mlmtTopologyBuilder.init("builder:1", dataBroker, LOG, processor);
        underlayTopologies = new ArrayList<String>();
        Map<String, List<MlmtTopologyProvider>> providersMap =
                (new MlmtProviderFactoryImpl()).createProvidersMap(dataBroker, LOG, processor, mlmt);
        mlmtProviders = providersMap.get(mlmt);

        listenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                MLMT_TOPOLOGY_IID, this, DataBroker.DataChangeScope.SUBTREE);
    }

    private InstanceIdentifier<Topology> buildTopologyIid(final String topologyName) {
        TopologyId tid = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
    }

    private void onObservedTopologyCreated(final LogicalDatastoreType type,
        final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver.onObservedTopologyCreated topologyInstanceId " + topologyInstanceId.toString());
        List<Node> lNode = topology.getNode();
        List<Link> lLink = topology.getLink();
        if (lNode != null && lNode.isEmpty() == false) {
            for (Node node : lNode) {
                TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
                mlmtTopologyBuilder.createNode(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID,
                        topologyKey.getTopologyId(), node);
            }
        }
        if (lLink != null && lLink.isEmpty() == false) {
            for (Link link : lLink) {
                mlmtTopologyBuilder.createLink(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID, link);
            }
        }
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyCreated(type, topologyInstanceId, topology);
        }
    }

    private void onMlmtTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver::onMlmtTopologyCreated topologyInstanceId " + topologyInstanceId.toString());
        List<UnderlayTopology> lUnderlay = topology.getUnderlayTopology();
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

        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyCreated(type, topologyInstanceId, topology);
        }
    }

    @Override
    public void onTopologyCreated(final LogicalDatastoreType type,
            final InstanceIdentifier<Topology> topologyInstanceId, final Topology topology) {
        LOG.info("MlmtTopologyObserver.onTopologyCreated topologyInstanceId: " + topologyInstanceId.toString());
        if (topologyInstanceId.equals(MLMT_TOPOLOGY_IID)) {
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
        LOG.info("MlmtTopologyObserver::onNodeCreated topologyInstanceId: " + topologyInstanceId.toString() +
                " nodeId: " + node.getNodeId());
        TopologyKey topologyKey = topologyInstanceId.firstKeyOf(Topology.class, TopologyKey.class);
        mlmtTopologyBuilder.createNode(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID, topologyKey.getTopologyId(), node);

        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onNodeCreated(type, topologyInstanceId, node);
        }
    }

    @Override
    public void onTpCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPoint tp) {
        LOG.info("MlmtTopologyObserver::onNodeCreated topologyInstanceId: " + topologyInstanceId.toString() +
                " nodeKey: " + nodeKey.toString() + " terminationPointId: " + tp.getTpId());
        mlmtTopologyBuilder.createTp(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID, nodeKey, tp);

        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpCreated(type, topologyInstanceId, nodeKey, tp);
        }
    }

    @Override
    public void onLinkCreated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        LOG.info("MlmtTopologyObserver::onLinkCreated topologyInstanceId: " + topologyInstanceId.toString() +
                " linkId: " + link.getLinkId());
        mlmtTopologyBuilder.createLink(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID, link);

        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkCreated(type, topologyInstanceId, link);
        }
    }

    @Override
    public void onTopologyUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Topology topology) {
        LOG.info("MlmtTopologyObserver::onTopologyUpdated topologyInstanceId: " + topologyInstanceId.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyUpdated(type, topologyInstanceId, topology);
        }
    }

    @Override
    public void onNodeUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Node node) {
         LOG.info("MlmtTopologyObserver::onNodeUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                 " nodeKey: " + node.getKey().toString());

         for (MlmtTopologyProvider provider : mlmtProviders) {
             provider.onNodeUpdated(type, topologyInstanceId, node);
         }
    }

    @Override
    public void onTpUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPoint tp) {
        LOG.info("MlmtTopologyObserver::onNodeUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                " nodeKey: " + nodeKey.toString() + " terminationPointKey " + tp.getKey());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTpUpdated(type, topologyInstanceId, nodeKey, tp);
        }
    }

    @Override
    public void onLinkUpdated(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final Link link) {
        LOG.info("MlmtTopologyObserver::onLinkUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                " linkKey: " + link.getKey().toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkUpdated(type, topologyInstanceId, link);
        }
    }

    @Override
    public void onTopologyDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId) {
        LOG.info("MlmtTopologyObserver::onLinkUpdated topologyInstanceId: " + topologyInstanceId.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onTopologyDeleted(type, topologyInstanceId);
        }
        mlmtTopologyBuilder.deleteTopology(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID);
    }

    @Override
    public void onNodeDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey) {
         LOG.info("MlmtTopologyObserver::onLinkUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                 " nodeKey: " + nodeKey.toString());
         for (MlmtTopologyProvider provider : mlmtProviders) {
             provider.onNodeDeleted(type, topologyInstanceId, nodeKey);
         }
         mlmtTopologyBuilder.deleteNode(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID, nodeKey);
    }

    @Override
    public void onTpDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final NodeKey nodeKey, final TerminationPointKey tpKey) {
         LOG.info("MlmtTopologyObserver::onLinkUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                 " nodeKey: " + nodeKey.toString() + " tpKey: " + tpKey.toString());
         for (MlmtTopologyProvider provider : mlmtProviders) {
             provider.onTpDeleted(type, topologyInstanceId, nodeKey, tpKey);
         }
         mlmtTopologyBuilder.deleteTp(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID, nodeKey, tpKey);
    }

    @Override
    public void onLinkDeleted(final LogicalDatastoreType type, final InstanceIdentifier<Topology> topologyInstanceId,
            final LinkKey linkKey) {
        LOG.info("MlmtTopologyObserver::onLinkUpdated topologyInstanceId: " + topologyInstanceId.toString() +
                " linkKey: " + linkKey.toString());
        for (MlmtTopologyProvider provider : mlmtProviders) {
            provider.onLinkDeleted(type, topologyInstanceId, linkKey);
        }
        mlmtTopologyBuilder.deleteLink(LogicalDatastoreType.OPERATIONAL, MLMT_TOPOLOGY_IID, linkKey);
    }

    @Override
    public synchronized void close() throws InterruptedException {
        LOG.info("MlmtTopologyObserver stopped.");
        if (this.listenerRegistration != null) {
            try {
                this.listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Failed to close listener registration", e);
            }
            listenerRegistration = null;
        }
        if (thread != null) {
            thread.interrupt();
            thread.join();
            thread = null;
        }
    }

    /**
     * Gets called during stop bundle
     *
     * @param context The execution context of the bundle being stopped.
     */
    @Override
    public void stopImpl(final BundleContext context) {
        try {
            this.close();
        } catch (final InterruptedException e) {
            LOG.error("Failed to stop provider", e);
        }
    }

    private void dumpMap(final Map<InstanceIdentifier<?>, DataObject> map, MlmtDataChangeEventType type){
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
     }

    /*
     * create and update events
     */
    private void handleData(final Map<InstanceIdentifier<?>, DataObject> c , final MlmtDataChangeEventType type) {
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
                    LOG.info("MlmtTopologyObserver.onDataChanged " + changeType + " Topology Id " + mlmt);
                    onTopologyCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, topology);
                }
                if (topologyName.equals(mlmt)) {
                    LOG.info("MlmtTopologyObserver::onDataChanged " + changeType + " Topology Id " + mlmt);
                    onTopologyCreated(LogicalDatastoreType.CONFIGURATION, MLMT_TOPOLOGY_IID, topology);
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
                    LOG.info("MlmtTopologyObserver.onDataChanged Node Id " + changeType +
                            " Node.class Node Id " +  nodeId.toString());
                    onNodeCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, node);
                } // end if
            } // end if target == Node.class
            else if (iid.getTargetType().equals(TerminationPoint.class)) {
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                LOG.info("MlmtTopologyObserver.onDataChanged " + changeType +
                       " TerminationPoint.class Topology Key " + topologyKey.toString() +
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
                LOG.info("MlmtTopologyObserver.onDataChanged" + changeType +
                        " TerminationPoint.class Topology Key " + topologyKey.toString() +
                        " Link Key " + linkKey.toString());
                if (underlayTopologies.contains(topologyName)) {
                    InstanceIdentifier<Topology> topologyInstanceId =
                            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                    final Link link = (Link)c.get(iid);
                    final LinkId linkId = link.getLinkId();
                    LOG.debug("MlmtTopologyObserver.onDataChanged Node Id " + type.toString() +
                            " Link.class Link Id " + linkId.toString());
                    onLinkCreated(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, link);
                 } // end if underlayTopologies
            } // end if target == Link.class
        } // end while
    }

    /*
     * delete events
     */
    private void handleData(final Set<InstanceIdentifier<?>> c , final MlmtDataChangeEventType type) {
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
                LOG.info("MlmtTopologyObserver::removed Key Node.class");
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                InstanceIdentifier<Topology> topologyInstanceId =
                        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                onNodeDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, nodeKey);
            }
            else if (iid.getTargetType().equals(TerminationPoint.class)) {
                LOG.info("MlmtTopologyObserver::removed Key Tp.class");
                NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
                TerminationPointKey tpKey = iid.firstKeyOf(TerminationPoint.class, TerminationPointKey.class);
                InstanceIdentifier<Topology> topologyInstanceId =
                        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
                InstanceIdentifier<Node> nodeInstanceId = topologyInstanceId.child(Node.class, nodeKey);
                InstanceIdentifier<TerminationPoint> tpInstanceId = nodeInstanceId.child(TerminationPoint.class, tpKey);
                onTpDeleted(LogicalDatastoreType.OPERATIONAL, topologyInstanceId, nodeKey, tpKey);
            }
            else if (iid.getTargetType().equals(Link.class)) {
                LOG.info("MlmtTopologyObserver::removed Key Link.class");
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
             handleData(r, MlmtDataChangeEventType.DELETED);
         }
    }
}
