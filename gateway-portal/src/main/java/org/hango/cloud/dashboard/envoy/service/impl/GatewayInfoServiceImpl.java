package org.hango.cloud.dashboard.envoy.service.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hango.cloud.dashboard.envoy.dao.GatewayInfoDao;
import org.hango.cloud.dashboard.envoy.meta.EnvoyVirtualHostInfo;
import org.hango.cloud.dashboard.envoy.web.dto.GatewayDto;
import org.hango.cloud.dashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.dashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.dashboard.envoy.service.IGatewayInfoService;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Date: 创建时间: 2018/1/17 下午5:27.
 */
@Service
public class GatewayInfoServiceImpl implements IGatewayInfoService {

	private static final Logger logger = LoggerFactory.getLogger(GatewayInfoServiceImpl.class);

	@Autowired
	private GatewayInfoDao gatewayInfoDao;

	@Autowired
	private EnvoyGatewayServiceImpl envoyGatewayService;

	@Override
	public GatewayInfo get(long id) {
		try {
			GatewayInfo gatewayInfo = gatewayInfoDao.get(id);
			gatewayInfo.setHostList(
				envoyGatewayService.getVirtualHostByGwIdAndProjectId(gatewayInfo.getId(), gatewayInfo.getProjectId())
				                   .getHostList());
			return gatewayInfo;
		} catch (Exception e) {
			logger.info("获取网关下的vh出现异常，e:{}", e);
		}
		return null;
	}

	@Override
	public GatewayInfo getGatewayByName(String gwName) {
		Map<String, Object> params = new HashMap<>();
		params.put("gwName", gwName);
		List<GatewayInfo> gatewayInfoList = gatewayInfoDao.getRecordsByField(params);
		if (gatewayInfoList.size() == 0) {
			return null;
		} else {
			return gatewayInfoList.get(0);
		}
	}

	@Override
	public GatewayInfo getGatewayInfoByGwClusterName(String gwClusterName) {
		Map<String, Object> params = new HashMap<>();
		params.put("gwClusterName", gwClusterName);
		List<GatewayInfo> gatewayInfoList = gatewayInfoDao.getRecordsByField(params);
		if (gatewayInfoList.size() == 0) {
			return null;
		} else {
			return gatewayInfoList.get(0);
		}
	}

	@Override
	public boolean updateGwInfo(GatewayInfo gatewayInfo, boolean updateProjectId) {
		if (gatewayInfo == null) {
			return false;
		}
		gatewayInfo.setModifyDate(System.currentTimeMillis());
		EnvoyVirtualHostInfo vhHost = envoyGatewayService.getVirtualHostByGwIdAndProjectId(gatewayInfo.getId(),
			                                                                                  gatewayInfo
				                                                                                  .getProjectId());
		envoyGatewayService.updateVirtualHost(vhHost.getId(), gatewayInfo.getHostList());
		return 1 == gatewayInfoDao.update(gatewayInfo);
	}

	@Override
	public List<GatewayInfo> findAll() {
		return gatewayInfoDao.findAll();
	}

	@Override
	public List<GatewayInfo> findGatewayByLimit(String pattern, long offset, long limit) {
		List<GatewayInfo> gatewayInfoList = gatewayInfoDao.getGatewayInfoByLimit(pattern, offset, limit);
		gatewayInfoList.forEach(item -> {
			item.setHostList(
				envoyGatewayService.getVirtualHostByGwIdAndProjectId(item.getId(), item.getProjectId()).getHostList());
		});
		return gatewayInfoList;
	}

	@Override
	public long getGatewayCount(String pattern) {
		if (StringUtils.isNotBlank(pattern)) {
			return gatewayInfoDao.getGatewayInfoCountsByPattern(pattern);
		}
		return gatewayInfoDao.getCountByFields(new HashMap<String, Object>());
	}

	/**
	 * 当前项目下是否存在相同的网关名称
	 */
	@Override
	public boolean isExistGwInstance(String gwName) {
		Map<String, Object> params = new HashMap<>();
		params.put("gwName", gwName);
		return gatewayInfoDao.getCountByFields(params) == 0 ? false : true;
	}

	@Override
	public boolean isGwExists(long gwId) {
		Map<String, Object> params = new HashMap<>();
		params.put("id", gwId);
		return gatewayInfoDao.getCountByFields(params) == 0 ? false : true;
	}

