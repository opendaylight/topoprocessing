/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

public class MlmtResourceNameCorrelator implements MlmtResourceNameCorrelation {

    private Map<String, NodeId> mlmtNodeNameMap;
    private Map<String, Map<String, TpId>> mlmtTpNameMap;
    private Map<String, LinkId> mlmtLinkNameMap;

    private Map<NodeId, String> mlmtNodeIdMap;
    private Map<String, Map<TpId, String>> mlmtTpIdMap;
    private Map<LinkId, String> mlmtLinkIdMap;

    private Map<NodeId, NodeId> underNodeId2MlmtNodeId;
    private Map<NodeId, Map<TpId, TpId>> underTpId2MlmtTpId;
    private Map<LinkId, LinkId> underLinkId2MlmtLinkId;

    private Map<NodeId, NodeId> mlmtNodeId2underNodeId;

    private Map<IsoSystemId, String> isoSystemId2NodeName;
    private Map<Ipv4Address, String> ipv4Address2NodeName;

    private Map<IsoSystemId, Map<String, TpId>> isoSystemId2TpId;
    private Map<Ipv4Address, Map<String, TpId>> ipv4Address2TpId;

    private Map<IsoSystemId, NodeId> isoSystemId2NodeId;
    private Map<Ipv4Address, NodeId> ipv4Address2NodeId;

    private Map<NodeId, IsoSystemId> nodeId2IsoSystemId;
    private Map<NodeId, Ipv4Address> nodeId2Ipv4Address;

    private Map<NodeId, String> nodeId2NodeName;

    private Map<NodeKey, NodeId> invNodeKey2NodeId;
    private Map<NodeKey, Map<NodeConnectorKey, TpId>> invNodeConnectorKey2TpId;

    public void init() {
        mlmtNodeNameMap = new HashMap<String, NodeId>();
        mlmtTpNameMap = new HashMap<String, Map<String, TpId>>();
        mlmtLinkNameMap = new HashMap<String, LinkId>();

        mlmtNodeIdMap = new HashMap<NodeId, String>();
        mlmtTpIdMap = new HashMap<String, Map<TpId, String>>();
        mlmtLinkIdMap = new HashMap<LinkId, String>();

        underNodeId2MlmtNodeId = new HashMap<NodeId, NodeId>();
        underTpId2MlmtTpId = new HashMap<NodeId, Map<TpId, TpId>>();
        underLinkId2MlmtLinkId = new HashMap<LinkId, LinkId>();

        mlmtNodeId2underNodeId = new HashMap<NodeId, NodeId>();

        isoSystemId2NodeName = new HashMap<IsoSystemId, String>();
        ipv4Address2NodeName = new HashMap<Ipv4Address, String>();

        isoSystemId2NodeId = new HashMap<IsoSystemId, NodeId>();
        ipv4Address2NodeId = new HashMap<Ipv4Address, NodeId>();

        ipv4Address2TpId = new HashMap<Ipv4Address, Map<String, TpId>>();

        nodeId2IsoSystemId = new HashMap<NodeId, IsoSystemId>();
        nodeId2Ipv4Address = new HashMap<NodeId, Ipv4Address>();

        nodeId2NodeName = new HashMap<NodeId, String>();

        invNodeKey2NodeId = new HashMap<NodeKey, NodeId>();

        invNodeConnectorKey2TpId = new HashMap<NodeKey, Map<NodeConnectorKey, TpId>>();
    }

    @Override
    public void putMlmtNodeId(final String nodeName, final NodeId nodeId) {
        synchronized (mlmtNodeNameMap) {
            mlmtNodeNameMap.put(nodeName, nodeId);
        }
        synchronized (mlmtNodeIdMap) {
            mlmtNodeIdMap.put(nodeId, nodeName);
        }
    }

    @Override
    public void removeMlmtNodeId(final String nodeName) {
        NodeId nodeId = getMlmtNodeId(nodeName);
        synchronized (mlmtNodeNameMap) {
            mlmtNodeNameMap.remove(nodeName);
        }
        if (nodeId != null) {
            synchronized (mlmtNodeIdMap) {
                mlmtNodeIdMap.remove(nodeId);
            }
        }
    }

    @Override
    public String getMlmtNodeName(final NodeId nodeId) {
        synchronized (mlmtNodeIdMap) {
            return mlmtNodeIdMap.get(nodeId);
        }
    }

