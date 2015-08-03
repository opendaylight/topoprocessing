/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator;

import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.topoprocessing.impl.util.InstanceIdentifiers;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.topoprocessing.impl.writer.TopologyWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * @author michal.polkorab
 */
@RunWith(MockitoJUnitRunner.class)
public class RpcRepublisherTest {

    private static final String TOPOLOGY1 = "pcep-topology:1";
    private static final String NODE_ID1 = "pcep:1";

    @Mock private RpcServices mockRpcServices;
    @Mock private GlobalSchemaContextHolder mockSchemaHolder;
    @Mock private NormalizedNode<?,?> mockNormalizedNode1;
    @Mock private DOMRpcService mockDOMRpcService;
    @Mock private ListenerRegistration<DOMRpcAvailabilityListener> mockListenerRegistration;
    @Mock private TopologyWriter writer;
    @Mock private SchemaContext mockSchemaContext;
    @Mock private DOMRpcProviderService mockDomRpcProviderService;
    @Mock private DOMRpcImplementationRegistration<DOMRpcImplementation> mockDomRpcImplementationRegistration;

    @Mock private DOMRpcImplementation mockDomRpcImplementation;
    @Mock private DOMRpcIdentifier mockDomRpcIdentifier;
    private YangInstanceIdentifier identifier = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
            .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY1).build();
    private OverlayItem logicalNode;

     /**
     * Republishes RPCs registered on underlay's topology node for the overlay's topology node.
     */
    @Test
    public void testRpcRepublishing() {
        RpcRepublisher republisher = new RpcRepublisher(mockRpcServices, identifier, mockSchemaHolder);
        Mockito.when(mockSchemaHolder.getSchemaContext()).thenReturn(mockSchemaContext);
        Mockito.when(mockRpcServices.getRpcProviderService()).thenReturn(mockDomRpcProviderService);
        Mockito.when(mockRpcServices.getRpcProviderService().registerRpcImplementation(
                (DOMRpcImplementation) any(),(DOMRpcIdentifier) any()))
                .thenReturn(mockDomRpcImplementationRegistration);

        YangInstanceIdentifier contextReference = YangInstanceIdentifier.builder(InstanceIdentifiers.TOPOLOGY_IDENTIFIER)
                .nodeWithKey(Topology.QNAME, TopologyQNames.TOPOLOGY_ID_QNAME, TOPOLOGY1)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, TopologyQNames.NETWORK_NODE_ID_QNAME, NODE_ID1)
                .build();

        SchemaPath schemaPath = SchemaPath.create(true, TopologyQNames.NETWORK_NODE_ID_QNAME);
        DOMRpcIdentifier domRpcIdentifier = DOMRpcIdentifier.create(schemaPath, contextReference);
        Collection<DOMRpcIdentifier> rpcs = new ArrayList<>();
        rpcs.add(domRpcIdentifier);
        republisher.onRpcAvailable(rpcs);

        List<UnderlayItem> physicalNodes = new ArrayList<>();
        UnderlayItem physicalNode = new UnderlayItem(mockNormalizedNode1, null, TOPOLOGY1, NODE_ID1, CorrelationItemEnum.Node);
        physicalNodes.add(physicalNode);
        logicalNode = new OverlayItem(physicalNodes, CorrelationItemEnum.Node);

        OverlayItemWrapper wrapper = new OverlayItemWrapper("node:1", logicalNode);
        republisher.registerRpcs(wrapper, logicalNode);

        Mockito.verify(mockDomRpcProviderService, Mockito.times(1))
            .registerRpcImplementation((DOMRpcImplementation) any(),(Set<DOMRpcIdentifier>) any());
    }

}
