package org.hango.cloud.ncegdashboard.envoy.web.dto;

/**
 * @version 1.0
 * @Type
 * @Desc
 * @date 2020/3/23
 */
public class PluginOrderItemDto {

	private boolean enable;

	private String name;

	private Object settings;

	public boolean getEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getSettings() {
		return settings;
	}

	public void setSettings(Object settings) {
		this.settings = settings;
	}

}
