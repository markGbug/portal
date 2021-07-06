package org.hango.cloud.dashboard.envoy.meta;

/**
 * @version 1.0
 * @Type
 * @Desc
 * @date 2019/8/18
 */
public enum RegistryCenterEnum {

	Kubernetes("Kubernetes", "%s");

	private String type;

	private String suffix;

	RegistryCenterEnum(String type, String suffix) {
		this.type = type;
		this.suffix = suffix;
	}

	public String getType() {
		return type;
	}

	public String getSuffix() {
		return suffix;
	}

	public static RegistryCenterEnum get(String type) {
		for (RegistryCenterEnum value : RegistryCenterEnum.values()) {
			if (value.getType().equals(type)) {
				return value;
			}
		}
		return null;
	}
}
