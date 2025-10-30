import React, { Component } from "react";
import AuthService from "../services/auth.service";

export default class Register extends Component {
  constructor(props) {
    super(props);
    this.onChangeUsername = this.onChangeUsername.bind(this);
    this.onChangeEmail = this.onChangeEmail.bind(this);
    this.onChangePassword = this.onChangePassword.bind(this);
    this.handleRegister = this.handleRegister.bind(this);

    this.state = {
      username: "",
      email: "",
      password: "",
      successful: false,
      message: ""
    };
  }

  onChangeUsername(e) {
    this.setState({
      username: e.target.value
    });
  }

  onChangeEmail(e) {
    this.setState({
      email: e.target.value
    });
  }

  onChangePassword(e) {
    this.setState({
      password: e.target.value
    });
  }

  handleRegister(e) {
    e.preventDefault();

    this.setState({
      message: "",
      successful: false
    });

    AuthService.register(this.state.username, this.state.password, this.state.email)
      .then(response => {
        this.setState({
          message: "Registration successful! Redirecting to login...",
          successful: true
        });
        setTimeout(() => {
          window.location.href = '/login';
        }, 2000);
      })
      .catch(error => {
        const resMessage =
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();

        this.setState({
          successful: false,
          message: resMessage
        });
      });
  }

  render() {
    return (
      <div className="col-md-12">
        <div className="card card-container">
          <div className="submit-form">
            <h3>Register</h3>
            <form onSubmit={this.handleRegister}>
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
                <label htmlFor="email">Email</label>
                <input
                  type="email"
                  className="form-control"
                  name="email"
                  value={this.state.email}
                  onChange={this.onChangeEmail}
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
                <button className="btn btn-primary btn-block">
                  Register
                </button>
              </div>

              {this.state.message && (
                <div className="form-group mt-3">
                  <div
                    className={
                      this.state.successful
                        ? "alert alert-success"
                        : "alert alert-danger"
                    }
                    role="alert"
                  >
                    {this.state.message}
                  </div>
                </div>
              )}

              <div className="form-group mt-3">
                <p>
                  Already have an account? <a href="/login">Login here</a>
                </p>
              </div>
            </form>
          </div>
        </div>
      </div>
    );
  }
}
