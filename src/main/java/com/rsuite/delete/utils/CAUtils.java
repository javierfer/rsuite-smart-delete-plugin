package com.rsuite.delete.utils;

import static com.reallysi.rsuite.api.ObjectType.CONTENT_ASSEMBLY_REF;
import static com.reallysi.rsuite.api.ObjectType.MANAGED_OBJECT_REF;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.ReferenceInfo;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.DependencyTracker;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectID;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.helpers.utils.MoUtils;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

public class CAUtils {

    private static final Log log = LogFactory.getLog(CAUtils.class);

    public static boolean isContainer(ExecutionContext context, User user, ManagedObject mo) {

        ObjectType type = mo.getObjectType();

        return type == CONTENT_ASSEMBLY_REF || type == ObjectType.CONTENT_ASSEMBLY
                || type == ObjectType.CONTENT_ASSEMBLY_NODE;
    }

    public static boolean isContainer(ExecutionContext context, User user, ContentAssemblyItem item)
            throws RSuiteException {
        ManagedObject mo = MoUtils.getRealMo(context, item.getMoTypeId());
        return isContainer(context, user, mo);
    }

    public static String formatContainerId(ContentAssemblyItem caItem) {
        return String.format("[%s] %s", caItem.getMoTypeId(), caItem.getDisplayName());
    }

    public static String getParentId(ExecutionContext context, User user, ManagedObjectID moId) throws RSuiteException {
        ManagedObjectService moSvc = context.getManagedObjectService();
        DependencyTracker tracker = moSvc.getDependencyTracker();

        List<ReferenceInfo> refList = tracker.listDirectReferences(user, moId);
        ManagedObjectID rootId = context.getContentAssemblyService().getRootFolder(user).getMoTypeId();

        if (refList.size() == 0) {
            throw new RSuiteException("Unable to localize parent. There are no references for " + moId);
        }

        ReferenceInfo ref = refList.get(0);
        String browseUri = ref.getParentBrowseUri();
        String[] parents = browseUri.split("\\/");

        ManagedObject managedObject = moSvc.getManagedObject(user, moId);
        if ((managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY
                || managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY_REF
                || managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY_NODE) && parents.length == 1
                && parents[0].split(":")[1].equals(rootId.getId()))
            return null;
        String refCaId = parents[parents.length - 1].split(":")[1];
        return RSuiteUtils.getContentAssemblyNodeContainer(context, user, new ManagedObjectID(refCaId)).getMoTypeId()
                .getId();
    }

    public static String getParentId(ExecutionContext context, User user, String moId) throws RSuiteException {
        return getParentId(context, user, new ManagedObjectID(moId));
    }

    /**
     * Get container from path, starting from the root folder "/".
     * 
     * @param context
     * @param user
     * @param pathParts
     * @return ca
     * @throws RSuiteException
     */
    public static ContentAssembly getContainerFromRootPath(ExecutionContext context, User user,
            String[] pathParts) throws RSuiteException {
        ContentAssemblyNodeContainer parentContainer = context.getContentAssemblyService()
                .getRootFolder(user);

        return getContainerFromPath(context, user, pathParts, parentContainer);
    }

    /**
     * Get container from path, starting from the root folder "/".
     * 
     * @param context
     * @param user
     * @param path    the RSuite absolute path, separated by "/". Example:
     *                "Home/Child1/Child2"
     * @return
     * @throws RSuiteException
     */
    public static ContentAssembly getContainerFromRootPath(ExecutionContext context, User user,
            String path) throws RSuiteException {

        String[] pathParts = StringUtils.split(path, "/");

        return getContainerFromRootPath(context, user, pathParts);
    }

    /**
     * Get container from path.
     * 
     * @param context
     * @param user
     * @param pathParts
     * @param parentContainer
     * @return ca
     * @throws RSuiteException
     */
    public static ContentAssembly getContainerFromPath(ExecutionContext context, User user,
            String[] pathParts, ContentAssemblyNodeContainer parentContainer) throws RSuiteException {

        return (ContentAssembly) RSuiteUtils.getCaContainerForPath(context, user, parentContainer, pathParts);
    }

