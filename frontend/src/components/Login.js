import React, { Component } from "react";
import AuthService from "../services/auth.service";
import jwtDecode from "jwt-decode";

export default class Login extends Component {
  constructor(props) {
    super(props);
    this.onChangeUsername = this.onChangeUsername.bind(this);
    this.onChangePassword = this.onChangePassword.bind(this);
    this.handleLogin = this.handleLogin.bind(this);

    this.state = {
      username: "",
      password: "",
      loading: false,
      message: ""
    };
  }

  onChangeUsername(e) {
    this.setState({
      username: e.target.value
    });
  }

  onChangePassword(e) {
    this.setState({
      password: e.target.value
    });
  }

  handleLogin(e) {
    e.preventDefault();

    this.setState({
      message: "",
      loading: true
    });

    AuthService.login(this.state.username, this.state.password)
      .then(response => {
        // Check if login was successful by checking the code field
        if (response.data.code !== 200) {
          // Login failed
          this.setState({
            loading: false,
            message: response.data.message || 'Login failed'
          });
          return;
        }

        const token = response.data.data || response.data;
        localStorage.setItem('token', token);

        // Decode JWT to get user info
        try {
          const decoded = jwtDecode(token);
          localStorage.setItem('user', JSON.stringify({
            id: decoded.userId,
            username: decoded.username
          }));
        } catch (e) {
          console.error('Failed to decode token:', e);
          this.setState({
            loading: false,
            message: 'Invalid token received'
          });
          return;
        }

        window.location.href = '/products';
      })
      .catch(error => {
        const resMessage =
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();

        this.setState({
          loading: false,
          message: resMessage
        });
      });
  }

  render() {
    return (
      <div className="col-md-12">
        <div className="card card-container">
          <div className="submit-form">
            <h3>Login</h3>
            <form onSubmit={this.handleLogin}>
              <div className="form-group">
                <label htmlFor="username">Username</label>
                <input
                  type="text"
                  className="form-control"
                  name="username"
                  value={this.state.username}
                  onChange={this.onChangeUsername}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="password">Password</label>
                <input
                  type="password"
                  className="form-control"
                  name="password"
                  value={this.state.password}
                  onChange={this.onChangePassword}
                  required
                />
              </div>

              <div className="form-group mt-3">
                <button
                  className="btn btn-primary btn-block"
                  disabled={this.state.loading}
                >
                  {this.state.loading && (
                    <span className="spinner-border spinner-border-sm"></span>
                  )}
                  <span>Login</span>
                </button>
              </div>

              {this.state.message && (
                <div className="form-group mt-3">
                  <div className="alert alert-danger" role="alert">
                    {this.state.message}
                  </div>
                </div>
              )}

              <div className="form-group mt-3">
                <p>
                  Don't have an account? <a href="/register">Register here</a>
                </p>
              </div>
            </form>
          </div>
        </div>
      </div>
    );
  }
}
