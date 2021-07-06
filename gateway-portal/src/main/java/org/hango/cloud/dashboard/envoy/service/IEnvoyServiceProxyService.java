package org.hango.cloud.dashboard.envoy.service;

import org.hango.cloud.dashboard.envoy.web.dto.GatewayDto;
import org.hango.cloud.dashboard.envoy.web.dto.PublishedDetailDto;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.dashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoyServiceProxyDto;
import org.hango.cloud.dashboard.envoy.web.dto.EnvoySubsetDto;

import java.util.List;

public interface IEnvoyServiceProxyService {

	/**
	 * 发布服务，网关服务元数据和envoy网关服务产生关联
	 *
	 * @param envoyServiceProxyDto envoy发布服务DTO
	 *
	 * @return 服务发布id
	 */
	long publishServiceToGw(EnvoyServiceProxyDto envoyServiceProxyDto);

	/**
	 * 更新已发布服务，EncoyServiceProxyInfo
	 *
	 * @param envoyServiceProxyDto envoy更新服务dto
	 *
	 * @return 更新受影响行数
	 */
	long updateServiceToGw(EnvoyServiceProxyDto envoyServiceProxyDto);

	/**
	 * 发布服务，校验服务发布参数
	 *
	 * @param envoyServiceProxyDto envoy发布服务dto
	 *
	 * @return {@link ErrorCode} 参数校验结果
	 */
	ErrorCode checkPublishParam(EnvoyServiceProxyDto envoyServiceProxyDto);

	/**
	 * 更新发布服务，校验更新参数
	 *
	 * @param envoyServiceProxyDto envoy更新发布服务dto
	 *
	 * @return {@link ErrorCode} 更新发布服务
	 */
	ErrorCode checkUpdatePublishParam(EnvoyServiceProxyDto envoyServiceProxyDto);

	/**
	 * 根据服务标识分页查询已发布（关联）至envoy相关服务信息,查询结果包括serviceName以及GatewayName
	 *
	 * @param gwId      网关id
	 * @param serviceId 服务元数据服务id
	 * @param projectId 分页查询项目id
	 * @param offset    分页查询offset
	 * @param limit     分页查询limit
	 *
	 * @return envoyServiceProxyInfo，网关已发布服务
	 */
	List<EnvoyServiceProxyInfo> getEnvoyServiceProxyByLimit(long gwId, long serviceId, long projectId, long offset,
	                                                        long limit);

	/**
	 * 根据服务id获取发布服务数量
	 *
	 * @param gwId      网关id
	 * @param serviceId 服务元数据服务id
	 *
	 * @return 服务发布数量
	 */
	long getServiceProxyCountByLimit(long gwId, long serviceId);

	/**
	 * 根据服务发布id删除服务发布信息
	 *
	 * @param id 服务发布id
	 */
	void deleteServiceProxy(long id);

	/**
	 * 下线已发布服务参数校验
	 *
	 * @param gwId      网关id
	 * @param serviceId 服务id
	 *
	 * @return 参数校验结果，{@link ErrorCode}
	 */
	ErrorCode checkDeleteServiceProxy(long gwId, long serviceId);

	/**
	 * 根据服务发布信息删除服务，需要调用api-plane相关删除crd接口
	 *
	 * @param gwId      网关id
	 * @param serviceId 元数据标识
	 *
	 * @return 返回删除服务发布结果，true:下线成功，false:下线失败
	 */
	boolean deleteServiceProxy(long gwId, long serviceId);

	/**
	 * 根据serviceId和gwId查询已发布服务信息
	 *
	 * @param gwId      网关id
	 * @param serviceId 服务id
	 *
	 * @return {@link EnvoyServiceProxyInfo} 服务发布信息
	 */
	EnvoyServiceProxyInfo getServiceProxyByServiceIdAndGwId(long gwId, long serviceId);

	EnvoyServiceProxyInfo getServiceProxyInterByServiceIdAndGwIds(List<Long> gwIds, long serviceId);

	/**
	 * 根据serviceId,gwId，backendService，publishType查询服务发布信息
	 *
	 * @param gwId           网关id
	 * @param serviceId      服务od
	 * @param backendService 服务发布信息
	 * @param publishType    服务发布类型
	 *
	 * @return {@link EnvoyServiceProxyInfo} 服务发布信息
	 */
	EnvoyServiceProxyInfo getServiceProxyByServicePublishInfo(long gwId, long serviceId, String backendService,
	                                                          String publishType);

	/**
	 * 查询某一服务的已发布服务相关信息，查询结果包括serviceName以及GatewayName
	 */
	List<EnvoyServiceProxyInfo> getServiceProxyByServiceId(long serviceId);

	/**
	 * 通过envoyServiceProxyInfo构造EnvoyServiceProxyDto
	 *
	 * @param envoyServiceProxyInfo EnvoyServiceProxyInfo信息
	 *
	 * @return EnvoyServiceProxyDto
	 */
	EnvoyServiceProxyDto fromMeta(EnvoyServiceProxyInfo envoyServiceProxyInfo);

	/**
	 * 返回包含服务健康状态的DTO
	 *
	 */
	EnvoyServiceProxyDto fromMetaWithStatus(EnvoyServiceProxyInfo envoyServiceProxyInfo);

	/**
	 * 包含返回Port的DTO
	 */
	EnvoyServiceProxyDto fromMetaWithPort(EnvoyServiceProxyInfo envoyServiceProxyInfo);

	/**
	 * 根据服务id查询已发布服务所发布的网关
	 *
	 * @param serviceId 服务id
	 *
	 * @return {@link GatewayDto}
	 */
	List<GatewayDto> getPublishedServiceGateway(long serviceId);

	/**
	 * 查询服务subset是否被已发布路由规则引用，如果存在引用，则返回第一个引用的路由规则名称
	 */
	ErrorCode getRouteRuleNameWithServiceSubset(EnvoyServiceProxyDto envoyServiceProxyDto);


	/**
	 * 当需要将版本信息发送到APIPlane时，采用此方法生成subset，因为subset在DR中的名称和用户输入的不同，需要加上-{gwClusterName}
	 */
	List<EnvoySubsetDto> setSubsetForDtoWhenSendToAPIPlane(EnvoyServiceProxyDto envoyServiceProxyDto,
	                                                       String gwClusterName);

	/**
	 * 根据网关id查询该网关中所有的已发布服务信息
	 *
	 * @param gwId 网关id
	 *
	 * @return {@link List<EnvoyServiceProxyInfo>} 指定网关中的所有已发布服务信息
	 */
	List<EnvoyServiceProxyInfo> getServiceProxyListByGwId(long gwId);

	/**
	 * 根据网关id、服务id列表批量查询已发布服务信息
	 *
	 * @param gwId          网关id
	 * @param serviceIdList 服务id列表
	 *
	 * @return {@link List<EnvoyServiceProxyInfo>} 指定网关中指定服务列表的所有已发布服务信息
	 */
	List<EnvoyServiceProxyInfo> batchGetServiceProxyList(long gwId, List<Long> serviceIdList);

	/**
	 * 当需要发送给APIPlane时BackendService，要根据服务注册中心的类型进行调整
	 */
	String getBackendServiceSendToApiPlane(EnvoyServiceProxyDto envoyServiceProxyDto);

	/**
	 * 获取subsetsName
	 */
	List<String> getSubsetsName(EnvoyServiceProxyInfo serviceProxyInfo);

	List<PublishedDetailDto> getPublishedDetailByService(long serviceId);
}
