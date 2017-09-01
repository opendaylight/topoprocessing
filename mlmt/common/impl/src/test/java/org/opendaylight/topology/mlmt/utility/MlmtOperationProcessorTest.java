/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

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
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MlmtOperationProcessorTest extends AbstractConcurrentDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtOperationProcessorTest.class);
    private static final String MLMT1 = "mlmt:1";
    private static TopologyId topologyId;
    private static TopologyKey topologyKey;
    private static InstanceIdentifier<Topology> topologyInstanceId;
    private final Object waitObject = new Object();
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;
    private Thread thread;
    private List<Topology> topologies;
    private int testRun = 0;

    public class ChangeListener implements DataTreeChangeListener<Topology> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Topology>> changes) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }

    @BeforeClass
    public static void allMethodsSetUp() {
        topologyId = new TopologyId(MLMT1);
        topologyKey = new TopologyKey(Preconditions.checkNotNull(topologyId));
        topologyInstanceId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
    }

    @Before
    public void setUp() {
        this.dataBroker = getDataBroker();
        assertNotNull(dataBroker);
        processor = new MlmtOperationProcessor(dataBroker);
        assertNotNull(processor);
        thread = new Thread(processor);
        assertNotNull(thread);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessorTest");
        thread.start();
        dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                topologyInstanceId), new ChangeListener());
        dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                topologyInstanceId), new ChangeListener());
        Topology tp = new TopologyBuilder().setKey(new TopologyKey(new TopologyId("mlmttopo" + testRun))).build();
        topologies = new ArrayList<>();
        topologies.add(tp);
        testRun ++;
    }

    @Test(timeout = 10000)
    public void testPutConfiguration() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testPutConfiguration: Configuration network topology not present",
                optional.isPresent());
        NetworkTopology rxNetworkTopology = optional.get();
        assertNotNull(rxNetworkTopology);
    }

    @Test(timeout = 10000)
    public void testPutConfigurationImmediateCommit() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }

            @Override
             public boolean isCommitNow() { return true; }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testPutConfiguration: Configuration network topology not present",
                optional.isPresent());
        NetworkTopology rxNetworkTopology = optional.get();
        assertNotNull(rxNetworkTopology);
    }

    @Test(timeout = 10000)
    public void testPutOperational() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testPutOperational: Operational network topology not present",
                optional.isPresent());
        NetworkTopology rxNetworkTopology = optional.get();
        assertNotNull(rxNetworkTopology);
    }

    @Test(timeout = 10000)
    public void testPutOperationalImmediateCommit() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }

            @Override
             public boolean isCommitNow() { return true; }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testPutOperational: Operational network topology not present",
                optional.isPresent());
        NetworkTopology rxNetworkTopology = optional.get();
        assertNotNull(rxNetworkTopology);
    }

    @Test(timeout = 10000)
    public void testMergeConfiguration() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testMergeOperational: Operational network topology not present",
                optional.isPresent());
        NetworkTopology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testMergeConfigurationImmediateCommit() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }

            @Override
            public boolean isCommitNow() { return true; }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testMergeOperational: Operational network topology not present",
                optional.isPresent());
        NetworkTopology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testMergeOperational() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testMergeOperational: Operational network topology not present",
                optional.isPresent());
        NetworkTopology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testMergeOperationalImmediateCommit() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                nb.setTopology(topologies);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class),
                        nb.build());
            }

            @Override
            public boolean isCommitNow() { return true; }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testMergeOperational: Operational network topology not present",
                optional.isPresent());
        NetworkTopology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testRun() throws Exception {
        MlmtOperationProcessor locProcessor = new MlmtOperationProcessor(dataBroker);
        assertNotNull(locProcessor);
        Thread locThread = new Thread(locProcessor);
        assertNotNull(locThread);
        locThread.setDaemon(true);
        locThread.setName("MlmtOperationProcessorTest");
        locThread.start();
        Thread.sleep(1000);
        locThread.stop();
        assertNotNull(locProcessor);
    }

    @Test(timeout = 10000)
    public void testTransactionChain() throws Exception {
        BindingTransactionChain transactionChain = this.dataBroker.createTransactionChain(processor);
        processor.onTransactionChainSuccessful(transactionChain);
        assertNotNull(processor);
    }

    @Test(timeout = 10000)
    public void testClose() throws Exception {
        processor.close();
        assertNotNull(processor);
    }

    @After
    public void clear() {
        try {
            if (thread != null) {
                thread.interrupt();
                thread.join();
                thread = null;
            }
        } catch (final InterruptedException e) {

        }
    }

    @AfterClass
    public static void allMethodsClear() {
        // NOOP
    }

}
