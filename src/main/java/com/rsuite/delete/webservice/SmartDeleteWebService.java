package com.rsuite.delete.webservice;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.service.ManagedObjectID;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;
import com.rsicms.rsuite.helpers.webservice.RemoteApiHandlerBase;
import com.rsuite.delete.utils.BatchDeleteOptions;
import com.rsuite.delete.utils.CAUtils;
import com.rsuite.delete.utils.DestroyerUtils;
import com.rsuite.delete.utils.MOUtils;

public class SmartDeleteWebService extends RemoteApiHandlerBase {

	private static final String SMART_DELETE_ID_LIST_PARAM = "smart-delete-id-list";
	private static final String INCLUDE_TOP_LEVEL_FOLDER_PARAM = "include-top-level-folder";
	private static final String FORCE_DELETE_PARAM = "force-delete";
	private static final String MESSAGE_TITLE = "Smart Delete";
	private static final Log log = LogFactory.getLog(SmartDeleteWebService.class);

	@Override
	public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args) throws RSuiteException {

		log.info(MESSAGE_TITLE + " start");

		User user = context.getSession().getUser();
		String idList = args.getFirstString(SMART_DELETE_ID_LIST_PARAM, args.getFirstString("rsuiteId"));
		
		int processedCount = 0;
		int errorCount = 0;

		Map<String, String> detailedErrorReport = new HashMap<>();
		try {

			Boolean includeTopCaForDeletion = args.getFirstBoolean(INCLUDE_TOP_LEVEL_FOLDER_PARAM, false);
			Boolean isForceDelete = args.getFirstBoolean(FORCE_DELETE_PARAM, false);

			log.info("Processing ID list: " + idList);

			BatchDeleteOptions options = new BatchDeleteOptions();
			options.setIncludeTopCaForDeletion(includeTopCaForDeletion);

			String[] idListSplitted = StringUtils.split(idList, ",");
			for (String id : idListSplitted) {

				try {
					ManagedObject mo = MOUtils.getRealMo(context, user, new ManagedObjectID(id));
					if (CAUtils.isContainer(context, user, mo)) {
						ContentAssembly ca = CAUtils.batchDelete(context, mo.getMoTypeId(), options);
						log.info("CA deleted: " + CAUtils.formatContainerId(ca));
					} else {
						DestroyerUtils.destroy(context, user, mo.getMoTypeId());
						log.info("MO deleted: " + RSuiteUtils.formatMoId(mo));
					}
					processedCount++;
				} catch (Throwable e) {
					try {
						if (isForceDelete) {
							DestroyerUtils.destroy(context, user, new ManagedObjectID(id));
						} else {
							errorCount = reportError(errorCount, detailedErrorReport, id, e);
						}
					} catch (Throwable t) {
						errorCount = reportError(errorCount, detailedErrorReport, id, e);
					}
				}
			}

			log.info("ID list processed. ID size: " + idListSplitted.length);

		} catch (Exception e) {
			log.error("Unexpected error processing RSuite IDs.", e);
			return new MessageDialogResult(MessageType.ERROR, MESSAGE_TITLE,
					format("Error: %s", e.getLocalizedMessage()));
		}

		log.info(MESSAGE_TITLE + " end");

		StringBuilder finalReport = new StringBuilder();
		finalReport.append(format(
				"ID list processed. Objects processed: %s - Errors: %s (check rsuite-server log for full details)",
				processedCount, errorCount));

		finalReport.append("<br><br><b>Errors: (ID: Error Message)</b><br><br>");
		for (String id : detailedErrorReport.keySet()) {
			finalReport.append(id).append(": ").append(detailedErrorReport.get(id)).append("<br>");
		}

		return new MessageDialogResult(MessageType.SUCCESS, MESSAGE_TITLE, finalReport.toString());
	}

	private int reportError(int errorCount, Map<String, String> detailedErrorReport, String id, Throwable e) {
		log.error(String.format("Error with ID: %s - %s", id, e.getLocalizedMessage()), e);
		errorCount++;
		detailedErrorReport.put(id, e.getLocalizedMessage());
		return errorCount;
	}

}
