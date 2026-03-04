import { useState } from "react";
import api from "./api/axios";
import { useNavigate } from "react-router-dom";

function Login() {
  const [usernameOrIdNumber, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const response = await api.post("/auth/login", {
        usernameOrIdNumber,
        password,
      });

      localStorage.setItem("token", response.data.token);
      localStorage.setItem("fullName", response.data.fullName);

      navigate("/dashboard");
    } catch (error) {
      alert("Login failed ❌");
    }
  };

  return (
    <div>
      <h2>Login</h2>
      <form onSubmit={handleLogin}>
        <input
          type="text"
          placeholder="Username"
          value={usernameOrIdNumber}
          onChange={(e) => setUsername(e.target.value)}
        />
        <br /><br />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <br /><br />
        <button type="submit">Login</button>
      </form>
    </div>
  );
}

export default Login;