package cz.csas.mw.pipes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import cz.csas.mw.translation.api.ContextParam;
import cz.csas.mw.translation.api.ContextParameters;
import cz.csas.mw.translation.api.ITranslationService;


public class ServiceEndpoint {

	private static final Logger log = LoggerFactory.getLogger(ServiceEndpoint.class.getName());

	private String servicePre = "MW_POC_DepositDashboardService_";
	private String servicePost = "_v02_01";
	private String backendUri = "http://localhost:8081/soapmock/services";

	@Autowired
	private ITranslationService translationService;
	
	
	public void setServicePre(String servicePre) {
		this.servicePre = servicePre;
	}

	public void setServicePost(String servicePost) {
		this.servicePost = servicePost;
	}

	public void setBackendUri(String backendUri) {
		this.backendUri = backendUri;
	}

	public void setTranslationService(ITranslationService translationService) {
		this.translationService = translationService;
	}

	@PayloadRoot(namespace = "http://csas.cz/services/DepositDashboardService/v02_01", localPart = "getAccountInstructionsRequest")
	public @ResponsePayload Source processRequest(@RequestPayload SoapMessage message) throws WsException {
		SoapBody sb = message.getSoapBody();
		SoapHeader sh = message.getSoapHeader();

		try {
			String soapBody = TranslationUtil.getBodyElement((DOMSource) sb.getSource());
			log.debug("Body=" + soapBody);

			String soapHeader = TranslationUtil.getBodyElement((DOMSource) sh.getSource());
			log.debug("Header=" + soapHeader);

			/**
			 * Set transform request context
			 */
			UUID mwMessageID = UUID.randomUUID();
			ContextParameters params = new ContextParameters();
			params.addParameter(new ContextParam(ContextParamNames.message.name(), soapBody));
			params.addParameter(new ContextParam(ContextParamNames.header.name(), soapHeader));
			params.addParameter(new ContextParam(ContextParamNames.msguidmw.name(), mwMessageID.toString()));

			String rootPayloadElem = TranslationUtil.getFirstElementName((DOMSource) sb.getSource());
			params.addParameter(new ContextParam(ContextParamNames.target.name(),
					servicePre + rootPayloadElem.replaceFirst("Request", "") + servicePost));
			/**
			 * Translate Request
			 */
			long transreq_tm = System.nanoTime();
			List<ContextParameters> beRequest = translationService.translateRequest(params);
			long transrpl_tm = System.nanoTime();

			log.debug("translateRequest time=[%d]ms", (long) (transrpl_tm - transreq_tm));
			if (beRequest.size() == 1) {
				/**
				 * Call be service
				 */
				long bereq_tm = System.nanoTime();
				String beResponse = sendToBackend(beRequest.get(0));
				long berpl_tm = System.nanoTime();
				log.debug("beresp_time=[%d]ms response=%s", (long) (berpl_tm - bereq_tm), beResponse.toString());

				ContextParameters responseParams = beRequest.get(0);
				/**
				 * Set response translate context
				 */
				responseParams.removeParameter(ContextParamNames.message.name());
				responseParams.removeParameter(ContextParamNames.header.name());
				responseParams.addParameter(
						new ContextParam(ContextParamNames.message.name(), TranslationUtil.getBodyElement(beResponse)));

				/**
				 * Translate response
				 */
				long transrplbegin_tm = System.nanoTime();
				ContextParameters feResponse = translationService.translateResponse(responseParams);
				long transrplend_tm = System.nanoTime();
				log.debug("translateResponse time=[%d]ms", (long) (transrplend_tm - transrplbegin_tm));

				String header = null;
				ContextParam headerParam = (ContextParam) params.getParameter(ContextParamNames.header.name());
				if (headerParam != null) {
					header = headerParam.getValue();
				}

				return new StringSource(TranslationUtil.getSoapMessage(
						(String) ((ContextParam) feResponse.getParameter(ContextParamNames.message.name())).getValue(),
						header));
			}
		} catch (Exception e) {
			log.error("Server exception", e);
			throw new WsException(e.getMessage());
		}
		return null;
	}

	protected String sendToBackend(ContextParameters params)
			throws TransformerException, SOAPException, ParserConfigurationException, SAXException, IOException {
		Object message = ((ContextParam) params.getParameter(ContextParamNames.message.name())).getValue();
		String header = null;
		ContextParam headerParam = (ContextParam) params.getParameter(ContextParamNames.header.name());
		if (headerParam != null) {
			header = headerParam.getValue();
		}
		MessageFactory factory = MessageFactory.newInstance();
		SOAPMessage soapMessage = factory.createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();
		SOAPEnvelope soapEnvp = soapPart.getEnvelope();
		/*
		 * Parse XML body from transformation
		 */
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
		Document bodyNode = docBuilder.parse(new ByteArrayInputStream(((String) message).getBytes()));
		/* Add transformed SOAP Header to outbound message */
		if (header != null) {
			Document headerNode = docBuilder.parse(new ByteArrayInputStream(((String) header).getBytes()));
			DocumentFragment docFrag = headerNode.createDocumentFragment();
			Element rootElement = headerNode.getDocumentElement();
			if (rootElement != null) {
				docFrag.appendChild(rootElement);
				Document ownerDoc = soapMessage.getSOAPHeader().getOwnerDocument();
				org.w3c.dom.Node replacingNode = ownerDoc.importNode(docFrag, true);
				soapMessage.getSOAPHeader().appendChild(replacingNode);
			}

		}
		/*
		 * Add XML document to SOAP body
		 */
		soapEnvp.getBody().addDocument((Document) bodyNode);
		soapMessage.saveChanges();

		WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
		webServiceTemplate.setDefaultUri(backendUri);
		StringSource source = new StringSource((String) message);
		StringResult result = new StringResult();
		webServiceTemplate.sendSourceAndReceiveToResult(source, result);

		StringWriter sw = (StringWriter) result.getWriter();
		return sw.getBuffer().toString();
	}
}
