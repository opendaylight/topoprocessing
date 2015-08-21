package org.opendaylight.topoprocessing.inventory;

import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoProcessingInventoryProvider implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(TopoProcessingInventoryProvider.class);

    @Override
    public void close() throws Exception {
        LOGGER.info("TopoprocessingNTProvider close");
    }

    public void startup(TopoProcessingProvider topoProvider) {
        LOGGER.info("TopoprocessingNTProvider startup");
    }
}
