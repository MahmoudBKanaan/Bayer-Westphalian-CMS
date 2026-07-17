import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { login, type AuthenticatedUser } from "@/api/auth";
import {
  AUTH_SESSION_CHANGED_EVENT,
  clearAuthSession,
  loadAuthSession,
  saveAuthSession,
  extractRolesFromAccessToken,
  type SystemRoleName,
} from "@/auth/sessionStorageStrategy";

type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthenticatedUser | null;
  roles: SystemRoleName[];
};

type AuthContextValue = AuthState & {
  isAuthenticated: boolean;
  hasAnyRole: (roles: SystemRoleName[]) => boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>(() => loadStoredAuthState());

  useEffect(() => {
    const handleAuthSessionChanged = () => {
      setAuthState(loadStoredAuthState());
    };

    // Same-tab updates (login/logout/refresh via this module).
    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, handleAuthSessionChanged);
    // Cross-tab updates: localStorage writes from other tabs fire the StorageEvent.
    const handleStorage = (event: StorageEvent) => {
      if (
        event.storageArea === localStorage &&
        (event.key === null ||
          event.key === "bwc.accessToken" ||
          event.key === "bwc.refreshToken" ||
          event.key === "bwc.currentUser")
      ) {
        setAuthState(loadStoredAuthState());
      }
    };
    window.addEventListener("storage", handleStorage);

    return () => {
      window.removeEventListener(AUTH_SESSION_CHANGED_EVENT, handleAuthSessionChanged);
      window.removeEventListener("storage", handleStorage);
    };
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const session = await login(email, password);
    saveAuthSession(session);
    setAuthState({
      accessToken: session.tokens.accessToken,
      refreshToken: session.tokens.refreshToken,
      user: session.user,
      roles: extractRolesFromAccessToken(session.tokens.accessToken),
    });
  }, []);

  const signOut = useCallback(() => {
    clearAuthSession();
    setAuthState({ accessToken: null, refreshToken: null, user: null, roles: [] });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...authState,
      isAuthenticated: Boolean(authState.accessToken && authState.user),
      hasAnyRole: (roles) => roles.some((role) => authState.roles.includes(role)),
      signIn,
      signOut,
    }),
    [authState, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const auth = useContext(AuthContext);
  if (auth == null) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return auth;
}

function loadStoredAuthState(): AuthState {
  const session = loadAuthSession();
  if (session == null) {
    return { accessToken: null, refreshToken: null, user: null, roles: [] };
  }

  return session;
}