    @Override
    public NodeId getMlmtNodeId(final String nodeName) {
        synchronized (mlmtNodeNameMap) {
            return mlmtNodeNameMap.get(nodeName);
        }
    }

    @Override
    public void putMlmtTerminationPointId(final String nodeName, final String tpName, final TpId tpId) {
        synchronized (mlmtTpNameMap) {
            Map<String, TpId> tpNameMap = mlmtTpNameMap.get(nodeName);
            if (tpNameMap == null) {
                tpNameMap = new HashMap<String, TpId>();
            }
            tpNameMap.put(tpName, tpId);
            mlmtTpNameMap.put(nodeName, tpNameMap);
        }

        synchronized (mlmtTpIdMap) {
            Map<TpId, String> tpIdMap = mlmtTpIdMap.get(nodeName);
            if (tpIdMap == null) {
                tpIdMap = new HashMap<TpId, String>();
            }
            tpIdMap.put(tpId, tpName);
            mlmtTpIdMap.put(nodeName, tpIdMap);
        }
    }

    @Override
    public void removeMlmtTerminationPointId(final String nodeName, final String tpName) {
        TpId tpId = getMlmtTerminationPointId(nodeName, tpName);
        synchronized (mlmtTpNameMap) {
            Map<String, TpId> tpNameMap = mlmtTpNameMap.get(nodeName);
            if (tpNameMap != null) {
                tpNameMap.remove(tpName);
                if (tpNameMap.size() > 0) {
                    mlmtTpNameMap.put(nodeName, tpNameMap);
                } else {
                    mlmtTpNameMap.remove(nodeName);
                }
            }
        }

        if (tpId != null) {
            synchronized (mlmtTpIdMap) {
                Map<TpId, String> tpIdMap = mlmtTpIdMap.get(nodeName);
                if (tpIdMap != null) {
                    tpIdMap.remove(tpId);
                    if (tpIdMap.size() > 0) {
                        mlmtTpIdMap.put(nodeName, tpIdMap);
                    } else {
                        mlmtTpIdMap.remove(nodeName);
                    }
                }
            }
        }
    }

    @Override
    public TpId getMlmtTerminationPointId(final String nodeName, final String tpName) {
        Map<String, TpId> tpNameMap = null;
        synchronized (mlmtTpNameMap) {
            tpNameMap = mlmtTpNameMap.get(nodeName);
        }
        if (tpNameMap == null) {
            return null;
        }

        return tpNameMap.get(tpName);
    }

    @Override
    public String getMlmtTerminationPointName(final NodeId nodeId, final TpId tpId) {
        String nodeName = null;
        synchronized (mlmtNodeIdMap) {
            nodeName = mlmtNodeIdMap.get(nodeId);
        }
        if (nodeName == null) {
            return null;
        }

        Map<TpId, String> tpIdMap = null;
        synchronized (mlmtTpIdMap) {
            tpIdMap = mlmtTpIdMap.get(nodeName);
        }
        if (tpIdMap == null) {
            return null;
        }

        return tpIdMap.get(tpId);
    }

    @Override
    public void putMlmtLinkId(final String linkName, final LinkId linkId) {
        synchronized (mlmtLinkNameMap) {
            mlmtLinkNameMap.put(linkName, linkId);
        }
        synchronized (mlmtLinkIdMap) {
            mlmtLinkIdMap.put(linkId, linkName);
        }
    }

    @Override
    public void removeMlmtLinkId(final String linkName) {
        LinkId linkId = null;
        synchronized (mlmtLinkNameMap) {
            linkId = mlmtLinkNameMap.get(linkName);
            mlmtLinkNameMap.remove(linkName);
        }
        if (linkId != null) {
            synchronized (mlmtLinkIdMap) {
                mlmtLinkIdMap.remove(linkId);
            }
        }
    }

    @Override
    public LinkId getMlmtLinkId(final String linkName) {
        synchronized (mlmtLinkNameMap) {
            return mlmtLinkNameMap.get(linkName);
        }
    }

    @Override
    public String getMlmtLinkName(final LinkId linkId) {
        synchronized (mlmtLinkIdMap) {
            return mlmtLinkIdMap.get(linkId);
        }
    }

