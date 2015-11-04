package org.opendaylight.topoprocessing.impl.request;

import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.correlation.rev150121.correlations.grouping.correlations.correlation.filtration.Filter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.link.computation.rev150824.link.computation.grouping.LinkComputation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author branislav.janosik
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestHandlerTest {


    @Mock
    private Filter filter;
    @Mock
    private Mapping mapping;
    @Mock
    private Rendering rendering;
    @Mock
    private LeafPath leafpath;
    @Mock
    private Map<Class<? extends FilterBase>, FiltratorFactory> filtrators;
    @Mock
    private Map<Class<? extends Model>, ModelAdapter> modelAdapters;
    @Mock
    private RpcServices rpc;
    @Mock
    private YangInstanceIdentifier yii;
    @Mock
    private PathTranslator pathTranslator;
    @Mock
    private UnderlayTopologyListener utl;
    @Mock
    private OverlayItemTranslator oit;
    @Mock
    private ModelAdapter ma;
    @Mock
    private DOMRpcService domRpcService;

    private TestTopologyRequestHandler handler;
    private GlobalSchemaContextHolder schemaHolder;



    @SuppressWarnings("unchecked")
    @Before
    public void init(){
        TestingDOMDataBroker broker = new TestingDOMDataBroker();
        PingPongDataBroker ppbroker = new PingPongDataBroker(broker);

        schemaHolder = mock(GlobalSchemaContextHolder.class);
        rpc = mock(RpcServices.class);
        filter = mock(Filter.class);
        leafpath = mock(LeafPath.class);
        filtrators = mock(Map.class);
        modelAdapters = mock(Map.class);
        yii = mock(YangInstanceIdentifier.class);
        mapping = mock(Mapping.class);
        pathTranslator = mock(PathTranslator.class);
        utl = mock(UnderlayTopologyListener.class);
        oit = mock(OverlayItemTranslator.class);
        ma = mock(ModelAdapter.class);
        domRpcService = mock(DOMRpcService.class);
        rendering = mock(Rendering.class);
        handler = new TestTopologyRequestHandler(ppbroker,schemaHolder,rpc,null);

        handler.setTranslator(pathTranslator);
        handler.setFiltrators(filtrators);
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

        when(mapping.getUnderlayTopology()).thenReturn("topo1");
        when(mapping.isAggregateInside()).thenReturn(true);

        when(mapping.getTargetField()).thenReturn(leafpath);
        when(leafpath.getValue()).thenReturn("f1");
        when(mapping.getInputModel()).thenReturn(null);

        List<String> applyFilters = new ArrayList<String>();
        applyFilters.add("filter1");
        when(mapping.getApplyFilters()).thenReturn(applyFilters);

        setTestHandler();
        handler.processNewRequest();

        verify(mapping, times(12)).getInputModel();
        verify(mapping, atMost(3)).getApplyFilters();
        assertTrue(handler.getListeners().size() == 3);
        handler.aggregationOnly = false;
    }

    @Test
    public void testProcessNewRequestRendering(){
        handler.renderingOnly = true;

        when(rendering.getUnderlayTopology()).thenReturn("topo1");
        when(rendering.getInputModel()).thenReturn(null);

        setTestHandler();
        TopologyOperator to = mock(TopologyOperator.class);
        when(utl.getOperator()).thenReturn(to);
        handler.processNewRequest();

        assertTrue(handler.getListeners().size() == 1);
        handler.renderingOnly = false;
    }

    @Test
    public void testProcessNewRequestFiltrationAggregation(){
        handler.filtrationAggregation = true;
        when(mapping.getUnderlayTopology()).thenReturn("topo1");
        when(mapping.isAggregateInside()).thenReturn(true);

        when(mapping.getTargetField()).thenReturn(leafpath);
        when(leafpath.getValue()).thenReturn("f1");
        when(mapping.getInputModel()).thenReturn(null);

        setFiltrationBehaviour();

        List<String> applyFilters = new ArrayList<String>();
        applyFilters.add("filter1");
        when(mapping.getApplyFilters()).thenReturn(applyFilters);

        setTestHandler();
        handler.processNewRequest();

        verify(mapping, times(15)).getInputModel();
        verify(mapping, times(6)).getApplyFilters();
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

        when(filter.getTargetField()).thenReturn(leafpath);
        when(leafpath.getValue()).thenReturn("f1");
        when(filter.getInputModel()).thenReturn(null);
        when(filter.getFilterType()).thenReturn(null);
        FiltratorFactory ff = mock(FiltratorFactory.class);
        when(filtrators.get(null)).thenReturn(ff);
    }

    @SuppressWarnings("unchecked")
    private void setTestHandler(){
        when(modelAdapters.get(null)).thenReturn(ma);
        when(modelAdapters.get(NetworkTopologyModel.class)).thenReturn(ma);
        when(ma.createOverlayItemTranslator()).thenReturn(oit);
        //when(ma.createTopologyIdentifier((String)any()).build()).thenReturn(yii);
        InstanceIdentifierBuilder iib = mock(InstanceIdentifierBuilder.class);
        when(modelAdapters.get(NetworkTopologyModel.class).createTopologyIdentifier("topo1")).thenReturn(iib);
        when(iib.build()).thenReturn(yii);
        when(modelAdapters.get(NetworkTopologyModel.class).buildItemIdentifier(iib, CorrelationItemEnum.Node)).thenReturn(yii);
        when(rpc.getRpcService()).thenReturn(domRpcService);
        handler.setModelAdapters(modelAdapters);
        when(modelAdapters.get(null).registerUnderlayTopologyListener((PingPongDataBroker)any(),(String) any(),(CorrelationItemEnum) any(),(DatastoreType) any(),(TopologyAggregator) any(),(List) any(),(YangInstanceIdentifier) any())).thenReturn(utl);
        when(pathTranslator.translate("f1",CorrelationItemEnum.Node,schemaHolder,null)).thenReturn(yii);
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
            Class<NetworkTopologyModel> model =  NetworkTopologyModel.class;
            return  model;
        }

        @Override
        protected String getTopologyId(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            return "topo1";
        }

        @Override
        protected Correlations getCorrelations(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
               CorrelationsBuilder csBuilder = new CorrelationsBuilder();
               List<Correlation> correlations = new ArrayList<Correlation>();

               CorrelationBuilder cBuilder = new CorrelationBuilder();
               cBuilder.setCorrelationItem(CorrelationItemEnum.Node);

               if(filtrationOnly || filtrationAggregation){
                   FiltrationBuilder fBuilder = new FiltrationBuilder();
                   fBuilder.setUnderlayTopology("topo1");

                   List<Filter> filters = new ArrayList<Filter>();
                   when(filter.getFilterId()).thenReturn("filter1");
                   filters.add(filter);
                   filters.add(filter);
                   fBuilder.setFilter(filters);
                   cBuilder.setType(FiltrationOnly.class);
                   cBuilder.setFiltration(fBuilder.build());
               }
               if(aggregationOnly || filtrationAggregation){
                   AggregationBuilder aBuilder = new AggregationBuilder();

                   List<Mapping> mappings = new ArrayList<Mapping>();
                   mappings.add(mapping);
                   mappings.add(mapping);
                   mappings.add(mapping);
                   aBuilder.setMapping(mappings);
                   aBuilder.setAggregationType(Equality.class);

                   if(filtrationAggregation){
                       cBuilder.setType(FiltrationAggregation.class);
                   }
                   else cBuilder.setType(AggregationOnly.class);
                   cBuilder.setAggregation(aBuilder.build());

               }
               if(renderingOnly){
                   cBuilder.setType(RenderingOnly.class);
                   cBuilder.setRendering(rendering);
               }
               if(dataMissing){
                   cBuilder.setType(null);
               }

               correlations.add(cBuilder.build());
               csBuilder.setCorrelation(correlations);

               return csBuilder.build();

        }

        @Override
        protected LinkComputation getLinkComputation(Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode) {
            return null;
        }
    }
}
