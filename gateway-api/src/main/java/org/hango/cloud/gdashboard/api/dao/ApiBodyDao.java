package org.hango.cloud.gdashboard.api.dao;

import org.hango.cloud.gdashboard.api.meta.ApiBody;

import java.util.List;

/**
 * @Date: 创建时间: 2018/1/2 15:46.
 */
public interface ApiBodyDao extends IBaseDao<ApiBody> {

	/**
	 * 获取API Request Body或Response Body
	 */
	List<ApiBody> getBody(long apiId, String type);

	/**
	 * 查询Body中的参数
	 */
	List<ApiBody> getBodyParam(String paramName, String type, long apiId);

	/**
	 * 根据paramId删除参数
	 */
	void delete(long paramId);

	/**
	 * 根据apiId删除body
	 */
	void deleteBody(long apiId);

	void deleteBody(long apiId, String type);

	/**
	 * 查询API Body
	 */
	List<ApiBody> getBodyByApiId(long apiId);

}
