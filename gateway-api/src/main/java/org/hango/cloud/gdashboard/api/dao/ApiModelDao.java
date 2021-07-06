package org.hango.cloud.gdashboard.api.dao;

import org.hango.cloud.gdashboard.api.meta.ApiModel;

import java.util.List;

/**
 * @Date: 创建时间: 2018/1/2 15:47.
 */
public interface ApiModelDao extends IBaseDao<ApiModel> {

	/**
	 * 分页获取数据库中API model数据
	 */
	List<ApiModel> findApiModelByProjectLimit(long projectId, long offset, long limit, String pattern);

	/**
	 * 通过分页serviceId，分页获取数据库中APi model数据
	 */
	List<ApiModel> findApiModelByServiceIdLimit(long serviceId, long offset, long limit, String pattern);

	/**
	 * 通过项目id和pattern获取model数量
	 */
	long getApiModelCountByProjectPattern(long projectId, String pattern);

	/**
	 * 通过服务id和pattern获取model数量
	 */
	long getApiModelCountByServicePattern(long serviceId, String pattern);

	long deleteApiModelByServiceId(long serviceId);

}
