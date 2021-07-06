package org.hango.cloud.dashboard.envoy.web.controller;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.hango.cloud.dashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.web.holder.ProjectTraceHolder;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteRuleInfo;
import org.hango.cloud.dashboard.envoy.service.IEnvoyRouteRuleInfoService;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyCopyRuleDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteRuleDto;
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

import javax.validation.constraints.Min;

/**
 * 路由规则管理Controller层
 * <p>
 * 2019-09-11
 */
@RestController
@Validated
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
public class EnvoyRouteRuleInfoController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyRouteRuleInfoController.class);

	@Autowired
	private IEnvoyRouteRuleInfoService routeRuleInfoService;

	@PostMapping(params = {"Action=CreateRouteRule"})
	public String createRouteRule(@Validated @RequestBody EnvoyRouteRuleDto routeRuleDto) {
		logger.info("创建路由规则，routeRuleDto:{}", routeRuleDto);

		ErrorCode checkResult = routeRuleInfoService.checkAddParam(routeRuleDto);
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}
		EnvoyRouteRuleInfo routeRuleInfo = routeRuleDto.toMeta();
		routeRuleInfo.setProjectId(ProjectTraceHolder.getProId());
		if (routeRuleInfoService.isSameRouteRuleInfo(routeRuleInfo)) {
			logger.info("创建路由规则，参数完全相同，不允许创建");
			return apiReturn(CommonErrorCode.SameParamRouteRuleExist);
		}
		long id = routeRuleInfoService.addRouteRule(routeRuleInfo);
		if (-1 == id) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}

		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		result.put("RouteRuleId", id);
		return apiReturn(HttpStatus.SC_CREATED, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@PostMapping(params = {"Action=UpdateRouteRule"})
	public String updateRouteRule(@Validated @RequestBody EnvoyRouteRuleDto routeRuleDto) {
		logger.info("更新路由规则，routeRuleDto", routeRuleDto);

		ErrorCode checkResult = routeRuleInfoService.checkUpdateParam(routeRuleDto);
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}
		EnvoyRouteRuleInfo routeRuleInfo = routeRuleDto.toMeta();
		routeRuleInfo.setProjectId(ProjectTraceHolder.getProId());
		if (routeRuleInfoService.isSameRouteRuleInfo(routeRuleInfo)) {
			logger.info("修改路由规则，存在参数完全相同路由规则，不允许修改");
			return apiReturn(CommonErrorCode.SameParamRouteRuleExist);
		}
		routeRuleInfo.setPublishStatus(
			routeRuleInfoService.getRouteRuleInfoById(routeRuleDto.getId()).getPublishStatus());
		boolean updateSuccess = routeRuleInfoService.updateRouteRule(routeRuleInfo);
		if (!updateSuccess) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, null);
	}

	@GetMapping(params = {"Action=DescribeRouteRule"})
	public String getRouteRule(@Min(1) @RequestParam(value = "RouteRuleId") long id) {
		logger.info("根据路由规则id查询路由规则详情，id:{}", id);
		EnvoyRouteRuleInfo routeRuleInfo = routeRuleInfoService.getRouteRuleInfoById(id);
		if (null == routeRuleInfo) {
			return apiReturn(CommonErrorCode.NoSuchRouteRule);
		}
		EnvoyRouteRuleDto routeRuleDto = routeRuleInfoService.fromMeta(routeRuleInfo);
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		result.put("RouteRule", routeRuleDto);
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@GetMapping(params = {"Action=DescribeRouteRuleList"})
	public Object routeRuleList(@RequestParam(value = "Pattern", required = false) String pattern,
	                            @RequestParam(value = "ServiceId", required = false, defaultValue = "0") int serviceId,
	                            @RequestParam(value = "PublishStatus", required = false,
	                                          defaultValue = "-1") int publishStatus,
	                            @RequestParam(value = "Offset", required = false, defaultValue = "0") long offset,
	                            @RequestParam(value = "Limit", required = false, defaultValue = "20") long limit,
	                            @RequestParam(value = "SortByKey", required = false) String sortKey,
	                            @RequestParam(value = "SortByValue", required = false) String sortValue) {
		logger.info("分页查询路由规则，pattern:{}, serviceId:{}", pattern, serviceId);
		//查询参数校验
		ErrorCode errorCode = routeRuleInfoService.checkDescribeParam(sortKey, sortValue, offset, limit);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		List<EnvoyRouteRuleInfo> routeRuleList = routeRuleInfoService.getRouteRuleInfoByPattern(pattern, publishStatus,
		                                                                                        serviceId,
		                                                                                        ProjectTraceHolder
			                                                                                        .getProId(),
		                                                                                        sortKey, sortValue,
		                                                                                        offset, limit);

		List<EnvoyRouteRuleDto> envoyServiceInfoDtos = routeRuleList.stream().map(routeRuleInfoService::fromMeta)
		                                                            .collect(Collectors.toList());
		Map<String, Object> result = new HashMap<>();
		result.put(TOTAL_COUNT, routeRuleInfoService.getRouteRuleInfoCount(pattern, publishStatus, serviceId,
		                                                                   ProjectTraceHolder.getProId()));
		result.put("RouteRuleList", envoyServiceInfoDtos);
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@GetMapping(params = {"Action=DeleteRouteRule"})
	public Object deleteRule(@Min(1) @RequestParam(value = "RouteRuleId") long id) {
		logger.info("根据路由规则id:{}，删除路由规则", id);

		ErrorCode errorCode = routeRuleInfoService.checkDeleteParam(id);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		routeRuleInfoService.deleteRouteRule(id);
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, null);
	}

}
