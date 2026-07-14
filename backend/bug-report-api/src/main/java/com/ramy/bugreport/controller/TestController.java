package com.ramy.bugreport.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class TestController {

    @GetMapping("/")
    public String getHelloWorld() {
        return "Hello World!";
    }

}
