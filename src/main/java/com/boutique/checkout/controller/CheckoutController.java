package com.boutique.checkout.controller;
import com.boutique.checkout.dto.*;
import com.boutique.checkout.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkouts")
public class CheckoutController {
 private final CheckoutService service;
 public CheckoutController(CheckoutService service){this.service=service;}
 @PostMapping public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request){return service.checkout(request);}
}
