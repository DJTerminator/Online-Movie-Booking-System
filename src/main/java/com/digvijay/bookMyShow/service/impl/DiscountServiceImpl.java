package com.digvijay.bookMyShow.service.impl;

import com.digvijay.bookMyShow.service.DiscountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DiscountServiceImpl implements DiscountService {

    private static final double MAX_DISCOUNT_PERCENT = 0.50; // Cap at 50%

    @Override
    public double calculateDiscount(double totalAmount, int numberOfSeats, boolean isAfternoonShow) {
        log.debug("Calculating discount — total: ₹{}, seats: {}, afternoon: {}", totalAmount, numberOfSeats, isAfternoonShow);

        double discount = 0.0;

        // Rule 1: 50% off the cheapest (average) seat when booking 3 or more
        if (numberOfSeats >= 3) {
            double perSeatAvg = totalAmount / numberOfSeats;
            double thirdTicketDiscount = perSeatAvg * 0.50;
            discount += thirdTicketDiscount;
            log.debug("3rd-ticket discount applied: ₹{}", thirdTicketDiscount);
        }

        // Rule 2: 20% off total for afternoon shows
        if (isAfternoonShow) {
            double afternoonDiscount = totalAmount * 0.20;
            discount += afternoonDiscount;
            log.debug("Afternoon discount applied: ₹{}", afternoonDiscount);
        }

        //Cap total discount to 50% of total to prevent over-discounting
        double maxDiscount = totalAmount * MAX_DISCOUNT_PERCENT;
        discount = Math.min(discount, maxDiscount);

        log.info("Final discount: ₹{} (on total ₹{})", discount, totalAmount);
        return discount;
    }
}
