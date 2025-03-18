package com.rsuite.delete.utils;

import org.apache.commons.io.FilenameUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.tools.AliasHelper;
import com.reallysi.rsuite.service.ManagedObjectID;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

public class MOUtils {

    private MOUtils() {
    }

    public static ManagedObject getRealMo(
            ExecutionContext context,
            User user,
            ManagedObjectID id)
            throws RSuiteException {
        ManagedObject mo = context.getManagedObjectService().getManagedObject(user, id);
        return RSuiteUtils.getRealMo(context, user, mo);
    }

    public static String getAliasFileName(ExecutionContext context, User user, ManagedObject mo)
            throws RSuiteException {
        AliasHelper aliasHelper = context.getManagedObjectService().getAliasHelper();

        return aliasHelper.getFilename(user, mo);
    }

    public static String getAliasFileExtension(ExecutionContext context, User user, ManagedObject mo)
            throws RSuiteException {
        String aliasFileName = getAliasFileName(context, user, mo);
        return FilenameUtils.getExtension(aliasFileName);
    }

    /**
     * Undo a check out if the MO is checked out.
     * 
     * @param context
     * @param user
     * @param id
     * 
     * @throws RSuiteException
     */
    public static void undoCheckout(
            ExecutionContext context,
            User user,
            ManagedObjectID id)
            throws RSuiteException {
        ManagedObjectService moService = context.getManagedObjectService();
        if (moService.isCheckedOut(user, id))
            moService.undoCheckout(user, id);
    }

    public static void remove(
            ExecutionContext context,
            User user,
            ManagedObjectID id) throws RSuiteException {
        ManagedObjectService moService = context.getManagedObjectService();
        moService.checkOut(user, id);
        moService.remove(user, id);
    }
}
