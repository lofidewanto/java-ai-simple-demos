package com.example.demo.controller.web;

import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class OrderWebController {

    private final OrderService orderService;

    public OrderWebController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/")
    public String redirectToOrders() {
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "orders/list";
    }

    @GetMapping("/orders/new")
    public String showNewOrderForm(Model model) {
        model.addAttribute("order", new Order());
        return "orders/form";
    }

    @PostMapping("/orders")
    public String createOrder(@ModelAttribute Order order, RedirectAttributes redirectAttributes) {
        orderService.createOrder(order);
        redirectAttributes.addFlashAttribute("successMessage", "Order submitted successfully!");
        return "redirect:/orders";
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable Long id, Model model,
                                  RedirectAttributes redirectAttributes) {
        Optional<Order> order = orderService.getOrderById(id);
        if (order.isPresent()) {
            model.addAttribute("order", order.get());
            return "orders/detail";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found!");
            return "redirect:/orders";
        }
    }

    @PostMapping("/orders/{id}/transition")
    public String transitionOrder(@PathVariable Long id,
                                  @RequestParam String action,
                                  RedirectAttributes redirectAttributes) {
        try {
            orderService.transitionOrder(id, action);
            redirectAttributes.addFlashAttribute("successMessage", "Order updated successfully!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid action for current order status.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found!");
        }
        return "redirect:/orders/" + id;
    }
}
