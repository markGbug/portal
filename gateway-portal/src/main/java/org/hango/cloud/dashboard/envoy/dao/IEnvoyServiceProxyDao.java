package org.hango.cloud.dashboard.envoy.dao;

import org.hango.cloud.dashboard.envoy.meta.EnvoyServiceProxyInfo;

import java.util.List;

public interface IEnvoyServiceProxyDao extends IBaseDao<EnvoyServiceProxyInfo> {

	/**
	 * 分页查询envoy网关发布（关联）服务相关
	 *
	 * @param gwId      网关id
	 * @param serviceId 分页查询服务元数据id
	 * @param projectId 项目id
	 * @param offset    分页查询offset
	 * @param limit     分页查询limit
	 *
	 * @return 查询envoy网关服务元数据list
	 */
	List<EnvoyServiceProxyInfo> getServiceProxyByLimit(long gwId, long serviceId, long projectId, long offset,
	                                                   long limit);

	/**
	 * 根据网关id、服务id列表批量查询已发布服务信息
	 *
	 * @param gwId          网关id
	 * @param serviceIdList serviceIdList 服务id列表
	 *
	 * @return {@link List<EnvoyServiceProxyInfo>} 指定网关中指定服务列表的所有已发布服务信息
	 */
	List<EnvoyServiceProxyInfo> batchGetServiceProxyList(long gwId, List<Long> serviceIdList);

}
