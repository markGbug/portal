package org.hango.cloud.dashboard.envoy.service.impl;

import static org.junit.Assert.assertTrue;

import org.hango.cloud.dashboard.BaseServiceImplTest;
import org.hango.cloud.dashboard.envoy.meta.ServiceInfo;
import org.hango.cloud.dashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.dashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteRuleInfo;
import org.hango.cloud.dashboard.envoy.service.IEnvoyRouteRuleInfoService;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteRuleDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteRuleMapMatchDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteStringMatchDto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.util.Arrays;
import java.util.List;

public class EnvoyRouteRuleInfoServiceImplTest extends BaseServiceImplTest {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyRouteRuleInfoServiceImplTest.class);

	private static long serviceId;

	@Autowired
	private IServiceInfoService serviceInfoService;

	@Autowired
	private IEnvoyRouteRuleInfoService routeRuleInfoService;

	private ServiceInfo serviceInfo;

	private EnvoyRouteRuleDto routeRuleDto = new EnvoyRouteRuleDto();

	@Before
	public void init() {
		//初始化ServiceInfo
		serviceInfo = new ServiceInfo();
		serviceInfo.setDisplayName(displayName);
		serviceInfo.setServiceName(serviceName);
		serviceInfo.setContacts(user);
		serviceInfo.setProjectId(projectId);
		serviceInfo.setServiceType(serviceType);
		//创建service
		serviceId = serviceInfoService.add(serviceInfo);

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

	}

	@After
	public void tearDownClass() {
		logger.info("tear down class .... ServiceProxyServiceImplTest");
		//清除service
		serviceInfoService.delete(serviceId);
	}

	@Test
	public void checkAddParam() {
		ErrorCode errorCode = routeRuleInfoService.checkAddParam(routeRuleDto);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

	@Test
	@Rollback
	public void isSameRouteRuleInfo() {
		routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		assertTrue(routeRuleInfoService.isSameRouteRuleInfo(routeRuleDto.toMeta()));
	}

	@Test
	@Rollback
	public void addRouteRule() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		assertTrue(routeRuleInfoService.getRouteRuleInfoByName(routeName).getId() == id);
	}

	@Test
	@Rollback
	public void checkUpdateParam() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		routeRuleDto.setRouteRuleName("update");
		routeRuleDto.setId(id);
		ErrorCode errorCode = routeRuleInfoService.checkUpdateParam(routeRuleDto);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

	@Test
	@Rollback
	public void checkDeleteParam() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		ErrorCode errorCode = routeRuleInfoService.checkDeleteParam(id);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

	@Test
	@Rollback
	public void updateRouteRule() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		routeRuleDto.setRouteRuleName("update");
		routeRuleDto.setId(id);
		routeRuleInfoService.updateRouteRule(routeRuleDto.toMeta());
	}

	@Test
	@Rollback
	public void getRouteRuleInfoById() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		EnvoyRouteRuleInfo routeRuleInfoById = routeRuleInfoService.getRouteRuleInfoById(id);
		assertTrue(routeName.equals(routeRuleInfoById.getRouteRuleName()));
	}

	@Test
	public void checkDescribeParam() {
		ErrorCode errorCode = routeRuleInfoService.checkDescribeParam("create_time", "desc", 0, 1000);
		assertTrue(errorCode.getCode().equals(CommonErrorCode.Success.getCode()));
	}

	@Test
	@Rollback
	public void getRouteRuleInfoByPattern() {
		routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		List<EnvoyRouteRuleInfo> routeRuleInfoByPattern = routeRuleInfoService.
			                                                                      getRouteRuleInfoByPattern("/a", 0,
			                                                                                                serviceId,
			                                                                                                0,
			                                                                                                "create_time",
			                                                                                                "desc", 0,
			                                                                                                100);
		assertTrue(routeRuleInfoByPattern.size() > 0);
	}

	@Test
	@Rollback
	public void getRouteRuleInfoCount() {
		routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		long routeRuleInfoCount = routeRuleInfoService.getRouteRuleInfoCount("/a", 0, serviceId, 0);
		assertTrue(routeRuleInfoCount == 1);
	}

	@Test
	@Rollback
	public void deleteRouteRule() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		assertTrue(routeRuleInfoService.deleteRouteRule(id));
	}

	@Test
	@Rollback
	public void fromMeta() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		EnvoyRouteRuleInfo routeRuleInfoById = routeRuleInfoService.getRouteRuleInfoById(id);
		EnvoyRouteRuleDto envoyRouteRuleDto = routeRuleInfoService.fromMeta(routeRuleInfoById);
		assertTrue(envoyRouteRuleDto.getRouteRuleName().equals(routeName));
	}

	@Test
	@Rollback
	public void getRouteRuleIdListByNameFuzzy() {
		routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		List<Long> routeRuleIdListByNameFuzzy = routeRuleInfoService.getRouteRuleIdListByNameFuzzy(routeName, 0);
		assertTrue(routeRuleIdListByNameFuzzy.size() > 0);
	}

	@Test
	@Rollback
	public void getRouteRuleList() {
		long id = routeRuleInfoService.addRouteRule(routeRuleDto.toMeta());
		List<EnvoyRouteRuleInfo> routeRuleList = routeRuleInfoService.getRouteRuleList(Arrays.asList(new Long[]{id}));
		assertTrue(routeRuleList.size() > 0);
	}

}