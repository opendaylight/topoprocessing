package org.opendaylight.topoprocessing.impl.request;

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
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.topoprocessing.api.filtration.FiltratorFactory;
import org.opendaylight.topoprocessing.impl.adapter.ModelAdapter;
import org.opendaylight.topoprocessing.impl.listener.UnderlayTopologyListener;
import org.opendaylight.topoprocessing.impl.operator.TopologyAggregator;
import org.opendaylight.topoprocessing.impl.operator.TopologyOperator;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.RenderingOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.Correlations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.CorrelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.Correlation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.CorrelationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.AggregationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.FiltrationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.Rendering;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.aggregation.mapping.TargetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.LinkComputation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;

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

    private TestTopologyRequestHandler handler;


    @Before
    public void init(){
        TestingDOMDataBroker testingDOMbroker = new TestingDOMDataBroker();
        PingPongDataBroker pingPongBroker = new PingPongDataBroker(testingDOMbroker);
        handler = new TestTopologyRequestHandler(pingPongBroker,schemaHolderMock,rpcServicesMock,null);
        handler.setTranslator(pathTranslatorMock);
        handler.setFiltrators(filtratorsMock);
        handler.setDatastoreType(DatastoreType.OPERATIONAL);
    }

    @Test
    public void testProcessNewRequestFiltration(){
        handler.filtrationOnly = true;

        setFiltrationBehaviour();
        handler.processNewRequest();

        assertTrue(handler.getListeners().size() == 2);
        handler.filtrationOnly = false;
    }

    @Test
    public void testProcessNewRequestAggregation(){
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
    public void testProcessNewRequestRendering(){
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
    public void testProcessNewRequestFiltrationAggregation(){
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

    @Test(expected=IllegalStateException.class)
    public void testProcessNewRequestDataMissing(){
        handler.dataMissing = true;
        handler.processNewRequest();
        handler.dataMissing = false;
    }

    private void setFiltrationBehaviour(){
        setTestHandler();

        when(filterMock.getTargetField()).thenReturn(leafpathMock);
        when(leafpathMock.getValue()).thenReturn("f1");
        when(filterMock.getInputModel()).thenReturn(null);
        when(filterMock.getFilterType()).thenReturn(null);
        FiltratorFactory filtratorFactoryMock = mock(FiltratorFactory.class);
        when(filtratorsMock.get(null)).thenReturn(filtratorFactoryMock);
    }

    private void setTestHandler(){
        when(modelAdaptersMock.get(null)).thenReturn(modelAdapterMock);
        when(modelAdaptersMock.get(I2rsModel.class)).thenReturn(modelAdapterMock);
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

    private class TestTopologyRequestHandler extends TopologyRequestHandler
    {
        private boolean dataMissing;
        private boolean filtrationOnly;
        private boolean aggregationOnly;
        private boolean renderingOnly;
        private boolean filtrationAggregation;

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

               CorrelationBuilder correlationBuilder = new CorrelationBuilder();
               correlationBuilder.setCorrelationItem(CorrelationItemEnum.Node);

               if(filtrationOnly || filtrationAggregation){
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
               if(aggregationOnly || filtrationAggregation){
                   AggregationBuilder aggregationBuilder = new AggregationBuilder();

                   List<Mapping> mappings = new ArrayList<Mapping>();
                   mappings.add(mappingMock);
                   mappings.add(mappingMock);
                   mappings.add(mappingMock);
                   aggregationBuilder.setMapping(mappings);
                   aggregationBuilder.setAggregationType(Equality.class);

                   if(filtrationAggregation){
                       correlationBuilder.setType(FiltrationAggregation.class);
                   }
                   else correlationBuilder.setType(AggregationOnly.class);
                   correlationBuilder.setAggregation(aggregationBuilder.build());

               }
               if(renderingOnly){
                   correlationBuilder.setType(RenderingOnly.class);
                   correlationBuilder.setRendering(renderingMock);
               }
               if(dataMissing){
                   correlationBuilder.setType(null);
               }

               correlations.add(correlationBuilder.build());
               correlationsBuilder.setCorrelation(correlations);

               return correlationsBuilder.build();

        }

        @Override
        protected LinkComputation getLinkComputation(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            return null;
        }
    }
}