    @Override
    public void putUnderNodeId(final NodeId underNodeId, final NodeId mlmtNodeId) {
        synchronized (underNodeId2MlmtNodeId) {
            underNodeId2MlmtNodeId.put(underNodeId, mlmtNodeId);
        }
        synchronized (mlmtNodeId2underNodeId) {
            mlmtNodeId2underNodeId.put(mlmtNodeId, underNodeId);
        }
    }

    @Override
    public void removeUnderNodeId(final NodeId underNodeId) {
        NodeId mlmtNodeId = getUnderNodeId2Mlmt(underNodeId);
        synchronized (underNodeId2MlmtNodeId) {
            underNodeId2MlmtNodeId.remove(underNodeId);
        }
        if (mlmtNodeId != null) {
            synchronized (mlmtNodeId2underNodeId) {
                mlmtNodeId2underNodeId.remove(mlmtNodeId);
            }
        }
    }

    @Override
    public NodeId getUnderNodeId2Mlmt(final NodeId underNodeId) {
        synchronized (underNodeId2MlmtNodeId) {
            return underNodeId2MlmtNodeId.get(underNodeId);
        }
    }

    @Override
    public NodeId getMlmtNodeId2Under(final NodeId mlmtNodeId) {
        synchronized (mlmtNodeId2underNodeId) {
            return mlmtNodeId2underNodeId.get(mlmtNodeId);
        }
    }

    @Override
    public void putUnderTerminationPointId2Mlmt(final NodeId underNodeId, final TpId underTpId, final TpId tpId) {
        synchronized (underTpId2MlmtTpId) {
            Map<TpId, TpId> underTpIdMap = underTpId2MlmtTpId.get(underNodeId);
            if (underTpIdMap == null) {
                underTpIdMap = new HashMap<TpId, TpId>();
            }
            underTpIdMap.put(underTpId, tpId);
            underTpId2MlmtTpId.put(underNodeId, underTpIdMap);
        }
    }

    @Override
    public void removeUnderTerminationPointId2Mlmt(final NodeId underNodeId, final TpId underTpId) {
        synchronized (underTpId2MlmtTpId) {
            Map<TpId, TpId> underTpIdMap = underTpId2MlmtTpId.get(underNodeId);
            if (underTpIdMap != null) {
                underTpIdMap.remove(underTpId);
                if (underTpIdMap.size() > 0 ) {
                    underTpId2MlmtTpId.put(underNodeId, underTpIdMap);
                } else {
                    underTpId2MlmtTpId.remove(underNodeId);
                }
            }
        }
    }

    @Override
    public TpId getUnderTerminationPointId2Mlmt(final NodeId underNodeId, final TpId underTpId) {
        Map<TpId, TpId> underTpIdMap = null;
        synchronized (underTpId2MlmtTpId) {
            underTpIdMap = underTpId2MlmtTpId.get(underNodeId);
        }
        if (underTpIdMap == null) {
            return null;
        }
        return underTpIdMap.get(underTpId);
    }

    @Override
    public void putIsoSystemId2NodeName(final IsoSystemId isoSystemId, final String nodeName) {
        synchronized (isoSystemId2NodeName) {
            isoSystemId2NodeName.put(isoSystemId, nodeName);
        }
    }

    @Override
    public void removeIsoSystemId2NodeName(final IsoSystemId isoSystemId) {
        synchronized (isoSystemId2NodeName) {
            isoSystemId2NodeName.remove(isoSystemId);
        }
    }

    @Override
    public String getNodeName(final IsoSystemId isoSystemId) {
        synchronized (isoSystemId2NodeName) {
            return isoSystemId2NodeName.get(isoSystemId);
        }
    }

    @Override
    public void putIsoSystemId2NodeId(final IsoSystemId isoSystemId, final NodeId nodeId) {
        synchronized (isoSystemId2NodeId) {
            isoSystemId2NodeId.put(isoSystemId, nodeId);
        }
        synchronized (nodeId2IsoSystemId) {
            nodeId2IsoSystemId.put(nodeId, isoSystemId);
        }
    }

    @Override
    public void removeIsoSystemId2NodeId(final IsoSystemId isoSystemId) {
        NodeId nodeId = isoSystemId2NodeId.get(isoSystemId);
        synchronized (isoSystemId2NodeId) {
            isoSystemId2NodeId.remove(isoSystemId);
        }
        if (nodeId != null) {
            synchronized (nodeId2IsoSystemId) {
                nodeId2IsoSystemId.remove(nodeId);
            }
        }
    }

