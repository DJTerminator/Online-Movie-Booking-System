package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.service.DiscountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DiscountServiceImpl implements DiscountService {

    @Override
    public double calculateDiscount(double totalAmount, int numberOfSeats, boolean isAfternoonShow) {
        log.debug("Calculating discount - Total: ₹{}, Seats: {}, Afternoon: {}",
                totalAmount, numberOfSeats, isAfternoonShow);

        double discount = 0.0;

        // Rule 1: 50% discount on the third ticket
        if (numberOfSeats >= 3) {
            double perSeatPrice = totalAmount / numberOfSeats;
            double thirdTicketDiscount = perSeatPrice * 0.50;
            discount += thirdTicketDiscount;
            log.debug("Applied 3rd ticket discount: ₹{}", thirdTicketDiscount);
        }

        // Rule 2: 20% discount for afternoon shows
        if (isAfternoonShow) {
            double afternoonDiscount = totalAmount * 0.20;
            discount += afternoonDiscount;
            log.debug("Applied afternoon show discount: ₹{}", afternoonDiscount);
        }

        log.info("Total discount calculated: ₹{}", discount);
        return discount;
    }
}