package cz.csas.mw.translation.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.stereotype.Component;

import cz.csas.mw.osb.JavaCalloutEntrypoint;
import cz.csas.mw.translation.api.ContextParam;
import cz.csas.mw.translation.api.ContextParameters;
import cz.csas.mw.translation.api.ITranslationService;
import cz.csas.mw.translation.api.TranslationException;

@Component
public class TranslationService implements ITranslationService {
	private static final String MESSAGE="message";
	private static final String HEADER="header";
	@Override
	public List<ContextParameters> translateRequest(ContextParameters mwMessage) throws TranslationException {
		try {
			XmlObject input = convert(mwMessage);
			XmlObject output = JavaCalloutEntrypoint.translateRequest(input);
			return convertToList(output);
		} catch (Exception e) {
			throw new TranslationException(e);
		}
	}

	@Override
	public ContextParameters translateResponse(ContextParameters mwMessage) throws TranslationException {
		try {
			XmlObject input = convert(mwMessage);
			XmlObject output = JavaCalloutEntrypoint.translateResponse(input);
			return convert(output);
		} catch (Exception e) {
			throw new TranslationException(e);
		}
	}

	@SuppressWarnings("unused")
	protected ContextParameters convert(XmlObject message) {
		XmlOptions opts = new XmlOptions();
		XmlObject[] xmlParam;
		opts.setSaveOuter();
		ContextParameters parameters = new ContextParameters();
	    /*Used for response translate - root elem <ContextParams>
	     request has collection <ContextParam> in <ContextParams> and input var is without root <ContextParams> 
	     xmlBeans add <xml-fragment> as root*/
	    XmlObject[] xmlParams = message.selectChildren(null, "ContextParams");
	    if(xmlParams.length>0)
	    {
	    	xmlParam = xmlParams[0].selectChildren(null, "ContextParam");	
	    	
	    }
	    else
	    {
	    	xmlParam = message.selectChildren(null, "ContextParam");
	    	
	   }
		for (XmlObject xmlObject : xmlParam) {
			/* Name is always string */
			String name = ((SimpleValue) xmlObject.selectChildren(null, "Name")[0]).getStringValue();
			/* Value message and header are xml*/
			String value;
			if (!name.equals(MESSAGE) && !name.equals(HEADER))
			{
				value=((SimpleValue)xmlObject.selectChildren(null, "Value")[0]).getStringValue();
			}
			else
			{
				value=xmlObject.selectChildren(null, "Value")[0].toString();
			}
			parameters.addParameter(new ContextParam(name, value));
		}
		return parameters;
	}

	protected List<ContextParameters> convertToList(XmlObject message) {
		List<ContextParameters> params = new ArrayList<ContextParameters>();
		XmlObject[] xmlParams = message.selectChildren(null, "Contexts");
		xmlParams = xmlParams[0].selectChildren(null, "ContextParams");
		/*<root element will be <xml-fragment>*/
		for (XmlObject xmlObject : xmlParams) {
			params.add(convert(xmlObject));
		}
		return params;
	}

	protected XmlObject convert(ContextParameters message) throws XmlException {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.print("<ContextParams>\n");
		// temporary, creating from String, will be done from XmlCursor later
		for (ContextParam param : message.getParameters()) {
			pw.print("    <ContextParam>\n");
			pw.print("      <Name>" + param.getName() + "</Name>\n");
			pw.print("      <Value>" + param.getValue() + "</Value>\n");
			pw.print("    </ContextParam>\n");
		}
		pw.print("</ContextParams>\n");
		return XmlObject.Factory.parse(sw.toString());
	}
}
