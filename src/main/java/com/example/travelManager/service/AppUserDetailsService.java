package com.example.travelManager.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.repository.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email not found: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (existingUser.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + existingUser.getRole().getName().toUpperCase()));
        }

        return new User(existingUser.getEmail(), existingUser.getPassWord(), authorities);
    }
}
