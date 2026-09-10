package com.ai_resume.dashboard_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.dto.DashboardInsights;
import com.ai_resume.dashboard_service.dto.LabelCount;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsightBuilderTest {

    private final InsightBuilder insightBuilder = new InsightBuilder();

    @Test
    void ranksMissingSkillsByFrequency() {
        DashboardInsights insights = insightBuilder.build(snapshot(List.of(
                analysis(List.of("Kubernetes", "Docker"), List.of()),
                analysis(List.of("Kubernetes"), List.of()),
                analysis(List.of("Kubernetes", "Terraform"), List.of()))), 10);

        List<LabelCount> top = insights.getTopMissingSkills();
        assertThat(top.get(0).getLabel()).isEqualTo("Kubernetes");
        assertThat(top.get(0).getCount()).isEqualTo(3);
        // Docker and Terraform tie on 1 and are ordered alphabetically for stability.
        assertThat(top).extracting(LabelCount::getLabel)
                .containsExactly("Kubernetes", "Docker", "Terraform");
    }

    @Test
    void countsSkillsCaseInsensitivelyButKeepsTheFirstSpelling() {
        DashboardInsights insights = insightBuilder.build(snapshot(List.of(
                analysis(List.of("React"), List.of()),
                analysis(List.of("react"), List.of()),
                analysis(List.of("REACT"), List.of()))), 10);

        // One entry, not three — otherwise casing noise crowds out real skills.
        assertThat(insights.getTopMissingSkills()).hasSize(1);
        assertThat(insights.getTopMissingSkills().get(0).getLabel()).isEqualTo("React");
        assertThat(insights.getTopMissingSkills().get(0).getCount()).isEqualTo(3);
    }

    @Test
    void topTechnologiesCombinesMissingAndRecommendedSkills() {
        DashboardInsights insights = insightBuilder.build(snapshot(List.of(
                analysis(List.of("AWS"), List.of("AWS")),
                analysis(List.of("Go"), List.of()))), 10);

        assertThat(insights.getTopTechnologies().get(0).getLabel()).isEqualTo("AWS");
        assertThat(insights.getTopTechnologies().get(0).getCount()).isEqualTo(2);
    }

    @Test
    void ranksCompaniesAndIgnoresBlanks() {
        AnalysisView acme1 = analysis(List.of(), List.of());
        acme1.setCompany("Acme");
        AnalysisView acme2 = analysis(List.of(), List.of());
        acme2.setCompany("Acme");
        AnalysisView blank = analysis(List.of(), List.of());
        blank.setCompany("   ");
        AnalysisView missing = analysis(List.of(), List.of());

        DashboardInsights insights = insightBuilder.build(
                snapshot(List.of(acme1, acme2, blank, missing)), 10);

        assertThat(insights.getMostAnalyzedCompanies()).hasSize(1);
        assertThat(insights.getMostAnalyzedCompanies().get(0).getCount()).isEqualTo(2);
    }

    @Test
    void respectsTheRequestedLimit() {
        DashboardInsights insights = insightBuilder.build(snapshot(List.of(
                analysis(List.of("A", "B", "C", "D", "E"), List.of()))), 3);

        assertThat(insights.getTopMissingSkills()).hasSize(3);
    }

    // ------------------------------ fixtures ------------------------------

    private DashboardData snapshot(List<AnalysisView> analyses) {
        return new DashboardData(
                List.of(), analyses, 0, analyses.size(), 0, -1, List.of(), List.of(), false);
    }

    private AnalysisView analysis(List<String> missing, List<String> suggested) {
        AnalysisView view = new AnalysisView();
        view.setStatus("COMPLETED");
        view.setMissingSkills(missing);
        view.setSuggestedSkills(suggested);
        return view;
    }
}

