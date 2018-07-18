package cz.csas.mw.translation.api;

import java.util.List;

public interface ITranslationService {

	List<ContextParameters> translateRequest(ContextParameters mwMessage) throws TranslationException;

	ContextParameters translateResponse(ContextParameters mwMessage) throws TranslationException;
}
