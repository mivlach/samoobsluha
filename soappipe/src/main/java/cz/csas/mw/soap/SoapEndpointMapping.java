package cz.csas.mw.soap;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.mapping.AbstractEndpointMapping;

public class SoapEndpointMapping extends AbstractEndpointMapping{

	private SoapEndpoint soapEndpoint;
	
	public void setSoapEndpoint(SoapEndpoint soapEndpoint) {
		this.soapEndpoint = soapEndpoint;
	}

	@Override
	protected Object getEndpointInternal(MessageContext messageContext) throws Exception {
		return soapEndpoint;
	}

}
