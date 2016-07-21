package org.opendaylight.topoprocessing.impl.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.LinkCalculator;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
import org.opendaylight.topoprocessing.impl.operator.filtrator.AbstractFiltrator;
import org.opendaylight.topoprocessing.impl.rpc.RpcServices;
import org.opendaylight.topoprocessing.impl.testUtilities.TestingDOMDataBroker;
import org.opendaylight.topoprocessing.impl.translator.OverlayItemTranslator;
import org.opendaylight.topoprocessing.impl.translator.PathTranslator;
import org.opendaylight.topoprocessing.impl.util.GlobalSchemaContextHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topoprocessing.provider.impl.rev150209.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.AggregationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.CorrelationItemEnum;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Equality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FilterBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationAggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.FiltrationOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.I2rsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.LeafPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.Model;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.NetworkTopologyModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.OpendaylightInventoryModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.RenderingOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.CorrelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.AggregationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.FiltrationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Rendering;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.MappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.LinkComputation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.link.computation.LinkInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.link.computation.LinkInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.link.computation.NodeInfo;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 *
 * @author branislav.janosik
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestHandlerTest {

    @Mock
    private Filter filterMock;
    @Mock
    private Mapping mappingMock;
    @Mock
    private Rendering renderingMock;
    @Mock
    private LeafPath leafpathMock;
    @Mock
    private Map<Class<? extends FilterBase>, FiltratorFactory> filtratorsMock;
    @Mock
    private Map<Class<? extends Model>, ModelAdapter> modelAdaptersMock;
    @Mock
    private RpcServices rpcServicesMock;
    @Mock
    private YangInstanceIdentifier yangInstanceIdentifierMock;
    @Mock
    private PathTranslator pathTranslatorMock;
    @Mock
    private UnderlayTopologyListener underlayTopologyListenerMock;
    @Mock
    private OverlayItemTranslator overlayItemTranslatorMock;
    @Mock
    private ModelAdapter modelAdapterMock;
    @Mock
    private DOMRpcService domRpcServiceMock;
    @Mock
    private GlobalSchemaContextHolder schemaHolderMock;
    @Mock
    private TargetField targetFieldMock;


    private TestTopologyRequestHandler handler;


    @Before
    public void init() {
        TestingDOMDataBroker testingDOMbroker = new TestingDOMDataBroker();
        PingPongDataBroker pingPongBroker = new PingPongDataBroker(testingDOMbroker);
        handler = new TestTopologyRequestHandler(pingPongBroker,schemaHolderMock,rpcServicesMock,null);
        handler.setTranslator(pathTranslatorMock);
        handler.setFiltrators(filtratorsMock);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);
    }

    @Test
    public void testProcessNewRequestFiltration() {
        handler.filtrationOnly = true;

        setFiltrationBehaviour();
        handler.processNewRequest();

        assertTrue(handler.getListeners().size() == 1);
        handler.filtrationOnly = false;
    }

    @Test
    public void testProcessNewRequestAggregation() {
        handler.aggregationOnly = true;

        when(mappingMock.getUnderlayTopology()).thenReturn("topo1");
        when(mappingMock.isAggregateInside()).thenReturn(true);

        TargetFieldBuilder targetFieldBuider = new TargetFieldBuilder()
                .setTargetFieldPath(leafpathMock)
                .setMatchingKey(0);
        List<TargetField> targetFields = new ArrayList<>(1);
        targetFields.add(targetFieldBuider.build());
        when(mappingMock.getTargetField()).thenReturn(targetFields);
        when(leafpathMock.getValue()).thenReturn("f1");
        when(mappingMock.getInputModel()).thenReturn(null);

        List<String> applyFilters = new ArrayList<String>();
        applyFilters.add("filter1");
        when(mappingMock.getApplyFilters()).thenReturn(applyFilters);

        setTestHandler();
        handler.processNewRequest();

        verify(mappingMock, atMost(3)).getApplyFilters();
        assertTrue(handler.getListeners().size() == 3);
        handler.aggregationOnly = false;
    }

    @Test
    public void testProcessNewRequestRendering() {
        handler.renderingOnly = true;

        when(renderingMock.getUnderlayTopology()).thenReturn("topo1");
        when(renderingMock.getInputModel()).thenReturn(null);

        setTestHandler();
        TopologyOperator to = mock(TopologyOperator.class);
        when(underlayTopologyListenerMock.getOperator()).thenReturn(to);
        handler.processNewRequest();

        assertTrue(handler.getListeners().size() == 1);
        handler.renderingOnly = false;
    }

    @Test
    public void testProcessNewRequestFiltrationAggregation() {
        handler.filtrationAggregation = true;
        when(mappingMock.getUnderlayTopology()).thenReturn("topo1");
        when(mappingMock.isAggregateInside()).thenReturn(true);

        TargetFieldBuilder targetFieldBuider = new TargetFieldBuilder()
                .setTargetFieldPath(leafpathMock)
                .setMatchingKey(0);
        List<TargetField> targetFields = new ArrayList<>(1);
        targetFields.add(targetFieldBuider.build());
        when(mappingMock.getTargetField()).thenReturn(targetFields);
        when(leafpathMock.getValue()).thenReturn("f1");
        when(mappingMock.getInputModel()).thenReturn(null);

        setFiltrationBehaviour();

        List<String> applyFilters = new ArrayList<String>();
        applyFilters.add("filter1");
        when(mappingMock.getApplyFilters()).thenReturn(applyFilters);

        setTestHandler();
        handler.processNewRequest();

        verify(mappingMock, times(6)).getApplyFilters();
        assertTrue(handler.getListeners().size() == 3);
        handler.filtrationAggregation = false;
    }

    @Test
    public void testProcessNewRequestAggregationWithFiltrationOfNodesAndTp() {
        handler.aggregationOfNodesAndTps = true;
        setFiltrationBehaviour();
        when(mappingMock.getUnderlayTopology()).thenReturn("topo1");
        when(mappingMock.isAggregateInside()).thenReturn(true);
        TargetFieldBuilder targetFieldBuider = new TargetFieldBuilder()
                .setTargetFieldPath(leafpathMock)
                .setMatchingKey(0);
        List<TargetField> targetFields = new ArrayList<>(1);
        targetFields.add(targetFieldBuider.build());
        when(mappingMock.getTargetField()).thenReturn(targetFields);
        when(leafpathMock.getValue()).thenReturn("f1");
        Mockito.doReturn(NetworkTopologyModel.class).when(mappingMock).getInputModel();
        List<String> applyFilters = new ArrayList<String>();
        applyFilters.add("filter1");
        when(mappingMock.getApplyFilters()).thenReturn(applyFilters);

        handler.processNewRequest();

        assertEquals(handler.getListeners().size(), 2);
        verify(mappingMock, times(8)).getApplyFilters();
        handler.aggregationOfNodesAndTps = false;
    }

    @Test(expected = IllegalStateException.class)
    public void testProcessNewRequestDataMissing() {
        handler.dataMissing = true;
        handler.processNewRequest();
        handler.dataMissing = false;
    }

    private void setFiltrationBehaviour() {
        setTestHandler();

        when(filterMock.getTargetField()).thenReturn(leafpathMock);
        when(leafpathMock.getValue()).thenReturn("f1");
        when(filterMock.getInputModel()).thenReturn(null);
        when(filterMock.getFilterType()).thenReturn(null);
        FiltratorFactory filtratorFactoryMock = mock(FiltratorFactory.class);
        when(filtratorsMock.get(null)).thenReturn(filtratorFactoryMock);
        AbstractFiltrator abstractFiltrator = mock(AbstractFiltrator.class);
        when(filtratorFactoryMock.createFiltrator((Filter)any(),(YangInstanceIdentifier) any()))
                .thenReturn(abstractFiltrator);
        when(abstractFiltrator.getPathIdentifier()).thenReturn(YangInstanceIdentifier.EMPTY);
        when(abstractFiltrator.isFiltered((NormalizedNode<?, ?>) any())).thenReturn(true);
    }

    private void setTestHandler() {
        when(modelAdaptersMock.get(null)).thenReturn(modelAdapterMock);
        when(modelAdaptersMock.get(NetworkTopologyModel.class)).thenReturn(modelAdapterMock);
        when(modelAdapterMock.createOverlayItemTranslator()).thenReturn(overlayItemTranslatorMock);
        InstanceIdentifierBuilder instanceIdentifierBuilderMock = mock(InstanceIdentifierBuilder.class);
        when(modelAdapterMock.createTopologyIdentifier("topo1")).thenReturn(instanceIdentifierBuilderMock);
        when(instanceIdentifierBuilderMock.build()).thenReturn(yangInstanceIdentifierMock);
        when(modelAdapterMock.buildItemIdentifier(instanceIdentifierBuilderMock,
                CorrelationItemEnum.Node)).thenReturn(yangInstanceIdentifierMock);
        when(rpcServicesMock.getRpcService()).thenReturn(domRpcServiceMock);
        when(modelAdapterMock.registerUnderlayTopologyListener((PingPongDataBroker)any(),(String) any(),
                (CorrelationItemEnum) any(),(DatastoreType) any(),(TopologyAggregator) any(),(List) any(),
                (Map<Integer, YangInstanceIdentifier>) any())).thenReturn(underlayTopologyListenerMock);
        when(pathTranslatorMock.translate("f1",CorrelationItemEnum.Node,
                schemaHolderMock,null)).thenReturn(yangInstanceIdentifierMock);
        handler.setModelAdapters(modelAdaptersMock);
    }

    @Test
    public void testInitLinkComputation() {
        LinkComputation linkComputationMock = mock(LinkComputation.class);
        NodeInfo nodeInfoMock = mock(NodeInfo.class);
        LeafPath leafPathMock = mock(LeafPath.class);
        PathTranslator translartorMock = mock(PathTranslator.class);
        InstanceIdentifierBuilder instanceIdentifierBuilderMock = mock(InstanceIdentifierBuilder.class);
        ArrayList<LinkInfo> linksInformations = new ArrayList<LinkInfo>();
        int iterator = 0;

        initLinkComputationConfig(true);

        setTestHandler();
        handler.setLinkComputation(linkComputationMock);
        handler.setTranslator(translartorMock);

        when(modelAdaptersMock.get(I2rsModel.class)).thenReturn(modelAdapterMock);
        when(modelAdapterMock.registerUnderlayTopologyListener((PingPongDataBroker)any(), (String)any(),
                (CorrelationItemEnum)any(), (DatastoreType)any(),
                (LinkCalculator)any(), (List<ListenerRegistration<DOMDataTreeChangeListener>>)any(),
                (Map<Integer, YangInstanceIdentifier>)any())).thenReturn(mock(UnderlayTopologyListener.class));
        when(modelAdapterMock.createTopologyIdentifier("underTopo1")).thenReturn(instanceIdentifierBuilderMock);
        when(modelAdapterMock.createTopologyIdentifier("overTopo1")).thenReturn(instanceIdentifierBuilderMock);
        when(modelAdapterMock.buildItemIdentifier(instanceIdentifierBuilderMock,
                CorrelationItemEnum.Node)).thenReturn(mock(YangInstanceIdentifier.class));
        when(modelAdapterMock.buildItemIdentifier(instanceIdentifierBuilderMock,
                CorrelationItemEnum.Link)).thenReturn(mock(YangInstanceIdentifier.class));

        when(linkComputationMock.getLinkInfo()).thenReturn(null);
        when(linkComputationMock.getNodeInfo()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeTopology()).thenReturn("overTopo1");

        when(targetFieldMock.getMatchingKey()).thenReturn(1);
        when(targetFieldMock.getTargetFieldPath()).thenReturn(leafPathMock);
        when(leafPathMock.getValue()).thenReturn("");

        //linksInformations is null, exception should be thrown
        try {
            handler.processNewRequest();
        } catch (IllegalStateException e) {
            iterator++;
        }

        when(linkComputationMock.getLinkInfo()).thenReturn(linksInformations);

        //linksInformations is empty, exception should be thrown
        try {
            handler.processNewRequest();
        } catch (IllegalStateException e) {
            iterator++;
            verify(linkComputationMock, times(0)).getNodeInfo();
        }

        linksInformations.add(getLinkList());

        //linksInformations is not empty, linkAggregation is null
        handler.processNewRequest();
        verify(translartorMock, times(0)).translate("", CorrelationItemEnum.Link, schemaHolderMock,
                NetworkTopologyModel.class);
        assertEquals(2, handler.getListeners().size());

        initLinkComputationConfig(false);
        handler.correctTopologyID = false;
        handler.getListeners().clear();

        //linksInformations is not empty, linkAggregation is not null, but
        //mapping s underlayTopology is not equals
        handler.processNewRequest();
        verify(translartorMock, times(0)).translate("", CorrelationItemEnum.Link, schemaHolderMock,
                NetworkTopologyModel.class);
        assertEquals(2, handler.getListeners().size());

        handler.correctTopologyID = true;
        handler.getListeners().clear();

        //linksInformations is not empty, linkAggregation is not null, mapping s
        //underlayTopology is equals
        handler.processNewRequest();
        verify(translartorMock, times(1)).translate("", CorrelationItemEnum.Link, schemaHolderMock,
                NetworkTopologyModel.class);
        assertEquals(2, handler.getListeners().size());

        verify(linkComputationMock, times(5)).getLinkInfo();
        assertEquals(2, iterator);
    }

    private LinkInfo getLinkList() {
        return new LinkInfo() {
            @Override
            public Class<? extends Model> getInputModel() {
                return I2rsModel.class;
            }

            @Override
            public <E extends Augmentation<LinkInfo>> E getAugmentation(Class<E> arg0) {
                return null;
            }

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }

            @Override
            public Boolean isAggregatedLinks() {
                return null;
            }

            @Override
            public String getLinkTopology() {
                return "underTopo1";
            }

            @Override
            public LinkInfoKey getKey() {
                return null;
            }
        };
    }

    private void initLinkComputationConfig(boolean agregationNull) {
        handler.aggregationOnly = true;
        handler.agregationCorelationIntemLink = true;
        handler.filtrationAggregation = false;
        handler.realMapping = true;
        handler.agregationNull = agregationNull;
    }

    private class TestTopologyRequestHandler extends TopologyRequestHandler {
        private boolean dataMissing;
        private boolean filtrationOnly;
        private boolean aggregationOnly;
        private boolean renderingOnly;
        private boolean filtrationAggregation;
        private boolean agregationCorelationIntemLink;
        private boolean agregationNull = false;
        private boolean realMapping = false;
        private boolean correctTopologyID = true;
        private boolean aggregationOfNodesAndTps = false;
        private LinkComputation linkComputationReturn;

        public TestTopologyRequestHandler(DOMDataBroker dataBroker, GlobalSchemaContextHolder schemaHolder,
                RpcServices rpcServices, Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            super(dataBroker, schemaHolder, rpcServices, fromNormalizedNode);
        }

        @Override
        protected Class<? extends Model> getModel(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            return  NetworkTopologyModel.class;
        }

        @Override
        protected String getTopologyId(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            return "topo1";
        }

        @Override
        protected Correlations getCorrelations(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            CorrelationsBuilder correlationsBuilder = new CorrelationsBuilder();
            List<Correlation> correlations = new ArrayList<Correlation>();

            if (!aggregationOfNodesAndTps) {
                CorrelationBuilder correlationBuilder = new CorrelationBuilder();
                if (agregationCorelationIntemLink) {
                    correlationBuilder.setCorrelationItem(CorrelationItemEnum.Link);
                } else {
                    correlationBuilder.setCorrelationItem(CorrelationItemEnum.Node);
                }

                if (filtrationOnly || filtrationAggregation) {
                    FiltrationBuilder filtrationBuilder = new FiltrationBuilder();
                    filtrationBuilder.setUnderlayTopology("topo1");

                    List<Filter> filters = new ArrayList<Filter>();
                    when(filterMock.getFilterId()).thenReturn("filter1");
                    filters.add(filterMock);
                    filters.add(filterMock);
                    filtrationBuilder.setFilter(filters);
                    correlationBuilder.setType(FiltrationOnly.class);
                    correlationBuilder.setFiltration(filtrationBuilder.build());
                }
                if (aggregationOnly || filtrationAggregation) {
                    AggregationBuilder aggregationBuilder = new AggregationBuilder();

                    List<Mapping> mappings = new ArrayList<Mapping>();
                    if (realMapping) {
                        mappings.add(getMapping(correctTopologyID));
                    } else {
                        mappings.add(mappingMock);
                        mappings.add(mappingMock);
                        mappings.add(mappingMock);
                    }
                    aggregationBuilder.setMapping(mappings);
                    aggregationBuilder.setAggregationType(Equality.class);

                    if (filtrationAggregation) {
                        correlationBuilder.setType(FiltrationAggregation.class);
                    } else {
                        correlationBuilder.setType(AggregationOnly.class);
                    }
                    if (agregationNull) {
                        correlationBuilder.setAggregation(null);
                    } else {
                        correlationBuilder.setAggregation(aggregationBuilder.build());
                    }

                }
                if (renderingOnly) {
                    correlationBuilder.setType(RenderingOnly.class);
                    correlationBuilder.setRendering(renderingMock);
                }
                if (dataMissing) {
                    correlationBuilder.setType(null);
                }

                correlations.add(correlationBuilder.build());
            } else {
                List<Filter> filters = new ArrayList<Filter>();
                when(filterMock.getFilterId()).thenReturn("filter1");
                filters.add(filterMock);
                filters.add(filterMock);

                FiltrationBuilder filtrationBuilder = new FiltrationBuilder();
                filtrationBuilder.setUnderlayTopology("topo1");
                filtrationBuilder.setFilter(filters);

                AggregationBuilder aggregationBuilder = new AggregationBuilder();
                List<Mapping> nodeMappings = new ArrayList<Mapping>();
                nodeMappings.add(mappingMock);
                nodeMappings.add(mappingMock);
                aggregationBuilder.setMapping(nodeMappings);
                aggregationBuilder.setAggregationType(Equality.class);

                CorrelationBuilder nodeCorrelationBuilder = new CorrelationBuilder();
                nodeCorrelationBuilder.setCorrelationItem(CorrelationItemEnum.Node);
                nodeCorrelationBuilder.setType(FiltrationAggregation.class);
                nodeCorrelationBuilder.setAggregation(aggregationBuilder.build());
                nodeCorrelationBuilder.setFiltration(filtrationBuilder.build());

                CorrelationBuilder tpCorrelationBuilder = new CorrelationBuilder();
                tpCorrelationBuilder.setCorrelationItem(CorrelationItemEnum.TerminationPoint);
                tpCorrelationBuilder.setType(FiltrationAggregation.class);
                tpCorrelationBuilder.setAggregation(aggregationBuilder.build());
                tpCorrelationBuilder.setFiltration(filtrationBuilder.build());

                correlations.add(nodeCorrelationBuilder.build());
                correlations.add(tpCorrelationBuilder.build());
            }
            correlationsBuilder.setCorrelation(correlations);
            return correlationsBuilder.build();
        }

        @Override
        protected LinkComputation getLinkComputation(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            return linkComputationReturn;
        }

        public void setLinkComputation(LinkComputation linkComputation) {
            linkComputationReturn = linkComputation;
        }

        private Mapping getMapping(final boolean correctTopologyID) {
            return new Mapping() {
                @Override
                public Class<? extends Model> getInputModel() {
                    return OpendaylightInventoryModel.class;
                }

                @Override
                public <E extends Augmentation<Mapping>> E getAugmentation(Class<E> arg0) {
                    return null;
                }

                @Override
                public Class<? extends DataContainer> getImplementedInterface() {
                    return null;
                }

                @Override
                public Boolean isAggregateInside() {
                    return null;
                }

                @Override
                public String getUnderlayTopology() {
                    if (correctTopologyID) {
                        return "underTopo1";
                    } else {
                        return "topology1";
                    }
                }

                @Override
                public List<TargetField> getTargetField() {
                    ArrayList<TargetField> targetFieldList = new ArrayList<TargetField>();
                    targetFieldList.add(targetFieldMock);
                    return targetFieldList;
                }

                @Override
                public MappingKey getKey() {
                    return null;
                }

                @Override
                public List<String> getApplyFilters() {
                    return null;
                }
            };
        }
    }
}