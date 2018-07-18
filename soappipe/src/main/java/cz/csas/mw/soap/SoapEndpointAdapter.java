package cz.csas.mw.soap;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointAdapter;
import org.springframework.ws.soap.SoapMessage;

public class SoapEndpointAdapter implements EndpointAdapter{

	@Override
	public boolean supports(Object endpoint) {
		return endpoint instanceof SoapEndpoint;
	}

	@Override
	public void invoke(MessageContext messageContext, Object endpoint) throws Exception {
		SoapEndpoint endp = (SoapEndpoint)endpoint;
		Source result = endp.processRequest((SoapMessage)messageContext.getRequest());
		if (result != null) {
			Source responseSource = (Source) result;
			WebServiceMessage response = messageContext.getResponse();
			TransformerFactory transformFactory = TransformerFactory.newInstance();
			Transformer transformer = transformFactory.newTransformer();
			transformer.setOutputProperty("omit-xml-declaration", "yes");
			transformer.transform(responseSource, response.getPayloadResult());
		}
	}

}
