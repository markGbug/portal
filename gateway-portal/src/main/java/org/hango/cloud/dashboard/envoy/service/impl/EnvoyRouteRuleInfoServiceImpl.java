package org.hango.cloud.dashboard.envoy.service.impl;

import com.google.common.collect.Lists;
import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hango.cloud.dashboard.envoy.meta.ServiceInfo;
import org.hango.cloud.dashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.dashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.dashboard.envoy.util.CommonUtil;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.dao.IEnvoyRouteRuleInfoDao;
import org.hango.cloud.dashboard.envoy.meta.EnvoyPluginBindingInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteRuleInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.hango.cloud.dashboard.envoy.service.IEnvoyPluginInfoService;
import org.hango.cloud.dashboard.envoy.service.IEnvoyRouteRuleInfoService;
import org.hango.cloud.dashboard.envoy.service.IEnvoyRouteRuleProxyService;
import org.hango.cloud.dashboard.envoy.service.IEnvoyServiceProxyService;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyCopyRuleDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyCopyRulePortDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyDestinationDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteRuleDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteRuleHeaderOperationDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteRuleProxyDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyRouteStringMatchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 路由规则管理Service层实现类
 * <p>
 * 2019-09-11
 */
@Service
public class EnvoyRouteRuleInfoServiceImpl implements IEnvoyRouteRuleInfoService {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyRouteRuleInfoServiceImpl.class);

	@Autowired
	private IEnvoyRouteRuleInfoDao routeRuleInfoDao;

	@Autowired
	private IServiceInfoService serviceInfoService;

	@Autowired
	private IEnvoyServiceProxyService serviceProxyService;

	@Autowired
	private IEnvoyPluginInfoService envoyPluginInfoService;

	@Autowired
	private IEnvoyRouteRuleProxyService routeRuleProxyService;

	@Override
	public ErrorCode checkAddParam(EnvoyRouteRuleDto ruleDto) {
		EnvoyRouteRuleInfo sameNameRule = getRouteRuleInfoByName(ruleDto.getRouteRuleName().trim());
		if (null != sameNameRule) {
			logger.info("同名规则已存在，不允许重复创建");
			return CommonErrorCode.SameNameRouteRuleExist;
		}
		ServiceInfo serviceInfoDb = serviceInfoService.getServiceByServiceId(ruleDto.getServiceId());
		if (serviceInfoDb == null) {
			logger.info("创建路由规则，所属服务不存在");
			return CommonErrorCode.NoSuchService;
		}
		EnvoyRouteStringMatchDto uriMatchDto = ruleDto.getUriMatchDto();
		if (CollectionUtils.isEmpty(uriMatchDto.getValue())) {
			logger.info("创建路由规则，uri path为空");
			return CommonErrorCode.NoRouteRulePath;
		}
		//正则中不允许出现nginx捕获正则
		if (!Const.URI_TYPE_EXACT.equals(uriMatchDto.getType()) && uriMatchDto.getValue().stream().anyMatch(
			path -> Pattern.matches(Const.NGINX_CAPTURE_REGEX, path))) {
			logger.info("创建路由，path中包含nginx 捕获正则，不允许创建");
			return CommonErrorCode.RouteRuleContainsNginxCapture;
		}
		if (ruleDto.getMethodMatchDto() != null && !Const.CONST_METHODS.containsAll(
			ruleDto.getMethodMatchDto().getValue())) {
			return CommonErrorCode.RouteRuleMethodInvalid;
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean isSameRouteRuleInfo(EnvoyRouteRuleInfo envoyRouteRuleInfo) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("uri", envoyRouteRuleInfo.getUri());
		if (StringUtils.isNotBlank(envoyRouteRuleInfo.getMethod())) {
			params.put("method", envoyRouteRuleInfo.getMethod());
		}
		if (StringUtils.isNotBlank(envoyRouteRuleInfo.getHost())) {
			params.put("host", envoyRouteRuleInfo.getHost());
		}
		if (StringUtils.isNotBlank(envoyRouteRuleInfo.getHeader())) {
			params.put("header", envoyRouteRuleInfo.getHeader());
		}
		if (StringUtils.isNotBlank(envoyRouteRuleInfo.getQueryParam())) {
			params.put("queryParam", envoyRouteRuleInfo.getQueryParam());
		}
		params.put("projectId", envoyRouteRuleInfo.getProjectId());
		params.put("priority", envoyRouteRuleInfo.getPriority());
		List<EnvoyRouteRuleInfo> envoyRouteRuleInfos = routeRuleInfoDao.getRecordsByField(params);
		if (CollectionUtils.isEmpty(envoyRouteRuleInfos)) {
			return false;
		}
		List<EnvoyRouteRuleInfo> equalsRouteRule = envoyRouteRuleInfos.stream().filter(
			item -> item.isSame(envoyRouteRuleInfo)).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(equalsRouteRule)) {
			return false;
		}
		//更新路由规则,如果两个对象routeId相同，则认为不重复，允许更新
		if (CollectionUtils.isNotEmpty(equalsRouteRule) && equalsRouteRule.size() == 1) {
			return envoyRouteRuleInfo.getId() != equalsRouteRule.get(0).getId();
		}
		return true;
	}

	@Override
	public long addRouteRule(EnvoyRouteRuleInfo routeRuleInfo) {
		if (null == routeRuleInfo) {
			logger.error("添加路由规则时传入的路由规则为空!");
			return Const.ERROR_RESULT;
		}
		routeRuleInfo.setCreateTime(System.currentTimeMillis());
		routeRuleInfo.setUpdateTime(System.currentTimeMillis());
		return routeRuleInfoDao.add(routeRuleInfo);
	}

	@Override
	public EnvoyRouteRuleInfo getRouteRuleInfoByName(String routeRuleName) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("routeRuleName", routeRuleName);
		List<EnvoyRouteRuleInfo> envoyRouteRuleInfos = routeRuleInfoDao.getRecordsByField(params);
		return CollectionUtils.isEmpty(envoyRouteRuleInfos) ? null : envoyRouteRuleInfos.get(0);
	}

	@Override
	public ErrorCode checkUpdateParam(EnvoyRouteRuleDto routeRuleDto) {
		EnvoyRouteRuleInfo ruleInDB = getRouteRuleInfoById(routeRuleDto.getId());
		if (null == ruleInDB) {
			logger.info("指定的路由规则不存在，id:{}", routeRuleDto.getId());
			return CommonErrorCode.NoSuchRouteRule;
		}
		EnvoyRouteRuleInfo sameNameRule = getRouteRuleInfoByName(routeRuleDto.getRouteRuleName().trim());
		if (null != sameNameRule && routeRuleDto.getId() != sameNameRule.getId()) {
			logger.info("同名规则已存在，不允许重复创建");
			return CommonErrorCode.SameNameRouteRuleExist;
		}
		EnvoyRouteStringMatchDto uriMatchDto = routeRuleDto.getUriMatchDto();
		if (CollectionUtils.isEmpty(uriMatchDto.getValue())) {
			logger.info("更新路由规则，uri path为空，不允许更新");
			return CommonErrorCode.NoRouteRulePath;
		}
		//正则中不允许出现nginx捕获正则
		if (!Const.URI_TYPE_EXACT.equals(uriMatchDto.getType()) && uriMatchDto.getValue().stream().anyMatch(
			path -> Pattern.matches(Const.NGINX_CAPTURE_REGEX, path))) {
			logger.info("更新路由，path中包含nginx 捕获正则，不允许更新");
			return CommonErrorCode.RouteRuleContainsNginxCapture;
		}
		if (routeRuleDto.getMethodMatchDto() != null && !Const.CONST_METHODS.containsAll(
			routeRuleDto.getMethodMatchDto().getValue())) {
			logger.info("更新路由，method参数填写不正确");
			return CommonErrorCode.RouteRuleMethodInvalid;
		}
		return CommonErrorCode.Success;
	}

	@Override
	public ErrorCode checkDeleteParam(long id) {
		EnvoyRouteRuleInfo ruleInDB = getRouteRuleInfoById(id);
		if (CollectionUtils.isNotEmpty(routeRuleProxyService.getRouteRuleProxyByRouteRuleId(id))) {
			logger.info("删除路由规则，已发布路由列表中仍然存在数据");
			return CommonErrorCode.RouteRuleAlreadyPublished;
		}
		if (null != ruleInDB && ruleInDB.getPublishStatus() == NumberUtils.INTEGER_ONE) {
			logger.info("删除路由规则，已发布路由列表不存在数据，路由列表存在脏数据，需要fix数据");
			return CommonErrorCode.RouteRuleAlreadyPublished;
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean updateRouteRule(EnvoyRouteRuleInfo envoyRouteRuleInfo) {
		EnvoyRouteRuleInfo ruleInDB = getRouteRuleInfoById(envoyRouteRuleInfo.getId());
		if (null == ruleInDB) {
			logger.error("更新路由规则时id指定的路由规则已不存在，请检查! id:{}", envoyRouteRuleInfo.getId());
			return false;
		}
		envoyRouteRuleInfo.setProjectId(ruleInDB.getProjectId());
		envoyRouteRuleInfo.setUpdateTime(System.currentTimeMillis());
		routeRuleInfoDao.update(envoyRouteRuleInfo);
		return true;
	}

	@Override
	public EnvoyRouteRuleInfo getRouteRuleInfoById(long id) {
		return routeRuleInfoDao.get(id);
	}

	@Override
	public ErrorCode checkDescribeParam(String sortKey, String sortValue, long offset, long limit) {
		if (StringUtils.isNotBlank(sortKey) && !Const.SORT_KEY.contains(sortKey)) {
			return CommonErrorCode.SortKeyInvalid;
		}
		if (StringUtils.isNotBlank(sortValue) && !Const.SORT_VALUE.contains(sortValue)) {
			return CommonErrorCode.SortValueInvalid;
		}
		return CommonUtil.checkOffsetAndLimit(offset, limit);
	}

	@Override
	public List<EnvoyRouteRuleInfo> getRouteRuleInfoByPattern(String pattern, int publishStatus, long serviceId,
	                                                          long projectId, String sortKey, String sortValue,
	                                                          long offset, long limit) {

		List<EnvoyRouteRuleInfo> envoyRouteRuleInfos;
		//查询所有服务下的路由规则
		if (serviceId == 0) {
			envoyRouteRuleInfos = routeRuleInfoDao.getRuleInfoByLimit(pattern, publishStatus, projectId, sortKey,
			                                                          sortValue, offset, limit);
		} else {
			envoyRouteRuleInfos = routeRuleInfoDao.getRuleInfoByServiceLimit(pattern, publishStatus, serviceId,
			                                                                 sortKey,
			                                                                 sortValue, offset, limit);
		}
		return envoyRouteRuleInfos;
	}

	@Override
	public long getRouteRuleInfoCount(String pattern, int publishStatus, long serviceId, long projectId) {
		//查询所有服务下的路由规则数量
		if (serviceId == 0) {
			return routeRuleInfoDao.getRuleInfoCount(pattern, publishStatus, projectId);
		}
		return routeRuleInfoDao.getRuleInfoByServiceCount(pattern, publishStatus, serviceId);
	}

	@Override
	public boolean deleteRouteRule(long id) {
		EnvoyRouteRuleInfo envoyRouteRuleInfo = routeRuleInfoDao.get(id);
		if (envoyRouteRuleInfo != null) {
			routeRuleInfoDao.delete(envoyRouteRuleInfo);
		}
		return true;
	}

	@Override
	public EnvoyRouteRuleDto fromMeta(EnvoyRouteRuleInfo ruleInfo) {
		EnvoyRouteRuleDto ruleDto = new EnvoyRouteRuleDto();
		ruleDto.setId(ruleInfo.getId());
		ruleDto.setServiceId(ruleInfo.getServiceId());
		ruleDto.setServiceName(serviceInfoService.getServiceByServiceId(ruleInfo.getServiceId()).getDisplayName());
		ruleDto.setServiceType(serviceInfoService.getServiceByServiceId(ruleInfo.getServiceId()).getServiceType());
		ruleDto.setCreateTime(ruleInfo.getCreateTime());
		ruleDto.setUpdateTime(ruleInfo.getUpdateTime());
		ruleDto.setRouteRuleName(ruleInfo.getRouteRuleName());
		ruleDto.setPublishStatus(ruleInfo.getPublishStatus());
		ruleDto.setPriority(ruleInfo.getPriority());
		ruleDto.setRouteRuleSource(ruleInfo.getRouteRuleSource());

		if (StringUtils.isNotBlank(ruleInfo.getHeaderOperation())) {
			ruleDto.setHeaderOperation(
				JSON.parseObject(ruleInfo.getHeaderOperation(), EnvoyRouteRuleHeaderOperationDto.class));
		}

		ruleDto.fromRouteMeta(ruleInfo);
		ruleDto.setDescription(ruleInfo.getDescription());
		return ruleDto;
	}

	@Override
	public List<Long> getRouteRuleIdListByNameFuzzy(String routeRuleName, long projectId) {
		List<Long> routeRuleIdList = routeRuleInfoDao.getRouteRuleIdListByNameFuzzy(routeRuleName, projectId);
		return CollectionUtils.isEmpty(routeRuleIdList) ? Lists.newArrayList() : routeRuleIdList;
	}

	@Override
	public List<EnvoyRouteRuleInfo> getRouteRuleList(List<Long> routeRuleIdList) {
		if (CollectionUtils.isEmpty(routeRuleIdList)) {
			return Lists.newArrayList();
		}
		List<EnvoyRouteRuleInfo> routeRuleInfoList = routeRuleInfoDao.getRouteRuleList(routeRuleIdList);
		return CollectionUtils.isEmpty(routeRuleIdList) ? Lists.newArrayList() : routeRuleInfoList;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean copyPluginInfo(long originRouteId, long destinationRouteId, long serviceId,
	                              List<EnvoyCopyRulePortDto> copyRulePortDtos) throws Exception {
		//不传port，gwId直接返回false
		if (CollectionUtils.isEmpty(copyRulePortDtos)) {
			return true;
		}
		try {
			for (EnvoyCopyRulePortDto copyRulePortDto : copyRulePortDtos) {

				EnvoyRouteRuleProxyDto routeRulePublishDto = new EnvoyRouteRuleProxyDto();
				routeRulePublishDto.setRouteRuleId(destinationRouteId);
				routeRulePublishDto.setServiceId(serviceId);
				//设置为非使能
				routeRulePublishDto.setEnableState(Const.ROUTE_RULE_DISABLE_STATE);
				routeRulePublishDto.setGwId(copyRulePortDto.getGwId());
				List<EnvoyDestinationDto> destinationServices = new ArrayList<>();
				EnvoyDestinationDto envoyDestinationDto = new EnvoyDestinationDto();
				envoyDestinationDto.setServiceId(serviceId);
				envoyDestinationDto.setPort(copyRulePortDto.getPort());
				destinationServices.add(envoyDestinationDto);
				routeRulePublishDto.setDestinationServices(destinationServices);
				//源路由未发布到该网关，不携带插件
				if (routeRuleProxyService.getRouteRuleProxyCount(copyRulePortDto.getGwId(), originRouteId) == 0) {
					routeRuleProxyService.publishRouteRule(routeRuleProxyService.toMeta(routeRulePublishDto),
					                                       Lists.newArrayList(), true);
				} else {
					List<EnvoyPluginBindingInfo> alreadyBindingPlugins = envoyPluginInfoService.getPluginBindingList(
						copyRulePortDto.getGwId(), String.valueOf(originRouteId),
						EnvoyPluginBindingInfo.BINDING_OBJECT_TYPE_ROUTE_RULE);
					if (CollectionUtils.isNotEmpty(alreadyBindingPlugins)) {
						alreadyBindingPlugins.forEach(envoyPluginBindingInfo -> {
							envoyPluginBindingInfo.setBindingObjectId(String.valueOf(destinationRouteId));
							envoyPluginBindingInfo.setCreateTime(System.currentTimeMillis());
							envoyPluginBindingInfo.setUpdateTime(System.currentTimeMillis());
							envoyPluginInfoService.bindingPluginToDb(envoyPluginBindingInfo);
						});
					}
					List<String> newPluginConfigurations = alreadyBindingPlugins.stream().map(
						EnvoyPluginBindingInfo::getPluginConfiguration).collect(Collectors.toList());
					routeRuleProxyService.publishRouteRule(routeRuleProxyService.toMeta(routeRulePublishDto),
					                                       newPluginConfigurations, true);
				}
			}
		} catch (Exception e) {
			logger.info("复制路由规则，发布网关，出现异常，e:{}", e);
			throw new Exception();
		}
		return true;
	}

}
