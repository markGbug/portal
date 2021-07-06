package org.hango.cloud.ncegdashboard.envoy.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import org.hango.cloud.ncegdashboard.BaseServiceImplTest;
import org.hango.cloud.ncegdashboard.envoy.meta.*;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.service.*;
import org.hango.cloud.ncegdashboard.envoy.web.dto.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvoyRouteRuleProxyServiceImplTest extends BaseServiceImplTest {

	@Autowired
	public IServiceInfoService serviceInfoService;

	@Autowired
	@InjectMocks
	public IEnvoyServiceProxyService serviceProxyService;

	@Autowired
	public IEnvoyRouteRuleInfoService routeRuleInfoService;

	@Autowired
	public IGatewayInfoService gatewayInfoService;

	@Autowired
	@InjectMocks
	public IEnvoyRouteRuleProxyService routeRuleProxyService;

	public ServiceInfo serviceInfo;

	public GatewayInfo gatewayInfo;

	public EnvoyServiceProxyDto envoyServiceProxyDto;

	public EnvoyRouteRuleDto routeRuleDto;

	public EnvoyRouteRuleProxyDto envoyRouteRuleProxyDto = new EnvoyRouteRuleProxyDto();

	public EnvoyRouteRuleProxyDto syncRouteProxyDto = new EnvoyRouteRuleProxyDto();

	public long gwId;

	public long serviceId;

	public long routeId;


	@Mock
	private GetFromApiPlaneServiceImpl getFromApiPlaneService;

	@Autowired
	private IEnvoyGatewayService envoyGatewayService;

	private EnvoyDestinationDto envoyDestinationDto;

	@Before
	public void init() {

		Mockito.when(getFromApiPlaneService.publishServiceByApiPlane(Mockito.any(), Mockito.any())).thenReturn(true);
		Mockito.doReturn(true).when(getFromApiPlaneService).offlineServiceByApiPlane(Mockito.any(), Mockito.any());
		Mockito.doReturn(true).when(getFromApiPlaneService).publishRouteRuleByApiPlane(Mockito.any(), Mockito.any());
		Mockito.doReturn(true).when(getFromApiPlaneService).deleteRouteRuleByApiPlane(Mockito.any());

		routeRuleDto = new EnvoyRouteRuleDto();

		//初始化ServiceInfo
		serviceInfo = new ServiceInfo();
		serviceInfo.setDisplayName(displayName);
		serviceInfo.setServiceName(serviceName);
		serviceInfo.setContacts(user);
		serviceInfo.setProjectId(projectId);
		serviceInfo.setServiceType(serviceType);
		//创建service
		serviceId = serviceInfoService.add(serviceInfo);

		gatewayInfo = gatewayInfoService.getGatewayByName(envoyGwName);
		gwId = gatewayInfo.getId();

		envoyServiceProxyDto = new EnvoyServiceProxyDto();
		envoyServiceProxyDto.setServiceId(serviceId);
		envoyServiceProxyDto.setBackendService("a.pilot-test.svc.cluster.local");
		envoyServiceProxyDto.setGwId(gwId);
		envoyServiceProxyDto.setPublishType(Const.DYNAMIC_PUBLISH_TYPE);
		envoyServiceProxyDto.setRegistryCenterType("Kubernetes");
		EnvoySubsetDto subsetDto = new EnvoySubsetDto();
		subsetDto.setName("testSubset");
		Map<String, String> label = new HashMap<>();
		label.put("aaa", "bbb");
		subsetDto.setLabels(label);
		envoyServiceProxyDto.setSubsets(Arrays.asList(new EnvoySubsetDto[]{subsetDto}));

		serviceProxyService.publishServiceToGw(envoyServiceProxyDto);

		//构造路由规则
		routeRuleDto.setServiceId(serviceId);
		routeRuleDto.setRouteRuleName(routeName);
		routeRuleDto.setDescription(description);
		EnvoyRouteRuleMapMatchDto headers = new EnvoyRouteRuleMapMatchDto();
		headers.setKey("abc");
		headers.setType(Const.URI_TYPE_EXACT);
		headers.setValue(Arrays.asList(new String[]{"abc"}));
		routeRuleDto.setHeaders(Arrays.asList(new EnvoyRouteRuleMapMatchDto[]{headers}));

		EnvoyRouteRuleMapMatchDto querys = new EnvoyRouteRuleMapMatchDto();
		querys.setKey("aaa");
		querys.setType(Const.URI_TYPE_EXACT);
		querys.setValue(Arrays.asList(new String[]{"caa"}));
		routeRuleDto.setQueryParams(Arrays.asList(new EnvoyRouteRuleMapMatchDto[]{querys}));

		EnvoyRouteStringMatchDto host = new EnvoyRouteStringMatchDto();
		host.setType("exact");
		host.setValue(Arrays.asList(new String[]{"abc.com"}));
		routeRuleDto.setHostMatchDto(host);

		EnvoyRouteStringMatchDto method = new EnvoyRouteStringMatchDto();
		method.setType("exact");
		method.setValue(Arrays.asList(new String[]{"GET"}));
		routeRuleDto.setMethodMatchDto(method);

		EnvoyRouteStringMatchDto uri = new EnvoyRouteStringMatchDto();
		uri.setType("exact");
		uri.setValue(Arrays.asList(new String[]{"/abc"}));
		routeRuleDto.setUriMatchDto(uri);

		routeRuleDto.setPriority(50);
		EnvoyRouteRuleInfo routeRuleInfo = routeRuleDto.toMeta();
		routeRuleInfo.setProjectId(projectId);

		routeId = routeRuleInfoService.addRouteRule(routeRuleInfo);

		envoyRouteRuleProxyDto.setRouteRuleId(routeId);
		envoyRouteRuleProxyDto.setServiceId(serviceId);
		envoyRouteRuleProxyDto.setGwIds(Arrays.asList(new Long[]{gwId}));
		envoyRouteRuleProxyDto.setGwId(gwId);
		envoyRouteRuleProxyDto.setTimeout(60000);
		envoyRouteRuleProxyDto.setEnableState(Const.ROUTE_RULE_ENABLE_STATE);

		envoyDestinationDto = new EnvoyDestinationDto();
		envoyDestinationDto.setPort(80);
		envoyDestinationDto.setServiceId(serviceId);
		envoyDestinationDto.setApplicationName("a.powerful-v13.svc.cluster.local");
		envoyDestinationDto.setSubsetName("testSubset");
		envoyDestinationDto.setWeight(100);
		envoyRouteRuleProxyDto.setDestinationServices(Arrays.asList(new EnvoyDestinationDto[]{envoyDestinationDto}));
		HttpRetryDto httpRetryDto = new HttpRetryDto();
		httpRetryDto.setAttempts(2);
		httpRetryDto.setPerTryTimeout(60000);
		httpRetryDto.setRetryOn("5xx");
		envoyRouteRuleProxyDto.setHttpRetryDto(httpRetryDto);

		syncRouteProxyDto.setRouteRuleId(routeId);
		syncRouteProxyDto.setServiceId(serviceId);
		syncRouteProxyDto.setGwIds(Arrays.asList(new Long[]{gwId}));
		syncRouteProxyDto.setPriority(55);
		EnvoyRouteStringMatchDto uriSync = new EnvoyRouteStringMatchDto();
		uriSync.setType("prefix");
		uriSync.setValue(Arrays.asList(new String[]{"/aaa"}));
		syncRouteProxyDto.setUriMatchDto(uriSync);
		EnvoyRouteStringMatchDto hostSync = new EnvoyRouteStringMatchDto();
		hostSync.setType("exact");
		hostSync.setValue(Arrays.asList(new String[]{"sync.com"}));
		syncRouteProxyDto.setHostMatchDto(hostSync);

		EnvoyVirtualHostInfo envoyVirtualHostInfo = new EnvoyVirtualHostInfo();
		envoyVirtualHostInfo.setGwId(2);
		envoyVirtualHostInfo.setProjectId(3);
		envoyVirtualHostInfo.setHostList(Arrays.asList("abc.com"));
		envoyGatewayService.createVirtualHost(envoyVirtualHostInfo);
	}

	@After
	public void tearDownClass() {
		//清除service
		serviceProxyService.deleteServiceProxy(gwId, serviceId);
		serviceInfoService.delete(serviceId);
		routeRuleInfoService.deleteRouteRule(routeId);
	}

	@Test
	public void checkPublishParam() {
		ErrorCode errorCode = routeRuleProxyService.checkPublishParam(envoyRouteRuleProxyDto);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));

		EnvoyDestinationDto envoyStaticDestinationDto = new EnvoyDestinationDto();
		envoyStaticDestinationDto.setServiceId(serviceId);
		envoyStaticDestinationDto.setSubsetName("testSubset1");
		envoyStaticDestinationDto.setWeight(100);
		envoyRouteRuleProxyDto.setDestinationServices(
			Arrays.asList(new EnvoyDestinationDto[]{envoyStaticDestinationDto}));
		errorCode = routeRuleProxyService.checkPublishParam(envoyRouteRuleProxyDto);
		assertEquals(CommonErrorCode.InvalidSubsetName.getCode(), errorCode.getCode());
	}

	@Test
	public void checkUpdateParam() {
		envoyRouteRuleProxyDto.setTimeout(70000);
		ErrorCode errorCode = routeRuleProxyService.checkUpdateParam(envoyRouteRuleProxyDto);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

	@Test
	@Rollback
	public void publishRouteRule() {
		routeRuleProxyService.publishRouteRule(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto),
		                                       Lists.newArrayList(), true);
		EnvoyRouteRuleProxyInfo routeRuleProxy = routeRuleProxyService.getRouteRuleProxy(gwId, routeId);
		EnvoyRouteRuleProxyDto routeRuleProxyDto = routeRuleProxyService.fromMeta(routeRuleProxy);
		assertTrue(routeRuleProxyDto.getUriMatchDto().getValue().contains("/abc"));
		assertTrue(routeRuleProxyService.deleteRouteRuleProxy(gwId, routeId));
	}

	@Test
	@Rollback
	public void publishRouteRuleBatch() {
		List<String> strings = routeRuleProxyService.publishRouteRuleBatch(Arrays.asList(new Long[]{gwId}),
		                                                                   envoyRouteRuleProxyDto);
		assertTrue(CollectionUtils.isEmpty(strings));
		assertTrue(routeRuleProxyService.deleteRouteRuleProxy(gwId, routeId));
	}

	@Test
	@Rollback
	public void addRouteRuleProxy() {
		long id = routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		assertTrue(id > 0);
	}

	@Test
	@Rollback
	public void getRouteRuleProxyList() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		List<EnvoyRouteRuleProxyInfo> routeRuleProxyList = routeRuleProxyService.getRouteRuleProxyList(serviceId);
		assertTrue(routeRuleProxyList.size() > 0);
	}

	@Test
	@Rollback
	public void getRouteRuleProxyList1() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		List<EnvoyRouteRuleProxyInfo> routeRuleProxyList = routeRuleProxyService.getRouteRuleProxyList(gwId, serviceId,
		                                                                                               "create_time",
		                                                                                               "desc", 0, 100);
		assertTrue(routeRuleProxyList.size() > 0);
	}

	@Test
	@Rollback
	public void getRouteRuleProxyCountByService() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		long count = routeRuleProxyService.getRouteRuleProxyCountByService(gwId, serviceId);
		assertTrue(count > 0);
	}

	//    @Test
	//    @Rollback
	//    public void getRouteRuleProxyCount() {
	//        routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
	//        long count = routeRuleProxyService.getRouteRuleProxyCount(gwId, routeId);
	//        assertTrue(count > 0);
	//    }

	@Test
	@Rollback
	public void getRouteRuleProxy() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		EnvoyRouteRuleProxyInfo routeRuleProxy = routeRuleProxyService.getRouteRuleProxy(gwId, routeId);
		assertTrue(routeRuleProxy.getServiceId() == serviceId);
	}

	@Test
	@Rollback
	public void deleteRouteRuleProxy() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		assertTrue(routeRuleProxyService.deleteRouteRuleProxy(gwId, routeId));
	}

	@Test
	@Rollback
	public void checkDeleteRouteRuleProxy() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		ErrorCode errorCode = routeRuleProxyService.checkDeleteRouteRuleProxy(gwId, routeId, Lists.newArrayList());
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

	@Test
	@Rollback
	public void checkUpdateEnableState() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		ErrorCode errorCode = routeRuleProxyService.checkUpdateEnableState(gwId, routeId,
		                                                                   Const.ROUTE_RULE_ENABLE_STATE);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

	@Test
	@Rollback
	public void updateEnableState() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		long count = routeRuleProxyService.updateEnableState(gwId, routeId, Const.ROUTE_RULE_DISABLE_STATE);
		assertTrue(count > 0);
	}

	@Test
	@Rollback
	public void getRouteRuleProxyByRouteRuleId() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		List<EnvoyRouteRuleProxyInfo> routeRuleProxyByRouteRuleId = routeRuleProxyService
			                                                            .getRouteRuleProxyByRouteRuleId(routeId);
		assertTrue(routeRuleProxyByRouteRuleId.size() > 0);
	}

	@Test
	@Rollback
	public void fromMeta() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		EnvoyRouteRuleProxyDto envoyRouteRuleProxyDto = routeRuleProxyService.fromMeta(
			routeRuleProxyService.getRouteRuleProxy(gwId, routeId));
		assertTrue(envoyRouteRuleProxyDto.getRouteRuleId() == routeId);
	}

	@Test
	@Rollback
	public void getRouteRuleProxyListByServiceId() {
		routeRuleProxyService.addRouteRuleProxy(routeRuleProxyService.toMeta(envoyRouteRuleProxyDto));
		List<EnvoyRouteRuleProxyInfo> routeList = routeRuleProxyService.getRouteRuleProxyListByServiceId(gwId,
		                                                                                                 serviceId);
		assertTrue(routeList.size() > 0);
	}

}