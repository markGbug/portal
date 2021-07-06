package org.hango.cloud.ncegdashboard.apiserver.service.impl;

import static org.junit.Assert.assertTrue;

import org.hango.cloud.ncegdashboard.BaseServiceImplTest;
import org.hango.cloud.ncegdashboard.envoy.web.dto.GatewayDto;
import org.hango.cloud.ncegdashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayInfoService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

public class GatewayInfoServiceImplTest extends BaseServiceImplTest {

	private static final Logger logger = LoggerFactory.getLogger(ServiceInfoImplTest.class);

	@Autowired
	private IGatewayInfoService gatewayInfoService;

	private GatewayDto gatewayDto;

	@PostConstruct
	public void init() {
		//初始化gatewayInfoMetaDto
		gatewayDto = new GatewayDto();
		gatewayDto.setGwName(gwName);
		gatewayDto.setGwAddr(gwAddr);
		gatewayDto.setProjectId(1);
		gatewayDto.setHostList(Arrays.asList("istio.com"));
	}

	@Test
	@Rollback
	public void updateGwInfo() {
		long gwId = gatewayInfoService.addGatewayByMetaDto(gatewayDto);
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		gatewayInfo.setGwName("testUnit2");
		gatewayInfoService.updateGwInfo(gatewayInfo, true);
		assertTrue(gatewayInfoService.get(gwId).getGwName().equals("testUnit2"));
	}

	@Test
	@Rollback
	public void findAll() {
		List<GatewayInfo> all = gatewayInfoService.findAll();
		assertTrue(all.size() > 0);
	}

	@Test
	@Rollback
	public void findGatewayByLimit() {
		gatewayInfoService.addGatewayByMetaDto(gatewayDto);
		List<GatewayInfo> gatewayByLimit = gatewayInfoService.findGatewayByLimit(gwName, 0, 20);
		assertTrue(gatewayByLimit.size() == 1);
	}

	@Test
	@Rollback
	public void getGatewayCount() {
		gatewayInfoService.addGatewayByMetaDto(gatewayDto);
		assertTrue(gatewayInfoService.getGatewayCount(gwName) == 1);
	}

	@Test
	public void checkGwIdParam() {
		gatewayInfoService.checkGwIdParam("");
		gatewayInfoService.checkGwIdParam(String.valueOf(System.currentTimeMillis()));
	}

	@Test
	@Rollback
	public void addGatewayByMetaDto() {
		//isExistGwInstance单元测试
		assertTrue(!gatewayInfoService.isExistGwInstance(gwName));
		//addGatewayByMetaDto单元测试
		long gwId = gatewayInfoService.addGatewayByMetaDto(gatewayDto);
		//get 单元测试
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		//isGwEnvExists单元测试
		assertTrue(gatewayInfoService.isGwExists(gwId));
		//getGatewayByName单元测试
		GatewayInfo gatewayByName = gatewayInfoService.getGatewayByName(gwName);
		assertTrue(gatewayInfo.getGwName().equals(gwName));
		assertTrue(gatewayByName.getGwName().equals(gwName));
	}

}