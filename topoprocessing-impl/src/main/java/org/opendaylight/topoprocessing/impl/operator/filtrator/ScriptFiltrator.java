/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import com.google.common.base.Optional;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.opendaylight.topoprocessing.api.filtration.Filtrator;
import org.opendaylight.topoprocessing.api.filtration.UnderlayItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.scripting.grouping.Scripting;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters items based on provided script
 * @author michal.polkorab
 */
public class ScriptFiltrator implements Filtrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptFiltrator.class);
    private YangInstanceIdentifier pathIdentifier;
    private ScriptEngine engine;
    private String script;

    /**
     * Creates {@link ScriptFiltrator} instance
     * @param scripting 
     * @param pathIdentifier 
     */
    public ScriptFiltrator(Scripting scripting, YangInstanceIdentifier pathIdentifier) {
        this.pathIdentifier = pathIdentifier;
        ScriptEngineManager factory = new ScriptEngineManager();
        engine = factory.getEngineByName(scripting.getLanguage());
        script = scripting.getScript();
    }

    @Override
    public boolean isFiltered(UnderlayItem item) {
        Optional<NormalizedNode<?, ?>> node = NormalizedNodes.findNode(item.getNode(), pathIdentifier);
        if (node.isPresent()) {
            engine.put("node", node);
            try {
                engine.eval(script);
            } catch (ScriptException e) {
                throw new IllegalStateException("Problem while evaluating script: " + script, e);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Node with value {} was filtered out", item.getNode());
        }
        return true;
    }

}
