/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.impl.rpc;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class OverlayRpcImplementationTest {

    private static final QName CONTEXT_REFERENCE = QName.create("urn:opendaylight:yang:extension:yang-ext",
            "2013-07-09", "context-reference").intern();
    private static final QName RPC_QNAME =
            QName.create("urn:opendaylight:topology:correlation", "2013-10-21", "rpc").intern();
    private static final QName TEST_QNAME =
            QName.create("urn:opendaylight:topology:correlation", "2013-10-21", "test").intern();
    private static final QName AUG_QNAME =
            QName.create("urn:opendaylight:topology:correlation", "2013-10-21", "aug").intern();

    private OverlayRpcImplementation overlayRpcImplementation;
    private YangInstanceIdentifier underlayNodeIdentifier;
    private DOMRpcIdentifier rpcIdentifier;

    @Mock private DOMRpcService rpcService;
    @Mock private SchemaContext schemaContext;
    @Mock private ContainerNode input;
    @Mock private Module module;
    @Mock private RpcDefinition rpcDefinition;
    @Mock private DataContainerChild<? extends PathArgument, ?> child1;
    @Mock private LeafNode<?> child2;
    @Mock private AugmentationNode child3;
    @Mock private ContainerSchemaNode rpcInput;
    @Mock private DataSchemaNode schemaNode;
    @Mock private UnknownSchemaNode extension;

    @Before
    public void setUp() throws Exception {
        underlayNodeIdentifier = YangInstanceIdentifier.of(RPC_QNAME);
        rpcIdentifier =  DOMRpcIdentifier.create(SchemaPath.create(true, RPC_QNAME));
        overlayRpcImplementation = new OverlayRpcImplementation(rpcService, schemaContext, underlayNodeIdentifier);
        Mockito.when(schemaContext.findModuleByNamespaceAndRevision((URI) Matchers.any(), (Date) Matchers.any()))
                .thenReturn(module);
        Mockito.when(module.getRpcs()).thenReturn(Collections.singleton(rpcDefinition));
    }

    @Test
    public void testRpcPresent() {
        Mockito.when(rpcDefinition.getQName()).thenReturn(RPC_QNAME);
        Mockito.when(rpcDefinition.getInput()).thenReturn(rpcInput);
        Mockito.when(rpcInput.getChildNodes()).thenReturn(Collections.singleton(schemaNode));
        Mockito.when(schemaNode.getUnknownSchemaNodes()).thenReturn(Collections.singletonList(extension));
        Mockito.when(schemaNode.getQName()).thenReturn(RPC_QNAME);
        Mockito.when(extension.getNodeType()).thenReturn(CONTEXT_REFERENCE);
        Mockito.when(extension.getQName()).thenReturn(RPC_QNAME);
        Mockito.when(input.getNodeType()).thenReturn(NetworkTopology.QNAME);
        ArrayList<DataContainerChild<? extends PathArgument, ?>> inputChilds = new ArrayList<>();
        inputChilds.add(child1);
        Mockito.when(child1.getNodeType()).thenReturn(RPC_QNAME);
        inputChilds.add(child2);
        Mockito.when(child2.getNodeType()).thenReturn(Node.QNAME);
        Mockito.when(child2.getIdentifier()).thenReturn(new NodeIdentifier(TEST_QNAME));
        inputChilds.add(child3);
        HashSet<QName> set = new HashSet<>();
        set.add(AUG_QNAME);
        Mockito.when(child3.getIdentifier()).thenReturn(new AugmentationIdentifier(set));
        Mockito.when(input.getValue()).thenReturn(inputChilds);
        overlayRpcImplementation.invokeRpc(rpcIdentifier, input);

        Mockito.verify(rpcService).invokeRpc((SchemaPath) Matchers.any(), (NormalizedNode<?, ?>) Matchers.any());
    }

    @Test
    public void testRpcAbsent() {
        Mockito.when(rpcDefinition.getQName()).thenReturn(null);
        CheckedFuture<DOMRpcResult, DOMRpcException> rpc = overlayRpcImplementation.invokeRpc(rpcIdentifier, input);
        Futures.addCallback(rpc, new FutureCallback<DOMRpcResult>() {
            @Override
            public void onSuccess(DOMRpcResult result) {
                Assert.fail("Object retrieving should be canceled");
            }

            @Override
            public void onFailure(Throwable t) {}
        });
    }
}
