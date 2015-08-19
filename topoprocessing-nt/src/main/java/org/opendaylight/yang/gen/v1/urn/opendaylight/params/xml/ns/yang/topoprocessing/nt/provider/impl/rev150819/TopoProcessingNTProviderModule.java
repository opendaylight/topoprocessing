package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.nt.provider.impl.rev150819;

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
        // TODO:implement
        LOG.info("I am in topoprocessing nt module createInstance() method");
        TopoProcessingProvider topoProvider = getTopoprocessingProviderDependency();
        if(topoProvider != null)
            LOG.info("topoProvider is set to: {}", topoProvider);
        else
            LOG.info("Error, topoProvider is not set");

        Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    LOG.info("I am in topoprocessing nt module createInstance() method");
            };
        };
        Thread th = new Thread(runnable);
        th.start();
        return null;
    }

}
