import { describe, it, expect } from 'vitest';
import { AxiosError } from 'axios';
import { extractErrorMessage } from './client';

function axiosErrorWithResponse(data: unknown, status = 400): AxiosError {
  const err = new AxiosError('Request failed');
  err.response = {
    data,
    status,
    statusText: '',
    headers: {},
    // @ts-expect-error minimal fake config for the test
    config: {},
  };
  return err;
}

describe('extractErrorMessage', () => {
  it('joins field errors when the response has fieldErrors', () => {
    const err = axiosErrorWithResponse({ fieldErrors: { name: 'Name is required', email: 'Email is invalid' } });
    expect(extractErrorMessage(err)).toBe('Name is required, Email is invalid');
  });

  it('returns the response message when present', () => {
    const err = axiosErrorWithResponse({ message: 'Delivery challan not found' });
    expect(extractErrorMessage(err)).toBe('Delivery challan not found');
  });

  it('falls back to the raw axios error message when the response has neither', () => {
    const err = axiosErrorWithResponse({});
    expect(extractErrorMessage(err)).toBe('Request failed');
  });

  it('returns a generic server-error message for a JSON blob response', () => {
    const err = axiosErrorWithResponse(new Blob(['{}'], { type: 'application/json' }), 500);
    expect(extractErrorMessage(err)).toBe('Server error (500) — check backend logs');
  });

  it('returns a generic message for a non-axios error', () => {
    expect(extractErrorMessage(new Error('boom'))).toBe('An unexpected error occurred');
    expect(extractErrorMessage('some string')).toBe('An unexpected error occurred');
  });
});
