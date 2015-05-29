package org.opendaylight.topoprocessing.impl.util;

import com.google.common.collect.SetMultimap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.util.AbstractSchemaContext;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalSchemaContextHolderTest {

    class SchemaContextTmp extends AbstractSchemaContext {

        @Override
        protected Map<ModuleIdentifier, String> getIdentifiersToSources() {
            return null;
        }

        @Override
        protected SetMultimap<URI, Module> getNamespaceToModules() {
            return null;
        }

        @Override
        protected SetMultimap<String, Module> getNameToModules() {
            return null;
        }

        @Override
        public Set<Module> getModules() {
            return null;
        }
    }

    @Test
    public void test() {
        SchemaContext globalSchemaContext1 = new SchemaContextTmp();
        GlobalSchemaContextHolder globalSchemaContextHolder = new GlobalSchemaContextHolder(globalSchemaContext1);

        // new instance
        Assert.assertEquals("Object should be the same", globalSchemaContext1,
                globalSchemaContextHolder.getSchemaContext());

        Assert.assertEquals("Object should be the same", DataSchemaContextTree.from(globalSchemaContext1),
                globalSchemaContextHolder.getContextTree());

        // update
        SchemaContext globalSchemaContext2 = new SchemaContextTmp();
        globalSchemaContextHolder.updateSchemaContext(globalSchemaContext2);

        Assert.assertEquals("Object should be the same", globalSchemaContext2,
                globalSchemaContextHolder.getSchemaContext());

        Assert.assertEquals("Object should be the same", DataSchemaContextTree.from(globalSchemaContext2),
                globalSchemaContextHolder.getContextTree());
    }
}
