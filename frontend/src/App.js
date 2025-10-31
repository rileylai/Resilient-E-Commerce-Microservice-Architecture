import React, { Component } from "react";
import { Link, Route, Routes } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "./App.css";

import Login from "./components/Login";
import Register from "./components/Register";
import ProductList from "./components/ProductList";
import Checkout from "./components/Checkout";
import OrderTracking from "./components/OrderTracking";
import OrderList from "./components/OrderList";
import Balance from "./components/Balance";
import AuthService from "./services/auth.service";

class App extends Component {
  constructor(props) {
    super(props);
    this.logout = this.logout.bind(this);
    this.updateUser = this.updateUser.bind(this);

    this.state = {
      currentUser: AuthService.getCurrentUser()
    };
  }

  componentDidMount() {
    // Update user state on mount
    this.updateUser();
  }

  updateUser() {
    this.setState({
      currentUser: AuthService.getCurrentUser()
    });
  }

  logout() {
    AuthService.logout();
    this.setState({
      currentUser: null
    });
    window.location.href = '/login';
  }

  render() {
    const { currentUser } = this.state;

    return (
      <div>
        <nav className="navbar navbar-expand navbar-dark bg-dark">
          <a href="/" className="navbar-brand">
            Store App
          </a>
          <div className="navbar-nav mr-auto">
            {currentUser && (
              <>
                <li className="nav-item">
                  <Link to={"/products"} className="nav-link">
                    Products
                  </Link>
                </li>
                <li className="nav-item">
                  <Link to={"/orders"} className="nav-link">
                    My Orders
                  </Link>
                </li>
                <li className="nav-item">
                  <Link to={"/balance"} className="nav-link">
                    Balance
                  </Link>
                </li>
              </>
            )}
          </div>

          <div className="navbar-nav ms-auto">
            {currentUser ? (
              <>
                <li className="nav-item">
                  <span className="nav-link">
                    Welcome, {currentUser.username}
                  </span>
                </li>
                <li className="nav-item">
                  <a href="/login" className="nav-link" onClick={this.logout}>
                    Logout
                  </a>
                </li>
              </>
            ) : (
              <>
                <li className="nav-item">
                  <Link to={"/login"} className="nav-link">
                    Login
                  </Link>
                </li>
                <li className="nav-item">
                  <Link to={"/register"} className="nav-link">
                    Register
                  </Link>
                </li>
              </>
            )}
          </div>
        </nav>

        <div className="container mt-3">
          <Routes>
            <Route path="/" element={currentUser ? <ProductList /> : <Login />} />
            <Route path="/login" element={<Login onLoginSuccess={this.updateUser} />} />
            <Route path="/register" element={<Register />} />
            <Route path="/products" element={currentUser ? <ProductList /> : <Login />} />
            <Route path="/checkout" element={currentUser ? <Checkout /> : <Login />} />
            <Route path="/order/:orderId" element={currentUser ? <OrderTracking /> : <Login />} />
            <Route path="/orders" element={currentUser ? <OrderList /> : <Login />} />
            <Route path="/balance" element={currentUser ? <Balance /> : <Login />} />
          </Routes>
        </div>
      </div>
    );
  }
}

export default App;
