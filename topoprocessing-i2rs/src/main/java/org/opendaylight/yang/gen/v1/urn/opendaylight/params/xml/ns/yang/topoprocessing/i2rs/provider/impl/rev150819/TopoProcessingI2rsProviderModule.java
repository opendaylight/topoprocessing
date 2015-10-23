package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.i2rs.provider.impl.rev150819;

import org.opendaylight.topoprocessing.i2rs.provider.TopoProcessingProviderI2RS;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;

public class TopoProcessingI2rsProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.i2rs.provider.impl.rev150819.AbstractTopoProcessingI2rsProviderModule {

    public TopoProcessingI2rsProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TopoProcessingI2rsProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.i2rs.provider.impl.rev150819.TopoProcessingI2rsProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        TopoProcessingProvider topoProvider = getTopoprocessingProviderDependency();
        TopoProcessingProviderI2RS i2rsProvider = new TopoProcessingProviderI2RS();
        i2rsProvider.startup(topoProvider);
        return i2rsProvider;
    }

}
