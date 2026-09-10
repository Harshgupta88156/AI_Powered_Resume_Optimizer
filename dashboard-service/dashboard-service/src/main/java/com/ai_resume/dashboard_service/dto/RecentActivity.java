package com.ai_resume.dashboard_service.dto;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.client.dto.NotificationView;
import com.ai_resume.dashboard_service.client.dto.ResumeView;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The "recent activity" panel.
 *
 * <p>The upstream view objects are passed straight through rather than re-mapped
 * into near-identical dashboard types: a copy would add no information and would
 * silently drop any field added upstream.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivity {

    private List<ResumeView> recentResumes;
    private List<AnalysisView> recentAnalyses;
    private List<NotificationView> recentNotifications;
}

