package org.hango.cloud.dashboard.envoy.service.impl;

import org.hango.cloud.gdashboard.api.service.IGetProjectIdService;
import org.hango.cloud.dashboard.envoy.web.holder.ProjectTraceHolder;
import org.springframework.stereotype.Service;

@Service
public class GetProjectIdServiceImpl implements IGetProjectIdService {

	@Override
	public long getProjectId() {
		return ProjectTraceHolder.getProId();
	}

}
