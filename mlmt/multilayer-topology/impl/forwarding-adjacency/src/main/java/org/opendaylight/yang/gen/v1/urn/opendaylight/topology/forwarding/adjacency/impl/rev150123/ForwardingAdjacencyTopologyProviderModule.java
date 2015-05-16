package org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.impl.rev150123;
import org.opendaylight.topology.forwarding.adjacency.ForwardingAdjacencyTopologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

public class ForwardingAdjacencyTopologyProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.impl.rev150123.AbstractForwardingAdjacencyTopologyProviderModule {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardingAdjacencyTopologyProviderModule.class);

    public ForwardingAdjacencyTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ForwardingAdjacencyTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.impl.rev150123.ForwardingAdjacencyTopologyProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

     @Override
       public java.lang.AutoCloseable createInstance() {

           LOG.info("*** ForwardingAdjacencyTopologyProviderModule createInstance. ***");

           final ForwardingAdjacencyTopologyProvider faTopologyProvider = new ForwardingAdjacencyTopologyProvider();

           DataBroker dataBrokerService = getDataBrokerDependency();
           faTopologyProvider.setDataProvider(dataBrokerService);

           final ForwardingAdjacencyTopologyProviderRuntimeRegistration runtimeReg = getRootRuntimeBeanRegistratorWrapper().register(
                   faTopologyProvider);

          final class AutoCloseableFaTopology implements AutoCloseable {

              @Override
              public void close() throws Exception {
                  runtimeReg.close();
                  closeQuietly(faTopologyProvider);
                  LOG.info("Multilayer provider (instance {}) torn down.", this);
              }

              private void closeQuietly(final AutoCloseable resource) {
                  try {
                      resource.close();
                  } catch (final Exception e) {
                      LOG.debug("Ignoring exception while closing {}", resource, e);
                  }
              }
          }

          AutoCloseable ret = new AutoCloseableFaTopology();
          LOG.info("ForwardingAdjacency provider (instance {}) initialized.", ret);
          return ret;
       }
}
