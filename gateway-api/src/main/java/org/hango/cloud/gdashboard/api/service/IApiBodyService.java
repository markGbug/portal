package org.hango.cloud.gdashboard.api.service;

import org.hango.cloud.gdashboard.api.dto.ApiBodysDto;
import org.hango.cloud.gdashboard.api.dto.ApiStatusCodesDto;
import org.hango.cloud.gdashboard.api.meta.ApiBody;
import org.hango.cloud.gdashboard.api.meta.ApiStatusCode;
import org.hango.cloud.gdashboard.api.meta.errorcode.ApiErrorCode;

import java.util.List;

public interface IApiBodyService {

	/**
	 * 通过ApiBodysDto生成ApiBody List
	 *
	 * @param apiBodysDto apiBody包装类型
	 * @param type        body类型，Request:RequestBody,Response:ResponseBody,QueryString
	 */
	List<ApiBody> generateApiBodyFromApiBodyList(ApiBodysDto apiBodysDto, String type);

	/**
	 * 通过ApiStatusCodeDto生成Api satus code list
	 *
	 * @param apiStatusCodesDto StatusCode包租黄类型
	 */
	List<ApiStatusCode> generateApiStatusCodeFromCodeList(ApiStatusCodesDto apiStatusCodesDto);

	/**
	 * ApiBodysDto参数校验，主要校验paramType id是否存在，不存在不允许创建
	 *
	 * @param apiBodysDto apiBody包装dto
	 *
	 * @return 参数校验结果
	 */
	ApiErrorCode checkApiBodyBasicInfo(ApiBodysDto apiBodysDto);

	/**
	 * 批量插入status code
	 *
	 * @param apiStatusCodes statusCode list
	 * @param objectId       apiId
	 */
	long addStatusCodes(List<ApiStatusCode> apiStatusCodes, long objectId, String type);

	/**
	 * 按照类型查找status code
	 *
	 * @param objectId 接口APIID
	 * @param type     类型，确定为API
	 */
	List<ApiStatusCode> listStatusCode(long objectId, String type);

	/**
	 * 查询restful风格的status code
	 */
	List<ApiStatusCode> listStatusCode(long apiId);

	/**
	 * 添加状态码
	 */
	long addStatusCode(ApiStatusCode apiStatusCode);

	/**
	 * 批量添加status code 提供修数据
	 */
	List<Long> addStatusCode(List<ApiStatusCode> apiStatusCodeList);

	/**
	 * 删除状态码
	 */
	void deleteStatusCode(long objectId, String type);

	/**
	 * 插入ApiBody
	 */
	long addBody(ApiBody apiBody);

	/**
	 * 通过类型获取ApiBody
	 */
	List<ApiBody> getBody(long apiId, String type);

	/**
	 * 根据API id和type删除Body
	 *
	 * @param apiId apiId
	 * @param type  Type Request,Response
	 */
	void deleteBody(long apiId, String type);

	/**
	 * 根据API id删除Body
	 *
	 * @param apiId apiId
	 */
	void deleteBody(long apiId);

	/**
	 * 删除body参数
	 */
	void deleteBodyParam(long paramId);

	/**
	 * 批量添加APIbody
	 */
	List<Long> addBody(List<ApiBody> apiBodyList);

	/**
	 * 判断body param 是否已经存在
	 */
	boolean getBodyParam(String paramName, String type, String apiId);

	/**
	 * 获取body列表
	 */
	List<ApiBody> listBodyParam();

	/**
	 * 提供值修数据，更新API Body
	 */
	long updateBodyParam(ApiBody apiBody);

	/**
	 * 查询API Req/Resp Body,QueryString,
	 */
	List<ApiBody> getBody(String apiId);

}
