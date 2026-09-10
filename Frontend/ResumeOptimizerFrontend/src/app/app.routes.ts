import { Routes } from '@angular/router';

import { Landing } from './features/landing/landing';
import { Dashboard } from './features/dashboard/dashboard';
import { MainLayout } from './layouts/main-layout/main-layout';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { OauthCallback } from './features/auth/oauth-callback/oauth-callback';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: Landing,
    title: 'Resume Optimizer',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: Login,
    title: 'Sign in - Resume Optimizer',
    canActivate: [guestGuard]
  },
  {
    path: 'register',
    component: Register,
    title: 'Create account - Resume Optimizer',
    canActivate: [guestGuard]
  },
  {
    path: 'oauth2/callback',
    component: OauthCallback
  },
  {
    path: 'callback',
    component: OauthCallback
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        title: 'Dashboard - Resume Optimizer',
        component: Dashboard
      },
      {
        path: 'resumes',
        title: 'Resumes - Resume Optimizer',
        loadComponent: () => import('./features/resumes/resume-list/resume-list').then(m => m.ResumeList)
      },
      {
        path: 'resumes/:id',
        title: 'Resume - Resume Optimizer',
        loadComponent: () => import('./features/resumes/resume-detail/resume-detail').then(m => m.ResumeDetail)
      },
      {
        path: 'job-descriptions',
        title: 'Job descriptions - Resume Optimizer',
        loadComponent: () => import('./features/job-descriptions/jd-list/jd-list').then(m => m.JdList)
      },
      {
        path: 'analysis',
        title: 'Analysis - Resume Optimizer',
        loadComponent: () => import('./features/analysis/analysis-trigger/analysis-trigger').then(m => m.AnalysisTrigger)
      },
      {
        path: 'analysis/:id',
        title: 'Analysis result - Resume Optimizer',
        loadComponent: () => import('./features/analysis/analysis-result/analysis-result').then(m => m.AnalysisResult)
      },
      {
        path: 'history',
        title: 'History - Resume Optimizer',
        loadComponent: () => import('./features/history/history-list/history-list').then(m => m.HistoryList)
      },
      {
        path: 'trends',
        title: 'Trends - Resume Optimizer',
        loadComponent: () => import('./features/trends/trends-page/trends-page').then(m => m.TrendsPage)
      },
      {
        path: 'profile',
        title: 'Profile - Resume Optimizer',
        loadComponent: () => import('./features/profile/profile-page/profile-page').then(m => m.ProfilePage)
      }
    ]
  },
  {
    // A real 404 page. Redirecting unknown URLs to '' silently swallowed
    // typos and stale links, so a mistyped /analysis/9999 looked like the app
    // had logged the user out.
    path: '**',
    loadComponent: () => import('./features/not-found/not-found').then(m => m.NotFound),
    title: 'Page not found - Resume Optimizer'
  }
];
