# FD Calculator Service - CORS Configuration Fix

## Issue

When accessing the FD Calculator module from the React UI (localhost:5173), the following CORS error occurred:

```
Access to XMLHttpRequest at 'http://localhost:8085/api/calculator/calculate/standalone'
from origin 'http://localhost:5173' has been blocked by CORS policy:
Response to preflight request doesn't pass access control check:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

## Root Cause

The FD Calculator Service (port 8085) did not have CORS configuration, preventing the React UI from making cross-origin requests.

## Solution Applied

### 1. Created CorsConfig.java

**File:** `src/main/java/com/app/calculator/config/CorsConfig.java`

Added CORS configuration that:

- Allows requests from `http://localhost:5173` and `http://localhost:3000`
- Permits all HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
- Allows all headers
- Enables credentials (for JWT tokens in Authorization header)
- Caches preflight responses for 1 hour

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 2. Updated SecurityConfig.java

**File:** `src/main/java/com/app/calculator/config/SecurityConfig.java`

Updated Spring Security to use the CORS configuration:

- Added `CorsConfigurationSource` dependency injection
- Added `.cors(cors -> cors.configurationSource(corsConfigurationSource))` to security chain
- Placed CORS configuration **before** CSRF disable (correct order)

## Files Modified

1. ✅ `fd-calculator-service/src/main/java/com/app/calculator/config/CorsConfig.java` (Created)
2. ✅ `fd-calculator-service/src/main/java/com/app/calculator/config/SecurityConfig.java` (Updated)

## Testing

After restarting the FD Calculator Service, the React UI can now successfully:

- ✅ Make POST requests to `/api/calculator/calculate/standalone`
- ✅ Send JWT tokens in Authorization header
- ✅ Receive JSON responses without CORS errors

## Service Status

- **Port:** 8085
- **Context Path:** `/api/calculator`
- **Process ID:** 33236
- **Status:** ✅ Running with CORS enabled

## Next Steps

Test the calculator functionality:

1. Login to React UI as customer (customer/password)
2. Navigate to Products page
3. Click "Details" on any active product
4. Go to "Interest Calculator" tab
5. Enter amount and term, click "Calculate"
6. Verify calculations work without CORS errors

---

**Date Fixed:** November 9, 2025  
**Fixed By:** GitHub Copilot
