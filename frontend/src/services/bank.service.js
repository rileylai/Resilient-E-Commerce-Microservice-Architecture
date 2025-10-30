import axios from "axios";

class BankService {
  getBalance(userId, currency = "AUD") {
    // Use frontend proxy to avoid CORS (configured in setupProxy.js)
    return axios.get(`/api/bank/account/${userId}/${currency}`, {
      headers: {
        "Content-type": "application/json"
      }
    });
  }
}

export default new BankService();
