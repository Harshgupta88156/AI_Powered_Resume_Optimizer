import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-features',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './features.html',
  styleUrls: ['./features.css']
})
export class Features {

  features = [
    {
      title: 'AI Resume Analysis',
      icon: '🧠',
      description:
        'Analyze resumes using AI to identify strengths, weaknesses and provide actionable recommendations.'
    },
    {
      title: 'ATS Compatibility',
      icon: '✅',
      description:
        'Improve ATS score by matching keywords, formatting and job description requirements.'
    },
    {
      title: 'Resume History',
      icon: '📄',
      description:
        'Store every uploaded resume and every analysis result for future comparison.'
    },
    {
      title: 'Job Description Matching',
      icon: '🎯',
      description:
        'Compare resumes against any job description and calculate the overall match percentage.'
    },
    {
      title: 'Skill Gap Detection',
      icon: '📈',
      description:
        'Identify missing technical and soft skills that recruiters expect for a role.'
    },
    {
      title: 'Enterprise Dashboard',
      icon: '📊',
      description:
        'Track all analyses, resumes and recommendations through a professional dashboard.'
    },
    {
      title: 'Trend Analytics',
      icon: '📉',
      description:
        'View hiring trends, in-demand technologies and popular skills across industries.'
    },
    {
      title: 'Secure Cloud Storage',
      icon: '🔒',
      description:
        'Store resumes securely with cloud integration and enterprise-grade architecture.'
    }
  ];

}
