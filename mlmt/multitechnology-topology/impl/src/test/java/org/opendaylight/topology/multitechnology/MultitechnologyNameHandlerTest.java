/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.multitechnology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.topology.mlmt.utility.MlmtOperationProcessor;
import org.opendaylight.topology.multitechnology.MultitechnologyNameHandlerTest.ChangeListener;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class MultitechnologyNameHandlerTest extends AbstractConcurrentDataBrokerTest {

    private final Object waitObject = new Object();
    private static final String MLMT = "mlmt:1";
    private static final String EXAMPLE = "example:1";
    private static final String NODENAMEFIELD = "NODE-NAME";
    private static final String TPNAMEFIELD = "TP-NAME";
    private static final String LINKNAMEFIELD = "LINK-NAME";
    private DataBroker dataBroker;
    private Thread thread;
    private MlmtOperationProcessor processor;
    private Topology mlmtTopology;
    InstanceIdentifier<Topology> mlmtTopologyIid;
    TopologyKey mlmtTopologyKey;

    public class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
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
        List<UnderlayTopology> lUnderlayTopology = new ArrayList<UnderlayTopology>();
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
        rwTx.put(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class), networkTopology);
        assertCommit(rwTx.submit());

        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                mlmtTopologyIid, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);

        mlmtTopology = buildMlmtTopology(MLMT);
        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid, mlmtTopology);
        assertCommit(rwTx.submit());

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Topology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, mlmtTopologyIid).get();
        assertNotNull(optional);
        assertTrue("Operational mlmt:1 topology ", optional.isPresent());
        final Topology rxTopology = optional.get();
        assertNotNull(rxTopology);

        final NodeBuilder nodeBuilder = new NodeBuilder();
        final String nodeName1 = "node:1";
        NodeId nodeId = new NodeId(nodeName1);
        nodeBuilder.setNodeId(nodeId);
        NodeKey nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        InstanceIdentifier<Node> nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, nodeIid, nodeBuilder.build());
        assertCommit(rwTx.submit());

        final String tpName1 = "tp:1";
        TpId tpId = new TpId(tpName1);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        InstanceIdentifier<TerminationPoint> instanceId = mlmtTopologyIid
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, instanceId, tpBuilder.build());
        assertCommit(rwTx.submit());

        final String nodeName2 = "node:2";
        nodeId = new NodeId(nodeName2);
        nodeBuilder.setNodeId(nodeId);
        nodeKey = new NodeKey(nodeId);
        nodeBuilder.setKey(nodeKey);
        nodeIid = mlmtTopologyIid.child(Node.class, nodeKey);

        rwTx = dataBroker.newWriteOnlyTransaction();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, nodeIid, nodeBuilder.build());
        assertCommit(rwTx.submit());

        final String tpName2 = "tp:2";
        tpId = new TpId(tpName2);
        tpKey = new TerminationPointKey(tpId);
        tpBuilder.setKey(tpKey);
        tpBuilder.setTpId(tpId);
        instanceId = mlmtTopologyIid.child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);

        LinkBuilder linkBuilder = new LinkBuilder();
        String linkName1 = "link:1";
        LinkId linkId = new LinkId(linkName1);
        LinkKey linkKey = new LinkKey(linkId);
        linkBuilder.setKey(linkKey);
        linkBuilder.setLinkId(linkId);

        SourceBuilder sourceBuilder = new SourceBuilder();
        nodeId = new NodeId(nodeName1);
        sourceBuilder.setSourceNode(nodeId);
        tpId = new TpId(tpName1);
        sourceBuilder.setSourceTp(tpId);
        linkBuilder.setSource(sourceBuilder.build());

        DestinationBuilder destinationBuilder = new DestinationBuilder();
        nodeId = new NodeId(nodeName2);
        destinationBuilder.setDestNode(nodeId);
        tpId = new TpId(tpName2);
        destinationBuilder.setDestTp(tpId);
        linkBuilder.setDestination(destinationBuilder.build());
        Link link = linkBuilder.build();
    }

    @Test(timeout = 10000)
    public void onNodeNameTest() throws Exception {
        String nodeName = "node:1";
        final NodeId nodeId = new NodeId(nodeName);
        final NodeKey nodeKey = new NodeKey(nodeId);

        MultitechnologyNodeNameHandler.putNodeName(dataBroker, processor, mlmtTopologyIid, nodeKey, NODENAMEFIELD,
                nodeName);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        String rxNodeName = MultitechnologyNodeNameHandler.getNodeName(dataBroker, processor,
                mlmtTopologyIid, nodeKey, NODENAMEFIELD);
        assertEquals(nodeName, rxNodeName);

        nodeName = "node:2";
        MultitechnologyNodeNameHandler.putNodeName(dataBroker, processor,
                mlmtTopologyIid, nodeKey, NODENAMEFIELD, nodeName);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        rxNodeName = MultitechnologyNodeNameHandler.getNodeName(dataBroker, processor,
                mlmtTopologyIid, nodeKey, NODENAMEFIELD);
        assertEquals(nodeName, rxNodeName);
    }

    @Test(timeout = 10000)
    public void onTpNameTest() throws Exception {
        String nodeName = "node:1";
        final NodeId nodeId = new NodeId(nodeName);
        final NodeKey nodeKey = new NodeKey(nodeId);

        String tpName = "tp:1";
        final TpId tpId = new TpId(tpName);
        final TerminationPointKey tpKey = new TerminationPointKey(tpId);

        MultitechnologyTpNameHandler.putTpName(dataBroker, processor,
                mlmtTopologyIid, nodeKey, tpKey, TPNAMEFIELD, tpName);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        String rxTpName = MultitechnologyTpNameHandler.getTpName(dataBroker, processor,
                mlmtTopologyIid, nodeKey, tpKey, TPNAMEFIELD);
        assertEquals(tpName, rxTpName);

        tpName = "tp:2";
        MultitechnologyTpNameHandler.putTpName(dataBroker, processor,
                mlmtTopologyIid, nodeKey, tpKey, TPNAMEFIELD, tpName);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        rxTpName = MultitechnologyTpNameHandler.getTpName(dataBroker, processor,
                mlmtTopologyIid, nodeKey, tpKey, TPNAMEFIELD);
        assertEquals(tpName, rxTpName);
    }

    @Test(timeout = 10000)
    public void onLinkNameTest() throws Exception {
        String linkName = "link:1";
        final LinkId linkId = new LinkId(linkName);
        final LinkKey linkKey = new LinkKey(linkId);

        MultitechnologyLinkNameHandler.putLinkName(dataBroker, processor,
                mlmtTopologyIid, linkKey, LINKNAMEFIELD, linkName);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        String rxLinkName = MultitechnologyLinkNameHandler.getLinkName(dataBroker, processor,
                mlmtTopologyIid, linkKey, LINKNAMEFIELD);
        assertEquals(linkName, rxLinkName);

        linkName = "link:2";
        MultitechnologyLinkNameHandler.putLinkName(dataBroker, processor,
                mlmtTopologyIid, linkKey, LINKNAMEFIELD, linkName);

        synchronized (waitObject) {
            waitObject.wait(2000);
        }

        rxLinkName = MultitechnologyLinkNameHandler.getLinkName(dataBroker, processor,
                mlmtTopologyIid, linkKey, LINKNAMEFIELD);
        assertEquals(linkName, rxLinkName);
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
