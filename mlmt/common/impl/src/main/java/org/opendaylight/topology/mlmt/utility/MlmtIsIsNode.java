/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.Iso;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.TedNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;

public class MlmtIsIsNode {

    static IsoSystemId isoSystemId(final Node node) {
        final Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 == null) {
            return null;
        }
        final IgpNodeAttributes igpNodeAttributes = node1.getIgpNodeAttributes();
        if (igpNodeAttributes == null) {
            return null;
        }
        IgpNodeAttributes1 igpNodeAttributes1 = igpNodeAttributes.getAugmentation(IgpNodeAttributes1.class);
        if (igpNodeAttributes1 == null) {
            return null;
        }
        IsisNodeAttributes isisNodeAttributes = igpNodeAttributes1.getIsisNodeAttributes();
        if (isisNodeAttributes == null) {
            return null;
        }
        Iso iso = isisNodeAttributes.getIso();
        if (iso == null) {
            return null;
        }

        return iso.getIsoSystemId();
    }

    static Ipv4Address ipv4Address(final Node node) {
        final Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 == null) {
            return null;
        }
        final IgpNodeAttributes igpNodeAttributes = node1.getIgpNodeAttributes();
        if (igpNodeAttributes == null) {
            return null;
        }
        IgpNodeAttributes1 igpNodeAttributes1 = igpNodeAttributes.getAugmentation(IgpNodeAttributes1.class);
        if (igpNodeAttributes1 == null) {
            return null;
        }
        IsisNodeAttributes isisNodeAttributes = igpNodeAttributes1.getIsisNodeAttributes();
        if (isisNodeAttributes == null) {
            return null;
        }
        TedNodeAttributes tedNodeAttributes = isisNodeAttributes.getTed();
        if (tedNodeAttributes == null) {
            return null;
        }

        return tedNodeAttributes.getTeRouterIdIpv4();
    }
}
