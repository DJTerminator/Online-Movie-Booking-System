package com.digvijay.bookMyShow.service;

public interface DiscountService {
    double calculateDiscount(double totalAmount, int numberOfSeats, boolean isAfternoonShow);
}
