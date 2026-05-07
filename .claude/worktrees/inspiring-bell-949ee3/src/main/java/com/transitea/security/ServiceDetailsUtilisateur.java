package com.transitea.security;

import com.transitea.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceDetailsUtilisateur implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public ServiceDetailsUtilisateur(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmailAndSupprimeFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable avec l'email : " + email));
    }
}
