package com.digvijay.bookMyShow.repository;

import com.digvijay.bookMyShow.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
