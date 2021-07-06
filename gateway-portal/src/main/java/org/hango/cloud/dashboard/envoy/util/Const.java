package org.hango.cloud.ncegdashboard.envoy.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * global constants.
 *
 */
public class Const {

	// 当不确定HashMap的初始值大小多少合适值，使用该值
	public static final int DEFAULT_MAP_SIZE = 16;

	public static final String DEFAULT_ENCODING = "utf-8";

	public static final String DEFAULT_CONTENT_TYPE = "application/json";


	public static final String UNKNOWN_STRING = "unknown";


	/**
	 * Http Method Type
	 */
	public static final String GET_METHOD = "GET";

	public static final String POST_METHOD = "POST";

	public static final String PUT_METHOD = "PUT";

	public static final String HEAD_METHOD = "HEAD";

	public static final String DELETE_METHOD = "DELETE";

	public static final String OPTIONS_METHOD = "OPTIONS";

	public static final String SP = "##";

	public static final int OK = 200;

	/**
	 * 服务名称：必填，支持中文、数字、英文大小写、中划线、下划线，最大长度32字符
	 */
	public static final String REGEX_SERVICE_NAME = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\.]{1,32}";

	/**
	 * 服务标识，必填，支持英文小写，数字，最长64字符
	 */
	public static final String REGEX_SERVICE_TAG = "^[a-z_\\-\\/A-Z0-9]{1,63}";

	/**
	 * 备注信息，选填，支持全文本，最长64字符
	 */
	public static final String REGEX_DESCRIPTION = "^[\\s\\S]{0,200}";

	public static final String REGEX_HEALTH_INTERFACE = "(/\\S{0,200}){0,1}";

	/**
	 * 网关名称：必填，支持中文，数字，英文大小写，中划线，下划线，最大长度32字符
	 */
	public static final String REGEX_GATEWAY_NAME = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-]{1,32}";
	/**
	 * 网关管理
	 */

	/**
	 * 网关地址，必填，必须与网关实际配置一样；最大长度64字符
	 */
	public static final String REGEX_GATEWAY_URL
		= "(https?|http)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]";

	/**
	 * 健康检查接口，必填，必须以/开头，支持英文大小写，数字，url规范，最大长度64字符
	 */
	public static final String REGEX_HEALTH = "^[/][\\S]{1,64}";


	//服务类型
	public static final String REGEX_SERVICE_TYPE = "http";

	//公共参数
	public static final String ACTION = "Action";

	public static final String VERSION = "Version";



	//记录操作日志时，区分对象类型
	public static final String API = "api";


	public static final String ENVOY_GATEWAY_PREFIX = "/gdashboard/envoy";

	/**
	 * 参数类型，分为REQUEST和RESPONSE和QUERYSTRING
	 */
	public static final String REQUEST_PARAM_TYPE = "REQUEST";

	public static final String RESPONSE_PARAM_TYPE = "RESPONSE";

	public static final String QUERYSTRING_PARAM_TYPE = "QUERYSTRING";


	/**
	 * envoy元数据服务名称，必填，支持任意字符，32位
	 */
	public static final String REGEX_ENVOY_SERVICE_NAME = "^[\\s\\S]{1,64}";

	public static final String STATIC_PUBLISH_TYPE = "STATIC";

	public static final String DYNAMIC_PUBLISH_TYPE = "DYNAMIC";

	/**
	 * 使能状态
	 */
	public static final String ROUTE_RULE_ENABLE_STATE = "enable";

	public static final String ROUTE_RULE_DISABLE_STATE = "disable";

	public static final long ERROR_RESULT = -1;

	public static final String desKey = "#%^1*&(*HRqzlUn]";

	/**
	 * nginx捕获相关正则，非标准正则，需要在创建路由中提示
	 */
	public static final String NGINX_CAPTURE_REGEX = ".*\\?<.*>.*";

	public static final String ENVOY_GATEWAY_TYPE = "envoy";

	public static final Set<String> CONST_METHODS = new HashSet<>(
		Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "PATCH", "OPTIONS", "TRACE", "CONNECT"));

	public static final Set<String> SORT_KEY = new HashSet<>(Arrays.asList("create_time", "priority"));

	public static final Set<String> SORT_VALUE = new HashSet<>(Arrays.asList("desc", "asc"));

	public static final String URI_TYPE_EXACT = "exact";

	public static final String URI_TYPE_PREFIX = "prefix";

	public static final String URI_TYPE_REGEX = "regex";

	public static String CONST_PRIORITY = "priority";

	public static String CONST_DESC = "desc";

	public static String CONST_ASC = "asc";

}
