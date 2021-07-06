package org.hango.cloud.gdashboard.api.dao;

import org.hango.cloud.gdashboard.api.meta.ApiHeader;

import java.util.List;

/**
 * @Date: 创建时间: 2018/1/2 15:46.
 */
public interface ApiHeaderDao extends IBaseDao<ApiHeader> {

	/**
	 * 查询API的Request Header或Response Header
	 */
	List<ApiHeader> getHeader(long apiId, String type);

	/**
	 * 查询Header中的某个参数
	 */
	List<ApiHeader> getHeaderParam(String paramName, String type, long apiId);

	/**
	 * 根据参数Id删除该参数
	 */
	void deleteHeaderParam(long paramId);

	/**
	 * 根据apiId删除Header
	 */
	void deleteHeader(long apiId);

	/**
	 * 根据apiId删除Request Header 或Response Header
	 */
	void deleteHeader(long apiId, String type);

	/**
	 * 根据ApiId查询
	 */
	List<ApiHeader> getHeaderByApiId(long apiId);

}
