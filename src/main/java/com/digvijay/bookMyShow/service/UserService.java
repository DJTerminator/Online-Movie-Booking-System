package com.digvijay.bookMyShow.service;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    // Inherits loadUserByUsername from UserDetailsService
    // Spring Security uses this directly
}
