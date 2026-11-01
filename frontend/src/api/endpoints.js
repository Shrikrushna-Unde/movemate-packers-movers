import api from './client.js';

/**
 * Every backend call the app makes, in one file.
 *
 * This is a thin "service layer" for the frontend. The value is not the few lines of axios it
 * wraps - it is that no component contains a URL string. When the backend renames
 * `/api/v1/quotes/estimate`, exactly one line changes here instead of a grep across the app.
 * It also makes the whole API surface readable at a glance, which is a genuine help in a
 * code review or an interview walkthrough.
 *
 * Each function returns `response.data` so components deal with plain objects, never with
 * axios response envelopes.
 */

// ---------------------------------------------------------------- auth

export const authApi = {
  register: (payload) => api.post('/auth/register', payload).then((r) => r.data),
  login: (payload) => api.post('/auth/login', payload).then((r) => r.data),
  me: () => api.get('/auth/me').then((r) => r.data),
};

// ---------------------------------------------------------------- public catalogue

export const catalogApi = {
  services: () => api.get('/catalog/services').then((r) => r.data),
  service: (slug) => api.get(`/catalog/services/${slug}`).then((r) => r.data),
  /** Dropdown options generated from the backend enums - never hardcode these in React. */
  options: () => api.get('/catalog/options').then((r) => r.data),
};

// ---------------------------------------------------------------- quotes & bookings

export const quoteApi = {
  estimate: (payload) => api.post('/quotes/estimate', payload).then((r) => r.data),
};

export const bookingApi = {
  create: (payload) => api.post('/bookings', payload).then((r) => r.data),
  /**
   * Tracking takes query parameters. Passing them through axios `params` rather than building
   * the string by hand means axios URL-encodes them, so a reference or phone number containing
   * a special character cannot break the request.
   */
  track: (reference, phone) =>
    api.get('/bookings/track', { params: { reference, phone } }).then((r) => r.data),
  mine: () => api.get('/bookings/me').then((r) => r.data),
};

// ---------------------------------------------------------------- testimonials & contact

export const testimonialApi = {
  approved: () => api.get('/testimonials').then((r) => r.data),
  submit: (payload) => api.post('/testimonials', payload).then((r) => r.data),
};

export const contactApi = {
  submit: (payload) => api.post('/contact', payload).then((r) => r.data),
};

// ---------------------------------------------------------------- admin

export const adminApi = {
  stats: () => api.get('/admin/stats').then((r) => r.data),

  /**
   * `params` is spread so callers pass only what they need:
   *   adminApi.bookings({ status: 'PENDING', page: 0 })
   * Axios omits undefined values from the query string entirely, which is exactly what the
   * backend's optional filters expect.
   */
  bookings: (params = {}) => api.get('/admin/bookings', { params }).then((r) => r.data),
  booking: (reference) => api.get(`/admin/bookings/${reference}`).then((r) => r.data),
  updateBookingStatus: (id, payload) =>
    api.patch(`/admin/bookings/${id}/status`, payload).then((r) => r.data),

  enquiries: (params = {}) => api.get('/admin/enquiries', { params }).then((r) => r.data),
  setEnquiryHandled: (id, handled) =>
    api.patch(`/admin/enquiries/${id}/handled`, null, { params: { handled } }).then((r) => r.data),

  testimonials: (params = {}) => api.get('/admin/testimonials', { params }).then((r) => r.data),
  setTestimonialApproval: (id, approved) =>
    api
      .patch(`/admin/testimonials/${id}/approval`, null, { params: { approved } })
      .then((r) => r.data),
  deleteTestimonial: (id) => api.delete(`/admin/testimonials/${id}`).then((r) => r.data),

  services: () => api.get('/admin/services').then((r) => r.data),
  createService: (payload) => api.post('/admin/services', payload).then((r) => r.data),
  updateService: (id, payload) => api.put(`/admin/services/${id}`, payload).then((r) => r.data),
  toggleService: (id) => api.patch(`/admin/services/${id}/toggle`).then((r) => r.data),
};
