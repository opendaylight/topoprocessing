/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.translator;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.topoprocessing.api.structure.OverlayItem;
import org.opendaylight.topoprocessing.api.structure.UnderlayItem;
import org.opendaylight.topoprocessing.impl.structure.OverlayItemWrapper;
import org.opendaylight.topoprocessing.impl.util.TopologyQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * @author michal.polkorab
 *
 */
public class OverlayItemTranslatorLinkTest {

    private OverlayItemTranslator translator = new OverlayItemTranslator();
    private static final String TOPOLOGY_NAME = "topology:1";

    @Mock private NormalizedNode<?, ?> mockNormalizedLink;
    @Mock private UnderlayItem mockUnderlayLink;

    /**
     * Tests link-id translation
     */
    @Test
    public void test() {
        String wrapperName = "overlaylink:1";
        UnderlayItem underlayLink = new UnderlayItem(mockNormalizedLink, null, TOPOLOGY_NAME, "link:1",
                CorrelationItemEnum.Link);
        OverlayItem logicalNode = new OverlayItem(Collections.singletonList(underlayLink), CorrelationItemEnum.Link);
        OverlayItemWrapper wrapper = new OverlayItemWrapper(wrapperName, logicalNode);
        NormalizedNode<?, ?> translatedLink = translator.translate(wrapper);

        NormalizedNode<?, ?> linkId = NormalizedNodes.findNode(translatedLink,
                YangInstanceIdentifier.of(TopologyQNames.NETWORK_LINK_ID_QNAME)).get();
        Assert.assertEquals("Link ID should be the same as the OverlayItemWrapper ID",
                wrapperName, linkId.getValue());
    }

    /**
     * Tests supporting links translation
     */
    @Test
    public void testSupportingLinks() {
        String topologyName = "mytopo:1";
        String wrapperName = "overlaylink:1";
        String name1 = "link:11";
        String name2 = "link:12";
        String name3 = "link:13";
        String name4 = "link:14";
        String name5 = "link:15";

        List<UnderlayItem> underlayLinks1 = new ArrayList<>();
        UnderlayItem underlayLink1 = new UnderlayItem(mockNormalizedLink, null, topologyName, name1,
                CorrelationItemEnum.Link);
        UnderlayItem underlayLink2 = new UnderlayItem(mockNormalizedLink, null, topologyName, name2,
                CorrelationItemEnum.Link);
        UnderlayItem underlayLink3 = new UnderlayItem(mockNormalizedLink, null, topologyName, name3,
                CorrelationItemEnum.Link);
        underlayLinks1.add(underlayLink1);
        underlayLinks1.add(underlayLink2);
        underlayLinks1.add(underlayLink3);
        OverlayItem overlayLink1 = new OverlayItem(underlayLinks1, CorrelationItemEnum.Link);

        List<UnderlayItem> underlayLinks2 = new ArrayList<>();
        UnderlayItem underlayLink4 = new UnderlayItem(mockNormalizedLink, null, topologyName, name4,
                CorrelationItemEnum.Link);
        UnderlayItem underlayLink5 = new UnderlayItem(mockNormalizedLink, null, topologyName, name5,
                CorrelationItemEnum.Link);
        underlayLinks2.add(underlayLink4);
        underlayLinks2.add(underlayLink5);
        OverlayItem overlayLink2 = new OverlayItem(underlayLinks2, CorrelationItemEnum.Link);

        // process
        OverlayItemWrapper wrapper = new OverlayItemWrapper(wrapperName, overlayLink1);
        wrapper.addOverlayItem(overlayLink2);
        NormalizedNode<?, ?> normalizedNode = translator.translate(wrapper);

        // supporting-links
        Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> mapNode =
              ((MapEntryNode) normalizedNode).getChild(new NodeIdentifier(SupportingLink.QNAME));
        Assert.assertTrue("OverlayLink should contain UnderlayLinks", mapNode.isPresent());
        Collection<MapEntryNode> entryNodes = (Collection<MapEntryNode>) mapNode.get().getValue();
        Assert.assertEquals("OverlayLink contains wrong amount of UnderlayLinks", 5, entryNodes.size());
    }
}
