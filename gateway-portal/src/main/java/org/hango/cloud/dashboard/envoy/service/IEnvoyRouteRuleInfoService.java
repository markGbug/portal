package org.hango.cloud.ncegdashboard.envoy.service;

import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCodeEnum;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyRouteRuleInfo;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyCopyRuleDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyRouteRuleDto;

import java.util.List;

/**
 * 路由规则管理Service层接口
 * <p>
 * 2019-09-11
 */
public interface IEnvoyRouteRuleInfoService {

	/**
	 * 添加路由规则参数校验
	 *
	 * @param routeRuleInfo 路由规则信息
	 *
	 * @return {@link ErrorCodeEnum#Success} ErrorCodeEnum.Success 即参数校验成功，否则参数校验失败并返回对应的错误码
	 */
	ErrorCode checkAddParam(EnvoyRouteRuleDto routeRuleInfo);

	/**
	 * 查询是否具有完全相同的路由规则，如果完全相同，不允许创建/修改
	 *
	 * @param envoyRouteRuleInfo envoyRouteRuleInfo 路由规则info信息
	 *
	 * @return boolean true 存在相同，false 不存在相同
	 */
	boolean isSameRouteRuleInfo(EnvoyRouteRuleInfo envoyRouteRuleInfo);

	/**
	 * 添加路由规则元信息
	 *
	 * @param routeRuleInfo 路由规则信息
	 *
	 * @return 路由规则在表中的主键id
	 */
	long addRouteRule(EnvoyRouteRuleInfo routeRuleInfo);

	/**
	 * 根据路由规则名称查询规则信息
	 *
	 * @param routeRuleName 路由规则名称
	 *
	 * @return {@link EnvoyRouteRuleInfo} 规则详情
	 */
	EnvoyRouteRuleInfo getRouteRuleInfoByName(String routeRuleName);

	/**
	 * 更新路由规则时的参数校验
	 *
	 * @param routeRuleDto 更新后的路由规则信息
	 *
	 * @return {@link ErrorCodeEnum#Success} ErrorCodeEnum.Success 即参数校验成功，否则参数校验失败并返回对应的错误码
	 */
	ErrorCode checkUpdateParam(EnvoyRouteRuleDto routeRuleDto);

	/**
	 * 删除路由规则时参数校验
	 *
	 * @param id 路由规则id
	 *
	 * @return {@link ErrorCode}
	 */
	ErrorCode checkDeleteParam(long id);

	/**
	 * 更新路由规则，只有当传入参数非空时才允许更新
	 *
	 * @param envoyRouteRuleInfo 需要更新的路由规则
	 *
	 * @return ture:更新成功； false:更新失败
	 */
	boolean updateRouteRule(EnvoyRouteRuleInfo envoyRouteRuleInfo);

	/**
	 * 根据路由规则id查询路由规则详情
	 *
	 * @param id 路由规则id
	 *
	 * @return {@link EnvoyRouteRuleInfo} 路由规则响应
	 */
	EnvoyRouteRuleInfo getRouteRuleInfoById(long id);

	/**
	 * 路由规则查询参数校验
	 *
	 * @param sortKey   查询key
	 * @param sortValue 查询value
	 * @param offset    查询offset
	 * @param limit     查询limit
	 *
	 * @return 参数校验结果
	 */
	ErrorCode checkDescribeParam(String sortKey, String sortValue, long offset, long limit);

	/**
	 * 分页获取路由规则
	 *
	 * @param pattern       路由规则模糊匹配,包括路由规则名称，path, host
	 * @param publishStatus 路由发布状态
	 * @param serviceId     服务id
	 * @param projectId     路由规则项目id
	 * @param sortKey       路由规则查询sortKey(create_time, priority)
	 * @param sortValue     路由规则查询sortValue（desc, asc）
	 * @param offset        分页查询offset
	 * @param limit         分页查询limit
	 *
	 * @return {@link List<EnvoyRouteRuleInfo>} 路由规则列表
	 */
	List<EnvoyRouteRuleInfo> getRouteRuleInfoByPattern(String pattern, int publishStatus, long serviceId,
	                                                   long projectId, String sortKey, String sortValue, long offset,
	                                                   long limit);

	/**
	 * 分页获取路由规则数量
	 *
	 * @param pattern       路由规则模糊匹配。包括路由规则名称，path,host
	 * @param publishStatus 路由发布状态
	 * @param serviceId     服务id
	 * @param projectId     路由规则所属项目id
	 *
	 * @return 查询数量
	 */
	long getRouteRuleInfoCount(String pattern, int publishStatus, long serviceId, long projectId);

	/**
	 * 根据路由规则id删除路由规则
	 *
	 * @param id 路由规则id
	 *
	 * @return 删除路由规则结果 true代表删除成功
	 */
	boolean deleteRouteRule(long id);

	/**
	 * 根据路由规则info生成dto信息
	 *
	 * @param ruleInfo {@link EnvoyRouteRuleInfo}
	 *
	 * @return {@link EnvoyRouteRuleDto}
	 */
	EnvoyRouteRuleDto fromMeta(EnvoyRouteRuleInfo ruleInfo);

	/**
	 * 根据路由规则名称模糊查询满足匹配条件的id列表
	 *
	 * @param routeRuleName 路由规则名称，支持模糊查询
	 * @param projectId     项目id
	 *
	 * @return {@link List<Long>} 满足条件的id列表
	 */
	List<Long> getRouteRuleIdListByNameFuzzy(String routeRuleName, long projectId);

	/**
	 * 根据路由规则id列表查询路由规则详情列表
	 *
	 * @param routeRuleIdList 路由规则id列表
	 *
	 * @return {@link List<EnvoyRouteRuleInfo>} 路由规则详情列表
	 */
	List<EnvoyRouteRuleInfo> getRouteRuleList(List<Long> routeRuleIdList);

}