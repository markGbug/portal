package org.hango.cloud.ncegdashboard.envoy.web.controller;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.hango.cloud.ncegdashboard.envoy.web.dto.GatewayDto;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyRouteRuleInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyRouteRuleProxyInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyRouteRuleInfoService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyRouteRuleProxyService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyServiceProxyService;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyRouteRuleProxyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 路由规则发布管理Controller
 * <p>
 * 2019-09-19
 */
@Validated
@RestController
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
public class EnvoyRouteRuleProxyController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyRouteRuleProxyController.class);

	@Autowired
	private IEnvoyRouteRuleProxyService routeRuleProxyService;

	@Autowired
	private IEnvoyRouteRuleInfoService routeRuleInfoService;

	@Autowired
	private IEnvoyServiceProxyService serviceProxyService;

	@Autowired
	private IServiceInfoService serviceInfoService;

	@GetMapping(params = {"Action=DescribeGatewayForRouteRuleProxy"})
	public Object describeGatewayForPublishedRule(@RequestParam(value = "RuleId") long ruleId) {
		EnvoyRouteRuleInfo routeRuleInfo = routeRuleInfoService.getRouteRuleInfoById(ruleId);
		if (routeRuleInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchRouteRule);
		}
		List<GatewayDto> gatewayDtos = serviceProxyService.getPublishedServiceGateway(routeRuleInfo.getServiceId());
		Map<String, Object> result = new HashMap<>();
		result.put("GatewayInfos", gatewayDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	@PostMapping(params = {"Action=PublishRouteRule"})
	public String publishRouteRule(@Valid @RequestBody EnvoyRouteRuleProxyDto routeRulePublishDto) {
		logger.info("发布路由规则, publishRouteRuleDto:{}", routeRulePublishDto);
		ErrorCode checkResult = routeRuleProxyService.checkPublishParam(routeRulePublishDto);
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}

		if (CollectionUtils.isEmpty(routeRulePublishDto.getGwIds())) {
			long routeRuleProxyId = routeRuleProxyService.publishRouteRule(
				routeRuleProxyService.toMeta(routeRulePublishDto), Lists.newArrayList(), true);
			if (Const.ERROR_RESULT == routeRuleProxyId) {
				return apiReturn(CommonErrorCode.InternalServerError);
			}
			return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, null);
		}

		List<String> errorGwName = routeRuleProxyService.publishRouteRuleBatch(routeRulePublishDto.getGwIds(),
		                                                                       routeRulePublishDto);
		if (CollectionUtils.isEmpty(errorGwName)) {
			return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, null);
		}
		return apiReturn(CommonErrorCode.BatchPublishRouteError(errorGwName.toString()));
	}

	@GetMapping(params = {"Action=DescribeRouteRuleProxyList"})
	public String getPublishRouteRuleList(
		@Min(0) @RequestParam(value = "GwId", required = false, defaultValue = "0") long gwId,
		@Min(0) @RequestParam(value = "ServiceId", required = false, defaultValue = "0") long serviceId,
		@Min(1) @Max(1000) @RequestParam(value = "Limit", required = false, defaultValue = "20") long limit,
		@Min(0) @RequestParam(value = "Offset", required = false, defaultValue = "0") long offset,
		@RequestParam(value = "SortByKey", required = false) String sortKey,
		@RequestParam(value = "SortByValue", required = false) String sortValue) {
		logger.info("分页查询已发布路由规则列表, gwId:{}, serviceId:{}, limit:{}, offset:{}", gwId, serviceId, limit, offset);
		//查询参数校验
		ErrorCode errorCode = routeRuleInfoService.checkDescribeParam(sortKey, sortValue, offset, limit);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		long count = routeRuleProxyService.getRouteRuleProxyCountByService(gwId, serviceId);
		List<EnvoyRouteRuleProxyInfo> routeRuleProxyInfos = routeRuleProxyService.getRouteRuleProxyList(gwId,
		                                                                                                serviceId,
		                                                                                                sortKey,
		                                                                                                sortValue,
		                                                                                                offset, limit);
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		result.put(TOTAL_COUNT, count);
		result.put("RouteRuleProxyList",
		           routeRuleProxyInfos.stream().map(routeRuleProxyService::fromMeta).collect(Collectors.toList()));
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}


	@GetMapping(params = {"Action=DescribeRouteRuleProxy"})
	public String describeRouteRuleProxy(
		@RequestParam(value = "Id", required = false,defaultValue = "0") long id,
		@RequestParam(value = "GwId", required = false, defaultValue = "0") long gwId,
		@RequestParam(value = "RouteRuleId", required = false, defaultValue = "0") long routeRuleId) {
		EnvoyRouteRuleProxyInfo routeRuleProxy = null;
		if (id > 0){
			routeRuleProxy = routeRuleProxyService.getRouteRuleProxy(id);
		}else if (gwId > 0 && routeRuleId > 0){
			routeRuleProxy = routeRuleProxyService.getRouteRuleProxy(gwId, routeRuleId);
		}
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		if (routeRuleProxy != null) {
			EnvoyRouteRuleProxyDto envoyRouteRuleProxyDto = routeRuleProxyService.fromMeta(routeRuleProxy);
			result.put("RouteRuleProxy", envoyRouteRuleProxyDto);
		}
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@GetMapping(params = {"Action=DescribeRouteRuleProxyByRouteRuleId"})
	public String describePublishRouteRule(@RequestParam(value = "RouteRuleId") long routeRuleId) {
		logger.info("根据路由规则routeRuleId:{},查询路由规则", routeRuleId);
		List<EnvoyRouteRuleProxyInfo> routeRuleProxyInfos = routeRuleProxyService.getRouteRuleProxyByRouteRuleId(
			routeRuleId);
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		result.put("RouteRuleProxyList",
		           routeRuleProxyInfos.stream().map(routeRuleProxyService::fromMeta).collect(Collectors.toList()));
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@GetMapping(params = {"Action=DeleteRouteRuleProxy"})
	public String deletePublishedRouteRule(@Min(1) @RequestParam(value = "GwId") long gwId,
	                                       @Min(1) @RequestParam(value = "RouteRuleId") long routeRuleId,
	                                       @RequestParam(value = "ServiceIds",
	                                                     required = false) List<Long> serviceIds) {
		logger.info("根据网关id gwId:{},路由规则id:{}下线路由规则,下线serviceId:{}", gwId, routeRuleId, serviceIds);

		//参数校验
		ErrorCode checkResult = routeRuleProxyService.checkDeleteRouteRuleProxy(gwId, routeRuleId, serviceIds);
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}
		boolean deleteSuccess = routeRuleProxyService.deleteRouteRuleProxy(gwId, routeRuleId);
		if (!deleteSuccess) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, null);
	}

	@GetMapping(params = {"Action=UpdateRouteRuleProxyEnableState"})
	public Object updateRouteRuleEnableState(@Min(1) @RequestParam(value = "RouteRuleId") long routeRuleId,
	                                         @Min(1) @RequestParam(value = "GwId") long gwId,
	                                         @RequestParam(value = "EnableState",
	                                                       defaultValue = "enable") String enableState) {
		logger.info("根据路由id：{},网关id：{}, 使能状态:{} 更新路由发布信息", routeRuleId, gwId, enableState);

		ErrorCode errorCode = routeRuleProxyService.checkUpdateEnableState(gwId, routeRuleId, enableState);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		long id = routeRuleProxyService.updateEnableState(gwId, routeRuleId, enableState);
		if (id == Const.ERROR_RESULT) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, null);
	}

	@PostMapping(params = {"Action=UpdateRouteRuleProxy"})
	public String updateRouteRuleProxy(@Valid @RequestBody EnvoyRouteRuleProxyDto routeRulePublishDto) {
		logger.info("更新路由规则, publishRouteRuleDto:{}", routeRulePublishDto);
		ErrorCode checkResult = routeRuleProxyService.checkUpdateParam(routeRulePublishDto);
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}

		long routeRuleProxyId = routeRuleProxyService.updateEnvoyRouteRuleProxy(
			routeRuleProxyService.toMeta(routeRulePublishDto));
		if (Const.ERROR_RESULT == routeRuleProxyId) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}

		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		result.put("Id", routeRuleProxyId);
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@RequestMapping(params = {"Action=DescribeServiceProxyForRouteProxy"}, method = RequestMethod.GET)
	public Object describeServiceProxy(@Min(1) @RequestParam(value = "ServiceId") long serviceId,
	                                   @RequestParam(value = "GwIds", required = false) List<Long> gwIds,
	                                   @RequestParam(value = "GwId", required = false, defaultValue = "0") long gwId) {
		logger.info("根据服务id：{},网关id：{}，查询服务相关端口", serviceId, gwIds);
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		EnvoyServiceProxyInfo serviceProxyInDb = null;
		if (CollectionUtils.isNotEmpty(gwIds)) {
			serviceProxyInDb = serviceProxyService.getServiceProxyInterByServiceIdAndGwIds(gwIds, serviceId);
		} else if (gwId != 0) {
			serviceProxyInDb = serviceProxyService.getServiceProxyByServiceIdAndGwId(gwId, serviceId);
		}
		if (serviceProxyInDb != null) {
			result.put("EnvoyServiceProxy", serviceProxyService.fromMetaWithPort(serviceProxyInDb));
		}
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}
}
