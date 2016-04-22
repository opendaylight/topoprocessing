/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.operator.filtrator;

import com.google.common.base.Preconditions;

import org.opendaylight.topoprocessing.impl.structure.ScriptResult;
import com.google.common.base.Optional;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.opendaylight.topoprocessing.api.filtration.Filtrator;
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
public class ScriptFiltrator extends AbstractFiltrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptFiltrator.class);
    private ScriptEngine engine;
    private String script;

    /**
     * Creates {@link ScriptFiltrator} instance
     * @param scripting script configuration - defines script language and script used
     * @param pathIdentifier path to node that the filtration
     */
    public ScriptFiltrator(Scripting scripting, YangInstanceIdentifier pathIdentifier) {
        super(pathIdentifier);
        Preconditions.checkNotNull(scripting, "Scripting configuration can't be null.");
        ScriptEngineManager factory = new ScriptEngineManager();
        engine = factory.getEngineByName(scripting.getLanguage());
        Preconditions.checkNotNull(engine, "ScriptEngine for language {} was not found.", scripting.getLanguage());
        LOGGER.debug("Engine for language {} is: {}", scripting.getLanguage(), engine);
        LOGGER.debug("Script provided : {}", scripting.getScript());
        script = scripting.getScript();
    }

    @Override
    public boolean isFiltered(NormalizedNode<?, ?> node) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Item received: {}", node);
        }
        ScriptResult filterOut = new ScriptResult();
        engine.put("filterOut", filterOut);
        engine.put("node", node);
        try {
            engine.eval(script);
            LOGGER.debug("Item filtered out: {}", filterOut.getResult());
            return filterOut.getResult();
        } catch (ScriptException e) {
            throw new IllegalStateException("Problem while evaluating script: " + script, e);
        }
    }

}