	@Override
	public ErrorCode checkGwIdParam(String gwId) {
		if (StringUtils.isBlank(gwId)) {
			logger.info("请求GwId为空");
			return CommonErrorCode.MissingParameter("GwId");
		}
		if (!isGwExists(NumberUtils.toLong(gwId))) {
			logger.info("请求GwId不存在，gwId:{]", gwId);
			return CommonErrorCode.InvalidParameterValue(gwId, "GwId");
		}
		return CommonErrorCode.Success;
	}

	@Override
	public long addGatewayByMetaDto(GatewayDto gatewayDto) {
		GatewayInfo gatewayInfo = GatewayDto.toMeta(gatewayDto);
		gatewayInfo.setCreateDate(System.currentTimeMillis());
		gatewayInfo.setModifyDate(System.currentTimeMillis());
		long gwId = gatewayInfoDao.add(gatewayInfo);
		EnvoyVirtualHostInfo virtualHostInfo = new EnvoyVirtualHostInfo(gatewayInfo.getProjectId(),
				gwId, gatewayInfo.getHostList());
		envoyGatewayService.createVirtualHost(virtualHostInfo);
		return gwId;
	}

	@Override
	public ErrorCode checkAddParam(GatewayDto gatewayDto) {
		if (isExistGwInstance(gatewayDto.getGwName())) {
			return CommonErrorCode.GwNameAlreadyExist;
		}
		return checkCommonParam(gatewayDto);
	}


	@Override
	public ErrorCode checkUpdateParam(GatewayDto gatewayDto) {
		GatewayInfo gatewayInDb = gatewayInfoDao.get(gatewayDto.getId());
		if (gatewayInDb == null) {
			return CommonErrorCode.NoSuchGateway;
		}
		GatewayInfo gatewayInfo = getGatewayByName(gatewayDto.getGwName());
		if (gatewayInfo != null && gatewayInfo.getId() != gatewayDto.getId()) {
			return CommonErrorCode.GwNameAlreadyExist;
		}
		return checkCommonParam(gatewayDto);
	}

	@Override
	public List<Long> getGwIdListByNameFuzzy(String gwName, long projectId) {
		List<Long> gwIdList = gatewayInfoDao.getGwIdListByNameFuzzy(gwName, projectId);
		return CollectionUtils.isEmpty(gwIdList) ? Lists.newArrayList() : gwIdList;
	}

	@Override
	public List<GatewayInfo> getGatewayInfoList(List<Long> gwIdList) {
		if (CollectionUtils.isEmpty(gwIdList)) {
			return Lists.newArrayList();
		}

		List<GatewayInfo> gatewayInfoList = gatewayInfoDao.getGatewayInfoList(gwIdList);
		return CollectionUtils.isEmpty(gatewayInfoList) ? Lists.newArrayList() : gatewayInfoList;
	}

	public GatewayInfo getGatewayByClusterName(String clusterName) {
		Map<String, Object> params = new HashMap<>();
		params.put("gwClusterName", clusterName);
		List<GatewayInfo> recordsByField = gatewayInfoDao.getRecordsByField(params);
		return (CollectionUtils.isEmpty(recordsByField)) ? null : recordsByField.get(0);
	}

	private ErrorCode checkCommonParam(GatewayDto gatewayDto) {
		//envoy网关
		if (Const.ENVOY_GATEWAY_TYPE.equals(gatewayDto.getGwType())) {
			if (StringUtils.isBlank(gatewayDto.getApiPlaneAddr())) {
				logger.info("创建/修改envoy网关，api-plane地址为空");
				return CommonErrorCode.MissingParameter("ApiPlaneAddr");
			}
			if (StringUtils.isBlank(gatewayDto.getGwClusterName())) {
				logger.info("创建/修改envoy网关，gw-cluster地址为空");
				return CommonErrorCode.MissingParameter("GwClusterName");
			}
			GatewayInfo sameClusterGateway = getGatewayByClusterName(gatewayDto.getGwClusterName());
			if (null != sameClusterGateway && sameClusterGateway.getId() != gatewayDto.getId()) {
				logger.info("创建/修改envoy网关，gw-cluster地址已存在");
				return CommonErrorCode.SameNameGatewayClusterExists;
			}
			if (CollectionUtils.isEmpty(gatewayDto.getHostList())) {
				logger.info("创建/修改envoy网关，域名列表不允许为空!");
				return CommonErrorCode.InvalidParameterValue(gatewayDto.getHostList(), "Hosts", "域名列表不允许为空");
			}
		}
		return CommonErrorCode.Success;
	}

}
