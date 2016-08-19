package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209;

import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.spi.provider.TopoProcessingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.osgi.framework.BundleContext;


public class TopoProcessingProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.AbstractTopoProcessingProviderModule {

    private BundleContext bundleContext;

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
        final WaitingServiceTracker<TopoProcessingProvider> tracker = WaitingServiceTracker.create(TopoProcessingProvider.class, bundleContext);
        final TopoProcessingProvider topoService = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final class MyAutoCloseableService implements TopoProcessingProvider {

            @Override
            public void close() {
                tracker.close();
            }

            @Override
            public void unregisterFiltratorFactory(Class<? extends FilterBase> filterType) {
                topoService.unregisterFiltratorFactory(filterType);
            }

            @Override
            public void registerFiltratorFactory(Class<? extends FilterBase> filterType,
                    FiltratorFactory filtratorFactory) {
                topoService.registerFiltratorFactory(filterType, filtratorFactory);
            }

            @Override
            public void registerModelAdapter(Class<? extends Model> model, Object modelAdapter) {
                topoService.registerModelAdapter(model, modelAdapter);
            }

            @Override
            public void startup() {
                topoService.startup();
            }
        }

        return new MyAutoCloseableService();
    }

    void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