    /**
     * Get container from path.
     * 
     * @param context
     * @param user
     * @param path
     * @param parentContainer
     * @return
     * @throws RSuiteException
     */
    public static ContentAssembly getContainerFromPath(ExecutionContext context, User user, String path,
            ContentAssemblyNodeContainer parentContainer) throws RSuiteException {

        String[] pathParts = StringUtils.split(path, "/");

        return getContainerFromPath(context, user, pathParts, parentContainer);
    }

    public static List<ManagedObject> getDirectChildrenMos(ExecutionContext context, User user, ContentAssembly ca)
            throws RSuiteException {

        ManagedObjectService mosvc = context.getManagedObjectService();

        List<ManagedObject> moList = new ArrayList<>();
        List<? extends ContentAssemblyItem> kids = ca.getChildrenObjects();
        for (ContentAssemblyItem kid : kids) {
            ManagedObject mo = RSuiteUtils.getRealMo(context, user, mosvc.getManagedObject(user, kid.getMoTypeId()));
            String type = mo.getObjectType().name();
            if (type.equals(ObjectType.MANAGED_OBJECT.name())) {
                moList.add(mo);
            } else if (type.equals(ObjectType.MANAGED_OBJECT_NONXML.name())) {
                moList.add(mo);
            }
        }
        return moList;
    }

    /**
     * Get content assembly
     * 
     * @param context
     * @param user
     * @param caId
     * @return content assembly
     * @throws RSuiteException
     */
    public static ContentAssembly getContentAssembly(ExecutionContext context, User user, ManagedObjectID caId)
            throws RSuiteException {
        return (ContentAssembly) RSuiteUtils.getContentAssemblyNodeContainer(context, user, caId);
    }

    /**
     * Get content assembly using system user
     * 
     * @param context
     * @param id
     * @return content assembly
     * @throws RSuiteException
     */
    public static ContentAssembly getContentAssembly(ExecutionContext context, ManagedObjectID id)
            throws RSuiteException {
        User systemUser = context.getAuthorizationService().getSystemUser();
        return getContentAssembly(context, systemUser, id);
    }

    public static List<ManagedObject> getDescendantMos(User user, ExecutionContext context, ContentAssembly ca,
            List<ManagedObject> moList) throws RSuiteException {
        ContentAssemblyService casvc = context.getContentAssemblyService();
        ManagedObjectService mosvc = context.getManagedObjectService();

        moList.add(mosvc.getManagedObject(user, ca.getMoTypeId()));
        List<? extends ContentAssemblyItem> kids = ca.getChildrenObjects();
        for (ContentAssemblyItem kid : kids) {
            ManagedObject mo = RSuiteUtils.getRealMo(context, user, mosvc.getManagedObject(user, kid.getMoTypeId()));
            String type = mo.getObjectType().name();
            if (type.equals(ObjectType.MANAGED_OBJECT.name())) {
                moList.add(mo);
            } else if (type.equals(ObjectType.MANAGED_OBJECT_NONXML.name())) {
                moList.add(mo);
            } else if (type.equals(ObjectType.CONTENT_ASSEMBLY.name())) {
                ContentAssembly childCa = casvc.getContentAssembly(user, mo.getMoTypeId());
                getDescendantMos(user, context, childCa, moList);
            }
        }
        return moList;
    }

