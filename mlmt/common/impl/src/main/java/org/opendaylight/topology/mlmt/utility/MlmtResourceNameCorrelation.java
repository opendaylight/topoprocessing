/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;

public interface MlmtResourceNameCorrelation {

    void putMlmtNodeId(final String nodeName, final NodeId nodeId);
    void removeMlmtNodeId(final String nodeName);
    String getMlmtNodeName(final NodeId nodeId);
    NodeId getMlmtNodeId(final String nodeName);

    void putMlmtTerminationPointId(final String nodeName, final String tpName, final TpId tpId);
    void removeMlmtTerminationPointId(final String nodeName, final String tpName);
    TpId getMlmtTerminationPointId(final String nodeName, final String tpName);
    String getMlmtTerminationPointName(final NodeId nodeId, final TpId tpId);

    void putMlmtLinkId(final String linkName, final LinkId linkId);
    void removeMlmtLinkId(final String linkName);
    LinkId getMlmtLinkId(final String linkName);
    String getMlmtLinkName(final LinkId linkId);

    void putUnderNodeId(final NodeId underNodeId, final NodeId mlmtNodeId);
    void removeUnderNodeId(final NodeId underNodeId);
    NodeId getUnderNodeId2Mlmt(final NodeId underNodeId);
    NodeId getMlmtNodeId2Under(final NodeId mlmtNodeId);

    void putUnderTerminationPointId2Mlmt(final NodeId underNodeId, final TpId underTpId, final TpId tpId);
    void removeUnderTerminationPointId2Mlmt(final NodeId underNodeId, final TpId underTpId);
    TpId getUnderTerminationPointId2Mlmt(final NodeId underNodeId, final TpId underTpId);

    void putUnderLinkId2Mlmt(final LinkId underlinkId, final LinkId linkId);
    LinkId getUnderLinkId2Mlmt(final LinkId underlinkId);
    void removeUnderLinkId2Mlmt(final LinkId underlinkId);

    void putIsoSystemId2NodeName(final IsoSystemId isoSystemId, final String nodeName);
    void removeIsoSystemId2NodeName(final IsoSystemId isoSystemId);
    String getNodeName(final IsoSystemId isoSystemId);

    void putIsoSystemId2NodeId(final IsoSystemId isoSystemId, final NodeId nodeId);
    void removeIsoSystemId2NodeId(final IsoSystemId isoSystemId);
    void removeNodeId2IsoSystemId(final NodeId nodeId);
    NodeId getNodeId(final IsoSystemId isoSystemId);
    IsoSystemId getIsoSystemId(final NodeId nodeId);

    void putIpv4Address2NodeName(final Ipv4Address ipv4Address, final String nodeName);
    void removeIpv4Address2NodeName(final Ipv4Address ipv4Address);
    String getNodeName(final Ipv4Address ipv4Address);

    void putIpv4Address2NodeId(final Ipv4Address ipv4Address, final NodeId nodeId);
    void removeIpv4Address2NodeId(final Ipv4Address ipv4Address);
    void removeNodeId2Ipv4Address(final NodeId nodeId);
    NodeId getNodeId(final Ipv4Address ipv4Address);
    Ipv4Address getIpv4Address(final NodeId nodeId);

    void putIpv4Address2TpId(final Ipv4Address ipv4Address, final String nodeName, final TpId TpId);
    TpId getTpId(final Ipv4Address ipv4Address, final String nodeName);
    void removeTpId2Ipv4Address(final Ipv4Address ipv4Address, final String nodeName);

    void putNodeId2NodeName(final NodeId nodeId, final String nodeName);
    String getNodeName(final NodeId nodeId);

    void putInvNodeKey2NodeId(final NodeKey invNodeKey, final NodeId nodeId);
    NodeId getNodeId(final NodeKey invNodeKey);

    void putInvNodeConnectorKey2TpId(final NodeKey invNodeKey,
            final NodeConnectorKey nodeConnectorKey, final TpId tpId);

    TpId getTpId(final NodeKey invNodeKey, final NodeConnectorKey nodeConnectorKey);
}
