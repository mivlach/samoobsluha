package cz.csas.mw.translation.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class ContextParameters {

	private HashMap<String, ContextParam> params = new HashMap<String, ContextParam>();

	public ContextParameters() {
		super();
	}

	public void addParameter(ContextParam param) {
		params.put(param.getName(), param);
	}

	public void removeParameter(String key) {
		params.remove(key);
	}

	public Object getParameter(String key) {
		return params.get(key);
	}

	public Collection<ContextParam> getParameters() {
		return params.values();
	}

	public Collection<ContextParam> getUnmodifiableParameters() {
		return Collections.unmodifiableCollection(params.values());
	}

	@Override
	public String toString() {
		return "ContextParameters [params=" + params + "]";
	}

}