    /**
     * Delete a content assembly structure -including content- in batch.
     * Cannot rollback, should be used with extra care.
     * 
     * Inspired in BatchDelete class from TE Batch Delete plugin
     * 
     * @param context
     * @param user
     * @param caId
     * @param options
     * @throws RSuiteException
     */
    public static ContentAssembly batchDelete(ExecutionContext context, User user, ManagedObjectID caId,
            BatchDeleteOptions options) throws RSuiteException {

        ManagedObjectService moSvc = context.getManagedObjectService();
        ContentAssemblyService caService = context.getContentAssemblyService();

        ManagedObject caMo = RSuiteUtils.getRealMo(context, user, moSvc.getManagedObject(user, caId));

        ContentAssembly ca = caService.getContentAssembly(user, caMo.getMoTypeId());

        log.info(format("Batch delete started. Getting moList for CA ID: %s", caId));

        List<ManagedObject> moList = new ArrayList<ManagedObject>();
        moList = getDescendantMos(user, context, ca, moList);

        log.info("MO count: " + moList.size());

        List<ManagedObject> caList = new ArrayList<ManagedObject>();

        for (ManagedObject mo : moList) {
            try {
                if (mo.getObjectType() == ObjectType.MANAGED_OBJECT
                        || mo.getObjectType() == ObjectType.MANAGED_OBJECT_NONXML) {

                    String fileExtension = MOUtils.getAliasFileExtension(context, user, mo);
                    if (!options.isExtensionExcluded(fileExtension)) {
                        DestroyerUtils.destroy(context, user, mo.getMoTypeId());
                    } else {
                        log.info(format("File extension excluded for deletion: '%s' - managed object: %s",
                                fileExtension, RSuiteUtils.formatMoId(mo)));
                    }
                } else {
                    if (mo.getMoTypeId().equals(caId)) {
                        if (options.isIncludeTopCaForDeletion())
                            caList.add(mo);
                    } else
                        caList.add(mo);
                }
            } catch (RSuiteException e) {
                log.error(e);
            }
        }

        log.info("CA count: " + caList.size());

        for (ManagedObject mo : caList) {
            try {
                moSvc.remove(user, mo.getMoTypeId());
            } catch (RSuiteException e) {
                log.error(e);
            }
        }

        if (!options.isIncludeTopCaForDeletion()) {
            /* Refresh top content assembly */
            ca = getContentAssembly(context, user, caId);
        }
        log.info(format("All content was deleted for CA ID: %s", caId));

        return ca;
    }

    public static ContentAssembly batchDelete(ExecutionContext context, ManagedObjectID caId,
            BatchDeleteOptions options) throws RSuiteException {
        User systemUser = context.getAuthorizationService().getSystemUser();
        return batchDelete(context, systemUser, caId, options);
    }

    public enum SearchStrategy {
        FILENAME,
        EXTENSION
    }

    public static List<ManagedObject> getMosByFileNameAliasFromContainer(ExecutionContext context, User user,
            String fileName, ContentAssemblyNodeContainer caContainer) throws RSuiteException {
        return getMosByFileNameAliasFromContainer(context, user, fileName, caContainer, SearchStrategy.FILENAME);
    }

    public static List<ManagedObject> getMosByFileNameAliasFromContainer(ExecutionContext context, User user,
            String searchCriteria, ContentAssemblyNodeContainer caContainer, SearchStrategy strategy)
            throws RSuiteException {
        ManagedObjectService moService = context.getManagedObjectService();

        List<ManagedObject> ret = new ArrayList<ManagedObject>();
        for (ContentAssemblyItem item : caContainer.getChildrenObjects()) {
            ObjectType objectType = item.getObjectType();
            if (objectType == MANAGED_OBJECT_REF) {
                ManagedObjectReference moRef = (ManagedObjectReference) item;

                ManagedObjectID targetId = null;
                try {
                    targetId = moRef.getTargetMoTypeId();
                } catch (Exception e) {
                    log.warn(
                            String.format("Unable to get managed object reference: '%s'", e.getLocalizedMessage()));
                }
                if (Objects.nonNull(targetId)) {
                    try {
                        ManagedObject candidateMo = moService.getManagedObject(user, targetId);
                        String filenameAliasRealMo = MOUtils.getAliasFileName(context, user, candidateMo);
                        if (SearchStrategy.FILENAME == strategy) {
                            if (StringUtils.equals(searchCriteria, filenameAliasRealMo)) {
                                ret.add(candidateMo);
                            }
                        } else {
                            if (StringUtils.endsWith(filenameAliasRealMo, searchCriteria)) {
                                ret.add(candidateMo);
                            }
                        }
                    } catch (Exception e) {
                        log.warn(String.format("Unable to get managed object by target id: '%s' - '%s'", targetId,
                                e.getLocalizedMessage()));
                    }
                }

            }
        }
        return ret;
    }

    public static List<ManagedObject> getDirectChildrenFromRootPath(ExecutionContext context, String path)
            throws RSuiteException {
        User systemUser = context.getAuthorizationService().getSystemUser();
        ContentAssembly modulesCa = CAUtils.getContainerFromRootPath(context, systemUser, path);
        return CAUtils.getDirectChildrenMos(context, systemUser, modulesCa);
    }

}
