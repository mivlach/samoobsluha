package cz.csas.mw.pipes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import cz.csas.mw.translation.api.ContextParam;

public class TranslationUtil {

	public static String soapElementToString(Source element) throws TransformerException {

		StringWriter stringResult = new StringWriter();
		TransformerFactory transformFactory = TransformerFactory.newInstance();
		Transformer transformer = transformFactory.newTransformer();

		transformer.setOutputProperty("omit-xml-declaration", "yes");
		transformer.transform(element, new StreamResult(stringResult));

		return stringResult.toString();

	}

	public static String getFirstElementName(DOMSource source) {
		Node bodyNode = source.getNode();
		for (int i = 0; i < bodyNode.getChildNodes().getLength(); i++) {
			if (bodyNode.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				Node b = bodyNode.getChildNodes().item(i);
				return b.getLocalName();
			}
		}
		return null;
	}

	public static String getBodyElement(DOMSource source) throws TransformerException {
		TransformerFactory transformFactory = TransformerFactory.newInstance();
		Transformer transformer = transformFactory.newTransformer();
		transformer.setOutputProperty("omit-xml-declaration", "yes");

		Node bodyNode = source.getNode();

		for (int i = 0; i < bodyNode.getChildNodes().getLength(); i++) {
			if (bodyNode.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				Node b = bodyNode.getChildNodes().item(i);
				StringWriter stringRespBody = new StringWriter();
				transformer.transform(new DOMSource(b), new StreamResult(stringRespBody));
				return stringRespBody.toString();
			}
		}
		return null;
	}

	public static String getBodyElement(String sourceMessage) throws TransformerException, IOException, SOAPException{
		InputStream is = new ByteArrayInputStream(sourceMessage.getBytes());
		SOAPMessage response = MessageFactory.newInstance().createMessage(null, is);
		SOAPBody responseBody = response.getSOAPBody();
		return getBodyElement(new DOMSource(responseBody));
	}
	
	public static String getSoapMessage(String message, String header) throws SOAPException, IOException, SAXException, ParserConfigurationException{
		MessageFactory factory = MessageFactory.newInstance();
		SOAPMessage soapMessage = factory.createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();
		SOAPEnvelope soapEnvp = soapPart.getEnvelope();
		SOAPBody soapBodyResponse = soapEnvp.getBody();
	
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder builder = dbFactory.newDocumentBuilder();
		Document bodyNode = builder.parse(new ByteArrayInputStream(message.getBytes()));

		soapBodyResponse.addDocument((Document) bodyNode);
	
		Document headerNode = builder.parse(new ByteArrayInputStream(((String) header).getBytes()));				
	    DocumentFragment docFrag = headerNode.createDocumentFragment();
	    Element rootElement = headerNode.getDocumentElement();
	    if(rootElement != null) {
	        docFrag.appendChild(rootElement);
	        Document ownerDoc = soapMessage.getSOAPHeader().getOwnerDocument();
	        org.w3c.dom.Node replacingNode = ownerDoc.importNode(docFrag, true);
	        soapMessage.getSOAPHeader().appendChild(replacingNode);
	    }	
		soapMessage.saveChanges();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		soapMessage.writeTo(out);
		return new String(out.toByteArray());

	}
}
