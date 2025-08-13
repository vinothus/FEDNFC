package com.company.invoice.api.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Frontend controller to handle React Router routes
 * Forwards all non-API routes to index.html for SPA routing
 * Also implements ErrorController to handle 404s
 */
@Controller
public class FrontendController implements ErrorController {

    /**
     * Forward React Router routes to index.html
     * This allows React Router to handle client-side routing
     */
    @RequestMapping(value = {
        "/dashboard",
        "/dashboard/**",
        "/invoices",
        "/invoices/**", 
        "/upload",
        "/upload/**",
        "/patterns",
        "/patterns/**",
        "/users", 
        "/users/**",
        "/settings",
        "/settings/**",
        "/login"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
    
    /**
     * Handle errors (including 404s) by forwarding to index.html
     * This allows React Router to handle unknown routes
     */
    @RequestMapping("/error")
    public String handleError() {
        return "forward:/index.html";
    }

}
