package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.topoprocessing.impl.provider.TopoProcessingProviderImpl;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;

public class TopoProcessingProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.AbstractTopoProcessingProviderModule {

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
        Broker broker = getBrokerDependency();
        ProviderSession session = broker.registerProvider(new NoOpTopoprocessingProvider());
        DOMRpcService rpcService = session.getService(DOMRpcService.class);
        DOMRpcProviderService rpcProviderService = session.getService(DOMRpcProviderService.class);
        RpcServices rpcServices = new RpcServices(rpcService, rpcProviderService);
        TopoProcessingProviderImpl provider = new TopoProcessingProviderImpl(getSchemaServiceDependency(),
                getDomDataBrokerDependency(), getBindingNormalizedNodeSerializerDependency(), rpcServices,
                getDatastoreType());
        provider.startup();
        return provider;
    }

}
