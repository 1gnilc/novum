# Client IP Resolution Reference

## Purpose

The original requirements pointed to an `IpUtils.java` file in an archive outside this repository. This appendix preserves the relevant source shape so the requirement remains understandable on any development machine. The archived implementation is reference material only; the confirmed behavior in the main specification is authoritative.

## Archived Source Excerpt

The original helper resolved several compatibility headers and then selected the first comma-separated value:

```java
public static String getRequestIp() {
    ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }
    return null;
}

private static boolean isInvalidIp(String ip) {
    return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
}
```

## Confirmed Novum Adaptation

Novum deliberately narrows and strengthens that behavior:

```text
resolveClientIp(request):
    for value in split(request.header("X-Forwarded-For"), ",") from left to right:
        candidate = trim(value)
        if candidate is a valid IPv4 or IPv6 literal:
            return normalized candidate

    candidate = trim(request.getRemoteAddr())
    if candidate is a valid IPv4 or IPv6 literal:
        return normalized candidate

    return "unknown"
```

- Process only `X-Forwarded-For` and `request.getRemoteAddr()`.
- Validate each candidate as an IP literal without DNS resolution.
- Skip blank, `unknown`, and malformed forwarded values rather than blindly accepting the first token.
- Do not process `Forwarded`, `Proxy-Client-IP`, `WL-Proxy-Client-IP`, `HTTP_CLIENT_IP`, or `HTTP_X_FORWARDED_FOR`.
- Do not reuse the archived helper's ip2region IPv4 lookup. Novum uses the separately confirmed IP2Location LITE DB3 IPv6 integration.
