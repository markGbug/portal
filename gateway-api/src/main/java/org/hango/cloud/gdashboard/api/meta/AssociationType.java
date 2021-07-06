package org.hango.cloud.gdashboard.api.meta;

/**
 * @version 1.0
 * @Type
 * @Desc
 * @date 2018/11/28
 */
public enum AssociationType {
	NORMAL;

	AssociationType() {
	}

	public static String getType(String associationType) {
		for (AssociationType value : AssociationType.values()) {
			if (value.name().equals(associationType)) {
				return value.name();
			}
		}
		return null;
	}
}
