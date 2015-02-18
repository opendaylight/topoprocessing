package org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mlmt.topology.observer.impl.rev150122;

import org.opendaylight.topology.mlmt.observer.MlmtTopologyObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MlmtTopologyObserverModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mlmt.topology.observer.impl.rev150122.AbstractMlmtTopologyObserverModule {
    private static final Logger log = LoggerFactory.getLogger(MlmtTopologyObserverModule.class);

    public MlmtTopologyObserverModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MlmtTopologyObserverModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mlmt.topology.observer.impl.rev150122.MlmtTopologyObserverModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final MlmtTopologyObserver mlmtTopologyObserver = new MlmtTopologyObserver();

        log.info("*** MlmtTopologyObserverModule createInstance. ***");

        final MlmtTopologyObserverRuntimeRegistration runtimeReg = getRootRuntimeBeanRegistratorWrapper().register(
                mlmtTopologyObserver);

        return mlmtTopologyObserver;
    }
}
