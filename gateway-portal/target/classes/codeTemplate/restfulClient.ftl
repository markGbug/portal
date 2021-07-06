package ${packageName};
import java.io.IOException;

import org.hango.cloud.restful.sdk.core.http.APIRequestClient;
<#list apiNameList as attr>
import org.hango.cloud.${serviceName? lower_case}.model.${attr}Request;
import org.hango.cloud.${serviceName? lower_case}.model.${attr}Response;
</#list>

/**
 *
 */
public class ${serviceName?cap_first}Client extends APIRequestClient{

<#list apiNameList as attr>
	public ${attr? cap_first}Response ${attr?uncap_first}(${attr? cap_first}Request ${attr?uncap_first}Request) {
		return super.invoke(${attr?uncap_first}Request,${attr? cap_first}Response.class);
	}
</#list>

}