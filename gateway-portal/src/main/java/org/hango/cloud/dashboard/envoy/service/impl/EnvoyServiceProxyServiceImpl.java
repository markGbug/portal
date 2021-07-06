package org.hango.cloud.dashboard.envoy.service.impl;

import com.google.common.collect.Lists;
import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hango.cloud.dashboard.envoy.web.dto.GatewayDto;
import org.hango.cloud.dashboard.envoy.web.dto.PublishedDetailDto;
import org.hango.cloud.dashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.dashboard.envoy.meta.RegistryCenterEnum;
import org.hango.cloud.dashboard.envoy.meta.ServiceInfo;
import org.hango.cloud.dashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCodeEnum;
import org.hango.cloud.dashboard.envoy.service.IGatewayInfoService;
import org.hango.cloud.dashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.dashboard.envoy.util.BeanUtil;
import org.hango.cloud.dashboard.envoy.util.CommonUtil;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.web.holder.ProjectTraceHolder;
import org.hango.cloud.dashboard.envoy.dao.IEnvoyServiceProxyDao;
import org.hango.cloud.dashboard.envoy.innerdto.EnvoyActiveHealthCheckRuleDto;
import org.hango.cloud.dashboard.envoy.innerdto.EnvoyPassiveHealthCheckRuleDto;
import org.hango.cloud.dashboard.envoy.innerdto.EnvoyPublishServiceDto;
import org.hango.cloud.dashboard.envoy.innerdto.EnvoyServiceWithPortDto;
import org.hango.cloud.dashboard.envoy.meta.EnvoyDestinationInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteRuleInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteRuleProxyInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.hango.cloud.dashboard.envoy.service.IEnvoyHealthCheckService;
import org.hango.cloud.dashboard.envoy.service.IEnvoyRouteRuleInfoService;
import org.hango.cloud.dashboard.envoy.service.IEnvoyRouteRuleProxyService;
import org.hango.cloud.dashboard.envoy.service.IEnvoyServiceProxyService;
import org.hango.cloud.dashboard.envoy.service.IGetFromApiPlaneService;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyServiceConnectionPoolDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyServiceConsistentHashDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyServiceLoadBalancerDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyServiceProxyDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyServiceTrafficPolicyDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoySubsetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * envoy网关发布服务相关，发布服务至指定网关，即将服务与api-plane相关数据进行关联
 */
