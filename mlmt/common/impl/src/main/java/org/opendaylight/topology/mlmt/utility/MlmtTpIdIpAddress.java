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

public class MlmtTpIdIpAddress {

    private static final String IPV4ADDRESS_PATTERN =
         "(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}"
         + "([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(%[\\p{N}\\p{L}]+)?";

    private static final String[] PATTERN_LIST = {"^.*?(?=ipv4=(.*?)(?=&))", "^.*?(?=ipv4=(.*?)$)"};

    private static boolean isIpv4(final String ipv4String) {
        String pattern = IPV4ADDRESS_PATTERN;
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(ipv4String);
        return m.matches();
    }

    private static String matchString(final String pattern, final String tpIdString) {
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(tpIdString);
        if (m.find()) {
            String ipv4String = m.group(1);
            if (isIpv4(ipv4String)) {
                return ipv4String;
            }
        }

        return null;
    }

    public static String ipv4Address(final TerminationPoint tp) {
        String tpIdString = tp.getKey().getTpId().getValue().toString();
        for (String pattern : PATTERN_LIST) {
            String match = matchString(pattern, tpIdString);
            if (match != null) {
                return match;
            }
        }

        return null;
    }
}
