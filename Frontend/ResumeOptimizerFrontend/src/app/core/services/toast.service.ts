import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: number;
  type: ToastType;
  text: string;
}

let nextId = 1;

@Injectable({ providedIn: 'root' })
export class ToastService {

  toasts = signal<ToastMessage[]>([]);

  private push(type: ToastType, text: string, durationMs = 4000): void {
    const id = nextId++;
    this.toasts.update(list => [...list, { id, type, text }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }

  success(text: string): void {
    this.push('success', text);
  }

  error(text: string): void {
    this.push('error', text, 5500);
  }

  info(text: string): void {
    this.push('info', text);
  }

  warning(text: string): void {
    this.push('warning', text);
  }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}

