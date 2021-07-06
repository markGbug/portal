package org.hango.cloud.ncegdashboard.envoy.web.controller;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.hango.cloud.ncegdashboard.envoy.innerdto.EnvoyServiceWithPortDto;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCodeEnum;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyServiceProxyService;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayInfoService;
import org.hango.cloud.ncegdashboard.envoy.service.IGetFromApiPlaneService;
import org.hango.cloud.ncegdashboard.envoy.util.CommonUtil;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyServiceProxyDto;
import org.hango.cloud.ncegdashboard.envoy.web.holder.ProjectTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.Min;

/**
 * 服务元数据发布至网关相关controller
 * 指服务元数据和网关服务进行关联，便于控制面展示
 */
@RestController
@Validated
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
public class EnvoyServiceProxyController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyServiceProxyController.class);

	@Autowired
	private IEnvoyServiceProxyService envoyServiceProxyService;

	@Autowired
	private IGatewayInfoService gatewayInfoService;

	@Autowired
	private IGetFromApiPlaneService getFromApiPlaneService;

	@GetMapping(params = {"Action=DescribeServiceListByGw"})
	public String describeServiceList(@RequestParam(value = "GwId") long gwId,
	                                  @RequestParam(value = "Name", required = false) String name,
	                                  @RequestParam(value = "RegistryCenterType") String registryCenterType) {
		logger.info("查询网关id：{}下的所有发布服务name:{}", gwId, name);
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		List<String> serviceNameList;

		List<EnvoyServiceWithPortDto> serviceListFromApiPlane = getFromApiPlaneService.getServiceListFromApiPlane(gwId,
		                                                                                                          name,
		                                                                                                          registryCenterType);

		if (CollectionUtils.isEmpty(serviceListFromApiPlane)) {
			return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
		}
		serviceNameList = serviceListFromApiPlane.stream().map(EnvoyServiceWithPortDto::getName).collect(
			Collectors.toList());

		result.put("ServiceList", serviceNameList);
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@PostMapping(params = {"Action=PublishService"})
	public String publishService(@Validated @RequestBody EnvoyServiceProxyDto envoyServiceProxyDto) {
		logger.info("发布服务至envoy网关，服务发布信息envoyServiceProxyDto:{}", envoyServiceProxyDto);
		ErrorCode errorCode = envoyServiceProxyService.checkPublishParam(envoyServiceProxyDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		long id = envoyServiceProxyService.publishServiceToGw(envoyServiceProxyDto);
		if (id == Const.ERROR_RESULT) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		result.put("Id", id);
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@PostMapping(params = {"Action=UpdateServiceProxy"})
	public String updatePublishService(@Validated @RequestBody EnvoyServiceProxyDto envoyServiceProxyDto) {
		logger.info("更新服务发布信息envoyServiceProxyDto:{}", envoyServiceProxyDto);
		ErrorCode errorCode = envoyServiceProxyService.checkUpdatePublishParam(envoyServiceProxyDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}

		//更新时，如果发现已被路由规则引用的版本被删除则不允许删除
		errorCode = envoyServiceProxyService.getRouteRuleNameWithServiceSubset(envoyServiceProxyDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}

		long id = envoyServiceProxyService.updateServiceToGw(envoyServiceProxyDto);
		if (id == Const.ERROR_RESULT) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		result.put("Id", envoyServiceProxyDto.getId());
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@GetMapping(params = {"Action=DeleteServiceProxy"})
	public String deleteServiceProxy(@Min(1) @RequestParam(value = "GwId") long gwId,
	                                 @Min(1) @RequestParam(value = "ServiceId") long serviceId) {
		logger.info("下线已经关联的服务，gwId:{},serviceId:{}", new Object[]{gwId, serviceId});

		ErrorCode errorCode = envoyServiceProxyService.checkDeleteServiceProxy(gwId, serviceId);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		if (!envoyServiceProxyService.deleteServiceProxy(gwId, serviceId)) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, null);
	}

	@GetMapping(params = {"Action=DescribeServiceProxyList"})
	public Object serviceProxyList(@RequestParam(value = "GwId", required = false, defaultValue = "0") long gwId,
	                               @RequestParam(value = "ServiceId", required = false,
	                                             defaultValue = "0") long serviceId,
	                               @RequestParam(value = "GwClusterName", required = false,
	                                             defaultValue = "") String gwClusterName,
	                               @RequestParam(value = "Offset", required = false, defaultValue = "0") long offset,
	                               @RequestParam(value = "Limit", required = false, defaultValue = "20") long limit) {
		logger.info("分页查询envoy service proxy list,gatewayId:{},serviceId:{}", gwId, serviceId);
		//offset,limit校验
		ErrorCode errorCode = CommonUtil.checkOffsetAndLimit(offset, limit);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		if (StringUtils.isNotBlank(gwClusterName)) {
			GatewayInfo gatewayInfoInDb = gatewayInfoService.getGatewayInfoByGwClusterName(gwClusterName);
			if (gatewayInfoInDb == null || !gatewayInfoInDb.getGwType().equals(Const.ENVOY_GATEWAY_TYPE)) {
				logger.info("查询envoy网关已发布服务存在脏数据，出现异常。网关不存在或网关非envoy类型");
				return apiReturn(CommonErrorCode.NoSuchGateway);
			}
			gwId = gatewayInfoInDb.getId();
		}
		List<EnvoyServiceProxyInfo> envoyServiceProxy = envoyServiceProxyService.getEnvoyServiceProxyByLimit(gwId,
		                                                                                                     serviceId,
		                                                                                                     ProjectTraceHolder
			                                                                                                     .getProId(),
		                                                                                                     offset,
		                                                                                                     limit);
		List<EnvoyServiceProxyDto> envoyServiceProxyDtos = new ArrayList<>();
		for (EnvoyServiceProxyInfo envoyServiceProxyInfo : envoyServiceProxy) {
			EnvoyServiceProxyDto envoyServiceProxyDto = envoyServiceProxyService.fromMetaWithStatus(
				envoyServiceProxyInfo);
			if (envoyServiceProxyDto != null) {
				envoyServiceProxyDtos.add(envoyServiceProxyDto);
			}
		}
		Map<String, Object> result = new HashMap<>();
		result.put(TOTAL_COUNT, envoyServiceProxyService.getServiceProxyCountByLimit(gwId, serviceId));
		result.put("ServiceProxyList", envoyServiceProxyDtos);
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}

	@GetMapping(params = {"Action=DescribeServiceProxy"})
	public Object describeServiceProxy(@Min(1) @RequestParam(value = "ServiceId") long serviceId,
	                                   @Min(1) @RequestParam(value = "GwId") long gwId) {
		logger.info("根据服务id：{},网关id：{}，查询服务发布信息", serviceId, gwId);
		EnvoyServiceProxyInfo serviceProxyInDb = envoyServiceProxyService.getServiceProxyByServiceIdAndGwId(gwId,
		                                                                                                    serviceId);
		Map<String, Object> result = new HashMap<>();
		if (serviceProxyInDb != null) {
			result.put("ServiceProxy", envoyServiceProxyService.fromMetaWithStatus(serviceProxyInDb));
		}
		return apiReturn(HttpStatus.SC_OK, StringUtils.EMPTY, StringUtils.EMPTY, result);
	}
}
