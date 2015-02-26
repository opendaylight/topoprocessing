package org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.impl.rev150123;
public class ForwardingAdjacencyTopologyProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.topology.forwarding.adjacency.impl.rev150123.AbstractForwardingAdjacencyTopologyProviderModule {
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
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public boolean canReuse (org.opendaylight.controller.config.spi.Module module) {
        return true;
    }
}
