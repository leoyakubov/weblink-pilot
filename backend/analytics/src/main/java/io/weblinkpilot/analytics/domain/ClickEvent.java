package io.weblinkpilot.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 32)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private OffsetDateTime clickedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "referrer", length = 2048)
    private String referrer;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "browser_family", length = 64)
    private String browserFamily;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    protected ClickEvent() {
    }

    public ClickEvent(String shortCode,
                      OffsetDateTime clickedAt,
                      String ipAddress,
                      String userAgent,
                      String referrer,
                      String country,
                      String browserFamily,
                      String deviceType) {
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.country = country;
        this.browserFamily = browserFamily;
        this.deviceType = deviceType;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public OffsetDateTime getClickedAt() {
        return clickedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getCountry() {
        return country;
    }

    public String getBrowserFamily() {
        return browserFamily;
    }

    public String getDeviceType() {
        return deviceType;
    }
}
