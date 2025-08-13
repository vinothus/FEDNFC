package com.company.invoice.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller to handle base URL redirects based on authentication status
 * When users access the root URL, this controller serves a page that checks
 * localStorage for JWT tokens and redirects accordingly using JavaScript
 */
@Controller
@Slf4j
public class BaseRedirectController {

    /**
     * Handle base URL (/) redirects
     * Serves a HTML page that checks localStorage for JWT tokens and redirects:
     * - If authenticated: redirect to /invoice-automation/dashboard
     * - If not authenticated: redirect to /invoice-automation/login
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String handleBaseRedirect(HttpServletRequest request, HttpServletResponse response) {
        log.info("🔄 Base URL accessed from IP: {}, User-Agent: {}", 
                getClientIpAddress(request), 
                request.getHeader("User-Agent"));
        
        return createRedirectHtml();
    }

    /**
     * Handle /invoice-automation redirect (when users access the app prefix)
     * This ensures consistent behavior even when accessing the prefixed URL
     */
    @GetMapping(value = "/invoice-automation", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String handleAppRedirect(HttpServletRequest request, HttpServletResponse response) {
        log.info("🔄 App prefix URL accessed from IP: {}, User-Agent: {}", 
                getClientIpAddress(request), 
                request.getHeader("User-Agent"));
        
        return createRedirectHtml();
    }

    /**
     * Creates the HTML page that performs client-side authentication check
     * and redirects to the appropriate page
     */
    private String createRedirectHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Invoice Automation - Redirecting...</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                    }
                    .loading-container {
                        text-align: center;
                        padding: 2rem;
                        background: rgba(255, 255, 255, 0.1);
                        border-radius: 10px;
                        backdrop-filter: blur(10px);
                        box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
                    }
                    .spinner {
                        border: 3px solid rgba(255, 255, 255, 0.3);
                        border-radius: 50%;
                        border-top: 3px solid white;
                        width: 40px;
                        height: 40px;
                        animation: spin 1s linear infinite;
                        margin: 0 auto 1rem;
                    }
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                    .message {
                        font-size: 1.1rem;
                        margin-bottom: 0.5rem;
                    }
                    .sub-message {
                        font-size: 0.9rem;
                        opacity: 0.8;
                    }
                </style>
            </head>
            <body>
                <div class="loading-container">
                    <div class="spinner"></div>
                    <div class="message">Invoice Automation System</div>
                    <div class="sub-message">Checking authentication...</div>
                </div>

                <script>
                    // Function to check if user is authenticated
                    async function checkAuthentication() {
                        try {
                            // Check for access token in localStorage
                            const accessToken = localStorage.getItem('accessToken');
                            const user = localStorage.getItem('user');
                            
                            console.log('🔍 BaseRedirect: Checking authentication...');
                            console.log('Access Token:', accessToken ? 'Present' : 'Missing');
                            console.log('User Data:', user ? 'Present' : 'Missing');
                            
                            if (accessToken && user) {
                                // Basic token format validation (should be JWT)
                                const tokenParts = accessToken.split('.');
                                if (tokenParts.length === 3) {
                                    console.log('✅ BaseRedirect: Valid token format found');
                                    
                                    // Validate token with server to ensure it's still valid
                                    try {
                                        const response = await fetch('/invoice-automation/auth/me', {
                                            method: 'GET',
                                            headers: {
                                                'Authorization': 'Bearer ' + accessToken,
                                                'Content-Type': 'application/json'
                                            }
                                        });
                                        
                                        if (response.ok) {
                                            console.log('✅ BaseRedirect: Token validated with server, redirecting to dashboard...');
                                            window.location.href = '/invoice-automation/dashboard';
                                            return;
                                        } else {
                                            console.log('⚠️ BaseRedirect: Token validation failed, redirecting to login...');
                                        }
                                    } catch (fetchError) {
                                        console.log('⚠️ BaseRedirect: Server validation failed, redirecting to login...', fetchError);
                                    }
                                }
                            }
                            
                            console.log('❌ BaseRedirect: No valid authentication found, redirecting to login...');
                            // No valid authentication found, redirect to login
                            window.location.href = '/invoice-automation/login';
                            
                        } catch (error) {
                            console.error('🚨 BaseRedirect: Error checking authentication:', error);
                            // On error, redirect to login as fallback
                            window.location.href = '/invoice-automation/login';
                        }
                    }

                    // Start authentication check immediately
                    checkAuthentication();
                    
                    // Fallback redirect after 3 seconds in case something goes wrong
                    setTimeout(() => {
                        console.log('⏰ Fallback redirect triggered');
                        window.location.href = '/invoice-automation/login';
                    }, 3000);
                </script>
            </body>
            </html>
            """;
    }

    /**
     * Get the real client IP address, accounting for proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
