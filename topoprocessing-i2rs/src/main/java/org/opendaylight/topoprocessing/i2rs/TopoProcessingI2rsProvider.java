package org.opendaylight.topoprocessing.i2rs;

import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoProcessingI2rsProvider implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TopoProcessingI2rsProvider.class);

    @Override
    public void close() throws Exception {
        LOGGER.info("TopoprocessingI2rsProvider close");
    }

    public void startup(TopoProcessingProvider topoProvider) {
        LOGGER.info("TopoprocessingI2rsProvider startup");
    }

}
