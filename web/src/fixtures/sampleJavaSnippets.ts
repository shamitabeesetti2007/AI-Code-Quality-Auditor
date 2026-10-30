/**
 * Sample Java Source Code Test Fixtures
 * Grounded in Project Bible: Section 1.3, Section 4, Section 14 (Testing Strategy).
 */

/**
 * SEC-001 Positive Test Fixture (LoginDao.java)
 * Vulnerable to SQL injection via concatenation into Statement.executeQuery().
 * From Project Bible Section 1.3 & Section 14.
 */
export const SAMPLE_VULNERABLE_LOGIN_DAO = `package com.example.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LoginDao {
    // SEC-001 positive test fixture: SQL built by string concatenation
    public boolean authenticate(Connection conn, String user, String pass) throws SQLException {
        Statement st = conn.createStatement();
        String query = "SELECT * FROM accounts WHERE username='" + user + "' AND password='" + pass + "'";
        ResultSet rs = st.executeQuery(query);
        return rs.next();
    }
}`;

/**
 * SEC-001 Remediated Source Fixture (LoginDao.java)
 * Verified clean fix using parameterized PreparedStatement with '?' placeholders.
 */
export const SAMPLE_REMEDIATED_LOGIN_DAO = `package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDao {
    // Verified remediation: Parameterized query eliminates SQL injection
    public boolean authenticate(Connection conn, String user, String pass) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE username = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}`;

/**
 * COMP-001 High Complexity Fixture (OrderProcessingService.java)
 * Demonstrates high cyclomatic (> 15) and cognitive complexity with nested loops.
 */
export const SAMPLE_COMPLEX_SERVICE = `package com.example.service;

import java.util.List;

public class OrderProcessingService {
    // COMP-001: High Cyclomatic (14 decision points) & deep cognitive nesting
    public double processDiscount(List<String> items, String tier, boolean isHoliday, int loyaltyPoints) {
        double discount = 0.0;
        if (items != null && !items.isEmpty()) {
            for (String item : items) {
                if (item.startsWith("VIP_")) {
                    if ("PLATINUM".equalsIgnoreCase(tier)) {
                        discount += isHoliday ? 0.25 : 0.20;
                    } else if ("GOLD".equalsIgnoreCase(tier)) {
                        discount += 0.15;
                    } else {
                        discount += 0.05;
                    }
                } else if (item.startsWith("SALE_")) {
                    discount += 0.10;
                }
            }
            if (loyaltyPoints > 1000) {
                discount += 0.05;
            }
        }
        return Math.min(discount, 0.50);
    }
}`;

/**
 * DUP-001 Clean Token Cache Fixture (TokenCache.java)
 * Clean Java class passing Duplication, Security, and Maintainability checks.
 */
export const SAMPLE_CLEAN_TOKEN_CACHE = `package com.example.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenCache {
    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    public void put(String token, long expiryEpoch) {
        if (token != null && !token.isBlank()) {
            cache.put(token, expiryEpoch);
        }
    }

    public boolean isValid(String token) {
        Long expiry = cache.get(token);
        return expiry != null && expiry > System.currentTimeMillis();
    }
}`;

/**
 * Syntax Error Test Fixture (BrokenSyntax.java)
 * Demonstrates PARSE_FAILED behavior: syntax errors become SYN-001 findings with grade 'N/A'.
 */
export const SAMPLE_SYNTAX_ERROR_SNIPPET = `package com.example.broken;

public class BrokenSyntax {
    public void unclosedMethod() {
        System.out.println("Missing closing brace and semicolon"
    // Syntax error on purpose to test Language Gate & JavaParser error outcome
`;

/**
 * Non-Java Rejection Test Fixture (Python snippet)
 * Used to test Language Gate HTTP 422 rejection (no Java type declaration).
 */
export const SAMPLE_NON_JAVA_SNIPPET = `def authenticate_user(username, password):
    # This is Python code, not Java source
    print(f"Authenticating user: {username}")
    return True
`;
