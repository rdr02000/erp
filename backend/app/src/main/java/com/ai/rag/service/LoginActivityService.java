package com.ai.rag.service;

import com.ai.rag.dto.AuthApiModels.LoginActivityView;
import com.ai.rag.dto.AuthApiModels.LoginLocation;
import com.ai.rag.entity.LoginActivity;
import com.ai.rag.repository.AppUserRepository;
import com.ai.rag.repository.LoginActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LoginActivityService {

    private static final int MAX_USER_AGENT_LENGTH = 2048;

    private final AppUserRepository appUserRepository;
    private final LoginActivityRepository loginActivityRepository;

    @Transactional
    public void recordSuccessfulLogin(
            long userId,
            String userAgentHeader,
            LoginLocation location) {
        String userAgent = truncate(userAgentHeader, MAX_USER_AGENT_LENGTH);
        ClientDetails details = parseClient(userAgent);
        LoginActivity activity = new LoginActivity(
                appUserRepository.getReferenceById(userId),
                details.deviceType(),
                details.browser(),
                details.operatingSystem(),
                userAgent,
                location == null ? null : location.latitude(),
                location == null ? null : location.longitude(),
                location == null ? null : location.accuracyMeters());
        loginActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<LoginActivityView> getRecentActivity(long userId) {
        return loginActivityRepository.findTop50ByUser_IdOrderByLoggedAtDesc(userId)
                .stream()
                .map(activity -> new LoginActivityView(
                        activity.getId(),
                        activity.getDeviceType(),
                        activity.getBrowser(),
                        activity.getOperatingSystem(),
                        activity.getLatitude(),
                        activity.getLongitude(),
                        activity.getAccuracyMeters(),
                        activity.getLoggedAt()))
                .toList();
    }

    private static ClientDetails parseClient(String userAgent) {
        String normalized = userAgent == null ? "" : userAgent;
        String lowerCase = normalized.toLowerCase();
        return new ClientDetails(
                deviceType(lowerCase),
                browser(normalized, lowerCase),
                operatingSystem(lowerCase));
    }

    private static String deviceType(String userAgent) {
        if (userAgent.contains("ipad") || userAgent.contains("tablet")) {
            return "Tablet";
        }
        if (userAgent.contains("mobile") || userAgent.contains("iphone")
                || userAgent.contains("android")) {
            return "Mobile";
        }
        return userAgent.isBlank() ? "Unknown" : "Desktop";
    }

    private static String browser(String userAgent, String lowerCase) {
        if (lowerCase.contains("edg/")) return browserWithVersion("Edge", userAgent, "Edg");
        if (lowerCase.contains("opr/")) return browserWithVersion("Opera", userAgent, "OPR");
        if (lowerCase.contains("firefox/")) return browserWithVersion("Firefox", userAgent, "Firefox");
        if (lowerCase.contains("chrome/")) return browserWithVersion("Chrome", userAgent, "Chrome");
        if (lowerCase.contains("safari/")) return browserWithVersion("Safari", userAgent, "Version");
        if (lowerCase.contains("msie ") || lowerCase.contains("trident/")) return "Internet Explorer";
        return userAgent.isBlank() ? "Unknown" : "Other";
    }

    private static String browserWithVersion(String browser, String userAgent, String marker) {
        Matcher matcher = Pattern.compile(
                        Pattern.quote(marker) + "/([0-9]+(?:\\.[0-9]+)*)",
                        Pattern.CASE_INSENSITIVE)
                .matcher(userAgent);
        return matcher.find() ? browser + " " + matcher.group(1) : browser;
    }

    private static String operatingSystem(String userAgent) {
        if (userAgent.contains("windows")) return "Windows";
        if (userAgent.contains("iphone") || userAgent.contains("ipad")
                || userAgent.contains("ipod")) return "iOS";
        if (userAgent.contains("android")) return "Android";
        if (userAgent.contains("mac os") || userAgent.contains("macintosh")) return "macOS";
        if (userAgent.contains("linux")) return "Linux";
        return userAgent.isBlank() ? "Unknown" : "Other";
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ClientDetails(String deviceType, String browser, String operatingSystem) {
    }
}
