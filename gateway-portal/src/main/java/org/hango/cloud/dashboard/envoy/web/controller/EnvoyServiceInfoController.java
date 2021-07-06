package org.hango.cloud.ncegdashboard.envoy.web.controller;

import org.apache.commons.collections.CollectionUtils;
import org.hango.cloud.gdashboard.api.service.IApiInfoService;
import org.hango.cloud.ncegdashboard.envoy.web.dto.ServiceInfoDto;
import org.hango.cloud.ncegdashboard.envoy.meta.ServiceInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.ncegdashboard.envoy.util.BeanUtil;
import org.hango.cloud.ncegdashboard.envoy.util.CommonUtil;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyServiceProxyService;
import org.hango.cloud.ncegdashboard.envoy.web.holder.ProjectTraceHolder;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyRouteRuleInfoService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

/**
 * 服务基本管理，包括服务创建，查询，修改
 */
@RestController
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
@Validated
public class EnvoyServiceInfoController extends AbstractController {

	private static Logger logger = LoggerFactory.getLogger(EnvoyServiceInfoController.class);

	@Autowired
	private IServiceInfoService serviceInfoService;

	@Autowired
	private IApiInfoService apiInfoService;

	@Autowired
	private IEnvoyRouteRuleInfoService routeRuleInfoService;

	@Autowired
	private IEnvoyServiceProxyService envoyServiceProxyService;

	/**
	 * 添加Service
	 */
	@PostMapping(params = {"Action=CreateService"})
	public Object addService(@Validated @RequestBody ServiceInfoDto serviceInfoDto) {
		logger.info("创建服务，serviceInfo:{}", serviceInfoDto);
		ErrorCode errorCode = serviceInfoService.checkCreateServiceParam(serviceInfoDto);
		//参数校验
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		} else {
			ServiceInfo serviceInfo = serviceInfoService.addServiceInfo(serviceInfoDto, ProjectTraceHolder.getProId());
			return apiReturnSuccess(serviceInfo.getId());
		}
	}

	/**
	 * 根据Id查询服务
	 */
	@GetMapping(params = {"Action=DescribeServiceById"})
	public Object getService(@RequestParam(value = "ServiceId") long serviceId) {
		logger.info("查询serviceId:{}服务", serviceId);
		ServiceInfo serviceInfo = serviceInfoService.getServiceByServiceId(serviceId);
		if (serviceInfo == null) {
			logger.info("不存在当前serviceId的服务");
			return apiReturn(CommonErrorCode.NoSuchService);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("ServiceInfoBasic", ServiceInfoDto.fromMeta(serviceInfo));
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 修改Service
	 */
	@PostMapping(params = {"Action=UpdateService"})
	public Object updateService(@Validated @RequestBody ServiceInfoDto serviceInfoDto) {
		logger.info("更新服务基本信息，serviceInfoFrontDto:{}", serviceInfoDto);
		ErrorCode errorCode = serviceInfoService.checkUpdateServiceParam(serviceInfoDto);
		//参数校验
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		} else {
			ServiceInfo serviceInfo = serviceInfoService.getServiceByServiceId(serviceInfoDto.getId());
			serviceInfo.setDisplayName(serviceInfoDto.getDisplayName());
			serviceInfo.setContacts(serviceInfoDto.getContacts());
			serviceInfo.setDescription(serviceInfoDto.getDescription());
			serviceInfo.setHealthInterfacePath(serviceInfoDto.getHealthInterfacePath());
			serviceInfo.setServiceName(serviceInfoDto.getServiceName());
			serviceInfoService.updateService(serviceInfo);
			return apiReturn(CommonErrorCode.Success);
		}
	}

	/**
	 * 查询Service列表，创建API时
	 */
	@GetMapping(params = {"Action=DescribeServiceForApi"})
	public Object serviceListForCreateApiOrModel() {
		logger.info("创建API，请求查询service列表");
		List<ServiceInfo> serviceInfos = serviceInfoService.findAllServiceByProjectId(ProjectTraceHolder.getProId());
		List<ServiceInfoDto> serviceInfoDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(serviceInfos)) {
			serviceInfoDtos = BeanUtil.copyList(serviceInfos, ServiceInfoDto.class);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("ServiceInfoList", serviceInfoDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 查询服务列表，返回前端当前项目下的所有服务信息
	 */
	@GetMapping(params = {"Action=DescribeServiceList"})
	public Object serviceList(@RequestParam(value = "Pattern", required = false) String pattern,
	                          @RequestParam(value = "Offset", required = false, defaultValue = "0") long offset,
	                          @RequestParam(value = "Limit", required = false, defaultValue = "20") long limit) {
		long projectId = ProjectTraceHolder.getProId();
		logger.info("获取当前项目下的service列表，projectId：{}", projectId);
		//offset,limit校验
		ErrorCode errorCode = CommonUtil.checkOffsetAndLimit(offset, limit);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		List<ServiceInfo> serviceInfos = serviceInfoService.findAllServiceByProjectIdLimit(pattern, offset, limit,
		                                                                                   projectId);
		List<ServiceInfoDto> serviceInfoDtos = serviceInfos.stream().map(ServiceInfoDto::fromMeta).collect(
			Collectors.toList());
		Map<String, Object> result = new HashMap<>();
		result.put("ServiceCount", serviceInfoService.getServiceCountByProjectId(pattern, projectId));
		result.put("ServiceInfoList", serviceInfoDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 根据服务ID删除服务
	 */
	@GetMapping(params = {"Action=DeleteService"})
	public Object deleteService(@RequestParam(value = "ServiceId") long serviceId) {
		logger.info("删除serviceId：{}下的服务", serviceId);
		ServiceInfo serviceInfo = serviceInfoService.getServiceByServiceId(serviceId);
		if (serviceInfo != null) {
			if (serviceInfo.getStatus() == 1) {
				logger.info("服务已发布，不允许删除");
				return apiReturn(CommonErrorCode.CannotDeleteOnlineService);
			}
		}
		if (apiInfoService.getApiCountByServiceId(serviceId) > 0) {
			logger.info("服务下存在API，不允许删除服务");
			return apiReturn(CommonErrorCode.CannotDeleteApiService);
		}
		if (routeRuleInfoService.getRouteRuleInfoCount("", -1, serviceId, 0) > 0) {
			logger.info("服务下存在路由，不允许删除服务");
			return apiReturn(CommonErrorCode.CannotDeleteRouteRuleService);
		}
		serviceInfoService.delete(serviceId);
		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 根据Service id查询服务发布详情
	 */
	@GetMapping(params = {"Action=DescribeServiceRoute"})
	public Object getPublishedServiceInfoById(@RequestParam(value = "ServiceId") @NotNull long serviceId) {
		logger.info("查询服务的具体发布详情，serviceId:{}", serviceId);
		if (!serviceInfoService.isServiceExists(serviceId)) {
			logger.info("查询服务的具体发布详情，服务不存在");
			return apiReturn(CommonErrorCode.NoSuchService);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("PublishDetails", envoyServiceProxyService.getPublishedDetailByService(serviceId));
		return apiReturn(CommonErrorCode.Success, result);
	}

}
