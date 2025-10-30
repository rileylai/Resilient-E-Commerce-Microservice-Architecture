import React, { Component } from "react";
import BankService from "../services/bank.service";
import AuthService from "../services/auth.service";

export default class Balance extends Component {
  constructor(props) {
    super(props);
    this.getBalance = this.getBalance.bind(this);

    this.state = {
      balance: null,
      currency: "AUD",
      userId: null,
      loading: true,
      message: ""
    };
  }

  componentDidMount() {
    this.getBalance();
  }

  getBalance() {
    const user = AuthService.getCurrentUser();
    if (!user) {
      window.location.href = '/login';
      return;
    }

    this.setState({ loading: true });

    // Use current user's ID to query their bank account
    BankService.getBalance(user.id, "AUD")
      .then(response => {
        const data = response.data.data || response.data;

        this.setState({
          balance: parseFloat(data.balance) || 0,
          currency: data.currency || "AUD",
          userId: data.userId || user.id,
          loading: false,
          message: ""
        });
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
    const { balance, currency, userId, loading, message } = this.state;
    const user = AuthService.getCurrentUser();

    if (loading) {
      return (
        <div className="text-center mt-5">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-2">Loading balance...</p>
        </div>
      );
    }

    return (
      <div>
        <h3>Account Balance</h3>

        {message && (
          <div className="alert alert-danger" role="alert">
            <strong>Error:</strong> {message}
            <p className="mb-0 mt-2">
              <small>Make sure the Store service can connect to Bank service</small>
            </p>
          </div>
        )}

        {balance !== null && !message && (
          <div className="card" style={{ maxWidth: '600px' }}>
            <div className="card-header bg-primary text-white">
              <h5 className="mb-0">Bank Account</h5>
            </div>
            <div className="card-body">
              <div className="mb-3">
                <small className="text-muted">Account Holder</small>
                <p className="mb-1"><strong>{user?.username || 'N/A'}</strong></p>
              </div>
              <div className="mb-3">
                <small className="text-muted">User ID</small>
                <p className="mb-1"><strong>{userId || user?.id || 'N/A'}</strong></p>
              </div>
              <hr />
              <div className="mb-3">
                <small className="text-muted">Current Balance</small>
                <h1 className="display-4 mb-0" style={{ color: balance >= 0 ? '#28a745' : '#dc3545' }}>
                  ${balance.toFixed(2)} {currency}
                </h1>
              </div>
              <hr />
              <div className="d-flex justify-content-between">
                <button className="btn btn-primary" onClick={this.getBalance}>
                  <span>↻</span> Refresh
                </button>
                <a href="/products" className="btn btn-secondary">
                  Continue Shopping
                </a>
              </div>
            </div>
            <div className="card-footer text-muted">
              <small>Last updated: {new Date().toLocaleString()}</small>
            </div>
          </div>
        )}
      </div>
    );
  }
}
