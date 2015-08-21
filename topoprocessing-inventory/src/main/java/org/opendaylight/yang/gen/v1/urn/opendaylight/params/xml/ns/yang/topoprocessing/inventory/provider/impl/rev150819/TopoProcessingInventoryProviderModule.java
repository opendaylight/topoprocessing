package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.inventory.provider.impl.rev150819;

import org.opendaylight.topoprocessing.inventory.TopoProcessingInventoryProvider;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;

public class TopoProcessingInventoryProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.inventory.provider.impl.rev150819.AbstractTopoProcessingInventoryProviderModule {

    public TopoProcessingInventoryProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TopoProcessingInventoryProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.inventory.provider.impl.rev150819.TopoProcessingInventoryProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        TopoProcessingProvider topoProvider = getTopoprocessingProviderDependency();
        TopoProcessingInventoryProvider inventoryProvider = new TopoProcessingInventoryProvider();
        inventoryProvider.startup(topoProvider);
        return inventoryProvider;
    }

}
