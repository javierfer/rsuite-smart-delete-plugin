package com.rsuite.delete.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.repository.ComposedXQuery;
import com.reallysi.rsuite.service.ManagedObjectID;

public class DestroyerUtils {

	private static Log log = LogFactory.getLog(DestroyerUtils.class);

	private static final String DESTROYER_XQY = "xquery version \"1.0-ml\";\n"
			+ "\n"
			+ "declare namespace r = \"http://www.rsuitecms.com/rsuite/ns/metadata\";\n"
			/* ONLY PLACEHOLDER: managed object id */
			+ "let $id := \"%s\"\n"
			+ "return (\n"
			+ "  (: browse tree  :)\n"
			+ "  xdmp:node-delete(fn:collection(\"rsuite:current\")/rs_ca_map/rs_ca/rs_moref[@href=$id]),\n"
			+ "  (: reference resource  :)\n"
			+ "  xdmp:document-delete(base-uri(/r:res[r:md/r:targetId=$id])),\n"
			+ "  (: resource  :)\n"
			+ "  xdmp:document-delete(base-uri(/r:res[@r:id=$id])),\n"
			+ "  (: content :)\n"
			+ "  xdmp:document-delete(base-uri(fn:collection(\"rsuite:current\")//*[@r:rsuiteId=$id])),\n"
			+ "  (: search :)\n"
			+ "  xdmp:document-delete(base-uri(fn:collection(\"rsuite:mv-current\")//*[@r:rsuiteId=$id])),\n"
			+ "  (: checkout bucket :)\n"
			+ "  xdmp:node-delete(/checkout-bucket/item[@id = $id])\n"
			+ ")";

	private DestroyerUtils() {
	}

	public static void destroy(ExecutionContext context, User systemUser, ManagedObjectID id) throws RSuiteException {

		try {
			ManagedObject realMo = MOUtils.getRealMo(context, systemUser, id);
			MOUtils.undoCheckout(context, systemUser, realMo.getMoTypeId());
			MOUtils.remove(context, systemUser, realMo.getMoTypeId());

		} catch (Throwable re) {
			log.warn("Unable to remove id: " + id.getId() + " error: " + re.getLocalizedMessage(), re);
			try {
				log.warn("Attempting to destroy id: " + id.getId());
				String xqy = String.format(DESTROYER_XQY, id.getId());
				context.getRepositoryService().queryAsNoResult(new ComposedXQuery(xqy));
			} catch (Throwable de) {
				log.warn("Unable to destroy id: " + id.getId() + " error: " + de.getLocalizedMessage(), de);
			}
		}
	}
}
