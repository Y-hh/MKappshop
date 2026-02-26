package com.MKappshop.MKappshop.repository.order;
import com.MKappshop.MKappshop.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository <Order, Long> {
}
