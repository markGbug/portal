package org.hango.cloud.ncegdashboard.envoy.service.impl;

import static org.junit.Assert.assertTrue;

import org.hango.cloud.ncegdashboard.BaseServiceImplTest;
import org.hango.cloud.ncegdashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.ServiceInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayInfoService;
import org.hango.cloud.ncegdashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.innerdto.EnvoyActiveHealthCheckRuleDto;
import org.hango.cloud.ncegdashboard.envoy.innerdto.EnvoyPassiveHealthCheckRuleDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyHealthCheckRuleDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyServiceProxyDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoySubsetDto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EnvoyHealthCheckServiceImplTest extends BaseServiceImplTest {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyHealthCheckServiceImplTest.class);

	private static long gwId;

	private static long serviceId;

	@Autowired
	private IServiceInfoService serviceInfoService;

	@Autowired
	@InjectMocks
	private EnvoyServiceProxyServiceImpl serviceProxyService;

	@Autowired
	private IGatewayInfoService gatewayInfoService;

	@Autowired
	@InjectMocks
	private EnvoyHealthCheckServiceImpl envoyHealthCheckService;

	@Mock
	private GetFromApiPlaneServiceImpl getFromApiPlaneService;

	private ServiceInfo serviceInfo;

	private GatewayInfo gatewayInfo;

	private EnvoyServiceProxyDto envoyServiceProxyDto;

	private EnvoyHealthCheckRuleDto envoyHealthCheckRuleDto = new EnvoyHealthCheckRuleDto();

	@Before
	public void init() {

		//        Mockito.doReturn(true).when(getFromApiPlaneService).publishServiceByApiPlane(Mockito.any(), Mockito
		//        .any());
		Mockito.when(getFromApiPlaneService.publishServiceByApiPlane(Mockito.any(), Mockito.any())).thenReturn(true);
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
		subsetDto.setName("testUnit");
		Map<String, String> label = new HashMap<>();
		label.put("aaa", "bbb");
		subsetDto.setLabels(label);
		envoyServiceProxyDto.setSubsets(Arrays.asList(new EnvoySubsetDto[]{subsetDto}));

		//健康检查配置
		envoyHealthCheckRuleDto.setServiceId(serviceId);
		envoyHealthCheckRuleDto.setGwId(gwId);
		//主动健康检查
		envoyHealthCheckRuleDto.setActiveSwitch(1);
		envoyHealthCheckRuleDto.setTimeout(6000);
		envoyHealthCheckRuleDto.setExpectedStatuses(Arrays.asList(new Integer[]{200}));
		envoyHealthCheckRuleDto.setHealthyInterval(2000);
		envoyHealthCheckRuleDto.setHealthyThreshold(2);
		envoyHealthCheckRuleDto.setUnhealthyInterval(2000);
		envoyHealthCheckRuleDto.setUnhealthyThreshold(2);
		envoyHealthCheckRuleDto.setPath("/health");
		//被动健康检查
		envoyHealthCheckRuleDto.setPassiveSwitch(1);
		envoyHealthCheckRuleDto.setBaseEjectionTime(3000);
		envoyHealthCheckRuleDto.setConsecutiveErrors(5);
		envoyHealthCheckRuleDto.setMaxEjectionPercent(50);
		envoyHealthCheckRuleDto.setMinHealthPercent(50);
		serviceProxyService.publishServiceToGw(envoyServiceProxyDto);
	}

	@After
	public void tearDownClass() {
		logger.info("tear down class .... ServiceProxyServiceImplTest");
		//清除service
		Mockito.doReturn(true).when(getFromApiPlaneService).offlineServiceByApiPlane(Mockito.any(), Mockito.any());
		serviceProxyService.deleteServiceProxy(gwId, serviceId);
		serviceInfoService.delete(serviceId);
	}

	@Test
	@Rollback
	public void updateHealthCheckRuleParam() {
		envoyHealthCheckService.updateHealthCheckRuleParam(EnvoyHealthCheckRuleDto.dtoToMeta(envoyHealthCheckRuleDto));
		EnvoyHealthCheckRuleDto healthCheckRule = envoyHealthCheckService.getHealthCheckRule(serviceId, gwId);
		assertTrue(healthCheckRule.getMinHealthPercent() == 50);
		assertTrue(healthCheckRule.getHealthyInterval() == 2000);
	}

	@Test
	@Rollback
	public void getPassiveHealthCheckRule() {
		envoyHealthCheckService.updateHealthCheckRuleParam(EnvoyHealthCheckRuleDto.dtoToMeta(envoyHealthCheckRuleDto));
		EnvoyPassiveHealthCheckRuleDto passiveHealthCheckRule = envoyHealthCheckService.getPassiveHealthCheckRule(
			serviceId, gwId);
		assertTrue(passiveHealthCheckRule.getMaxEjectionPercent() == 50);
	}

	@Test
	@Rollback
	public void getActiveHealthCheckRule() {
		envoyHealthCheckService.updateHealthCheckRuleParam(EnvoyHealthCheckRuleDto.dtoToMeta(envoyHealthCheckRuleDto));
		EnvoyActiveHealthCheckRuleDto activeHealthCheckRule = envoyHealthCheckService.getActiveHealthCheckRule(
			serviceId, gwId);
		assertTrue(activeHealthCheckRule.getPath().equals("/health"));
	}

	@Test
	@Rollback
	public void shutdownHealthCheck() {
		envoyHealthCheckService.updateHealthCheckRuleParam(EnvoyHealthCheckRuleDto.dtoToMeta(envoyHealthCheckRuleDto));
		envoyHealthCheckService.shutdownHealthCheck(serviceId, gwId);
		assertTrue(envoyHealthCheckService.getHealthCheckRule(serviceId, gwId).getActiveSwitch() == 0);
	}

	@Test
	@Rollback
	public void deleteHealthCheckRule() {
		envoyHealthCheckService.updateHealthCheckRuleParam(EnvoyHealthCheckRuleDto.dtoToMeta(envoyHealthCheckRuleDto));
		envoyHealthCheckService.deleteHealthCheckRule(serviceId);
		assertTrue(envoyHealthCheckService.getHealthCheckRule(serviceId, gwId) == null);
	}

	@Test
	@Rollback
	public void getServiceInstanceList() {
		envoyHealthCheckService.updateHealthCheckRuleParam(EnvoyHealthCheckRuleDto.dtoToMeta(envoyHealthCheckRuleDto));
		envoyHealthCheckService.getServiceInstanceList(serviceInfo, gatewayInfo);
	}

	@Test
	@Rollback
	public void getServiceHealthyStatus() {
		envoyHealthCheckService.updateHealthCheckRuleParam(EnvoyHealthCheckRuleDto.dtoToMeta(envoyHealthCheckRuleDto));
		envoyHealthCheckService.getServiceHealthyStatus(serviceInfo, gatewayInfo);
	}

	@Test
	public void checkUpdateHealthCheckRuleParam() {
		ErrorCode errorCode = envoyHealthCheckService.checkUpdateHealthCheckRuleParam(envoyHealthCheckRuleDto);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

}