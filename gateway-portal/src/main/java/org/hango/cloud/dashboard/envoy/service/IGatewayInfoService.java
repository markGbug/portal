package org.hango.cloud.dashboard.envoy.service;

import org.hango.cloud.dashboard.envoy.web.dto.GatewayDto;
import org.hango.cloud.dashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;

import java.util.List;

/**
 * @Date: 创建时间: 2018/1/17 下午5:26.
 */
public interface IGatewayInfoService {

	/**
	 * 根据Id获取某个环境信息
	 */
	GatewayInfo get(long id);

	/**
	 * 通过网关名称获取网关
	 */
	GatewayInfo getGatewayByName(String gwName);

	/**
	 * 用于envoy网关，通过gwClusterName查询网关信息
	 *
	 * @param gwClusterName 网关集群名称
	 *
	 * @return {@link GatewayInfo} 网关信息
	 */
	GatewayInfo getGatewayInfoByGwClusterName(String gwClusterName);

	/**
	 * 更新环境信息
	 *
	 * @param updateProjectId Envoy网关是否更新project_id字段
	 */
	boolean updateGwInfo(GatewayInfo gatewayInfo, boolean updateProjectId);

	/**
	 * 获取所有网关
	 */
	List<GatewayInfo> findAll();

	/**
	 * 分页获取所有网关
	 *
	 * @param pattern 支持根据网关名称进行模糊匹配
	 */
	List<GatewayInfo> findGatewayByLimit(String pattern, long offset, long limit);

	/**
	 * 获取网关数量
	 */
	long getGatewayCount(String pattern);

	boolean isExistGwInstance(String gwName);

	boolean isGwExists(long gwId);

	ErrorCode checkGwIdParam(String gwId);

	long addGatewayByMetaDto(GatewayDto gatewayDto);


	/**
	 * 创建网关参数校验
	 *
	 * @param gatewayDto 创建网关dto
	 *
	 * @return 返回校验结果，{@link ErrorCode # Success}
	 */
	ErrorCode checkAddParam(GatewayDto gatewayDto);

	/**
	 * 更新网关参数校验
	 *
	 * @param gatewayDto 网关dto
	 *
	 * @return 返回校验结果，{@link ErrorCode # Success}
	 */
	ErrorCode checkUpdateParam(GatewayDto gatewayDto);

	/**
	 * 根据gwName查询满足条件网关id列表
	 *
	 * @param gwName    网关名称，支持模糊查询
	 * @param projectId 项目id
	 *
	 * @return {@link List<Long>} 满足条件的网关id列表
	 */
	List<Long> getGwIdListByNameFuzzy(String gwName, long projectId);

	/**
	 * 根据网关id列表查询网关信息列表
	 *
	 * @param gwIdList 网关id列表
	 *
	 * @return {@link List<GatewayInfo>} 网关信息列表
	 */
	List<GatewayInfo> getGatewayInfoList(List<Long> gwIdList);

}

