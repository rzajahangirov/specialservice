package az.techvibeds.specialservice.security.interceptors;

import az.techvibeds.specialservice.enums.SubscriptionStatus;
import az.techvibeds.specialservice.models.Subscription;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.SubscriptionRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModuleAccessInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    private static final Map<String, String> MODULE_PREFIX_MAP = Map.ofEntries(
            Map.entry("/api/v1/invoices", "SALES"),
            Map.entry("/api/v1/partners", "PARTNER"),
            Map.entry("/api/v1/transaction", "PARTNER"),
            Map.entry("/api/v1/inventory", "INVENTORY"),
            Map.entry("/api/v1/warehouses", "INVENTORY"),
            Map.entry("/api/v1/category", "INVENTORY"),
            Map.entry("/api/v1/employees", "HR"),
            Map.entry("/api/v1/hr", "HR"),
            Map.entry("/api/v1/payrolls", "HR"),
            Map.entry("/api/v1/contracts", "HR"),
            Map.entry("/api/v1/deductions", "HR"),
            Map.entry("/api/v1/leave-requests", "HR"),
            Map.entry("/api/v1/reports", "REPORT"),
            Map.entry("/api/v1/manufacturing", "MANUFACTURING"),
            Map.entry("/api/v1/production-order", "MANUFACTURING"),
            Map.entry("/api/v1/semi-products", "MANUFACTURING"),
            Map.entry("/api/v1/indirect-expenses", "MANUFACTURING"),
            Map.entry("/api/v1/kassa", "FINANCE"), 
            Map.entry("/api/v1/services", "SERVICE"),
            Map.entry("/api/v1/units", "SERVICE"),
            Map.entry("/api/v1/assignees", "SERVICE"),
            Map.entry("/api/v1/transactions", "FINANCE"),
            Map.entry("/api/v1/construction", "CONSTRUCTION"),
            Map.entry("/api/v1/projects", "CONSTRUCTION"),
            Map.entry("/api/v1/project-expenses", "CONSTRUCTION"),
            Map.entry("/api/v1/project-stages", "CONSTRUCTION"),
            Map.entry("/api/v1/project-contractors", "CONSTRUCTION"),
            Map.entry("/api/v1/security", "SECURITY"),
            Map.entry("/api/v1/audit-logs", "SECURITY"),
            Map.entry("/api/v1/dashboard", "DASHBOARD"),
            Map.entry("/api/v1/finance", "FINANCE"),
            Map.entry("/api/v1/taxes", "FINANCE"),
            Map.entry("/api/v1/notifications", "NOTIFICATION")
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. İSTİSNA EDİLƏN (PUBLIC VƏ YA HƏMİŞƏ AÇIQ) URL-lər
        if (uri.startsWith("/api/v1/auth") ||
            uri.startsWith("/api/v1/subscriptions") ||
            uri.startsWith("/api/v1/payment-plans") ||
            uri.startsWith("/swagger") ||
            uri.startsWith("/v3/api-docs")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            return true; // Əgər login olmayıbsa Spring Security (JwtAuthFilter) onsuz da 401 qaytaracaq.
        }

        // Qlobal admin hər yerə girə bilər
        boolean isGlobalAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GLOBAL_ADMIN"));
        if (isGlobalAdmin) {
            return true;
        }

        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty() || userOpt.get().getCompany() == null) {
            return true;
        }

        User user = userOpt.get();
        Long companyId = user.getCompany().getId();

        // 2. ABUNƏLİK YOXLANIŞI (FROZEN/ACTIVE)
        Optional<Subscription> subOpt = subscriptionRepository.findFirstByCompanyIdOrderByEndDateDesc(companyId);
        if (subOpt.isPresent() && subOpt.get().getStatus() == SubscriptionStatus.FROZEN) {
            log.warn("Access denied for user {}: Company {} subscription is FROZEN", email, companyId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Şirkətin abunəliyi dondurulub. Zəhmət olmasa abunəliyi yeniləyin.\"}");
            return false;
        }

        // 3. MODUL İCAZƏSİ YOXLANIŞI
        String requiredModule = null;
        for (Map.Entry<String, String> entry : MODULE_PREFIX_MAP.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                requiredModule = entry.getValue();
                break;
            }
        }

        if (requiredModule != null) {
            boolean hasAccess = false;
            if (requiredModule.equals("FINANCE") && uri.startsWith("/api/v1/kassa")) {
                // Kassa is shared between FINANCE, SALES and PURCHASING
                hasAccess = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().startsWith("MODULE:FINANCE:") ||
                                       a.getAuthority().startsWith("MODULE:SALES:") ||
                                       a.getAuthority().startsWith("MODULE:PURCHASING:"));
            } else {
                final String modulePrefix = "MODULE:" + requiredModule + ":";
                hasAccess = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().startsWith(modulePrefix));
            }
            
            if (!hasAccess) {
                log.warn("Access denied for user {}: Missing role for module {}", email, requiredModule);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\": \"Bu modula giriş icazəniz yoxdur.\"}");
                return false;
            }
        }

        return true;
    }
}
