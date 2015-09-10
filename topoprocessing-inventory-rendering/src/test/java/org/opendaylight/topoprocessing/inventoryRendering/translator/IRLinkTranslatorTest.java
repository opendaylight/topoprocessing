/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.topoprocessing.inventoryRendering.translator;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author andrej.zan
 *
 */
public class IRLinkTranslatorTest {
    private final IRLinkTranslator linkTranslator = new IRLinkTranslator();

    @Test
    public void testTranslate() {
        Assert.assertNull(linkTranslator.translate(null));
    }
}
