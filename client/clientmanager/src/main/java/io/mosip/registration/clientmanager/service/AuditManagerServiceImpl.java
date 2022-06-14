package io.mosip.registration.clientmanager.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.mosip.registration.clientmanager.BuildConfig;
import io.mosip.registration.clientmanager.R;
import io.mosip.registration.clientmanager.config.SessionManager;
import io.mosip.registration.clientmanager.constant.AuditEvent;
import io.mosip.registration.clientmanager.constant.AuditEventType;
import io.mosip.registration.clientmanager.constant.AuditReferenceIdTypes;
import io.mosip.registration.clientmanager.constant.Components;
import io.mosip.registration.clientmanager.dto.registration.RegistrationDto;
import io.mosip.registration.clientmanager.entity.Audit;
import io.mosip.registration.clientmanager.repository.AuditRepository;
import io.mosip.registration.clientmanager.repository.GlobalParamRepository;
import io.mosip.registration.clientmanager.spi.AuditManagerService;
import io.mosip.registration.clientmanager.spi.RegistrationService;

/**
 * Class to Audit the events of Android Registration Client.
 *
 * @author Anshul Vanawat
 * @since 1.0.0
 */

@Singleton
public class AuditManagerServiceImpl implements AuditManagerService {

    private String TAG = AuditManagerServiceImpl.class.getSimpleName();
    public static final String AUDIT_EXPORTED_TILL = "AuditExportedTill";
    public static final String REGISTRATION_EVENTS = "REG-EVT";

    private Context context;
    private AuditRepository auditRepository;
    private GlobalParamRepository globalParamRepository;
    private RegistrationService registrationService;

    @Inject
    public AuditManagerServiceImpl(Context context, AuditRepository auditRepository, GlobalParamRepository globalParamRepository, RegistrationService registrationService) {
        this.auditRepository = auditRepository;
        this.globalParamRepository = globalParamRepository;
        this.registrationService = registrationService;
        this.context = context;
    }

    @Override
    public void audit(AuditEvent auditEventEnum, Components appModuleEnum, String refId, String refIdType) {
        SharedPreferences sharedPreferences = this.context.getSharedPreferences(this.context.getString(R.string.app_name),
                Context.MODE_PRIVATE);

        String hostName = BuildConfig.BASE_URL;
        String hostIP = BuildConfig.BASE_URL;
        String sessionUserId = sharedPreferences.getString(SessionManager.USER_NAME, null);
        String sessionUserName = sharedPreferences.getString(SessionManager.USER_NAME, null);
        String applicationId = this.context.getString(R.string.app_name);
        String applicationName = this.context.getString(R.string.app_name);

        Audit audit = new Audit(
                System.currentTimeMillis(),
                auditEventEnum.getId(),
                auditEventEnum.getName(),
                auditEventEnum.getType(),
                System.currentTimeMillis(),
                hostName,
                hostIP,
                applicationId,
                applicationName,
                sessionUserId,
                sessionUserName,
                refId,
                refIdType,
                sessionUserId,
                appModuleEnum.getName(),
                appModuleEnum.getId(),
                "");

        auditRepository.insertAudit(audit);

    }

    @Override
    public void audit(AuditEvent auditEventEnum, Components appModuleEnum) {
        SharedPreferences sharedPreferences = this.context.getSharedPreferences(this.context.getString(R.string.app_name),
                Context.MODE_PRIVATE);

        String sessionUserId = sharedPreferences.getString(SessionManager.USER_NAME, null);

        RegistrationDto registrationDto = null;
        try {
            registrationDto = registrationService.getRegistrationDto();
        } catch (Exception ignored) {
        }

        String refId, refIdType;
        if (auditEventEnum.getId().contains(REGISTRATION_EVENTS)
                && registrationDto != null
                && registrationDto.getRId() != null) {
            refId = registrationDto.getRId();
            refIdType = AuditReferenceIdTypes.REGISTRATION_ID.getReferenceTypeId();
        } else if (sessionUserId != null) {
            refId = sessionUserId;
            refIdType = AuditReferenceIdTypes.USER_ID.name();
        } else {
            refId = this.context.getString(R.string.app_name);
            refIdType = AuditReferenceIdTypes.APPLICATION_ID.name();
        }

        audit(auditEventEnum, appModuleEnum, refId, refIdType);
    }

    @Override
    public boolean deleteAuditLogs() {
        Log.i(TAG, "Deletion of Audit Logs Started");

        String tillDate = globalParamRepository.getGlobalParamValue(AUDIT_EXPORTED_TILL);

        if (tillDate != null && !tillDate.isEmpty()) {
            try {
                /* Delete Audits before given Time */
                long tillDateLong = Long.parseLong(tillDate);
                auditRepository.deleteAllAuditsTillDate(tillDateLong);
                Log.i(TAG, "deleteAuditLogs: Deletion of Audit Logs Completed for datetime before : {}" + tillDateLong);
                return true;
            } catch (RuntimeException runtimeException) {
                Log.e(TAG, "deleteAuditLogs: Deletion of Audit Logs failed", runtimeException);
                return false;
            }
        } else {
            Log.e(TAG, "deleteAuditLogs: Deletion of Audit Logs failed, tillDate missing");
            return false;
        }
    }
}
