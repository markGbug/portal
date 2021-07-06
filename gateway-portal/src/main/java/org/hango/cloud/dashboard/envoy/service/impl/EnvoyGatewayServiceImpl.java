package org.hango.cloud.ncegdashboard.envoy.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hango.cloud.ncegdashboard.envoy.config.ApiServerConfig;
import org.hango.cloud.ncegdashboard.envoy.handler.PluginHandler;
import org.hango.cloud.ncegdashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.common.HttpClientResponse;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.PermissionScopeDto;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayInfoService;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayProjectService;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.dao.IEnvoyVirtualHostInfoDao;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyVirtualHostInfo;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyGatewayService;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyPluginManagerDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.PluginOrderDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.PluginOrderItemDto;
import org.hango.cloud.ncegdashboard.envoy.web.util.HttpCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Envoy网关Service层实现类
 * <p>
 * 2020-01-08
 */
@Service
public class EnvoyGatewayServiceImpl implements IEnvoyGatewayService {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyGatewayServiceImpl.class);

	@Autowired
	private IEnvoyVirtualHostInfoDao envoyVirtualHostInfoDao;

	@Autowired
	private IGatewayInfoService gatewayInfoService;

	@Autowired
	private IGatewayProjectService gatewayProjectService;

	@Autowired
	private ApiServerConfig apiServerConfig;

	@Override
	public ErrorCode checkVirtualHostList(long gwId, List<EnvoyVirtualHostInfo> vhList) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		if (null == gatewayInfo || !Const.ENVOY_GATEWAY_TYPE.equals(gatewayInfo.getGwType())) {
			logger.info("gwId指定的网关不存在，或者不是Envoy网关! gwId:{}", gwId);
			return CommonErrorCode.NoSuchGateway;
		}
		for (EnvoyVirtualHostInfo vhInfo : vhList) {
			ErrorCode checkResult = checkVirtualHost(vhInfo);
			if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
				return checkResult;
			}
		}
		return CommonErrorCode.Success;
	}

	@Override
	public ErrorCode checkCreateVirtualHost(EnvoyVirtualHostInfo vhInfo) {
		EnvoyVirtualHostInfo virtualHostInfo = getVirtualHostByGwIdAndProjectId(vhInfo.getGwId(),
		                                                                        vhInfo.getProjectId());
		if (null != virtualHostInfo) {
			logger.info("对应的virtual host已存在! gwId:{}, projectId:{}", vhInfo.getGwId(), vhInfo.getProjectId());
			return CommonErrorCode.VirtualHostAlreadyExist;
		}
		return checkVirtualHost(vhInfo);
	}

	@Override
	public boolean createVirtualHost(EnvoyVirtualHostInfo vhInfo) {
		vhInfo.setCreateTime(System.currentTimeMillis());
		vhInfo.setUpdateTime(System.currentTimeMillis());
		PermissionScopeDto project = gatewayProjectService.getProjectScopeDto(vhInfo.getProjectId());
		if (null == project || 0 == project.getId()) {
			throw new RuntimeException("项目id对应的项目不存在!");
		}
		vhInfo.setVirtualHostCode(
			project.getPermissonScopeEnvName() + "-" + vhInfo.getProjectId() + "-" + vhInfo.getGwId());
		envoyVirtualHostInfoDao.add(vhInfo);
		return true;
	}

	@Override
	public List<EnvoyVirtualHostInfo> getVirtualHostListByGwId(long gwId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		return envoyVirtualHostInfoDao.getRecordsByField(params);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateVirtualHostList(long gwId, List<EnvoyVirtualHostInfo> vhList) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		if (null == gatewayInfo) {
			return false;
		}
		List<EnvoyVirtualHostInfo> vhListInDb = getVirtualHostListByGwId(gwId);

		Set<Long> projectIdSet = vhList.stream().map(EnvoyVirtualHostInfo::getProjectId).collect(Collectors.toSet());
		Set<Long> projectIdSetInDb = vhListInDb.stream().map(EnvoyVirtualHostInfo::getProjectId).collect(
			Collectors.toSet());
		Map<Long, EnvoyVirtualHostInfo> vhMapInDb = vhListInDb.stream().collect(
			Collectors.toMap(EnvoyVirtualHostInfo::getProjectId, item -> item));

		List<EnvoyVirtualHostInfo> vhListToAdd = vhList.stream().filter(
			item -> !projectIdSetInDb.contains(item.getProjectId())).collect(Collectors.toList());
		vhListToAdd.forEach(item -> {
			item.setCreateTime(System.currentTimeMillis());
			item.setUpdateTime(System.currentTimeMillis());
			PermissionScopeDto project = gatewayProjectService.getProjectScopeDto(item.getProjectId());
			if (null == project || 0 == project.getId()) {
				throw new RuntimeException("项目id对应的项目不存在!");
			}
			item.setVirtualHostCode(project.getPermissonScopeEnvName() + "-" + item.getProjectId() + "-" + gwId);
			envoyVirtualHostInfoDao.add(item);
		});

		List<EnvoyVirtualHostInfo> vhListToDelete = vhListInDb.stream().filter(
			item -> !projectIdSet.contains(item.getProjectId())).collect(Collectors.toList());
		vhListToDelete.forEach(item -> envoyVirtualHostInfoDao.delete(item));

		List<EnvoyVirtualHostInfo> vhListToUpdate = vhList.stream().filter(
			item -> projectIdSetInDb.contains(item.getProjectId())).collect(Collectors.toList());
		vhListToUpdate.forEach(item -> {
			EnvoyVirtualHostInfo vhInDb = vhMapInDb.get(item.getProjectId());
			vhInDb.setUpdateTime(System.currentTimeMillis());
			vhInDb.setHosts(item.getHosts());
			envoyVirtualHostInfoDao.update(vhInDb);
		});

		gatewayInfoService.updateGwInfo(gatewayInfo, true);

		return true;
	}

	@Override
	public EnvoyVirtualHostInfo getVirtualHostByGwIdAndProjectId(long gwId, long projectId) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("gwId", gwId);
		params.put("projectId", projectId);
		List<EnvoyVirtualHostInfo> virtualHostInfoList = envoyVirtualHostInfoDao.getRecordsByField(params);
		return CollectionUtils.isEmpty(virtualHostInfoList) ? null : virtualHostInfoList.get(0);
	}

	@Override
	public ErrorCode checkUpdateVirtualHost(long virtualHostId, List<String> hostList) {
		EnvoyVirtualHostInfo vhInfo = getVirtualHost(virtualHostId);
		if (null == vhInfo) {
			logger.info("vh id指定的vh不存在! virtualHostId:{}", virtualHostId);
			return CommonErrorCode.NoSuchVirtualHost;
		}
		if (CollectionUtils.isEmpty(hostList)) {
			logger.info("域名列表不允许为空!");
			return CommonErrorCode.InvalidParameterValue(hostList, "Hosts", "域名列表不允许为空");
		}

		for (String host : hostList) {
			if (host.startsWith("*") || host.contains(":")) {
				logger.info("域名不正确! 不允许最左侧为*（不支持泛域名）, host:{}", host);
				return CommonErrorCode.InvalidParameterValue(hostList, "Hosts", "不支持泛域名，host: " + host);
			}
		}

		List<String> repetitiveHosts = findExistHosts(hostList, vhInfo.getGwId(), vhInfo.getProjectId());
		if (!CollectionUtils.isEmpty(repetitiveHosts)) {
			logger.info("存在域名冲突，冲突域名列表, repetitiveHosts:{}", repetitiveHosts);
			return CommonErrorCode.InvalidParameterValue(hostList, "Hosts", "域名已被其他项目关联：" + repetitiveHosts);
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean updateVirtualHost(long virtualHostId, List<String> hostList) {
		EnvoyVirtualHostInfo vhInfo = getVirtualHost(virtualHostId);
		if (null == vhInfo) {
			return false;
		}
		vhInfo.setHosts(JSON.toJSONString(hostList));
		vhInfo.setUpdateTime(System.currentTimeMillis());
		envoyVirtualHostInfoDao.update(vhInfo);
		return true;
	}

	@Override
	public Long getVirtualHostCount(long gwId, List<Long> projectIdList, String domain) {
		return envoyVirtualHostInfoDao.getVirtualHostCount(gwId, projectIdList, domain);
	}

	@Override
	public List<EnvoyVirtualHostInfo> getVirtualHostList(long gwId, List<Long> projectIdList, String domain,
	                                                     long limit,
	                                                     long offset) {
		return envoyVirtualHostInfoDao.getVirtualHostList(gwId, projectIdList, domain, limit, offset);
	}

	@Override
	public EnvoyVirtualHostInfo getVirtualHost(long virtualHostId) {
		return envoyVirtualHostInfoDao.get(virtualHostId);
	}


	@Override
	public List<EnvoyPluginManagerDto> getEnvoyPluginManager(GatewayInfo gatewayInfo) {
		if (gatewayInfo == null) {
			return Collections.emptyList();
		}
		List<PluginOrderItemDto> envoyPluginManager = getEnvoyPluginManager(gatewayInfo.getApiPlaneAddr(),
		                                                                    gatewayInfo.getGwClusterName());
		return envoyPluginManager.stream().map(e -> toPluginManagerDto(e, apiServerConfig.getPluginManagerMap()))
		                         .filter(e -> !PluginHandler.pluginIgnoreList.contains(e.getName())).collect(
				Collectors.toList());
	}

	@Override
	public List<PluginOrderItemDto> getEnvoyPluginManager(String apiPlaneAddr, String gwClusterName) {

		PluginOrderDto pluginOrderDto = new PluginOrderDto();
		HashMap<String, String> gatewayLabels = Maps.newHashMap();
		gatewayLabels.put("gw_cluster", gwClusterName);
		pluginOrderDto.setGatewayLabels(gatewayLabels);
		Map<String, String> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("Action", "GetPluginOrder");
		params.put("Version", "2019-07-25");

		HttpClientResponse response = HttpCommonUtil.getFromApiPlane(apiPlaneAddr + "/api", params,
		                                                             JSON.toJSONString(pluginOrderDto), null,
		                                                             HttpMethod.POST.name());
		if (null == response) {
			return Collections.emptyList();
		}

		if (!HttpCommonUtil.isNormalCode(response.getStatusCode())) {
			logger.error("获取网关插件配置失败，返回http status code非2xx，httpStatusCode:{},errMsg:{}", response.getStatusCode(),
			             response.getResponseBody());
			return Collections.emptyList();
		}
		JSONObject jsonObject = JSON.parseObject(response.getResponseBody());
		PluginOrderDto result = jsonObject.getObject("Result", PluginOrderDto.class);
		if (result == null) {
			logger.info("未能找到对应网关的插件配置");
			return Collections.emptyList();
		}
		List<PluginOrderItemDto> plugins = result.getPlugins();
		if (CollectionUtils.isEmpty(plugins)) {
			logger.info("网关对应的插件配置为空");
			return Collections.emptyList();
		}
		return result.getPlugins();
	}

	@Override
	public ErrorCode checkEnvoyPluginManager(GatewayInfo gatewayInfo, String name, boolean enable) {
		if (gatewayInfo == null) {
			return CommonErrorCode.NoSuchGateway;
		}
		List<PluginOrderItemDto> envoyPluginManager = getEnvoyPluginManager(gatewayInfo.getApiPlaneAddr(),
		                                                                    gatewayInfo.getGwClusterName());
		Optional<PluginOrderItemDto> plugin = envoyPluginManager.stream().filter(
			e -> (PluginHandler.pluginUseSubNameList.containsKey(e.getName()) ? PluginHandler.pluginUseSubNameList.get(
				e.getName()).getName(e) : e.getName()).equals(name)).findFirst();
		if (!plugin.isPresent()) {
			return CommonErrorCode.InvalidPluginName;
		}
		return CommonErrorCode.Success;
	}

	@Override
	public boolean updateEnvoyPluginManager(GatewayInfo gatewayInfo, String name, boolean enable) {
		if (gatewayInfo == null) {
			logger.info("网关信息为空");
			return false;
		}
		List<PluginOrderItemDto> envoyPluginManager = getEnvoyPluginManager(gatewayInfo.getApiPlaneAddr(),
		                                                                    gatewayInfo.getGwClusterName());
		envoyPluginManager.stream().filter(e -> {
			String itemName = PluginHandler.pluginUseSubNameList.containsKey(e.getName())
			                  ? PluginHandler.pluginUseSubNameList.get(e.getName()).getName(e) : e.getName();
			if (itemName.equals(name)) {
				e.setEnable(enable);
			}
			return true;
		}).collect(Collectors.toList());

		return publishPluginToAPIPlane(gatewayInfo, envoyPluginManager);
	}

	@Override
	public Boolean publishPluginToAPIPlane(GatewayInfo gatewayInfo, List<PluginOrderItemDto> envoyPluginManager) {
		Map<String, String> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("Action", "PublishPluginOrder");
		params.put("Version", "2019-07-25");

		PluginOrderDto pluginOrderDto = new PluginOrderDto();
		HashMap<String, String> gatewayLabels = Maps.newHashMap();
		gatewayLabels.put("gw_cluster", gatewayInfo.getGwClusterName());
		pluginOrderDto.setGatewayLabels(gatewayLabels);
		pluginOrderDto.setPlugins(envoyPluginManager);

		String body = JSON.toJSONString(pluginOrderDto);
		HttpClientResponse response = HttpCommonUtil.getFromApiPlane(gatewayInfo.getApiPlaneAddr() + "/api", params,
		                                                             body, null, HttpMethod.POST.name());
		if (null == response) {
			return false;
		}
		if (!HttpCommonUtil.isNormalCode(response.getStatusCode())) {
			logger.error("调用api-plane删除服务接口失败，返回http status code非2xx，httpStatusCoed:{},errMsg:{}",
			             response.getStatusCode(), response.getResponseBody());
			return false;
		}
		return true;
	}

	public EnvoyPluginManagerDto toPluginManagerDto(PluginOrderItemDto item, Map<String, String> extra) {
		EnvoyPluginManagerDto envoyPluginManagerDto = new EnvoyPluginManagerDto();
		envoyPluginManagerDto.setEnable(item.getEnable());
		String name = PluginHandler.pluginUseSubNameList.containsKey(item.getName())
		              ? PluginHandler.pluginUseSubNameList.get(item.getName()).getName(item) : item.getName();
		envoyPluginManagerDto.setName(name);
		String displayName = extra.get(name);
		envoyPluginManagerDto.setDisplayName(StringUtils.isEmpty(displayName) ? name : displayName);
		return envoyPluginManagerDto;
	}

	private ErrorCode checkVirtualHost(EnvoyVirtualHostInfo vhInfo) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(vhInfo.getGwId());
		if (null == gatewayInfo) {
			logger.info("传入网关Id对应的网关不存在！ gwId:{}", vhInfo.getGwId());
			return CommonErrorCode.NoSuchGateway;
		}

		PermissionScopeDto project = gatewayProjectService.getProjectScopeDto(vhInfo.getProjectId());
		if (null == project || 0 == project.getId()) {
			logger.info("传入的项目id对应的项目不存在! projectId:{}", vhInfo.getProjectId());
			return CommonErrorCode.NoSuchProject;
		}

		if (CollectionUtils.isEmpty(vhInfo.getHostList())) {
			logger.info("域名列表不允许为空!");
			return CommonErrorCode.InvalidParameterValue(vhInfo.getHostList(), "Hosts", "域名列表不允许为空");
		}

		for (String host : vhInfo.getHostList()) {
			if (host.startsWith("*") || host.contains(":")) {
				logger.info("域名不正确! 不允许最左侧为*（不支持泛域名）, host:{}", host);
				return CommonErrorCode.InvalidParameterValue(vhInfo.getHostList(), "Hosts", "不支持泛域名，host: " + host);
			}
		}

		List<String> repetitiveHosts = findExistHosts(vhInfo.getHostList(), vhInfo.getGwId(), vhInfo.getProjectId());
		if (!CollectionUtils.isEmpty(repetitiveHosts)) {
			logger.info("存在域名冲突，冲突域名列表, repetitiveHosts:{}", repetitiveHosts);
			return CommonErrorCode.InvalidParameterValue(vhInfo.getHostList(), "Hosts",
			                                             "域名已被其他项目关联：" + repetitiveHosts);
		}

		return CommonErrorCode.Success;
	}

	private List<String> findExistHosts(List<String> hosts, long gwId, long projectId) {
		if (CollectionUtils.isEmpty(hosts)) {
			return Lists.newArrayList();
		}

		// 由于流量灰度时会两个物理网关共用一个域名，所以virtual host的域名配置冲突也仅在同一个网关中校验
		List<EnvoyVirtualHostInfo> allVirtualHost = getVirtualHostListByGwId(gwId);
		if (CollectionUtils.isEmpty(allVirtualHost)) {
			return Lists.newArrayList();
		}

		Set<String> alreadyHosts = allVirtualHost.stream().filter(item -> item.getProjectId() != projectId).flatMap(
			item -> item.getHostList().stream()).collect(Collectors.toSet());

		return hosts.stream().filter(alreadyHosts::contains).collect(Collectors.toList());
	}

}
