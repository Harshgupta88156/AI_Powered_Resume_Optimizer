import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extracts a user-friendly message from a backend error response.
 * Handles the GlobalExceptionHandler format: { status, error, message, errors? }
 */
export function extractErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object') {
      if (body.errors && typeof body.errors === 'object') {
        const fieldErrors = Object.values(body.errors as Record<string, string>);
        if (fieldErrors.length) {
          return fieldErrors.join(' ');
        }
      }
      if (typeof body.message === 'string' && body.message.trim()) {
        return body.message;
      }
    }
    if (error.status === 0) {
      return 'Unable to reach the server. Please check your connection.';
    }
  }
  return fallback;
}

