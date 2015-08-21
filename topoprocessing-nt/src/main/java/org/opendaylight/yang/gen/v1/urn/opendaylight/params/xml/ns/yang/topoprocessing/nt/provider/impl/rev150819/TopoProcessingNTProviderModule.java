package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.nt.provider.impl.rev150819;

import org.opendaylight.topoprocessing.nt.TopoProcessingNTProvider;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoProcessingNTProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.nt.provider.impl.rev150819.AbstractTopoProcessingNTProviderModule {

    private static final Logger LOG = LoggerFactory.getLogger(TopoProcessingNTProviderModule.class);
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
        TopoProcessingNTProvider ntProvider = new TopoProcessingNTProvider();
        ntProvider.startup(topoProvider);
        return ntProvider;
    }

}
