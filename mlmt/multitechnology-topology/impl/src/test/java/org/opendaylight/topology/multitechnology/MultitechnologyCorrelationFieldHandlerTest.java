/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multitechnology;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.UnderlayTopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MultitechnologyCorrelationFieldHandlerTest extends AbstractConcurrentDataBrokerTest {

    private final Object waitObject = new Object();
    private static final String MLMT = "mlmt:1";
    private static final String EXAMPLE = "example:1";
    private DataBroker dataBroker;
    private Thread thread;
    private MlmtOperationProcessor processor;
    private Topology mlmtTopology;
    InstanceIdentifier<Topology> mlmtTopologyIid;
    TopologyKey mlmtTopologyKey;

    public class ChangeListener implements DataTreeChangeListener<Topology> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Topology>> changes) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }

    private InstanceIdentifier<Topology> buildTopologyIid(final String topologyName) {
        TopologyId tid = new TopologyId(topologyName);
        TopologyKey key = new TopologyKey(Preconditions.checkNotNull(tid));
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, key);
    }

    private Topology buildMlmtTopology(final String topologyName) {
        TopologyId topologyId = new TopologyId(topologyName);
        mlmtTopologyKey = new TopologyKey(Preconditions.checkNotNull(topologyId));
        final UnderlayTopologyBuilder underlayTopologyBuilder = new UnderlayTopologyBuilder();
        final TopologyTypesBuilder topologyTypesBuilder = new TopologyTypesBuilder();
        TopologyId underlayTopologyRef = new TopologyId(EXAMPLE);
        underlayTopologyBuilder.setTopologyRef(underlayTopologyRef);
        UnderlayTopologyKey underlayKey = new UnderlayTopologyKey(underlayTopologyRef);
        underlayTopologyBuilder.setKey(underlayKey);
        UnderlayTopology underlayTopology = underlayTopologyBuilder.build();
        List<UnderlayTopology> lUnderlayTopology = new ArrayList<>();
        lUnderlayTopology.add(underlayTopology);
        final TopologyBuilder tbuilder = new TopologyBuilder();
        tbuilder.setKey(mlmtTopologyKey);
        tbuilder.setTopologyId(topologyId);
        final Topology top = tbuilder.setTopologyTypes(topologyTypesBuilder.build())
                .setUnderlayTopology(lUnderlayTopology).build();

        return top;
    }

    @BeforeClass
    public static void allMethodsSetUp() {
        // NOOP
    }

    @Before
    public void setUp() throws Exception {
        this.dataBroker = getDataBroker();
        assertNotNull(dataBroker);
        processor = new MlmtOperationProcessor(dataBroker);
        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessor");
        thread.start();

        mlmtTopologyIid = buildTopologyIid(MLMT);
        /*
         * It is necessary to create the network-topology containers in
         * both configuration and operational data storage
         */
        final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
        final NetworkTopology networkTopology = nb.build();
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                mlmtTopologyIid), new ChangeListener());

        mlmtTopology = buildMlmtTopology(MLMT);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid, mlmtTopology);
        assertCommit(rwTx.submit());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Configuration mlmt:1 topology ", optional.isPresent());
        final Topology rxTopology = optional.get();
        assertNotNull(rxTopology);

        NodeBuilder nodeBuilder = new NodeBuilder();
        String nodeName1 = "node:1";
        NodeId nodeId1 = new NodeId(nodeName1);
        nodeBuilder.setNodeId(nodeId1);
        NodeKey nodeKey1 = new NodeKey(nodeId1);
        nodeBuilder.setKey(nodeKey1);
        InstanceIdentifier<Node> nodeIid1 = mlmtTopologyIid.child(Node.class, nodeKey1);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, nodeIid1, nodeBuilder.build());
        assertCommit(rwTx.submit());

        nodeBuilder = new NodeBuilder();
        String nodeName2 = "node:2";
        NodeId nodeId2 = new NodeId(nodeName2);
        nodeBuilder.setNodeId(nodeId2);
        NodeKey nodeKey2 = new NodeKey(nodeId2);
        nodeBuilder.setKey(nodeKey2);
        InstanceIdentifier<Node> nodeIid2 = mlmtTopologyIid.child(Node.class, nodeKey2);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, nodeIid2, nodeBuilder.build());
        assertCommit(rwTx.submit());

        TpId tpId1 = new TpId("tp:1");
        final TerminationPointKey tpKey1 = new TerminationPointKey(tpId1);
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey1);
        tpBuilder.setTpId(tpId1);
        final InstanceIdentifier<TerminationPoint> tpIid1 = mlmtTopologyIid
                .child(Node.class, nodeKey1).child(TerminationPoint.class, tpKey1);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, tpIid1, tpBuilder.build());
        assertCommit(rwTx.submit());

        TpId tpId2 = new TpId("tp:2");
        final TerminationPointKey tpKey2 = new TerminationPointKey(tpId2);
        tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey2);
        tpBuilder.setTpId(tpId2);
        final InstanceIdentifier<TerminationPoint> tpIid2 = mlmtTopologyIid
                .child(Node.class, nodeKey2).child(TerminationPoint.class, tpKey2);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, tpIid2, tpBuilder.build());
        assertCommit(rwTx.submit());

        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName = "link:1";
        LinkId linkId = new LinkId(linkName);
        linkBuilder.setLinkId(linkId);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);

        SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(nodeId1);
        sourceBuilder.setSourceTp(tpId1);
        linkBuilder.setSource(sourceBuilder.build());

        DestinationBuilder destinationBuilder = new DestinationBuilder();
        destinationBuilder.setDestNode(nodeId2);
        destinationBuilder.setDestTp(tpId2);
        linkBuilder.setDestination(destinationBuilder.build());

        InstanceIdentifier<Link> linkIid = mlmtTopologyIid.child(Link.class, linkKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, linkIid, linkBuilder.build());
        assertCommit(rwTx.submit());
    }

    @Test(timeout = 10000)
    public void onNodeCorrelationFieldTest() throws Exception {
        final String nodeName = "node:1";
        final String correlation_field = "ISO-SYSTEM-ID";
        final String iso_system_id = "1921.6800.1077";
        final NodeId nodeId = new NodeId(nodeName);
        final NodeKey nodeKey = new NodeKey(nodeId);

        MultitechnologyNodeCorrelationFieldHandler.putCorrelationField(dataBroker, processor,
                mlmtTopologyIid, nodeKey, correlation_field, iso_system_id);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        String rxCorrelation = MultitechnologyNodeCorrelationFieldHandler.getCorrelationField(
                dataBroker, processor, mlmtTopologyIid, nodeKey);
        assertNotNull(rxCorrelation);
    }

    @Test(timeout = 10000)
    public void onTpCorrelationFieldTest() throws Exception {
        String nodeName = "node:1";
        final NodeId nodeId = new NodeId(nodeName);
        final NodeKey nodeKey = new NodeKey(nodeId);
        final String tpName = "tp:1";
        final String correlation_field = "IPV4-ADDRESS";
        final String ip_address = "1.77.88.1";
        final TpId tpId = new TpId(tpName);
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);

        MultitechnologyTpCorrelationFieldHandler.putCorrelationField(dataBroker, processor,
                mlmtTopologyIid, nodeKey, tpKey, correlation_field, ip_address);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        String rxCorrelation = MultitechnologyTpCorrelationFieldHandler.getCorrelationField(
                dataBroker, processor, mlmtTopologyIid, nodeKey, tpKey);
        assertNotNull(rxCorrelation);
    }

    @Test(timeout = 10000)
    public void onLinkCorrelationFieldTest() throws Exception {
        final String linkName = "link:1";
        final LinkId linkId = new LinkId(linkName);
        final LinkKey linkKey = new LinkKey(linkId);
        final String correlation_field = "SOURCE-TP-NAME";
        final String source_tp_name = "SSR7:1.66.77.2";

        MultitechnologyLinkCorrelationFieldHandler.putCorrelationField(dataBroker, processor,
                mlmtTopologyIid, linkKey, correlation_field, source_tp_name);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        String rxCorrelationField = MultitechnologyLinkCorrelationFieldHandler.getCorrelationField(
                dataBroker, processor, mlmtTopologyIid, linkKey);
        assertNotNull(rxCorrelationField);
    }

    @After
    public void clear() {
        // NOOP
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }
}
