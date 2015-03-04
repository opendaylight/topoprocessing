package org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.impl.rev150122;

import org.opendaylight.topology.multitechnology.MultitechnologyTopologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

public class MultitechnologyTopologyProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.impl.rev150122.AbstractMultitechnologyTopologyProviderModule {
    private static final Logger LOG = LoggerFactory.getLogger(MultitechnologyTopologyProviderModule.class);

    public MultitechnologyTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MultitechnologyTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.topology.multitechnology.impl.rev150122.MultitechnologyTopologyProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final MultitechnologyTopologyProvider mtTopologyProvider = new MultitechnologyTopologyProvider();

        DataBroker dataBrokerService = getDataBrokerDependency();
        mtTopologyProvider.setDataProvider(dataBrokerService);
        LOG.info("*** MultitechnologyTopologyProviderModule createInstance. ***");

        final MultitechnologyTopologyProviderRuntimeRegistration runtimeReg = getRootRuntimeBeanRegistratorWrapper().register(
                mtTopologyProvider);

       final class AutoCloseableMultitechnologyTopology implements AutoCloseable {

           @Override
           public void close() throws Exception {
               runtimeReg.close();
               closeQuietly(mtTopologyProvider);
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

       AutoCloseable ret = new AutoCloseableMultitechnologyTopology();
       LOG.info("Multitechnology provider (instance {}) initialized.", ret);
       return ret;
    }
}
