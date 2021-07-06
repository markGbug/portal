package org.hango.cloud.gdashboard.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import org.hango.cloud.gdashboard.api.meta.ApiBody;

import java.util.List;
import java.util.Map;

/**
 * 将API转换成普通json对象，以及swagger原生json对象
 *
 * @Date: 创建时间: 2018/4/19 19:52.
 */
public interface IApiConvertToJsonService {

	/**
	 * 将body转换成Map，为了生成json格式
	 */
	Map generateJsonForApi(long apiId, String type);

	/**
	 * 将request body转换成swagger 中 BodyParameter 对象
	 */
	BodyParameter convertRequestBodyToSwagger(long apiId, Swagger swagger);

	/**
	 * 将response body转换成swagger 中 ObjectProperty 对象
	 */
	Map<String, Object> convertResponseBodyToSwagger(long apiId, Swagger swagger);

	/**
	 * 将返回码status code转换成swagger response对象
	 */
	void convertStatusCodeToSwaggerResponse(Operation operation, long apiId);

	/**
	 * 生成swagger Operation对象，Operation对象对应swagger文档页面上的基本元素
	 */
	Operation generateOperstaionForSwagger(long apiId, Swagger swagger);

	/**
	 * 生成swagger json
	 */
	String generateSwaggerJson(long apiId) throws JsonProcessingException;

	/**
	 * 根据传入的json生成ApiBody对象
	 */
	List<ApiBody> generateApiBodyByJson(long apiId, long serviceId, Map<String, Object> params, String type);

}