@Service
public class EnvoyServiceProxyServiceImpl implements IEnvoyServiceProxyService {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyServiceProxyServiceImpl.class);

	/**
	 * 负载均衡策略相关
	 */
	private static final String SERVICE_LOADBALANCER_SIMPLE = "Simple";

	private static final String SERVICE_LOADBALANCER_SIMPLE_ROUND_ROBIN = "ROUND_ROBIN";

	private static final String SERVICE_LOADBALANCER_SIMPLE_LEAST_CONN = "LEAST_CONN";

	private static final String SERVICE_LOADBALANCER_SIMPLE_RANDOM = "RANDOM";

	private static final String SERVICE_LOADBALANCER_HASH = "ConsistentHash";

	private static final String SERVICE_LOADBALANCER_HASH_HTTPHEADERNAME = "HttpHeaderName";

	private static final String SERVICE_LOADBALANCER_HASH_HTTPCOOKIE = "HttpCookie";

	private static final String SERVICE_LOADBALANCER_HASH_USESOURCEIP = "UseSourceIp";

	private static final String STATIC_EP_VERSION = "version";

	/**
	 * nsf eureka其别名为NSF，在初始化注册中心时会配置到db中
	 */
	private static final String NSF_EUREKA_ALIAS = "NSF";

	@Autowired
	private IGatewayInfoService gatewayInfoService;

	@Autowired
	private IEnvoyServiceProxyDao envoyServiceProxyDao;

	@Autowired
	private IServiceInfoService serviceInfoService;

	@Autowired
	private IEnvoyRouteRuleProxyService envoyRouteRuleProxyService;

	@Autowired
	private IEnvoyHealthCheckService envoyHealthCheckService;

	@Autowired
	private IEnvoyRouteRuleInfoService envoyRouteRuleInfoService;

	@Autowired
	private IGetFromApiPlaneService getFromApiPlaneService;

	@Override
	public long publishServiceToGw(EnvoyServiceProxyDto envoyServiceProxyDto) {
		if (!getFromApiPlaneService.publishServiceByApiPlane(envoyServiceProxyDto, null)) {
			return Const.ERROR_RESULT;
		}

		EnvoyServiceProxyInfo envoyServiceProxyInfo = EnvoyServiceProxyDto.toMeta(envoyServiceProxyDto);
		ServiceInfo serviceInfoInDb = serviceInfoService.getServiceByServiceId(envoyServiceProxyDto.getServiceId());
		try {
			envoyServiceProxyInfo.setProjectId(serviceInfoInDb.getProjectId());
		} catch (Exception e) {
			logger.error("发布服务至envoy网关存在脏数据，服务不存在，e:{}", e);
			return Const.ERROR_RESULT;
		}
		envoyServiceProxyInfo.setCreateTime(System.currentTimeMillis());
		envoyServiceProxyInfo.setUpdateTime(System.currentTimeMillis());
		long publishServiceId = envoyServiceProxyDao.add(envoyServiceProxyInfo);
		if (publishServiceId > NumberUtils.INTEGER_ZERO) {
			//更新发布状态,未发布更新为已发布
			if (serviceInfoInDb != null && serviceInfoInDb.getStatus() == NumberUtils.INTEGER_ZERO) {
				serviceInfoInDb.setStatus(NumberUtils.INTEGER_ONE);
				serviceInfoService.updateService(serviceInfoInDb);
			}
		}
		return publishServiceId;
	}

	@Override
	public long updateServiceToGw(EnvoyServiceProxyDto envoyServiceProxyDto) {
		//判断版本有没有删除的，如果有删除的，需要先调用APIPlane删除接口，然后进行新建
		EnvoyServiceProxyInfo envoyServiceProxyInfoInDB = getServiceProxyByServiceIdAndGwId(
			envoyServiceProxyDto.getGwId(), envoyServiceProxyDto.getServiceId());
		List<EnvoySubsetDto> subsets = envoyServiceProxyDto.getSubsets();
		if (StringUtils.isNotBlank(envoyServiceProxyInfoInDB.getSubsets())) {
			GatewayInfo gatewayInfo = gatewayInfoService.get(envoyServiceProxyDto.getGwId());
			List<String> envoySubsetListInDB = EnvoyServiceProxyDto.setSubsetForDto(envoyServiceProxyInfoInDB).stream()
			                                                       .map(EnvoySubsetDto::getName).collect(
					Collectors.toList());
			List<String> subsetNameList = new ArrayList<>();
			if (envoyServiceProxyDto.getSubsets() != null) {
				List<String> envoySubsetList = envoyServiceProxyDto.getSubsets().stream().map(EnvoySubsetDto::getName)
				                                                   .collect(Collectors.toList());
				subsetNameList = envoySubsetListInDB.stream().filter(s -> !envoySubsetList.contains(s)).collect(
					Collectors.toList());
			}
			if (subsetNameList.size() > 0) {
				List<EnvoySubsetDto> needDeleteSubsetList = new ArrayList<>();
				for (String name : subsetNameList) {
					EnvoySubsetDto envoySubsetDto = new EnvoySubsetDto();
					envoySubsetDto.setName(name + "-" + gatewayInfo.getGwClusterName());
					needDeleteSubsetList.add(envoySubsetDto);
				}
				envoyServiceProxyDto.setSubsets(needDeleteSubsetList);
				getFromApiPlaneService.offlineServiceByApiPlane(gatewayInfo.getApiPlaneAddr(),
				                                                deleteSomeSubset(envoyServiceProxyDto,
				                                                                 gatewayInfo.getGwClusterName()));
			}
		}

		envoyServiceProxyDto.setSubsets(subsets);
		if (!getFromApiPlaneService.publishServiceByApiPlane(envoyServiceProxyDto, null)) {
			return Const.ERROR_RESULT;
		}

		EnvoyServiceProxyInfo envoyServiceProxyInfo = EnvoyServiceProxyDto.toMeta(envoyServiceProxyDto);
		envoyServiceProxyInfo.setUpdateTime(System.currentTimeMillis());
		return envoyServiceProxyDao.update(envoyServiceProxyInfo);
	}

	@Override
	public ErrorCode checkPublishParam(EnvoyServiceProxyDto envoyServiceProxyDto) {
		ServiceInfo serviceInfoDb = serviceInfoService.getServiceByServiceId(envoyServiceProxyDto.getServiceId());
		if (serviceInfoDb == null) {
			return CommonErrorCode.NoSuchService;
		}

		ErrorCode errorCode = checkPublishServiceAndGw(envoyServiceProxyDto.getServiceId(),
		                                               envoyServiceProxyDto.getGwId());
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return errorCode;
		}
		errorCode = checkRegistryCenterInfo(envoyServiceProxyDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return errorCode;
		}

		return checkEnvoyServiceProxyDto(envoyServiceProxyDto);
	}

	@Override
	public ErrorCode checkUpdatePublishParam(EnvoyServiceProxyDto envoyServiceProxyDto) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("serviceId", envoyServiceProxyDto.getServiceId());
		params.put("gwId", envoyServiceProxyDto.getGwId());
		params.put("id", envoyServiceProxyDto.getId());
		if (envoyServiceProxyDao.getCountByFields(params) == 0) {
			return CommonErrorCode.ServiceNotPublished;
		}
		ServiceInfo serviceInfoDb = serviceInfoService.getServiceByServiceId(envoyServiceProxyDto.getServiceId());
		if (serviceInfoDb == null) {
			return CommonErrorCode.NoSuchService;
		}
		GatewayInfo gatewayInDb = gatewayInfoService.get(envoyServiceProxyDto.getGwId());
		if (gatewayInDb == null) {
			return CommonErrorCode.NoSuchGateway;
		}
		ErrorCode errorCode = checkRegistryCenterInfo(envoyServiceProxyDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return errorCode;
		}

		return checkEnvoyServiceProxyDto(envoyServiceProxyDto);
	}

	@Override
	public List<EnvoyServiceProxyInfo> getEnvoyServiceProxyByLimit(long gwId, long serviceId, long projectId,
	                                                               long offset, long limit) {
		return envoyServiceProxyDao.getServiceProxyByLimit(gwId, serviceId, projectId, offset, limit);
	}

	@Override
	public long getServiceProxyCountByLimit(long gwId, long serviceId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		//服务id或网关id有一项等于0
		if (NumberUtils.INTEGER_ZERO == serviceId || NumberUtils.INTEGER_ZERO == gwId) {
			params.put("projectId", ProjectTraceHolder.getProId());
		}
		//查询指定的服务发布数量
		if (NumberUtils.INTEGER_ZERO != serviceId) {
			params.put("serviceId", serviceId);
		}
		//查询指定的网关发布数量
		if (NumberUtils.INTEGER_ZERO != gwId) {
			params.put("gwId", gwId);
		}

		return envoyServiceProxyDao.getCountByFields(params);
	}

	@Override
	public void deleteServiceProxy(long id) {
		EnvoyServiceProxyInfo envoyServiceProxyInfo = envoyServiceProxyDao.get(id);
		if (envoyServiceProxyInfo != null) {
			envoyServiceProxyDao.delete(envoyServiceProxyInfo);
		}
	}

	@Override
	public ErrorCode checkDeleteServiceProxy(long gwId, long serviceId) {
		GatewayInfo gatewayById = gatewayInfoService.get(gwId);
		if (gatewayById == null) {
			return CommonErrorCode.NoSuchGateway;
		}
		EnvoyServiceProxyInfo servicePublishInfo = getServiceProxyByServiceIdAndGwId(gwId, serviceId);
		if (servicePublishInfo == null) {
			return CommonErrorCode.ServiceNotPublished;
		}
		long count = envoyRouteRuleProxyService.getRouteRuleProxyCountByService(gwId, serviceId);
		if (count > 0) {
			return CommonErrorCode.RouteRuleAlreadyPublished;
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean deleteServiceProxy(long gwId, long serviceId) {
		GatewayInfo gatewayById = gatewayInfoService.get(gwId);
		if (gatewayById == null) {
			logger.info("deleteServiceProxy, gatewayById is null. gwId:{}, serviceId:{}", gwId, serviceId);
			return false;
		}
		EnvoyServiceProxyInfo servicePublishInfo = getServiceProxyByServiceIdAndGwId(gwId, serviceId);
		if (servicePublishInfo == null) {
			logger.info("deleteServiceProxy, servicePublishInfo is null. gwId:{}, serviceId:{}", gwId, serviceId);
			return false;
		}
		String publishType = servicePublishInfo.getPublishType();
		String backendService = servicePublishInfo.getBackendService();
		if (!getFromApiPlaneService.offlineServiceByApiPlane(gatewayById.getApiPlaneAddr(), deleteAllSusbet(
			EnvoyServiceProxyDto.toDto(servicePublishInfo)))) {
			return false;
		}

		return deleteServiceProxyWithTransactional(gwId, serviceId, backendService, publishType);
	}

	@Override
	public EnvoyServiceProxyInfo getServiceProxyByServiceIdAndGwId(long gwId, long serviceId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		params.put("serviceId", serviceId);
		List<EnvoyServiceProxyInfo> envoyServiceProxyInfos = envoyServiceProxyDao.getRecordsByField(params);
		return CollectionUtils.isEmpty(envoyServiceProxyInfos) ? null : envoyServiceProxyInfos.get(0);
	}

	@Override
	public EnvoyServiceProxyInfo getServiceProxyInterByServiceIdAndGwIds(List<Long> gwIds, long serviceId) {
		if (CollectionUtils.isEmpty(gwIds)) {
			return null;
		}
		List<EnvoyServiceProxyInfo> serviceProxyInfo = gwIds.stream().map(
			item -> getServiceProxyByServiceIdAndGwId(item, serviceId)).
				                                                           filter(CommonUtil.distinctByKey(
					                                                           item -> item.getBackendService()))
		                                                    .collect(Collectors.toList());
		if (CollectionUtils.isEmpty(serviceProxyInfo) || serviceProxyInfo.size() > 1) {
			logger.info("多网关后端服务发布不同，返回前端无应用数据");
			return null;
		}
		return serviceProxyInfo.get(0);
	}

	@Override
	public EnvoyServiceProxyInfo getServiceProxyByServicePublishInfo(long gwId, long serviceId, String backendService,
	                                                                 String publishType) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		params.put("serviceId", serviceId);
		params.put("backendService", backendService);
		params.put("publishType", publishType);
		List<EnvoyServiceProxyInfo> envoyServiceProxyInfos = envoyServiceProxyDao.getRecordsByField(params);
		return CollectionUtils.isEmpty(envoyServiceProxyInfos) ? null : envoyServiceProxyInfos.get(0);
	}

	@Override
	public List<EnvoyServiceProxyInfo> getServiceProxyByServiceId(long serviceId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("serviceId", serviceId);
		return envoyServiceProxyDao.getRecordsByField(params);
	}

	@Override
	public EnvoyServiceProxyDto fromMeta(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		EnvoyServiceProxyDto envoyServiceProxyDto = BeanUtil.copy(envoyServiceProxyInfo, EnvoyServiceProxyDto.class);
		GatewayInfo gatewayInfo = gatewayInfoService.get(envoyServiceProxyInfo.getGwId());
		ServiceInfo serviceInfo = serviceInfoService.getServiceByServiceId(envoyServiceProxyInfo.getServiceId());
		;
		envoyServiceProxyDto.setServiceName(serviceInfo.getDisplayName());
		envoyServiceProxyDto.setServiceTag(serviceInfo.getServiceName());
		envoyServiceProxyDto.setGwName(gatewayInfo.getGwName());
		envoyServiceProxyDto.setGwAddr(gatewayInfo.getGwAddr());
		envoyServiceProxyDto.setServiceType(serviceInfo.getServiceType());

		//增加健康检查
		envoyServiceProxyDto.setHealthyStatus(
			envoyHealthCheckService.getServiceHealthyStatus(serviceInfo, gatewayInfo));
		//增加版本信息
		envoyServiceProxyDto.setSubsets(EnvoyServiceProxyDto.setSubsetForDto(envoyServiceProxyInfo));
		envoyServiceProxyDto.setTrafficPolicy(EnvoyServiceProxyDto.setTrafficPolicyForDto(envoyServiceProxyInfo));
		return envoyServiceProxyDto;
	}

	@Override
	public EnvoyServiceProxyDto fromMetaWithStatus(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		EnvoyServiceProxyDto envoyServiceProxyDto = BeanUtil.copy(envoyServiceProxyInfo, EnvoyServiceProxyDto.class);
		ServiceInfo serviceInfo = serviceInfoService.getServiceByServiceId(envoyServiceProxyInfo.getServiceId());
		GatewayInfo gatewayInfo = gatewayInfoService.get(envoyServiceProxyInfo.getGwId());
		envoyServiceProxyDto.setServiceName(serviceInfo.getDisplayName());
		envoyServiceProxyDto.setServiceTag(serviceInfo.getServiceName());
		envoyServiceProxyDto.setGwName(gatewayInfo.getGwName());
		envoyServiceProxyDto.setGwAddr(gatewayInfo.getGwAddr());
		envoyServiceProxyDto.setServiceType(serviceInfo.getServiceType());
		envoyServiceProxyDto.setSubsets(EnvoyServiceProxyDto.setSubsetForDto(envoyServiceProxyInfo));
		envoyServiceProxyDto.setTrafficPolicy(EnvoyServiceProxyDto.setTrafficPolicyForDto(envoyServiceProxyInfo));
		//调用APIPlane查询当前节点状态
		envoyServiceProxyDto.setHealthyStatus(
			envoyHealthCheckService.getServiceHealthyStatus(serviceInfo, gatewayInfo));
		return envoyServiceProxyDto;
	}

	@Override
	public EnvoyServiceProxyDto fromMetaWithPort(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		EnvoyServiceProxyDto envoyServiceProxyDto = fromMeta(envoyServiceProxyInfo);
		//静态发布，不获取port信息
		if (Const.STATIC_PUBLISH_TYPE.equals(envoyServiceProxyInfo.getPublishType())) {
			return envoyServiceProxyDto;
		}
		List<EnvoyServiceWithPortDto> serviceListFromApiPlane = getFromApiPlaneService.getServiceListFromApiPlane(
			envoyServiceProxyInfo.getGwId(), envoyServiceProxyInfo.getBackendService(),
			envoyServiceProxyInfo.getRegistryCenterType());
		if (CollectionUtils.isEmpty(serviceListFromApiPlane)) {
			logger.info("从api-plane获取服务port信息，api-plane返回为空");
			return envoyServiceProxyDto;
		}
		envoyServiceProxyDto.setPort(serviceListFromApiPlane.get(0).getPorts());

		return envoyServiceProxyDto;
	}

	@Override
	public List<GatewayDto> getPublishedServiceGateway(long serviceId) {
		List<EnvoyServiceProxyInfo> serviceProxies = getServiceProxyByServiceId(serviceId);
		if (CollectionUtils.isEmpty(serviceProxies)) {
			return new ArrayList<>();
		}
		List<Long> gatewayId =
			serviceProxies.stream().map(EnvoyServiceProxyInfo::getGwId).collect(Collectors.toList());
		List<GatewayInfo> gatewayInfos = gatewayId.stream().map(gatewayInfoService::get).collect(Collectors.toList());
		return BeanUtil.copyList(gatewayInfos, GatewayDto.class);
	}

	@Override
	public ErrorCode getRouteRuleNameWithServiceSubset(EnvoyServiceProxyDto envoyServiceProxyDto) {
		long serviceId = envoyServiceProxyDto.getServiceId();
		List<EnvoySubsetDto> envoySubsetDtos = envoyServiceProxyDto.getSubsets();
		if (envoySubsetDtos == null) {
			return CommonErrorCode.Success;
		}
		List<String> subsets = envoySubsetDtos.stream().map(EnvoySubsetDto::getName).collect(Collectors.toList());
		List<EnvoyRouteRuleProxyInfo> envoyRouteRuleInfoList = envoyRouteRuleProxyService.getRouteRuleProxyList(
			serviceId);
		for (EnvoyRouteRuleProxyInfo envoyRouteRuleProxyInfo : envoyRouteRuleInfoList) {
			for (EnvoyDestinationInfo destinationInfo : envoyRouteRuleProxyInfo.getDestinationServiceList()) {
				if (StringUtils.isNotBlank(destinationInfo.getSubsetName()) && !subsets.contains(
					destinationInfo.getSubsetName())) {
					EnvoyRouteRuleInfo envoyRouteRuleInfo = envoyRouteRuleInfoService.getRouteRuleInfoById(
						envoyRouteRuleProxyInfo.getRouteRuleId());
					return CommonErrorCode.SubsetUsedByRouteRule(envoyRouteRuleInfo.getRouteRuleName());
				}
			}
		}
		return CommonErrorCode.Success;
	}

	/**
	 * 当需要将版本信息发送到APIPlane时，采用此方法生成subset，因为subset在DR中的名称和用户输入的不同，需要加上-{gwClusterName}
	 */
	@Override
	public List<EnvoySubsetDto> setSubsetForDtoWhenSendToAPIPlane(EnvoyServiceProxyDto envoyServiceProxyDto,
	                                                              String gwClusterName) {
		List<EnvoySubsetDto> envoySubsetDtoList = generateSubsetDtoName(envoyServiceProxyDto, gwClusterName);
		List<EnvoySubsetDto> envoySubsetDtoListForAPIPlane = new ArrayList<>();
		envoySubsetDtoList.stream().forEach(envoySubsetDto -> {
			//静态地址发布，则增加labels
			if (Const.STATIC_PUBLISH_TYPE.equals(envoyServiceProxyDto.getPublishType())) {
				HashMap labelMap = new HashMap(1);
				labelMap.put(STATIC_EP_VERSION, envoySubsetDto.getName().toLowerCase());
				envoySubsetDto.setLabels(labelMap);
			}

			//todo subset traffic
			EnvoyServiceTrafficPolicyDto envoyServiceTrafficPolicyDto = envoySubsetDto.getTrafficPolicy() == null
			                                                            ? new EnvoyServiceTrafficPolicyDto()
			                                                            : envoySubsetDto.getTrafficPolicy();
			//增加subset健康检查配置
			EnvoyActiveHealthCheckRuleDto envoyActiveHealthCheckRuleDto = envoyHealthCheckService
				                                                              .getActiveHealthCheckRule(
					                                                              envoyServiceProxyDto.getServiceId(),
					                                                              envoyServiceProxyDto.getGwId());
			EnvoyPassiveHealthCheckRuleDto envoyPassiveHealthCheckRuleDto = envoyHealthCheckService
				                                                                .getPassiveHealthCheckRule(
					                                                                envoyServiceProxyDto.getServiceId(),
					                                                                envoyServiceProxyDto.getGwId());
			envoyServiceTrafficPolicyDto.setActiveHealthCheckRule(envoyActiveHealthCheckRuleDto);
			envoyServiceTrafficPolicyDto.setPassiveHealthCheckRule(envoyPassiveHealthCheckRuleDto);
			envoySubsetDtoListForAPIPlane.add(envoySubsetDto);
		});

		return envoySubsetDtoListForAPIPlane;
	}

	@Override
	public List<EnvoyServiceProxyInfo> getServiceProxyListByGwId(long gwId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		return envoyServiceProxyDao.getRecordsByField(params);
	}

	@Override
	public List<EnvoyServiceProxyInfo> batchGetServiceProxyList(long gwId, List<Long> serviceIdList) {
		return envoyServiceProxyDao.batchGetServiceProxyList(gwId, serviceIdList);
	}

	/**
	 * 当需要发送给APIPlane时BackendService，要根据服务注册中心的类型进行调整
	 */
	@Override
	public String getBackendServiceSendToApiPlane(EnvoyServiceProxyDto envoyServiceProxyDto) {
		String backendService = envoyServiceProxyDto.getBackendService();
		return backendService;
	}

	@Override
	public List<String> getSubsetsName(EnvoyServiceProxyInfo serviceProxyInfo) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(serviceProxyInfo.getGwId());
		if (gatewayInfo == null) {
			logger.error("获取subsetsname存在脏数据，gwId:{}", serviceProxyInfo.getGwId());
			return Lists.newArrayList();
		}
		List<String> subsetNames = Lists.newArrayList();
		//默认subset
		subsetNames.add((serviceProxyInfo.getCode() + "-" + gatewayInfo.getGwClusterName()).toLowerCase());
		if (StringUtils.isBlank(serviceProxyInfo.getSubsets())) {
			return subsetNames;
		}

		//用户自定义的版本
		List<EnvoySubsetDto> envoySubsetDtos = generateSubsetDtoName(EnvoyServiceProxyDto.toDto(serviceProxyInfo),
		                                                             gatewayInfo.getGwClusterName());
		List<String> subSets = envoySubsetDtos.stream().map(EnvoySubsetDto::getName).collect(Collectors.toList());
		subsetNames.addAll(subSets);
		return subsetNames;
	}

	/**
	 * 校验服务和版本 负载均衡策略 & 连接池 且 根据Type字段将冗余字段置空不处理
	 */
	public ErrorCode checkEnvoyServiceProxyDto(EnvoyServiceProxyDto envoyServiceProxyDto) {
		EnvoyServiceTrafficPolicyDto envoyServiceTrafficPolicyDto = envoyServiceProxyDto.getTrafficPolicy();
		ErrorCode errorCode = checkTrafficPolicy(envoyServiceTrafficPolicyDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return errorCode;
		}

		return checkSubsetWhenPublishService(envoyServiceProxyDto);
	}

	/**
	 * 校验服务发布时，填写的版本信息
	 */
	public ErrorCode checkSubsetWhenPublishService(EnvoyServiceProxyDto envoyServiceProxyDto) {
		List<EnvoySubsetDto> envoySubsetDtoList = envoyServiceProxyDto.getSubsets();
		ErrorCode errorCode = CommonErrorCode.Success;
		if (envoySubsetDtoList == null || envoySubsetDtoList.size() == 0) {
			return errorCode;
		}

		Set<String> subsetName = new HashSet<>();
		envoySubsetDtoList.stream().forEach(envoySubsetDto -> {
			subsetName.add(envoySubsetDto.getName());
		});
		if (subsetName.size() < envoySubsetDtoList.size()) {
			return CommonErrorCode.DuplicatedSubsetName;
		}

		if (Const.STATIC_PUBLISH_TYPE.equals(envoyServiceProxyDto.getPublishType())) {
			//静态发布的服务，版本中包含的地址要在发布地址列表中
			List<String> addrList = Arrays.asList(envoyServiceProxyDto.getBackendService().split(","));
			for (EnvoySubsetDto envoySubsetDto : envoySubsetDtoList) {
				if (envoySubsetDto.getStaticAddrList() == null || envoySubsetDto.getStaticAddrList().size() == 0
				    || !addrList.containsAll(envoySubsetDto.getStaticAddrList())) {
					return CommonErrorCode.InvalidSubsetStaticAddr;
				}
				Set<String> addrSet = new HashSet<>();
				addrSet.addAll(envoySubsetDto.getStaticAddrList());
				if (addrSet.size() < envoySubsetDto.getStaticAddrList().size()) {
					//同一个版本里配置的静态地址不能重复
					return CommonErrorCode.DuplicatedSubsetStaticAddr;
				}
			}

			//每个地址仅能出现在0或1个版本中
			for (String addr : addrList) {
				int count = 0;
				for (EnvoySubsetDto envoySubsetDto : envoySubsetDtoList) {
					if (envoySubsetDto.getStaticAddrList().contains(addr)) {
						count++;
					}
				}
				if (count > 1) {
					return CommonErrorCode.DuplicatedStaticAddr;
				}
			}
		}

		for (EnvoySubsetDto envoySubsetDto : envoySubsetDtoList) {
			errorCode = checkTrafficPolicy(envoySubsetDto.getTrafficPolicy());
			if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
				return errorCode;
			}
		}

		return errorCode;
	}

	/**
	 * 获取所有版本，用于服务删除
	 */
	EnvoyPublishServiceDto deleteAllSusbet(EnvoyServiceProxyDto envoyServiceProxyDto) {
		GatewayInfo gatewayById = gatewayInfoService.get(envoyServiceProxyDto.getGwId());
		if (gatewayById == null) {
			return null;
		}
		ServiceInfo serviceInfo = serviceInfoService.getServiceByServiceId(envoyServiceProxyDto.getServiceId());
		if (serviceInfo == null) {
			return null;
		}
		EnvoyPublishServiceDto envoyPublishServiceDto = new EnvoyPublishServiceDto();
		envoyPublishServiceDto.setCode(envoyServiceProxyDto.getCode());
		//网关集群名称
		envoyPublishServiceDto.setGateway(gatewayById.getGwClusterName());
		envoyPublishServiceDto.setBackendService(getBackendServiceSendToApiPlane(envoyServiceProxyDto));
		envoyPublishServiceDto.setType(envoyServiceProxyDto.getPublishType());
		envoyPublishServiceDto.setServiceTag(serviceInfo.getServiceName());
		envoyPublishServiceDto.setProtocol(envoyServiceProxyDto.getPublishProtocol());

		//下线服务时同时下线服务下所有的subset，包含默认生成的subset
		List<EnvoySubsetDto> subsetDtoList = new ArrayList<>();
		EnvoySubsetDto envoySubsetDto = new EnvoySubsetDto();
		envoySubsetDto.setName((envoyServiceProxyDto.getCode() + "-" + gatewayById.getGwClusterName()).toLowerCase());
		subsetDtoList.add(envoySubsetDto);
		if (envoyServiceProxyDto.getSubsets() != null) {
			subsetDtoList.addAll(
				setSubsetForDtoWhenSendToAPIPlane(envoyServiceProxyDto, gatewayById.getGwClusterName()));
		}
		envoyPublishServiceDto.setSubsets(subsetDtoList);
		//网关集群名称
		envoyPublishServiceDto.setGateway(gatewayById.getGwClusterName());
		return envoyPublishServiceDto;
	}

	//    /**
	//     * 为dto增加版本信息，因为db中存储的是字符串，dto中是list，不能直接用BeanUtil.copy来赋值
	//     * 用于前端展示
	//     *
	//     * @param envoyServiceProxyInfo
	//     * @return
	//     */
	//    @Override
	//    public List<EnvoySubsetDto> setSubsetForDto(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
	//        //增加版本信息
	//        if (StringUtils.isNotBlank(envoyServiceProxyInfo.getSubsets())) {
	//            List<JSONObject> subsetObject = JSON.parseObject(envoyServiceProxyInfo.getSubsets(), List.class);
	//            List<EnvoySubsetDto> subsets = new ArrayList<>();
	//            for (JSONObject subsetTemp : subsetObject) {
	//                subsets.add(JSONObject.toJavaObject(subsetTemp, EnvoySubsetDto.class));
	//            }
	//            return subsets;
	//        }
	//        return null;
	//    }

	/**
	 * 用于仅删除服务版本，服务更新时
	 */
	EnvoyPublishServiceDto deleteSomeSubset(EnvoyServiceProxyDto envoyServiceProxyDto, String gwClusterName) {
		GatewayInfo gatewayById = gatewayInfoService.get(envoyServiceProxyDto.getGwId());
		if (gatewayById == null) {
			return null;
		}
		ServiceInfo serviceInfo = serviceInfoService.getServiceByServiceId(envoyServiceProxyDto.getServiceId());
		if (serviceInfo == null) {
			return null;
		}
		EnvoyPublishServiceDto envoyPublishServiceDto = new EnvoyPublishServiceDto();
		envoyPublishServiceDto.setCode(envoyServiceProxyDto.getCode());
		//网关集群名称
		envoyPublishServiceDto.setGateway(gatewayById.getGwClusterName());
		envoyPublishServiceDto.setBackendService(getBackendServiceSendToApiPlane(envoyServiceProxyDto));
		envoyPublishServiceDto.setType(envoyServiceProxyDto.getPublishType());
		envoyPublishServiceDto.setServiceTag(serviceInfo.getServiceName());
		envoyPublishServiceDto.setProtocol(envoyServiceProxyDto.getPublishProtocol());
		envoyPublishServiceDto.setSubsets(envoyServiceProxyDto.getSubsets());
		//网关集群名称
		envoyPublishServiceDto.setGateway(gatewayById.getGwClusterName());
		return envoyPublishServiceDto;
	}

	/**
	 * 服务下线时，需要级联删除的记录
	 */
	@Transactional(rollbackFor = Exception.class)
	boolean deleteServiceProxyWithTransactional(long gwId, long serviceId, String backendService, String publishType) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		params.put("serviceId", serviceId);
		params.put("backendService", backendService);
		params.put("publishType", publishType);
		List<EnvoyServiceProxyInfo> recordsByField = envoyServiceProxyDao.getRecordsByField(params);
		if (CollectionUtils.isEmpty(recordsByField)) {
			return true;
		}
		EnvoyServiceProxyInfo envoyServiceProxyInfo = recordsByField.get(0);
		envoyServiceProxyDao.delete(envoyServiceProxyInfo);
		//修改服务发布状态为未发布
		if (CollectionUtils.isEmpty(getServiceProxyByServiceId(serviceId))) {
			ServiceInfo serviceInfoInDb = serviceInfoService.getServiceByServiceId(serviceId);
			if (serviceInfoInDb != null) {
				serviceInfoInDb.setStatus(NumberUtils.INTEGER_ZERO);
				serviceInfoService.updateService(serviceInfoInDb);
			}
		}

		//服务下线后，需要关闭健康检查功能
		envoyHealthCheckService.shutdownHealthCheck(serviceId, gwId);
		return true;
	}

	private ErrorCode checkPublishServiceAndGw(long serviceId, long gwId) {
		GatewayInfo gatewayInDb = gatewayInfoService.get(gwId);
		if (gatewayInDb == null) {
			logger.info("发布服务，指定网关不存在，网关id:{}", gwId);
			return CommonErrorCode.NoSuchGateway;
		}
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("serviceId", serviceId);
		params.put("gwId", gwId);
		int countByFields = envoyServiceProxyDao.getCountByFields(params);
		if (countByFields > 0) {
			logger.info("发布服务，服务已发布到当前网关,服务id:{},网关id:{}，不允许再次发布", serviceId, gwId);
			return CommonErrorCode.ServiceAlreadyPublished;
		}
		return CommonErrorCode.Success;
	}

	private ErrorCode checkRegistryCenterInfo(EnvoyServiceProxyDto envoyServiceProxyDto) {
		if (!Const.DYNAMIC_PUBLISH_TYPE.equals(envoyServiceProxyDto.getPublishType())) {
			return CommonErrorCode.Success;
		}

		if (StringUtils.isBlank(envoyServiceProxyDto.getRegistryCenterType()) || RegistryCenterEnum.Kubernetes.getType()
		                                                                                                      .equals(
			                                                                                                      envoyServiceProxyDto
				                                                                                                      .getRegistryCenterType())) {
			//默认Kubernetes
			envoyServiceProxyDto.setRegistryCenterType(RegistryCenterEnum.Kubernetes.getType());
			return CommonErrorCode.Success;
		}
		return CommonErrorCode.Success;
	}

	/**
	 * 校验负载均衡策略 & 连接池 且 根据Type字段将冗余字段置空不处理
	 */
	private ErrorCode checkTrafficPolicy(EnvoyServiceTrafficPolicyDto envoyServiceTrafficPolicyDto) {
		if (envoyServiceTrafficPolicyDto == null) {
			return CommonErrorCode.Success;
		}

		EnvoyServiceLoadBalancerDto envoyServiceLoadBalancerDto = envoyServiceTrafficPolicyDto.getLoadBalancer();
		if (envoyServiceLoadBalancerDto != null) {
			if (SERVICE_LOADBALANCER_SIMPLE.equals(envoyServiceLoadBalancerDto.getType())) {
				//Simple类型，包含ROUND_ROBIN|LEAST_CONN|RANDOM
				final List<String> simpleList = new ArrayList<>();
				simpleList.add(SERVICE_LOADBALANCER_SIMPLE_ROUND_ROBIN);
				simpleList.add(SERVICE_LOADBALANCER_SIMPLE_LEAST_CONN);
				simpleList.add(SERVICE_LOADBALANCER_SIMPLE_RANDOM);
				if (StringUtils.isBlank(envoyServiceLoadBalancerDto.getSimple()) || !simpleList.contains(
					envoyServiceLoadBalancerDto.getSimple())) {
					return CommonErrorCode.InvalidSimpleLoadBanlanceType;
				}
				//将Cookie相关参数置空
				envoyServiceLoadBalancerDto.setConsistentHash(null);
			} else if (SERVICE_LOADBALANCER_HASH.equals(envoyServiceLoadBalancerDto.getType())) {
				//一致性哈希
				EnvoyServiceConsistentHashDto envoyServiceConsistentHashDto = envoyServiceLoadBalancerDto
					                                                              .getConsistentHash();
				if (envoyServiceConsistentHashDto == null) {
					//不能为空
					return CommonErrorCode.InvalidConsistentHashObject;
				}
				final List<String> hashList = new ArrayList<>();
				hashList.add(SERVICE_LOADBALANCER_HASH_HTTPHEADERNAME);
				hashList.add(SERVICE_LOADBALANCER_HASH_HTTPCOOKIE);
				hashList.add(SERVICE_LOADBALANCER_HASH_USESOURCEIP);

				if (StringUtils.isBlank(envoyServiceConsistentHashDto.getType()) || !hashList.contains(
					envoyServiceConsistentHashDto.getType())) {
					return CommonErrorCode.InvalidConsistentHashType;
				}
				if (SERVICE_LOADBALANCER_HASH_HTTPCOOKIE.equals(envoyServiceConsistentHashDto.getType())) {
					EnvoyServiceConsistentHashDto.EnvoyServiceConsistentHashCookieDto
						envoyServiceConsistentHashCookieDto = envoyServiceConsistentHashDto.getCookieDto();
					if (envoyServiceConsistentHashCookieDto == null) {
						//cookie不能为空
						return CommonErrorCode.InvalidConsistentHashHttpCookieObject;
					}
					String name = envoyServiceConsistentHashCookieDto.getName();
					if (StringUtils.isBlank(name) || name.length() > 255) {
						return CommonErrorCode.InvalidConsistentHashHttpCookieName;
					}
					Integer ttl = envoyServiceConsistentHashCookieDto.getTtl();
					if (ttl == null || ttl < 0) {
						return CommonErrorCode.InvalidConsistentHashHttpCookieTtl;
					}
					envoyServiceConsistentHashDto.setHttpHeaderName(null);
					envoyServiceConsistentHashDto.setUseSourceIp(null);
				}

				if (SERVICE_LOADBALANCER_HASH_HTTPHEADERNAME.equals(envoyServiceConsistentHashDto.getType())) {
					if (StringUtils.isBlank(envoyServiceConsistentHashDto.getHttpHeaderName())) {
						return CommonErrorCode.InvalidConsistentHashHttpHeaderName;
					}
					envoyServiceConsistentHashDto.setCookieDto(null);
					envoyServiceConsistentHashDto.setUseSourceIp(null);
				}

				if (SERVICE_LOADBALANCER_HASH_USESOURCEIP.equals(envoyServiceConsistentHashDto.getType())) {
					if (envoyServiceConsistentHashDto.getUseSourceIp() == null) {
						return CommonErrorCode.InvalidConsistentHashSourceIP;
					}
					envoyServiceConsistentHashDto.setHttpHeaderName(null);
					envoyServiceConsistentHashDto.setCookieDto(null);
				}
				//将simple类型相关参数置空
				envoyServiceLoadBalancerDto.setSimple(null);
			} else {
				//type不合法
				return CommonErrorCode.InvalidLoadBanlanceType;
			}
		}
		EnvoyServiceConnectionPoolDto envoyServiceConnectionPoolDto = envoyServiceTrafficPolicyDto
			                                                              .getConnectionPoolDto();
		if (envoyServiceConnectionPoolDto != null) {
			EnvoyServiceConnectionPoolDto.EnvoyServiceHttpConnectionPoolDto envoyServiceHttpConnectionPoolDto
				= envoyServiceConnectionPoolDto.getServiceHttpConnectionPoolDto();
			EnvoyServiceConnectionPoolDto.EnvoyServiceTcpConnectionPoolDto envoyServiceTcpConnectionPoolDto
				= envoyServiceConnectionPoolDto.getServiceTcpConnectionPoolDto();
			if (envoyServiceHttpConnectionPoolDto != null) {
				Integer http1MaxPendingRequests = envoyServiceHttpConnectionPoolDto.getHttp1MaxPendingRequests();
				Integer http2MaxRequests = envoyServiceHttpConnectionPoolDto.getHttp2MaxRequests();
				Integer idleTimeout = envoyServiceHttpConnectionPoolDto.getIdleTimeout();
				Integer maxRequestsPerConnection = envoyServiceHttpConnectionPoolDto.getMaxRequestsPerConnection();
				if (http1MaxPendingRequests < 0) {
					return CommonErrorCode.InvalidHttp1MaxPendingRequests;
				}
				if (http2MaxRequests < 0) {
					return CommonErrorCode.InvalidHttp2MaxRequests;
				}
				if (idleTimeout < 0) {
					return CommonErrorCode.InvalidIdleTimeout;
				}
				if (maxRequestsPerConnection < 0) {
					return CommonErrorCode.InvalidMaxRequestsPerConnection;
				}
			}
			if (envoyServiceTcpConnectionPoolDto != null) {
				Integer maxConnections = envoyServiceTcpConnectionPoolDto.getMaxConnections();
				Integer connectTimeout = envoyServiceTcpConnectionPoolDto.getConnectTimeout();
				if (maxConnections < 0) {
					return CommonErrorCode.InvalidMaxConnections;
				}
				if (connectTimeout < 0) {
					return CommonErrorCode.InvalidConnectTimeout;
				}
			}
		}
		return CommonErrorCode.Success;
	}

	private List<EnvoySubsetDto> generateSubsetDtoName(EnvoyServiceProxyDto envoyServiceProxyDto,
	                                                   String gwClusterName) {
		if (envoyServiceProxyDto.getSubsets() == null) {
			return Lists.newArrayList();
		}
		String subsetsString = JSON.toJSONString(envoyServiceProxyDto.getSubsets());
		List<EnvoySubsetDto> envoySubsetDtoList = JSON.parseArray(subsetsString, EnvoySubsetDto.class);
		envoySubsetDtoList.stream().map(item -> {
			item.setName((item.getName() + "-" + gwClusterName).toLowerCase());
			return item;
		}).collect(Collectors.toList());
		return envoySubsetDtoList;
	}

	@Override
	public List<PublishedDetailDto> getPublishedDetailByService(long serviceId) {
		List<EnvoyServiceProxyInfo> envoyServiceProxy = getServiceProxyByServiceId(serviceId);
		List<PublishedDetailDto> publishedDetailDtos = new ArrayList<>();
		if (!org.springframework.util.CollectionUtils.isEmpty(envoyServiceProxy)) {
			envoyServiceProxy.forEach(envoyServiceProxyInfo -> {
				PublishedDetailDto publishedDetailDto = new PublishedDetailDto();
				EnvoyServiceProxyDto envoyServiceProxyDto = fromMeta(envoyServiceProxyInfo);
				publishedDetailDto.setGwName(envoyServiceProxyDto.getGwName());
				publishedDetailDto.setGwId(envoyServiceProxyDto.getGwId());
				publishedDetailDto.setHealthyStatus(envoyServiceProxyDto.getHealthyStatus());
				publishedDetailDto.setGwType(gatewayInfoService.get(envoyServiceProxyDto.getGwId()).getGwType());
				publishedDetailDto.setServiceAddr(envoyServiceProxyDto.getBackendService().split(","));
				publishedDetailDto.setRegistryCenterAddr(envoyServiceProxyInfo.getRegistryCenterAddr());
				publishedDetailDto.setRegistryCenterType(envoyServiceProxyInfo.getRegistryCenterType());
				publishedDetailDtos.add(publishedDetailDto);
			});
		}
		return publishedDetailDtos;
	}

}
