/*
 * Copyright (c) 2015 Ericsson, AB.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topology.mlmt.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

public class MlmtTpIdIpAddress {

    public static String ipv4Address(final TerminationPoint tp) {
        String tpIdString = tp.getKey().getTpId().getValue().toString();
        String pattern = "^.*?(?=ipv4=(.*?)(?=&))";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(tpIdString);
        if (!m.find()) {
            pattern = "ipv4=((?:[1-9]{1,3}\\.){3}[0-9]{1,3})$";
            r = Pattern.compile(pattern);
            m = r.matcher(tpIdString);
            if (!m.find()) {
                return null;
            }
        }

        return m.group(1);
    }
}
