package org.hango.cloud.ncegdashboard.envoy.service;

import org.hango.cloud.ncegdashboard.envoy.innerdto.EnvoyPublishServiceDto;
import org.hango.cloud.ncegdashboard.envoy.innerdto.EnvoyServiceWithPortDto;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyHealthCheckRuleInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyRouteRuleProxyInfo;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyServiceProxyDto;

import java.util.List;

public interface IGetFromApiPlaneService {

	/**
	 * 获取api-plane对应集群中的服务
	 *
	 * @param name                模糊查询服务名
	 * @param gwId                网关id（根据id查询网关所属的api-plane）
	 * @param registryCenterType  注册中心类型
	 *
	 * @return 服务list
	 */
	List<EnvoyServiceWithPortDto> getServiceListFromApiPlane(long gwId, String name, String registryCenterType);

	/**
	 * 发布服务，网关服务元数据和envoy网关服务产生关联
	 *
	 * @param envoyServiceProxyDto envoy发布服务DTO
	 *
	 * @return 服务发布id
	 */
	boolean publishServiceByApiPlane(EnvoyServiceProxyDto envoyServiceProxyDto,
	                                 EnvoyHealthCheckRuleInfo envoyHealthCheckRuleInfo);

	/**
	 * 通过api-plane 下线服务
	 *
	 * @param apiPlaneAddr api-plane地址
	 *
	 * @return 下线结果
	 */
	boolean offlineServiceByApiPlane(String apiPlaneAddr, EnvoyPublishServiceDto envoyPublishServiceDto);

	/**
	 * 通过api-plane 发布服务
	 *
	 * @param routeRuleProxyInfo   路由ProxyInfo
	 * @param pluginConfigurations 插件配置
	 *
	 * @return 发布结果
	 */
	boolean publishRouteRuleByApiPlane(EnvoyRouteRuleProxyInfo routeRuleProxyInfo, List<String> pluginConfigurations);

	/**
	 * 通过api-plane 下线路由
	 *
	 * @param routeRuleProxyInfo routeRuleInfo
	 *
	 * @return 下线结果 true/fale
	 */
	boolean deleteRouteRuleByApiPlane(EnvoyRouteRuleProxyInfo routeRuleProxyInfo);

}
