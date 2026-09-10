import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-spinner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './spinner.html',
  styleUrl: './spinner.css'
})
export class Spinner {
  @Input() label = 'Loading...';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
}


