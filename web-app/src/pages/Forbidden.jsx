import { Link } from "react-router-dom";

export default function Forbidden() {
  return (
    <main style={{ maxWidth: 640, margin: "96px auto", padding: 24, textAlign: "center" }}>
      <h1>Access denied</h1>
      <p>Your account does not have permission to access this area.</p>
      <Link to="/">Return to your workspace</Link>
    </main>
  );
}