    @Override
    public void removeNodeId2IsoSystemId(final NodeId nodeId) {
        final IsoSystemId isoSystemId = nodeId2IsoSystemId.get(nodeId);
        synchronized (isoSystemId2NodeId) {
            isoSystemId2NodeId.remove(isoSystemId);
        }
        synchronized (nodeId2IsoSystemId) {
            nodeId2IsoSystemId.remove(nodeId);
        }
    }

    @Override
    public NodeId getNodeId(final IsoSystemId isoSystemId) {
        synchronized (isoSystemId2NodeName) {
            return isoSystemId2NodeId.get(isoSystemId);
        }
    }

    @Override
    public IsoSystemId getIsoSystemId(final NodeId nodeId) {
        synchronized (nodeId2IsoSystemId) {
            return nodeId2IsoSystemId.get(nodeId);
        }
    }

    @Override
    public void putIpv4Address2NodeName(final Ipv4Address ipv4Address, final String nodeName) {
        synchronized (ipv4Address2NodeName) {
            ipv4Address2NodeName.put(ipv4Address, nodeName);
        }
    }

    @Override
    public void removeIpv4Address2NodeName(final Ipv4Address ipv4Address) {
        synchronized (ipv4Address2NodeName) {
            ipv4Address2NodeName.remove(ipv4Address);
        }
    }

    @Override
    public String getNodeName(final Ipv4Address ipv4Address) {
        synchronized (ipv4Address2NodeName) {
            return ipv4Address2NodeName.get(ipv4Address);
        }
    }

    @Override
    public NodeId getNodeId(final Ipv4Address ipv4Address) {
        synchronized (ipv4Address2NodeId) {
            return ipv4Address2NodeId.get(ipv4Address);
        }
    }

    @Override
    public Ipv4Address getIpv4Address(final NodeId nodeId) {
        synchronized (nodeId2Ipv4Address) {
            return nodeId2Ipv4Address.get(nodeId);
        }
    }

    @Override
    public void putIpv4Address2NodeId(final Ipv4Address ipv4Address, final NodeId nodeId) {
        synchronized (ipv4Address2NodeId) {
            ipv4Address2NodeId.put(ipv4Address, nodeId);
        }
        synchronized (nodeId2Ipv4Address) {
            nodeId2Ipv4Address.put(nodeId, ipv4Address);
        }
    }

    @Override
    public void removeIpv4Address2NodeId(final Ipv4Address ipv4Address) {
        final NodeId nodeId = ipv4Address2NodeId.get(ipv4Address);
        synchronized (ipv4Address2NodeId) {
            ipv4Address2NodeId.remove(ipv4Address);
        }
        if (nodeId != null) {
            synchronized (nodeId2Ipv4Address) {
                nodeId2Ipv4Address.remove(nodeId);
            }
        }
    }

    @Override
    public void removeNodeId2Ipv4Address(final NodeId nodeId) {
        final Ipv4Address ipv4Address = nodeId2Ipv4Address.get(nodeId);
        synchronized (ipv4Address2NodeId) {
            ipv4Address2NodeId.remove(ipv4Address);
        }
        synchronized (nodeId2Ipv4Address) {
            nodeId2Ipv4Address.remove(nodeId);
        }
    }

    @Override
    public void putIpv4Address2TpId(final Ipv4Address ipv4Address, final String nodeName, final TpId tpId) {
        synchronized (ipv4Address2TpId) {
            Map<String, TpId> mapTpId = ipv4Address2TpId.get(ipv4Address);
            if (mapTpId == null) {
                mapTpId = new HashMap<String, TpId>();
            }
            mapTpId.put(nodeName, tpId);
            ipv4Address2TpId.put(ipv4Address, mapTpId);
        }
    }

    @Override
    public TpId getTpId(final Ipv4Address ipv4Address, final String nodeName) {
        synchronized (ipv4Address2TpId) {
            Map<String, TpId> mapTpId = ipv4Address2TpId.get(ipv4Address);
            if (mapTpId == null) {
                return null;
            }
            return mapTpId.get(nodeName);
        }
    }

