package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.inventology.provider.impl.rev150831;

import org.opendaylight.topoprocessing.inventology.provider.TopoProcessingProviderItg;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;

public class TopoProcessingProviderItgModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.inventology.provider.impl.rev150831.AbstractTopoProcessingProviderItgModule {
    public TopoProcessingProviderItgModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TopoProcessingProviderItgModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.inventology.provider.impl.rev150831.TopoProcessingProviderItgModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        TopoProcessingProvider topoProvider = getTopoprocessingProviderDependency();
        TopoProcessingProviderItg provider = new TopoProcessingProviderItg();
        provider.startup(topoProvider);
        return provider;
    }

}
