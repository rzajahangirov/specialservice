package az.techvibeds.specialservice.security;


import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.UserModuleRoleRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserModuleRoleRepository userModuleRoleRepository;

    @Autowired
    private az.techvibeds.specialservice.repositories.ModuleRepository moduleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        var authorities = new ArrayList<SimpleGrantedAuthority>();

        // 1. GLOBAL ROLES
        boolean isGlobalAdmin = false;
        for (az.techvibeds.specialservice.models.Role role : user.getRoles()) {
            String roleName = role.getName();
            if (roleName.equals("ROLE_GLOBAL_ADMIN") || roleName.equals("GLOBAL_ADMIN")) {
                isGlobalAdmin = true;
            }
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            authorities.add(new SimpleGrantedAuthority(roleName));
        }

        // 2. MODULE ROLES
        if (isGlobalAdmin) {
            log.info("Granting all module authorities to GLOBAL_ADMIN: {}", user.getEmail());
            // Dinamik modulları bazadan gətiririk
            moduleRepository.findAll().forEach(m -> {
                authorities.add(new SimpleGrantedAuthority("MODULE:" + m.getName().toUpperCase() + ":SUPER_ADMIN"));
            });
            
            // Fallback: Əgər baza boşdursa və ya bəzi modullar çatışmırsa, vacib olanları əlavə edirik
            List<String> essentialModules = List.of(
                "TRANSACTION", "INVOICE", "PARTNER", "INVENTORY", "HR", "REPORT", 
                "MANUFACTURING", "SERVICE", "CONSTRUCTION", "SECURITY", 
                "DASHBOARD", "FOREIGN_PURCHASE", "NOTIFICATION", "SALES"
            );
            essentialModules.forEach(m -> {
                String auth = "MODULE:" + m + ":SUPER_ADMIN";
                if (authorities.stream().noneMatch(a -> a.getAuthority().equals(auth))) {
                    authorities.add(new SimpleGrantedAuthority(auth));
                }
            });

            // GLOBAL_ADMIN həm də bütün USER_ADMIN səlahiyyətlərinə sahib olmalıdır
            if (authorities.stream().noneMatch(a -> a.getAuthority().equals("ROLE_USER_ADMIN"))) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER_ADMIN"));
            }
        } else {
            userModuleRoleRepository.findByUserId(user.getId())
                    .forEach(umr -> {
                        if (umr.getCompanyModule() != null && umr.getCompanyModule().getModule() != null) {
                            String moduleName = umr.getCompanyModule().getModule().getName().toUpperCase();
                            String roleName = umr.getRole().name(); // SUPER_ADMIN, ADMIN, USER
                            authorities.add(new SimpleGrantedAuthority("MODULE:" + moduleName + ":" + roleName));
                        }
                    });
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
