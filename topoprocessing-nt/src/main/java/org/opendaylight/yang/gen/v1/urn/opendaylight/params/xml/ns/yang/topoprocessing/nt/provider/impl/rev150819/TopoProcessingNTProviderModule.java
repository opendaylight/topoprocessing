package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.nt.provider.impl.rev150819;

import org.opendaylight.topoprocessing.nt.provider.TopoProcessingProviderNT;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;

public class TopoProcessingNTProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.nt.provider.impl.rev150819.AbstractTopoProcessingNTProviderModule {

    public TopoProcessingNTProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TopoProcessingNTProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.nt.provider.impl.rev150819.TopoProcessingNTProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        TopoProcessingProvider topoProvider = getTopoprocessingProviderDependency();
        TopoProcessingProviderNT ntProvider = new TopoProcessingProviderNT();
        ntProvider.startup(topoProvider);
        return ntProvider;
    }

}
