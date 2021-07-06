package org.hango.cloud.gdashboard.api.meta;

/**
 * @version 1.0
 * @Type
 * @Desc
 * @date 2018/11/6
 */
public enum ServiceType {

	http;

	/**
	 * 获取服务类别（默认HTTP服务）
	 */
	public static String getByName(String serviceType) {
		for (ServiceType type : ServiceType.values()) {
			if (type.name().equalsIgnoreCase(serviceType)) {
				return type.name();
			}
		}
		return http.name();
	}
}
