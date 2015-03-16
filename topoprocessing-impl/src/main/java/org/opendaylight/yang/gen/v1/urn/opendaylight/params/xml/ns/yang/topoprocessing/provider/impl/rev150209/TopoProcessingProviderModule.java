package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209;

import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoProcessingProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.AbstractTopoProcessingProviderModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopoProcessingProviderModule.class);

    public TopoProcessingProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TopoProcessingProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.TopoProcessingProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOGGER.error("TOPOPROCESSING CREATE INSTANCE");
        return new TopoProcessingProviderImpl(getSchemaServiceDependency(), getDomDataBrokerDependency());
    }

}
