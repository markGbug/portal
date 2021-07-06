package org.hango.cloud.gdashboard.api.dao;

import org.hango.cloud.gdashboard.api.meta.ApiModelParam;

/**
 * @Date: 创建时间: 2018/1/2 16:08.
 */
public interface ApiModelParamDao extends IBaseDao<ApiModelParam> {

	/**
	 * 根据ModelId删除模型参数
	 */
	long deleteApiModelParamByModelId(long modelId);

}
