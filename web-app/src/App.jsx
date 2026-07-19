import { useEffect, useState } from "react";
import AppRoutes from "./routes/AppRoutes";
import { restoreSession } from "./services/authenticationService";

function App() {
  const [sessionReady, setSessionReady] = useState(false);

  useEffect(() => {
    restoreSession().finally(() => setSessionReady(true));
  }, []);

  if (!sessionReady) {
    return null;
  }

  return <AppRoutes />;
}

export default App;
