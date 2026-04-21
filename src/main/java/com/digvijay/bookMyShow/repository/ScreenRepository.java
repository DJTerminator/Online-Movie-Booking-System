package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, String> {
}