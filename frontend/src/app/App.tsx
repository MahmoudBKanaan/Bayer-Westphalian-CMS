import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { RouterProvider } from "react-router-dom";
import { router } from "@/app/router";

export const PROJECT_TITLE = "Bayer-Westphalian Campaign Management Platform";
export const API_HEALTH_PLACEHOLDER = "API health: backend not connected yet";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      retry: 1,
    },
  },
});

export function App() {
  useEffect(() => {
    document.title = PROJECT_TITLE;
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