    @Override
    public void removeTpId2Ipv4Address(final Ipv4Address ipv4Address, final String nodeName) {
        synchronized (ipv4Address2TpId) {
            Map<String, TpId> mapTpId = ipv4Address2TpId.get(ipv4Address);
            if (mapTpId == null) {
                return;
            }
            mapTpId.remove(nodeName);
            if (mapTpId.size() > 0) {
                ipv4Address2TpId.put(ipv4Address, mapTpId);
            } else {
                ipv4Address2TpId.remove(ipv4Address);
            }
        }
    }

    @Override
    public void putNodeId2NodeName(final NodeId nodeId, final String nodeName) {
        synchronized (nodeId2NodeName) {
            nodeId2NodeName.put(nodeId, nodeName);
        }
    }

    @Override
    public String getNodeName(final NodeId nodeId) {
        synchronized (nodeId2NodeName) {
            return nodeId2NodeName.get(nodeId);
        }
    }

    @Override
    public void removeNodeId2NodeName(final NodeId nodeId) {
        synchronized (nodeId2NodeName) {
            nodeId2NodeName.remove(nodeId);
        }
    }

    @Override
    public void putInvNodeKey2NodeId(final NodeKey invNodeKey, final NodeId nodeId) {
        synchronized (invNodeKey2NodeId) {
            invNodeKey2NodeId.put(invNodeKey, nodeId);
        }
    }

    @Override
    public NodeId getNodeId(final NodeKey invNodeKey) {
        synchronized (invNodeKey2NodeId) {
            return invNodeKey2NodeId.get(invNodeKey);
        }
    }

    @Override
    public void removeInvNodeKey2NodeId(final NodeKey invNodeKey) {
        synchronized (invNodeKey2NodeId) {
            invNodeKey2NodeId.remove(invNodeKey);
        }
    }

    @Override
    public void putInvNodeConnectorKey2TpId(final NodeKey invNodeKey,
            final NodeConnectorKey nodeConnectorKey,final TpId tpId) {
        synchronized (invNodeConnectorKey2TpId) {
            Map<NodeConnectorKey, TpId> tpIdMap = invNodeConnectorKey2TpId.get(invNodeKey);
            if (tpIdMap == null) {
                tpIdMap = new HashMap<NodeConnectorKey, TpId>();
            }
            tpIdMap.put(nodeConnectorKey, tpId);
            invNodeConnectorKey2TpId.put(invNodeKey, tpIdMap);
        }
    }

    @Override
    public TpId getTpId(final NodeKey invNodeKey, final NodeConnectorKey nodeConnectorKey) {
        synchronized (invNodeConnectorKey2TpId) {
            Map<NodeConnectorKey, TpId> tpIdMap = invNodeConnectorKey2TpId.get(invNodeKey);
            if (tpIdMap == null) {
                return null;
            }
            return tpIdMap.get(nodeConnectorKey);
        }
    }

    @Override
    public void removeInvNodeConnectorKey2TpId(final NodeKey invNodeKey,
            final NodeConnectorKey nodeConnectorKey) {
        synchronized (invNodeConnectorKey2TpId) {
            Map<NodeConnectorKey, TpId> tpIdMap = invNodeConnectorKey2TpId.get(invNodeKey);
            if (tpIdMap == null) {
                return;
            }
            tpIdMap.remove(nodeConnectorKey);
            if (tpIdMap.size() > 0) {
                invNodeConnectorKey2TpId.put(invNodeKey, tpIdMap);
            } else {
                invNodeConnectorKey2TpId.remove(invNodeKey);
            }
        }
    }

    @Override
    public void putUnderLinkId2Mlmt(final LinkId underLinkId, final LinkId linkId) {
        synchronized (underLinkId2MlmtLinkId) {
            underLinkId2MlmtLinkId.put(underLinkId, linkId);
        }
    }

    @Override
    public LinkId getUnderLinkId2Mlmt(final LinkId underLinkId) {
        synchronized (underLinkId2MlmtLinkId) {
            return underLinkId2MlmtLinkId.get(underLinkId);
        }
    }

    @Override
    public void removeUnderLinkId2Mlmt(final LinkId underLinkId) {
        synchronized (underLinkId2MlmtLinkId) {
            underLinkId2MlmtLinkId.remove(underLinkId);
        }
    }
}
