import axios from 'axios';

// Single Axios instance for the whole app.
// "/api" works in dev (Vite proxy) and in production (nginx).
const httpClient = axios.create({
  baseURL: '/api',
});

export default httpClient;