
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TrendsService } from '../../../core/services/trends.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { LabelCount, TimeSeriesPoint } from '../../../core/models/trends.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { BarChart } from '../../../shared/components/bar-chart/bar-chart';
import { DonutChart } from '../../../shared/components/donut-chart/donut-chart';
import { LineChart, SeriesPoint } from '../../../shared/components/line-chart/line-chart';

@Component({
  selector: 'app-trends-page',
  standalone: true,
  imports: [CommonModule, Spinner, EmptyState, BarChart, DonutChart, LineChart],
  templateUrl: './trends-page.html',
  styleUrl: './trends-page.css'
})
export class TrendsPage implements OnInit {

  private trendsService = inject(TrendsService);

  loading = signal(true);
  error = signal('');

  missingSkills = signal<LabelCount[]>([]);
  suggestedSkills = signal<LabelCount[]>([]);
  technologies = signal<LabelCount[]>([]);
  companies = signal<LabelCount[]>([]);
  jobTitles = signal<LabelCount[]>([]);
  uploadActivity = signal<TimeSeriesPoint[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    const params = { limit: 10 };

    Promise.all([
      this.trendsService.missingSkills(params).toPromise(),
      this.trendsService.suggestedSkills(params).toPromise(),
      this.trendsService.technologies(params).toPromise(),
      this.trendsService.companies(params).toPromise(),
      this.trendsService.jobTitles(params).toPromise(),
      this.trendsService.uploadActivity({
        granularity: 'DAY'
      }).toPromise()
    ])
      .then(([missing, suggested, tech, companies, titles, activity]) => {

        this.missingSkills.set(missing ?? []);
        this.suggestedSkills.set(suggested ?? []);
        this.technologies.set(tech ?? []);
        this.companies.set(companies ?? []);
        this.jobTitles.set(titles ?? []);
        this.uploadActivity.set(activity ?? []);

        this.loading.set(false);
      })
      .catch((err) => {

        this.error.set(
          extractErrorMessage(
            err,
            'Could not load trends data.'
          )
        );

        this.loading.set(false);
      });
  }

  hasAnyData(): boolean {
    return this.missingSkills().length > 0
      || this.suggestedSkills().length > 0
      || this.technologies().length > 0
      || this.companies().length > 0
      || this.jobTitles().length > 0
      || this.uploadActivity().length > 0;
  }

  get leadingTechnology(): LabelCount | null {
    return this.technologies()[0] ?? null;
  }

  get leadingSkill(): LabelCount | null {
    return this.suggestedSkills()[0]
      ?? this.missingSkills()[0]
      ?? null;
  }

  get leadingJobTitle(): LabelCount | null {
    return this.jobTitles()[0] ?? null;
  }

  get leadingCompany(): LabelCount | null {
    return this.companies()[0] ?? null;
  }

  get topSkills(): LabelCount[] {
    return this.suggestedSkills().slice(0, 8);
  }

  get topTechnologies(): LabelCount[] {
    return this.technologies().slice(0, 8);
  }

  get topMissingSkills(): LabelCount[] {
    return this.missingSkills().slice(0, 8);
  }

  get topJobTitles(): LabelCount[] {
    return this.jobTitles().slice(0, 8);
  }

  get topCompanies(): LabelCount[] {
    return this.companies().slice(0, 8);
  }

  /** Maps the API's TimeSeriesPoint onto the chart's generic shape. */
  get activitySeries(): SeriesPoint[] {
    return this.uploadActivity().map(p => ({
      label: p.label || p.date,
      value: p.count
    }));
  }

  formatCount(value: number): string {
    return new Intl.NumberFormat('en-US').format(value);
  }
}

