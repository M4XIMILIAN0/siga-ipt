import axios from 'axios';
import keycloak from '../auth/keycloak';

const httpClient = axios.create({
  baseURL: '/api',
});

// Runs BEFORE every request: refreshes the token if needed and adds it to the header
httpClient.interceptors.request.use(async (config) => {
  try {
    await keycloak.updateToken(30);   // refresh if it expires in less than 30 seconds
  } catch (err) {
    keycloak.login();                 // session fully expired -> log in again
  }
  config.headers.Authorization = `Bearer ${keycloak.token}`;
  return config;
});

export default httpClient;