/*
 * Copyright (c)2015 Ericsson, AB. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topology.mlmt.utility;

import java.util.Collections;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class MlmtOperationProcessorTest extends AbstractDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MlmtOperationProcessorTest.class);
    private static final String MLMT1 = "mlmt:1";
    private static TopologyId topologyId;
    private static TopologyKey topologyKey;
    private static InstanceIdentifier<Topology> topologyInstanceId;
    private final Object waitObject = new Object();
    private ProviderContext session;
    private DataBroker dataBroker;
    private MlmtOperationProcessor processor;
    private Thread thread;

    public class ChangeListener implements DataChangeListener {
        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }

    @Override
    protected void setupWithDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @BeforeClass
    public static void allMethodsSetUp() {
        topologyId = new TopologyId(MLMT1);
        topologyKey = new TopologyKey(Preconditions.checkNotNull(topologyId));
        topologyInstanceId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topologyKey);
    }

    @Before
    public void setUp() {
        processor = new MlmtOperationProcessor(dataBroker);
        assertNotNull(processor);
        thread = new Thread(processor);
        assertNotNull(thread);
        thread.setDaemon(true);
        thread.setName("MlmtOperationProcessorTest");
        thread.start();
        dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                topologyInstanceId, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);
        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                topologyInstanceId, new ChangeListener(), DataBroker.DataChangeScope.SUBTREE);
    }

    @Test(timeout = 10000)
    public void testPutConfiguration() throws Exception {
        assertNotNull(processor);
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                transaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class), nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testPutConfiguration: Configuration network topology not present", optional.isPresent());
        NetworkTopology rxNetworkTopology = optional.get();
        assertNotNull(rxNetworkTopology);
    }

    @Test(timeout = 10000)
    public void testPutOperational() throws Exception {
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                transaction.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class), nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testPutOperational: Operational network topology not present", optional.isPresent());
        NetworkTopology rxNetworkTopology = optional.get();
        assertNotNull(rxNetworkTopology);
    }

    @Test(timeout = 10000)
    public void testMergeConfiguration() throws Exception {
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                transaction.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class), nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testMergeOperational: Operational network topology not present", optional.isPresent());
        NetworkTopology rxTopology = optional.get();
        assertNotNull(rxTopology);
    }

    @Test(timeout = 10000)
    public void testMergeOperational() throws Exception {
        processor.enqueueOperation(new MlmtTopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                final NetworkTopologyBuilder nb = new NetworkTopologyBuilder();
                transaction.merge(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class), nb.build());
            }
        });

        synchronized (waitObject) {
            waitObject.wait(5000);
        }

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<NetworkTopology> optional = rTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetworkTopology.class)).get();
        assertNotNull(optional);
        assertTrue("MlmtOperationProcessorTest.testMergeOperational: Operational network topology not present", optional.isPresent());
        NetworkTopology rxTopology = optional.get();
        assertNotNull(rxTopology);
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
