import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  label: string;
  path: string;
  /** Inline SVG path data so the nav does not depend on an icon font loading. */
  icon: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {

  @Input() open = false;

  /** Lets the shell close the drawer once a link is followed on mobile. */
  @Output() navigate = new EventEmitter<void>();

  navItems: NavItem[] = [
    // Explicit route back to the public landing page. The header logo goes to
    // the dashboard, so signed-in users previously had no way back to Home
    // short of editing the URL.
    { label: 'Home',             path: '/',                 icon: 'M3 11l9-8 9 8M5 10v10h14V10' },
    { label: 'Dashboard',        path: '/dashboard',        icon: 'M3 3h8v8H3zM13 3h8v5h-8zM13 10h8v11h-8zM3 13h8v8H3z' },
    { label: 'Resumes',          path: '/resumes',          icon: 'M6 2h8l4 4v16H6zM14 2v4h4M9 12h6M9 16h6' },
    { label: 'Job Descriptions', path: '/job-descriptions', icon: 'M4 4h16v16H4zM8 9h8M8 13h8M8 17h4' },
    { label: 'Analysis',         path: '/analysis',         icon: 'M12 3a9 9 0 1 0 9 9h-9z M12 3v9h9' },
    { label: 'History',          path: '/history',          icon: 'M12 3a9 9 0 1 0 9 9M12 7v5l3 2' },
    { label: 'Trends',           path: '/trends',           icon: 'M3 17l6-6 4 4 7-7M14 8h6v6' },
    { label: 'Profile',          path: '/profile',          icon: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0' }
  ];

  onNavigate(): void {
    this.navigate.emit();
  }
}
