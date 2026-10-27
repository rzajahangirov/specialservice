package az.techvibeds.specialservice.services;

import java.util.Set;

/**
 * Şirkətin modul icazələrini yoxlayan yardımçı service.
 * Hesabatlıq və digər modullar tərəfindən istifadə olunur —
 * şirkətin hər hansı modula aktiv icazəsi olub-olmadığını müəyyən edir.
 */
public interface ModuleAccessService {

    /**
     * Şirkətin backendKey ilə verilmiş modula aktiv icazəsi var mı?
     *
     * @param companyId  şirkətin ID-si
     * @param backendKey modulun backendKey dəyəri (məs: "FINANCE", "CONSTRUCTION")
     * @return true — modul aktivdir, false — icazə yoxdur
     */
    boolean hasModuleAccess(Long companyId, String backendKey);

    /**
     * Şirkətin aktiv modullarının backendKey set-ini qaytarır.
     *
     * @param companyId şirkətin ID-si
     * @return aktiv modul backendKey-lərinin set-i
     */
    Set<String> getActiveModuleKeys(Long companyId);

    /**
     * Şirkətin aktiv modullarının backendKey set-ini qaytarır (userEmail ilə).
     *
     * @param userEmail istifadəçinin email ünvanı
     * @return aktiv modul backendKey-lərinin set-i
     */
    Set<String> getActiveModuleKeys(String userEmail);
}
