package com.ai_resume.trends_service.scheduler;

import com.ai_resume.trends_service.dto.LabelCount;
import com.ai_resume.trends_service.dto.NotificationWeeklyTrendsRequest;
import com.ai_resume.trends_service.service.NotificationClientService;
import com.ai_resume.trends_service.service.TrendsService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Weekly job: trends-service computes the analytics (it owns that data),
 * builds the email payload, and asks notification-service to render + send
 * it. notification-service never calculates trend data itself.
 *
 * <p>Runs on a cron schedule so a stalled/late run never blocks anything else
 * in the system - this is a background job, not a request/response flow.
 */
@Component
@Slf4j
public class WeeklyTrendsScheduler {

    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d");
    private static final int TOP_N = 5;

    private final TrendsService trendsService;
    private final NotificationClientService notificationClientService;
    private final List<String> recipients;

    public WeeklyTrendsScheduler(
            TrendsService trendsService,
            NotificationClientService notificationClientService,
            @Value("${notification.weekly-trends.recipients:}") String recipientsCsv) {
        this.trendsService = trendsService;
        this.notificationClientService = notificationClientService;
        this.recipients = Arrays.stream(recipientsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** Default: every Monday at 07:00 server time. Overridable via env var. */
    @Scheduled(cron = "${notification.weekly-trends.cron:0 0 7 * * MON}")
    public void sendWeeklyTrendsEmail() {
        if (recipients.isEmpty()) {
            log.info("No weekly trends recipients configured; skipping this run");
            return;
        }

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(7);

        List<String> trendingSkills = topLabels(trendsService.getTopSuggestedSkills(from, to, TOP_N));
        List<String> mostAnalyzedJobRoles = topLabels(trendsService.getTopJobTitles(from, to, TOP_N));
        List<String> atsImprovements = topLabels(trendsService.getTopMissingSkills(from, to, TOP_N));
        long uploadsThisWeek = trendsService.getUploadActivity(from, to, null).stream()
                .mapToLong(point -> point.getCount())
                .sum();

        List<String> weeklyInsights = List.of(
                uploadsThisWeek + " resumes analyzed this week",
                trendingSkills.isEmpty()
                        ? "Not enough data yet to surface trending skills"
                        : "Top requested skill: " + trendingSkills.get(0));

        NotificationWeeklyTrendsRequest request = NotificationWeeklyTrendsRequest.builder()
                .recipientEmails(recipients)
                .weekRangeLabel(from.format(LABEL_FORMAT) + " - " + to.format(LABEL_FORMAT) + ", " + to.getYear())
                .trendingSkills(trendingSkills)
                .mostAnalyzedJobRoles(mostAnalyzedJobRoles)
                .atsImprovements(atsImprovements)
                .weeklyInsights(weeklyInsights)
                .build();

        try {
            notificationClientService.sendWeeklyTrendsEmail(request);
            log.info("Weekly trends email requested for {} recipient(s)", recipients.size());
        } catch (Exception ex) {
            // A failed weekly email must never fail the scheduled job itself.
            log.warn("Weekly trends email could not be requested: {}", ex.getMessage());
        }
    }

    private List<String> topLabels(List<LabelCount> labelCounts) {
        return labelCounts.stream().map(LabelCount::getLabel).toList();
    }
}

