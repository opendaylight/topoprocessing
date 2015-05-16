package org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.impl.rev150123;
import org.opendaylight.topology.multilayer.MultilayerTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.rev150123.MultilayerTopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

public class MultilayerTopologyProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.impl.rev150123.AbstractMultilayerTopologyProviderModule {
    private static final Logger LOG = LoggerFactory.getLogger(MultilayerTopologyProviderModule.class);

    public MultilayerTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MultilayerTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multilayer.impl.rev150123.MultilayerTopologyProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        LOG.info("*** MultilayerTopologyProviderModule createInstance. ***");

        final MultilayerTopologyProvider mlTopologyProvider = new MultilayerTopologyProvider();

        DataBroker dataBrokerService = getDataBrokerDependency();
        mlTopologyProvider.setDataProvider(dataBrokerService);

        final BindingAwareBroker.RpcRegistration<MultilayerTopologyService> rpcRegistration = getRpcRegistryDependency()
                .addRpcImplementation(MultilayerTopologyService.class, mlTopologyProvider);

        final MultilayerTopologyProviderRuntimeRegistration runtimeReg = getRootRuntimeBeanRegistratorWrapper().register(
                mlTopologyProvider);

       final class AutoCloseableMultilayerTopology implements AutoCloseable {

           @Override
           public void close() throws Exception {
               runtimeReg.close();
               closeQuietly(mlTopologyProvider);
               LOG.info("MultiTechnology provider (instance {}) torn down.", this);
           }

           private void closeQuietly(final AutoCloseable resource) {
               try {
                   resource.close();
               } catch (final Exception e) {
                   LOG.debug("Ignoring exception while closing {}", resource, e);
               }
           }
       }

       AutoCloseable ret = new AutoCloseableMultilayerTopology();
       LOG.info("Multilayer provider (instance {}) initialized.", ret);
       return ret;
    }
}
