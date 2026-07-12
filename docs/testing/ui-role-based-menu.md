# UI Role-Based Menu Visibility

Hiding unauthorized features in the main application shell navigation for the
Bayer-Westphalian Campaign Management Platform (KB role matrix, **NFR-001**, Sprint 15 item **607**).

## Acceptance (item 607)

**Role-based menu hides unauthorized features** when:

1. After login, **Main navigation** only lists links allowed for the user’s roles.
2. Admin-only **Users** and **Settings** are hidden from non-admins.
3. **Builder** is limited to Admin / Campaign Manager.
4. Persona menus match the KB matrix (BI Analyst, Compliance Officer, Executive Viewer, etc.).
5. Multi-role users see the union of allowed items (nav order preserved).
6. Users with no roles see an empty navigation state (not a full feature dump).

Backend authorization remains authoritative; the menu is UX least-privilege only.

## Journey

```text
Authenticate → Resolve roles → Filter nav items → Render visible menu
```

## Implementation map

| Layer | Path |
| --- | --- |
| Nav allow-lists + filters | `frontend/src/features/auth/roleBasedMenu.ts` |
| Unit tests | `frontend/src/features/auth/roleBasedMenu.test.ts` |
| Shell | `frontend/src/components/AppLayout.tsx` (+ `AppLayout.test.tsx`) |
| Integration | `frontend/src/test/integration/roleBasedMenu.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/role-based-menu.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/auth/roleBasedMenu.test.ts
npm test -- --run src/components/AppLayout.test.tsx
npm test -- --run src/test/integration/roleBasedMenu.integration.test.tsx
npx playwright test tests/e2e/role-based-menu.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **596** | Frontend integration tests (includes earlier role navigation suite) |
| **606** | Dashboard loads analytics |
| **607** | Role-based menu hides unauthorized features (this document) |
| **608** | Keyboard navigation works for core forms — see [ui-keyboard-navigation.md](ui-keyboard-navigation.md) |
