// Deliberately memory-only: an XSS payload cannot read a persisted access token.
// The refresh token is held only in the HttpOnly cookie managed by Identity Service.
let accessToken = null;

export const setToken = (token) => {
  accessToken = token || null;
};

export const getToken = () => {
  return accessToken;
};

export const removeToken = () => {
  accessToken = null;
};
