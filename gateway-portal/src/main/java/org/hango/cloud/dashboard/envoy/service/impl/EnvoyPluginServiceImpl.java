package org.hango.cloud.ncegdashboard.envoy.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hango.cloud.ncegdashboard.envoy.config.ApiServerConfig;
import org.hango.cloud.ncegdashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.common.HttpClientResponse;
import org.hango.cloud.ncegdashboard.envoy.meta.ServiceInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayInfoService;
import org.hango.cloud.ncegdashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.web.holder.ProjectTraceHolder;
import org.hango.cloud.ncegdashboard.envoy.dao.IEnvoyPluginBindingInfoDao;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyPluginBindingInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyPluginInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyPluginTemplateInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyRouteRuleInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyRouteRuleProxyInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyVirtualHostInfo;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyGatewayService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyPluginInfoService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyPluginTemplateService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyRouteRuleInfoService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyRouteRuleProxyService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyServiceProxyService;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyPluginBindingDto;
import org.hango.cloud.ncegdashboard.envoy.web.util.HttpCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Envoy插件Service层实现类
 * <p>
 * 2019-10-23
 */
@Service
public class EnvoyPluginServiceImpl implements IEnvoyPluginInfoService {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyPluginServiceImpl.class);

	@Autowired
	private IGatewayInfoService gatewayInfoService;

	@Autowired
	private IServiceInfoService serviceInfoService;

	@Autowired
	private IEnvoyServiceProxyService envoyServiceProxyService;

	@Autowired
	private IEnvoyRouteRuleInfoService envoyRouteRuleInfoService;

	@Autowired
	private IEnvoyPluginBindingInfoDao envoyPluginBindingInfoDao;

	@Autowired
	private IEnvoyRouteRuleProxyService envoyRouteRuleProxyService;

	@Autowired
	private IEnvoyGatewayService envoyGatewayService;

	@Autowired
	private IEnvoyPluginTemplateService envoyPluginTemplateService;

	@Autowired
	private ApiServerConfig apiServerConfig;

	@Override
	public ErrorCode checkDescribePlugin(long gwId) {
		if (0 < gwId) {
			GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
			if (null == gatewayInfo) {
				logger.info("查询插件信息时指定的网关不存在! gwId:{}", gwId);
				return CommonErrorCode.NoSuchGateway;
			}
		}
		return CommonErrorCode.Success;
	}

	@Override
	public List<EnvoyPluginInfo> getPluginInfoListFromApiPlane(long gwId) {
		if (0 < gwId) {
			GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
			if (null == gatewayInfo) {
				logger.error("gwId对应的网关信息不存在! gwId:{}", gwId);
				return Lists.newArrayList();
			}
			return getEnvoyPluginInfos(gatewayInfo);
		}
		// 如果不传入网关id，将所有网关的插件列表合并
		List<GatewayInfo> gatewayInfos = gatewayInfoService.findAll();
		if (CollectionUtils.isEmpty(gatewayInfos)) {
			return Lists.newArrayList();
		}
		Set<EnvoyPluginInfo> envoyPluginInfoSet = Sets.newHashSet();
		for (GatewayInfo gatewayInfo : gatewayInfos) {
			if (Const.ENVOY_GATEWAY_TYPE.equals(gatewayInfo.getGwType())) {
				envoyPluginInfoSet.addAll(getEnvoyPluginInfos(gatewayInfo));
			}
		}
		return Lists.newArrayList(envoyPluginInfoSet);
	}

	@Override
	public EnvoyPluginInfo getPluginInfoFromApiPlane(long gwId, String pluginType) {
		if (0 < gwId) {
			GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
			if (null == gatewayInfo) {
				logger.error("gwId对应的网关信息不存在! gwId:{}", gwId);
				return null;
			}
			return getEnvoyPluginInfo(gatewayInfo, pluginType);
		}
		// 如果不传网关id，只要任意一个网关查询到即可
		List<GatewayInfo> gatewayInfos = gatewayInfoService.findAll();
		for (GatewayInfo gatewayInfo : gatewayInfos) {
			if (Const.ENVOY_GATEWAY_TYPE.equals(gatewayInfo.getGwType())) {
				EnvoyPluginInfo envoyPluginInfo = getEnvoyPluginInfo(gatewayInfo, pluginType);
				if (null != envoyPluginInfo) {
					return envoyPluginInfo;
				}
			}
		}
		return null;
	}

	@Override
	public ErrorCode checkBindingPlugin(long gwId, String bindingObjectId, String bindingObjectType, String pluginType,
	                                    String pluginConfiguration, long projectId, long templateId) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		if (null == gatewayInfo) {
			logger.info("绑定插件时指定的网关id不存在！ gwId：{}", gwId);
			return CommonErrorCode.NoSuchGateway;
		}
		if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE.equals(bindingObjectType)) {
			EnvoyRouteRuleProxyInfo routeRuleProxyInfo = envoyRouteRuleProxyService.getRouteRuleProxy(gwId, Integer
				                                                                                                .valueOf(
					                                                                                                bindingObjectId));
			if (null == routeRuleProxyInfo) {
				logger.info("路由规则尚未发布到指定网关，不允许绑定插件! gwId:{}, routeRuleId:{}", gwId, bindingObjectId);
				return CommonErrorCode.RouteRuleNotPublished;
			}
		} else if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_GLOBAL.equals(bindingObjectType)) {
			bindingObjectId = String.valueOf(projectId);
			// TODO 若后续改为项目级，则需要增加校验网关与项目的关联关系
			EnvoyVirtualHostInfo virtualHostInfo = envoyGatewayService.getVirtualHostByGwIdAndProjectId(gwId,
			                                                                                            Long.valueOf(
				                                                                                            bindingObjectId));
			if (null == virtualHostInfo) {
				logger.info("绑定全局插件时指定的virtual host不存在! gwId:{}, projectId:{}", gwId, Long.valueOf(bindingObjectId));
				return CommonErrorCode.ProjectNotAssociatedGateway;
			}
		} else {
			EnvoyServiceProxyInfo serviceProxyInfo = envoyServiceProxyService.getServiceProxyByServiceIdAndGwId(gwId,
			                                                                                                    Integer
				                                                                                                    .valueOf(
					                                                                                                    bindingObjectId));
			if (null == serviceProxyInfo) {
				logger.info("服务尚未发布到指定网关，不允许绑定插件！ gwId:{}, serviceId:{}", gwId, bindingObjectId);
			}
		}

		if (StringUtils.isBlank(pluginType)) {
			logger.info("绑定插件时参数 pluginType 为空!");
			return CommonErrorCode.MissingParameter("PluginType");
		}

		EnvoyPluginBindingInfo bindingInfoList = getBindingInfo(gwId, bindingObjectId, bindingObjectType, pluginType);
		if (null != bindingInfoList) {
			logger.info("已绑定该插件，不允许重复绑定");
			return CommonErrorCode.CannotDuplicateBinding;
		}

		if (0 < templateId) {
			EnvoyPluginTemplateInfo templateInfo = envoyPluginTemplateService.getTemplateById(templateId);
			if (null == templateInfo) {
				logger.info("指定插件模板不存在! templateId:{}", templateId);
				return CommonErrorCode.NoSuchPluginTemplate;
			}
			if (!pluginType.equals(templateInfo.getPluginType())) {
				logger.info("插件模板与插件类型不匹配! pluginType:{}, templatePluginType:{}", pluginType,
				            templateInfo.getPluginType());
				return CommonErrorCode.NoSuchPluginTemplate;
			}
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean bindingPlugin(long gwId, String bindingObjectId, String bindingObjectType, String pluginType,
	                             String pluginConfiguration, long projectId, long templateId) {
		if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_GLOBAL.equals(bindingObjectType)) {
			bindingObjectId = String.valueOf(projectId);
		}
		EnvoyPluginInfo pluginInfo = getPluginInfoFromApiPlane(gwId, pluginType);
		if (null == pluginInfo) {
			return false;
		}
		EnvoyPluginTemplateInfo templateInfo = null;
		if (0 < templateId) {
			templateInfo = envoyPluginTemplateService.getTemplateById(templateId);
			if (null == templateInfo) {
				return false;
			}
			pluginConfiguration = templateInfo.getPluginConfiguration();
		}

		boolean bindingResult = bindingPluginToApiPlane(gwId, bindingObjectId, bindingObjectType, pluginConfiguration,
		                                                pluginType);
		if (!bindingResult) {
			return false;
		}
		EnvoyPluginBindingInfo bindingInfo = new EnvoyPluginBindingInfo();
		bindingInfo.setProjectId(ProjectTraceHolder.getProId());
		bindingInfo.setGwId(gwId);
		bindingInfo.setUpdateTime(System.currentTimeMillis());
		bindingInfo.setCreateTime(System.currentTimeMillis());
		bindingInfo.setPluginConfiguration(pluginConfiguration);
		if (0 < templateId) {
			bindingInfo.setTemplateId(templateId);
			bindingInfo.setTemplateVersion(templateInfo.getTemplateVersion());
		}
		bindingInfo.setBindingObjectType(bindingObjectType);
		bindingInfo.setBindingObjectId(bindingObjectId);
		bindingInfo.setPluginType(pluginType);
		bindingInfo.setPluginPriority(pluginInfo.getPluginPriority());
		bindingInfo.setBindingStatus(EnvoyPluginBindingInfo.BINDING_STATUS_ENABLE);
		long bindingInfoId = envoyPluginBindingInfoDao.add(bindingInfo);
		return true;
	}

	@Override
	public long bindingPluginToDb(EnvoyPluginBindingInfo bindingInfo) {
		return envoyPluginBindingInfoDao.add(bindingInfo);
	}

	@Override
	public long deletePluginFromDb(EnvoyPluginBindingInfo bindingInfo) {
		return envoyPluginBindingInfoDao.delete(bindingInfo);
	}

	@Override
	public List<EnvoyPluginBindingInfo> getEnablePluginBindingList(long gwId, String bindingObjectId,
	                                                               String bindingObjectType) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		params.put("bindingObjectId", bindingObjectId);
		params.put("bindingObjectType", bindingObjectType);
		params.put("bindingStatus", EnvoyPluginBindingInfo.BINDING_STATUS_ENABLE);
		List<EnvoyPluginBindingInfo> pluginBindingInfoList = envoyPluginBindingInfoDao.getRecordsByField(params);
		return CollectionUtils.isEmpty(pluginBindingInfoList) ? Lists.newArrayList() : pluginBindingInfoList;
	}

	@Override
	public List<EnvoyPluginBindingInfo> getPluginBindingList(long gwId, String bindingObjectId,
	                                                         String bindingObjectType) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		params.put("bindingObjectId", bindingObjectId);
		params.put("bindingObjectType", bindingObjectType);
		List<EnvoyPluginBindingInfo> pluginBindingInfoList = envoyPluginBindingInfoDao.getRecordsByField(params);
		return CollectionUtils.isEmpty(pluginBindingInfoList) ? Lists.newArrayList() : pluginBindingInfoList;
	}

	@Override
	public boolean unbindingPlugin(long pluginBindingInfoId) {
		EnvoyPluginBindingInfo bindingInfo = getPluginBindingInfo(pluginBindingInfoId);
		if (null == bindingInfo) {
			logger.info("解绑时指定的绑定不存在，不继续解绑, pluginBindingInfoId:{}", pluginBindingInfoId);
			return true;
		}

		if (!unbindingPluginToApiPlane(bindingInfo)) {
			return false;
		}

		envoyPluginBindingInfoDao.delete(bindingInfo);
		return true;
	}

	@Override
	public EnvoyPluginBindingInfo getPluginBindingInfo(long pluginBindingInfoId) {
		return envoyPluginBindingInfoDao.get(pluginBindingInfoId);
	}

	@Override
	public ErrorCode checkUpdatePluginConfiguration(long pluginBindingInfoId, String pluginConfiguration,
	                                                long templateId) {
		if (0 < templateId) {
			EnvoyPluginTemplateInfo templateInfo = envoyPluginTemplateService.getTemplateById(templateId);
			if (null == templateInfo) {
				logger.info("更新插件配置时，指定的模板不存在! templateId:{}", templateId);
				return CommonErrorCode.NoSuchPluginTemplate;
			}
			pluginConfiguration = templateInfo.getPluginConfiguration();
		}
		if (StringUtils.isBlank(pluginConfiguration)) {
			logger.info("更新插件配置时，参数PluginConfiguration缺失!");
			return CommonErrorCode.MissingParameter("PluginConfiguration");
		}
		EnvoyPluginBindingInfo pluginBindingInfo = getPluginBindingInfo(pluginBindingInfoId);
		if (null == pluginBindingInfo) {
			logger.info("更新插件配置时，指定的绑定关系不存在! pluginBindingInfoId:{}", pluginBindingInfoId);
			return CommonErrorCode.NoSuchPluginBinding;
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean updatePluginConfiguration(long pluginBindingInfoId, String pluginConfiguration, long templateId) {
		EnvoyPluginBindingInfo bindingInfo = getPluginBindingInfo(pluginBindingInfoId);
		if (null == bindingInfo) {
			logger.error("更新插件配置时指定的绑定关系不存在! pluginBindingInfoId:{}", pluginBindingInfoId);
			return false;
		}
		EnvoyPluginTemplateInfo templateInfo = null;
		if (0 < templateId) {
			templateInfo = envoyPluginTemplateService.getTemplateById(templateId);
			pluginConfiguration = templateInfo.getPluginConfiguration();
		}

		if (!updateRouteIfEnable(pluginBindingInfoId, pluginConfiguration, bindingInfo)) {
			return false;
		}

		bindingInfo.setUpdateTime(System.currentTimeMillis());
		bindingInfo.setPluginConfiguration(pluginConfiguration);
		if (0 >= templateId) {
			bindingInfo.setTemplateId(0);
			bindingInfo.setTemplateVersion(0);
		} else {
			bindingInfo.setTemplateId(templateId);
			bindingInfo.setTemplateVersion(templateInfo.getTemplateVersion());
		}
		envoyPluginBindingInfoDao.update(bindingInfo);
		return true;
	}

	@Override
	public boolean updatePluginConfiguration(long pluginBindingInfoId, String pluginConfiguration, long templateId,
	                                         long pluginTemplateVersion) {
		try {
			updatePluginConfiguration(pluginBindingInfoId, pluginConfiguration, templateId);
			EnvoyPluginBindingInfo bindingInfo = getPluginBindingInfo(pluginBindingInfoId);
			bindingInfo.setTemplateVersion(pluginTemplateVersion);
			bindingInfo.setUpdateTime(System.currentTimeMillis());
			envoyPluginBindingInfoDao.update(bindingInfo);
			return true;
		} catch (Exception e) {
			logger.info("同步模板配置到插件失败!", e);
			return false;
		}
	}

	@Override
	public long getBindingPluginCount(long gwId, long projectId, String bindingObjectId,
	                                  List<String> bindingObjectTypeList, String pattern) {
		List<Long> gwIdList = getGwIdList(gwId, projectId, pattern);
		List<String> bindingObjectIdList = getBindingObjectIdList(projectId, bindingObjectId,
		                                                          Sets.newHashSet(bindingObjectTypeList), pattern);

		return envoyPluginBindingInfoDao.getBindingPluginCount(projectId, gwId, gwIdList, bindingObjectId,
		                                                       bindingObjectIdList, bindingObjectTypeList, pattern);
	}

	@Override
	public List<EnvoyPluginBindingInfo> getBindingPluginList(long gwId, long projectId, String bindingObjectId,
	                                                         List<String> bindingObjectTypeList, String pattern,
	                                                         long offset, long limit, String sortKey,
	                                                         String sortValue) {
		List<Long> gwIdList = getGwIdList(gwId, projectId, pattern);
		List<String> bindingObjectIdList = getBindingObjectIdList(projectId, bindingObjectId,
		                                                          Sets.newHashSet(bindingObjectTypeList), pattern);

		List<EnvoyPluginBindingInfo> pluginBindingInfoList = envoyPluginBindingInfoDao.getBindingPluginList(projectId,
		                                                                                                    gwId,
		                                                                                                    gwIdList,
		                                                                                                    bindingObjectId,
		                                                                                                    bindingObjectIdList,
		                                                                                                    bindingObjectTypeList,
		                                                                                                    pattern,
		                                                                                                    offset,
		                                                                                                    limit,
		                                                                                                    sortKey,
		                                                                                                    sortValue);
		return CollectionUtils.isEmpty(pluginBindingInfoList) ? Lists.newArrayList() : pluginBindingInfoList;
	}

	@Override
	public long deletePluginList(long gwId, String bindingObjectId, String bindingObjectType) {
		return envoyPluginBindingInfoDao.batchDeleteBindingInfo(
			getPluginBindingList(gwId, bindingObjectId, bindingObjectType));
	}

	@Override
	public ErrorCode checkUpdatePluginBindingStatus(long pluginBindingInfoId, String bindingStatus) {
		EnvoyPluginBindingInfo bindingInfo = getPluginBindingInfo(pluginBindingInfoId);
		if (null == bindingInfo) {
			logger.error("修改插件绑定关系状态时，指定插件绑定关系不存在! pluginBindinginfoId:{}", pluginBindingInfoId);
			return CommonErrorCode.NoSuchPluginBinding;
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean updatePluginBindingStatus(long pluginBindingInfoId, String bindingStatus) {
		EnvoyPluginBindingInfo pluginBindingInfo = getPluginBindingInfo(pluginBindingInfoId);
		if (null == pluginBindingInfo) {
			logger.error("修改插件绑定关系状态时，指定插件绑定关系不存在! pluginBindingInfoId:{}", pluginBindingInfoId);
			return false;
		}

		if (EnvoyPluginBindingInfo.BINDING_STATUS_DISABLE.equals(bindingStatus.trim().toLowerCase())) {
			if (!unbindingPluginToApiPlane(pluginBindingInfo)) {
				return false;
			}
			pluginBindingInfo.setBindingStatus(EnvoyPluginBindingInfo.BINDING_STATUS_DISABLE);
			pluginBindingInfo.setUpdateTime(System.currentTimeMillis());
			envoyPluginBindingInfoDao.update(pluginBindingInfo);
			return true;
		} else {
			if (!bindingPluginToApiPlane(pluginBindingInfo.getGwId(), pluginBindingInfo.getBindingObjectId(),
			                             pluginBindingInfo.getBindingObjectType(),
			                             pluginBindingInfo.getPluginConfiguration(),
			                             pluginBindingInfo.getPluginType())) {
				return false;
			}
			pluginBindingInfo.setBindingStatus(EnvoyPluginBindingInfo.BINDING_STATUS_ENABLE);
			pluginBindingInfo.setUpdateTime(System.currentTimeMillis());
			envoyPluginBindingInfoDao.update(pluginBindingInfo);
			return true;
		}
	}

	@Override
	public void fillDtoFiled(List<EnvoyPluginBindingDto> envoyPluginBindingDtoList) {
		if (CollectionUtils.isEmpty(envoyPluginBindingDtoList)) {
			return;
		}

		Set<Long> templateIdSet = envoyPluginBindingDtoList.stream().map(EnvoyPluginBindingDto::getTemplateId).collect(
			Collectors.toSet());
		Set<Long> gwIdSet = envoyPluginBindingDtoList.stream().map(EnvoyPluginBindingDto::getGwId).collect(
			Collectors.toSet());
		Set<Long> serviceIdSet = envoyPluginBindingDtoList.stream().filter(
			item -> EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_SERVICE.equals(item.getBindingObjectType())).map(
			item -> Long.valueOf(item.getBindingObjectId())).collect(Collectors.toSet());
		Set<Long> routeRuleIdSet = envoyPluginBindingDtoList.stream().filter(
			item -> EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE.equals(item.getBindingObjectType())).map(
			item -> Long.valueOf(item.getBindingObjectId())).collect(Collectors.toSet());

		Map<Long, GatewayInfo> gatewayInfoMap = gatewayInfoService.getGatewayInfoList(Lists.newArrayList(gwIdSet))
		                                                          .stream().collect(
				Collectors.toMap(GatewayInfo::getId, item -> item));
		Map<Long, ServiceInfo> serviceInfoMap = serviceInfoService.getServiceInfoList(Lists.newArrayList(serviceIdSet))
		                                                          .stream().collect(
				Collectors.toMap(ServiceInfo::getId, item -> item));
		Map<Long, EnvoyRouteRuleInfo> routeRuleInfoMap = envoyRouteRuleInfoService.getRouteRuleList(
			Lists.newArrayList(routeRuleIdSet)).stream().collect(
			Collectors.toMap(EnvoyRouteRuleInfo::getId, item -> item));
		Map<Long, EnvoyPluginTemplateInfo> templateInfoMap = envoyPluginTemplateService.batchGet(
			Lists.newArrayList(templateIdSet)).stream().collect(
			Collectors.toMap(EnvoyPluginTemplateInfo::getId, item -> item));

		envoyPluginBindingDtoList.forEach(item -> {
			GatewayInfo gatewayInfo = gatewayInfoMap.get(item.getGwId());
			item.setGwName(null == gatewayInfo ? StringUtils.EMPTY : gatewayInfo.getGwName());
			if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE.equals(item.getBindingObjectType())) {
				EnvoyRouteRuleInfo routeRuleInfo = routeRuleInfoMap.get(Long.valueOf(item.getBindingObjectId()));
				item.setBindingObjectName(null == routeRuleInfo ? StringUtils.EMPTY :
				                          routeRuleInfo.getRouteRuleName());
			} else if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_GLOBAL.equals(item.getBindingObjectType())) {
				item.setBindingObjectName(null == gatewayInfo ? StringUtils.EMPTY : gatewayInfo.getGwName());
			} else {
				ServiceInfo serviceInfo = serviceInfoMap.get(Long.valueOf(item.getBindingObjectId()));
				item.setBindingObjectName(null == serviceInfo ? StringUtils.EMPTY : serviceInfo.getDisplayName());
			}
			EnvoyPluginTemplateInfo templateInfo = templateInfoMap.get(item.getTemplateId());
			if (0 == item.getTemplateId() || null == templateInfo || item.getTemplateVersion() == templateInfo
				                                                                                      .getTemplateVersion()) {
				item.setTemplateStatus(EnvoyPluginTemplateInfo.STATUS_NO_NEED_SYNC);
			} else {
				item.setTemplateStatus(EnvoyPluginTemplateInfo.STATUS_NEED_SYNC);
			}
		});
	}

	@Override
	public List<EnvoyPluginBindingInfo> getBindingListByTemplateId(long templateId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("templateId", templateId);
		return envoyPluginBindingInfoDao.getRecordsByField(params);
	}

	@Override
	public List<EnvoyPluginBindingInfo> getBindingListByTemplateId(long templateId, long gwId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("templateId", templateId);
		params.put("gwId", gwId);
		return envoyPluginBindingInfoDao.getRecordsByField(params);
	}

	@Override
	public boolean batchDissociateTemplate(List<Long> bindingInfoList) {
		envoyPluginBindingInfoDao.batchDissociateTemplate(bindingInfoList);
		return true;
	}

	@Override
	public List<EnvoyPluginBindingInfo> batchGetById(List<Long> bindingInfoIdList) {
		return envoyPluginBindingInfoDao.batchGetById(bindingInfoIdList);
	}

	public HttpClientResponse publishGlobalPluginToApiPlane(String apiPlaneUrl, Map<String, String> params,
	                                                        String body,
	                                                        Map<String, String> headers, String methodType) {
		HttpClientResponse response = HttpCommonUtil.getFromApiPlane(apiPlaneUrl, params, body, headers, methodType);
		return response;
	}

	private List<EnvoyPluginInfo> getEnvoyPluginInfos(GatewayInfo gatewayInfo) {
		Map<String, String> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("Action", "GetPluginList");
		params.put("Version", "2019-07-25");

		HttpClientResponse response = HttpCommonUtil.getFromApiPlane(gatewayInfo.getApiPlaneAddr() + "/api/plugin",
		                                                             params, null, null, HttpMethod.GET.name());
		if (null == response) {
			logger.error("调用api-plane查询插件列表接口响应为空!");
			return Lists.newArrayList();
		}

		if (!HttpCommonUtil.isNormalCode(response.getStatusCode())) {
			logger.error("调用api-plane查询插件列表接口失败，返回http status code非2xx，httpStatusCoed:{}, errMsg:{}",
			             response.getStatusCode(), response.getResponseBody());
			return Lists.newArrayList();
		}

		JSONObject result = JSONObject.parseObject(response.getResponseBody());
		return result.getJSONArray("Plugins").stream().map(item -> {
			JSONObject pluginInfo = JSONObject.parseObject(item.toString());
			EnvoyPluginInfo envoyPluginInfo = new EnvoyPluginInfo();
			envoyPluginInfo.setPluginName(pluginInfo.getString("name"));
			envoyPluginInfo.setAuthor(pluginInfo.getString("author"));
			envoyPluginInfo.setPluginType(pluginInfo.getString("name"));
			envoyPluginInfo.setPluginScope(pluginInfo.getString("pluginScope"));
			envoyPluginInfo.setCreateTime(getLongValueFromJsonWithDefault(pluginInfo, "createTime", 0));
			envoyPluginInfo.setUpdateTime(getLongValueFromJsonWithDefault(pluginInfo, "updateTime", 0));
			envoyPluginInfo.setPluginPriority(getLongValueFromJsonWithDefault(pluginInfo, "pluginPriority", 0));
			envoyPluginInfo.setInstructionForUse(pluginInfo.getString("instructionForUse"));
			return envoyPluginInfo;
		}).collect(Collectors.toList());
	}

	private EnvoyPluginInfo getEnvoyPluginInfo(GatewayInfo gatewayInfo, String pluginType) {
		Map<String, String> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("Action", "GetPluginDetail");
		params.put("Version", "2019-07-25");
		params.put("Name", pluginType);

		HttpClientResponse response = HttpCommonUtil.getFromApiPlane(gatewayInfo.getApiPlaneAddr() + "/api/plugin",
		                                                             params, null, null, HttpMethod.GET.name());
		if (null == response) {
			logger.error("调用api-plane查询插件详情接口响应为空!");
			return null;
		}

		if (!HttpCommonUtil.isNormalCode(response.getStatusCode())) {
			logger.error("调用api-plane查询插件详情接口失败，返回http status code非2xx，httpStatusCoed:{}, errMsg:{}",
			             response.getStatusCode(), response.getResponseBody());
			return null;
		}

		JSONObject result = JSONObject.parseObject(response.getResponseBody());
		EnvoyPluginInfo pluginInfo = new EnvoyPluginInfo();
		pluginInfo.setPluginSchema(result.getString("Schema"));
		pluginInfo.setAuthor(result.getJSONObject("Plugin").getString("author"));
		pluginInfo.setPluginName(result.getJSONObject("Plugin").getString("name"));
		pluginInfo.setPluginPriority(
			getLongValueFromJsonWithDefault(result.getJSONObject("Plugin"), "pluginPriority", 0));
		pluginInfo.setCreateTime(getLongValueFromJsonWithDefault(result.getJSONObject("Plugin"), "createTime", 0));
		pluginInfo.setUpdateTime(getLongValueFromJsonWithDefault(result.getJSONObject("Plugin"), "updateTime", 0));
		pluginInfo.setInstructionForUse(result.getJSONObject("Plugin").getString("instructionForUse"));
		pluginInfo.setPluginScope(result.getJSONObject("Plugin").getString("pluginScope"));
		pluginInfo.setPluginType(result.getJSONObject("Plugin").getString("name"));
		pluginInfo.setPluginHandler(result.getJSONObject("Plugin").getString("processor"));
		return pluginInfo;
	}

	private long getLongValueFromJsonWithDefault(JSONObject jsonObject, String key, long defaultValue) {
		return jsonObject.getLong(key) == null ? defaultValue : jsonObject.getLong(key);
	}

	private EnvoyPluginBindingInfo getBindingInfo(long gwId, String bindingObjectId, String bindingObjectType,
	                                              String pluginType) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		params.put("bindingObjectId", bindingObjectId);
		params.put("bindingObjectType", bindingObjectType);
		params.put("pluginType", pluginType);
		List<EnvoyPluginBindingInfo> pluginBindingInfoList = envoyPluginBindingInfoDao.getRecordsByField(params);
		return CollectionUtils.isEmpty(pluginBindingInfoList) ? null : pluginBindingInfoList.get(0);
	}

	private boolean bindingPluginToApiPlane(long gwId, String bindingObjectId, String bindingObjectType,
	                                        String pluginConfiguration, String pluginType) {
		List<EnvoyPluginBindingInfo> alreadyEnablePlugins = getEnablePluginBindingList(gwId, bindingObjectId,
		                                                                               bindingObjectType);
		List<String> newPluginConfigurations = alreadyEnablePlugins.stream().map(
			EnvoyPluginBindingInfo::getPluginConfiguration).collect(Collectors.toList());
		newPluginConfigurations.add(pluginConfiguration);

		if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE.equals(bindingObjectType)) {
			EnvoyRouteRuleProxyInfo routeRuleProxyInfo = envoyRouteRuleProxyService.getRouteRuleProxy(gwId, Integer
				                                                                                                .valueOf(
					                                                                                                bindingObjectId));
			long publishRouteRuleId = envoyRouteRuleProxyService.publishRouteRule(routeRuleProxyInfo,
			                                                                      newPluginConfigurations, false);
			return Const.ERROR_RESULT != publishRouteRuleId;
		} else if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_GLOBAL.equals(bindingObjectType)) {
			return publishGlobalPlugin(gwId, bindingObjectId, pluginConfiguration, pluginType);
		}
		return true;
	}

	private boolean publishGlobalPlugin(long gwId, String bindingObjectId, String pluginConfiguration,
	                                    String pluginType) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		EnvoyVirtualHostInfo virtualHostInfo = envoyGatewayService.getVirtualHostByGwIdAndProjectId(gwId, Long.valueOf(
			bindingObjectId));
		JSONObject gatewayPlugin = buildGatewayPlugin(gatewayInfo.getGwClusterName(), virtualHostInfo,
		                                              pluginConfiguration, pluginType);
		return publishGlobalPluginToApiPlane(gatewayInfo, gatewayPlugin);
	}

	private JSONObject buildGatewayPlugin(String gwCluster, EnvoyVirtualHostInfo virtualHostInfo,
	                                      String pluginConfiguration, String pluginType) {
		JSONObject gatewayPlugin = new JSONObject();
		gatewayPlugin.put("Gateway", gwCluster);
		gatewayPlugin.put("Code", virtualHostInfo.getVirtualHostCode() + "-" + pluginType);
		gatewayPlugin.put("Hosts", virtualHostInfo.getHostList());
		gatewayPlugin.put("Plugins", Lists.newArrayList(pluginConfiguration));
		return gatewayPlugin;
	}

	private boolean publishGlobalPluginToApiPlane(GatewayInfo gatewayInfo, JSONObject gatewayPlugin) {
		Map<String, String> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("Action", "PublishGlobalPlugin");
		params.put("Version", "2019-07-25");
		Map<String, String> headers = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		headers.put("Content-type", Const.DEFAULT_CONTENT_TYPE);
		HttpClientResponse response = publishGlobalPluginToApiPlane(gatewayInfo.getApiPlaneAddr() + "/api/portal",
		                                                            params, gatewayPlugin.toJSONString(), headers,
		                                                            HttpMethod.POST.name());
		return null != response && HttpCommonUtil.isNormalCode(response.getStatusCode());
	}

	private boolean unbindingPluginToApiPlane(EnvoyPluginBindingInfo bindingInfo) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(bindingInfo.getGwId());

		List<EnvoyPluginBindingInfo> alreadyEnablePlugins = getEnablePluginBindingList(bindingInfo.getGwId(),
		                                                                               bindingInfo.getBindingObjectId(),
		                                                                               bindingInfo
			                                                                               .getBindingObjectType());
		List<String> newPluginConfigurations = alreadyEnablePlugins.stream().filter(
			pluginBindingInfo -> pluginBindingInfo.getId() != bindingInfo.getId()).map(
			EnvoyPluginBindingInfo::getPluginConfiguration).collect(Collectors.toList());

		if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE.equals(bindingInfo.getBindingObjectType())) {
			EnvoyRouteRuleProxyInfo routeRuleProxyInfo = envoyRouteRuleProxyService.getRouteRuleProxy(
				bindingInfo.getGwId(), Integer.valueOf(bindingInfo.getBindingObjectId()));
			return Const.ERROR_RESULT != envoyRouteRuleProxyService.publishRouteRule(routeRuleProxyInfo,
			                                                                         newPluginConfigurations, false);
		} else if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_GLOBAL.equals(bindingInfo.getBindingObjectType())) {
			return unbindingGlobalPlugin(gatewayInfo, bindingInfo.getBindingObjectId(),
			                             bindingInfo.getPluginConfiguration(), bindingInfo.getPluginType());
		}
		return true;
	}

	private boolean unbindingGlobalPlugin(GatewayInfo gatewayInfo, String bindingObjectId, String pluginConfiguration,
	                                      String pluginType) {
		EnvoyVirtualHostInfo virtualHostInfo =
			envoyGatewayService.getVirtualHostByGwIdAndProjectId(gatewayInfo.getId(),
		                                                                                            Long.valueOf(
			                                                                                            bindingObjectId));
		JSONObject gatewayPlugin = buildGatewayPlugin(gatewayInfo.getGwClusterName(), virtualHostInfo,
		                                              pluginConfiguration, pluginType);
		return unbindingGlobalPluginToApiPlane(gatewayInfo, gatewayPlugin);
	}

	private boolean unbindingGlobalPluginToApiPlane(GatewayInfo gatewayInfo, JSONObject gatewayPlugin) {
		Map<String, String> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("Action", "DeleteGlobalPlugin");
		params.put("Version", "2019-07-25");
		Map<String, String> headers = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		headers.put("Content-type", Const.DEFAULT_CONTENT_TYPE);

		HttpClientResponse response = publishGlobalPluginToApiPlane(gatewayInfo.getApiPlaneAddr() + "/api/portal",
		                                                            params, gatewayPlugin.toJSONString(), headers,
		                                                            HttpMethod.POST.name());
		return null != response && HttpCommonUtil.isNormalCode(response.getStatusCode());
	}

	private boolean updateRouteIfEnable(long pluginBindingInfoId, String pluginConfiguration,
	                                    EnvoyPluginBindingInfo bindingInfo) {
		if (EnvoyPluginBindingInfo.BINDING_STATUS_ENABLE.equals(bindingInfo.getBindingStatus())) {
			List<EnvoyPluginBindingInfo> alreadyBindingPlugins = getEnablePluginBindingList(bindingInfo.getGwId(),
			                                                                                bindingInfo
				                                                                                .getBindingObjectId(),
			                                                                                bindingInfo
				                                                                                .getBindingObjectType());
			List<String> newPluginConfigurations = alreadyBindingPlugins.stream().map(bindingInfoItem -> {
				if (pluginBindingInfoId != bindingInfoItem.getId()) {
					return bindingInfoItem.getPluginConfiguration();
				}
				return pluginConfiguration;
			}).collect(Collectors.toList());

			if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE.equals(bindingInfo.getBindingObjectType())) {
				EnvoyRouteRuleProxyInfo routeRuleProxyInfo = envoyRouteRuleProxyService.getRouteRuleProxy(
					bindingInfo.getGwId(), Integer.valueOf(bindingInfo.getBindingObjectId()));
				return Const.ERROR_RESULT != envoyRouteRuleProxyService.publishRouteRule(routeRuleProxyInfo,
				                                                                         newPluginConfigurations,
				                                                                         false);
			} else if (EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_GLOBAL.equals(bindingInfo.getBindingObjectType())) {
				return publishGlobalPlugin(bindingInfo.getGwId(), bindingInfo.getBindingObjectId(),
				                           pluginConfiguration,
				                           bindingInfo.getPluginType());
			}
		}
		return true;
	}

	private List<Long> getGwIdList(long gwId, long projectId, String pattern) {
		List<Long> gwIdList = Lists.newArrayList();
		if (0 < gwId) {
			return gwIdList;
		} else if (StringUtils.isNotBlank(pattern)) {
			gwIdList = gatewayInfoService.getGwIdListByNameFuzzy(pattern, projectId);
		}
		return gwIdList;
	}

	private List<String> getBindingObjectIdList(long projectId, String bindingObjectId,
	                                            Set<String> bindingObjectTypeList, String pattern) {
		List<String> bindingObjectIdList = Lists.newArrayList();

		// 如果传入了绑定对象id，则不进行路由名称、服务名称的模糊查询
		if (StringUtils.isNotBlank(bindingObjectId)) {
			return bindingObjectIdList;
		}

		// 如果未传入绑定对象id，且传入了pattern，则使用pattern进行路由名称、服务名称的模糊查询
		Set<String> routeRuleIdList = Sets.newHashSet();
		Set<String> serviceIdList = Sets.newHashSet();
		if (StringUtils.isNotBlank(pattern)) {
			if (CollectionUtils.isEmpty(bindingObjectTypeList) || bindingObjectTypeList.contains(
				EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE)) {
				routeRuleIdList = envoyRouteRuleInfoService.getRouteRuleIdListByNameFuzzy(pattern, projectId).stream()
				                                           .map(String::valueOf).collect(Collectors.toSet());
			}
			if (CollectionUtils.isEmpty(bindingObjectTypeList) || bindingObjectTypeList.contains(
				EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_SERVICE)) {
				serviceIdList = serviceInfoService.getServiceIdListByDisplayNameFuzzy(pattern, projectId).stream().map(
					String::valueOf).collect(Collectors.toSet());
			}
		}
		bindingObjectIdList.addAll(routeRuleIdList);
		bindingObjectIdList.addAll(serviceIdList);
		return bindingObjectIdList;
	}

}
