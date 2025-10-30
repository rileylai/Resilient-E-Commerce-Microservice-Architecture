import http from "../http-common";

class AuthService {
  login(username, password) {
    return http.post("/user/login", {
      username,
      password
    });
  }

  register(username, password, email) {
    return http.post("/user/register", {
      username,
      password,
      email
    });
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      return JSON.parse(userStr);
    }
    return null;
  }

  isAuthenticated() {
    return !!localStorage.getItem('token');
  }
}

export default new AuthService();
