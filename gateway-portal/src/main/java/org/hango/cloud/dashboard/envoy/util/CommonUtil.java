package org.hango.cloud.ncegdashboard.envoy.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.web.holder.ProjectTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Date: 创建时间: 2018/2/8 下午3:11.
 */
public class CommonUtil {

	/**
	 * IPV4格式 1~255.0~255.0~255.0~255
	 */
	private static final String IPV4_BASIC_PATTERN_STRING = "(([1-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){1}"
	                                                        + "(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.)"
	                                                        + "{2}"
	                                                        + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])";

	private static final Pattern IPV4_PATTERN = Pattern.compile("^" + IPV4_BASIC_PATTERN_STRING + "$");

	private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

	public static String getRegexFromApi(String apiPath) {
		//1.去除path中带的参数
		if (apiPath.contains("?")) {
			String pathString[] = apiPath.split("\\?");
			apiPath = pathString[0];
		}
		//2.将path中的{tenantId}替换成*
		String regex = apiPath.replaceAll("\\{[^}]*\\}", "*");

		return regex;
	}

	/**
	 * 判断请求的projectId是否存在且正确
	 * 目前正确与否根据是否存在进行判断，dashboard不对有效性进行判断
	 */
	public static boolean checkProjectId() {
		return
			ProjectTraceHolder.DEFAULT_PROJECT_ID == ProjectTraceHolder.getProId() || ProjectTraceHolder.getProId() < 0
			? false : true;
	}

	public static void responseWrite(HttpServletResponse httpServletResponse, int httpStatus, String message) {
		httpServletResponse.setCharacterEncoding(Charsets.UTF_8.name());
		httpServletResponse.setContentType(MappingJackson2JsonView.DEFAULT_CONTENT_TYPE);
		httpServletResponse.setStatus(httpStatus);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("code", httpStatus);
		map.put("message", message);
		String responseBody = JSON.toJSONString(map);

		try {
			httpServletResponse.getWriter().write(responseBody);
		} catch (Exception e) {
			logger.error("response写回出现异常，请排查{}", e.getMessage());
		}
	}

	/**
	 * 获取IP
	 */
	public static String getIp(HttpServletRequest request) {
		String xip = request.getHeader("X-Real-IP");
		String xFor = request.getHeader("X-Forwarded-For");
		if (StringUtils.isNotEmpty(xFor) && Const.UNKNOWN_STRING.equalsIgnoreCase(xFor)) {
			// 多次反向代理后会有多个ip值，第一个ip才是真实ip
			int index = xFor.indexOf(",");
			if (index != -1) {
				return xFor.substring(0, index);
			} else {
				return xFor;
			}
		}
		xFor = xip;
		if (StringUtils.isNotEmpty(xFor) && !Const.UNKNOWN_STRING.equalsIgnoreCase(xFor)) {
			return xFor;
		}
		if (StringUtils.isBlank(xFor) || Const.UNKNOWN_STRING.equalsIgnoreCase(xFor)) {
			xFor = request.getHeader("Proxy-Client-IP");
		}
		if (StringUtils.isBlank(xFor) || Const.UNKNOWN_STRING.equalsIgnoreCase(xFor)) {
			xFor = request.getHeader("WL-Proxy-Client-IP");
		}
		if (StringUtils.isBlank(xFor) || Const.UNKNOWN_STRING.equalsIgnoreCase(xFor)) {
			xFor = request.getHeader("HTTP_CLIENT_IP");
		}
		if (StringUtils.isBlank(xFor) || Const.UNKNOWN_STRING.equalsIgnoreCase(xFor)) {
			xFor = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (StringUtils.isBlank(xFor) || Const.UNKNOWN_STRING.equalsIgnoreCase(xFor)) {
			xFor = request.getRemoteAddr();
		}
		return xFor;
	}

	public static ErrorCode checkOffsetAndLimit(long offset, long limit) {
		if (offset < 0) {
			return CommonErrorCode.InvalidParameter(String.valueOf(offset), "Offset");
		} else if (limit < 0 || limit > 1000) {
			return CommonErrorCode.InvalidParameter(String.valueOf(limit), "Limit");
		}
		return CommonErrorCode.Success;
	}

	/**
	 * 检查IP、IP网段格式是否正确
	 *
	 * @param ipAddress IP地址或网段
	 *
	 * @return true:格式正确 false:格式错误
	 */
	public static boolean ipCheck(String ipAddress) {
		if (ipAddress.indexOf('/') > 0) {
			try {
				String[] addressAndMask = StringUtils.split(ipAddress, "/");
				ipAddress = addressAndMask[0];
				int nMaskBits = Integer.parseInt(addressAndMask[1]);
				if (nMaskBits < 0 || nMaskBits > 32) {
					return false;
				}
				return IPV4_PATTERN.matcher(ipAddress).matches();
			} catch (Exception e) {
				logger.info("解析IP地址格式时发生异常!", e);
				return false;
			}
		}
		return IPV4_PATTERN.matcher(ipAddress).matches();
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

}
