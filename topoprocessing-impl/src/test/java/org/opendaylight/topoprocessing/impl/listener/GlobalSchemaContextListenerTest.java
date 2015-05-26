package org.opendaylight.topoprocessing.impl.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import static org.junit.Assert.*;

/**
 * @author matus.marko
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalSchemaContextListenerTest {

    @Mock private GlobalSchemaContextHolder schemaHolder;
    @Mock private SchemaContext context;

    @Test
    public void testOnGlobalContextUpdated() throws Exception {
        GlobalSchemaContextListener listener = new GlobalSchemaContextListener(schemaHolder);
        listener.onGlobalContextUpdated(context);
        Mockito.verify(schemaHolder).updateSchemaContext(context);
    }
}
