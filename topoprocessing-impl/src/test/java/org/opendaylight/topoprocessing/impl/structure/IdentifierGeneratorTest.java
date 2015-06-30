/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.structure;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;

/**
 * @author martin.uhlir
 *
 */
public class IdentifierGeneratorTest {

    private static final int THREADS = 5;
    private static final int IDS_PER_THREAD = 10;

    ConcurrentLinkedDeque<String> concurrentDeque; 
    private IdentifierGenerator idGenerator = new IdentifierGenerator();

    public class IdGeneratorRunnable implements Runnable {
        public void run() {
            for (int i = 0; i < IDS_PER_THREAD; i++) {
                String generatedNodeId = idGenerator.getNextIdentifier(CorrelationItemEnum.Node);
                concurrentDeque.add(generatedNodeId);
            }
        }
      }

    @Test
    public void testNodeLinkTerminationPointIdGeneration() {
        String nodeId = idGenerator.getNextIdentifier(CorrelationItemEnum.Node);
        String linkId = idGenerator.getNextIdentifier(CorrelationItemEnum.Link);
        String terminationPointId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        Assert.assertEquals("node:1", nodeId);
        Assert.assertEquals("link:1", linkId);
        Assert.assertEquals("tp:1", terminationPointId);
        linkId = idGenerator.getNextIdentifier(CorrelationItemEnum.Link);
        terminationPointId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        terminationPointId = idGenerator.getNextIdentifier(CorrelationItemEnum.TerminationPoint);
        Assert.assertEquals("node:1", nodeId);
        Assert.assertEquals("link:2", linkId);
        Assert.assertEquals("tp:3", terminationPointId);
    }

    @Test
    public void testConcurrentIdGeneration() {
        concurrentDeque = new ConcurrentLinkedDeque<>();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) {
            taskExecutor.execute(new IdGeneratorRunnable());
        }
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Error while waiting for the threads to terminate. " + e);
        }
        for (int i = 1; i <= THREADS * IDS_PER_THREAD; i++) {
            String nodeToBeFound = "node:" + i;
            boolean nodeFound = concurrentDeque.remove(nodeToBeFound);
            if (!nodeFound) {
                Assert.fail("Concurrent Id generation error. In generated nodes ID array, following Node was not found: "
                        + nodeToBeFound);
            }
        }
        if (concurrentDeque.size() > 0) {
            Assert.fail("Concurrent Id generation error. Following Nodes should not be present in the generated Nodes ID"
                    + concurrentDeque.toString());
        }
    }

}
