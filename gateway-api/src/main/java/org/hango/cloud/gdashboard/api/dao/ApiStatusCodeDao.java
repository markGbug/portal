package org.hango.cloud.gdashboard.api.dao;

import org.hango.cloud.gdashboard.api.meta.ApiStatusCode;

/**
 * @Date: 创建时间: 2018/4/24 14:50.
 */
public interface ApiStatusCodeDao extends IBaseDao<ApiStatusCode> {

	/**
	 * 根据ObjectId来删除对应的status code
	 */
	int delete(long objectId, String type);

}
