# UI Keyboard Navigation for Core Forms

Keyboard operability for primary business forms on the Bayer-Westphalian Campaign
Management Platform (KB **NFR-011**, Sprint 15 items **587** / **608**).

## Acceptance (item 608)

**Keyboard navigation works for core forms** when:

1. Users can open a core form and reach its first labeled field with keyboard focus.
2. **Tab** moves focus forward through primary fields, then the submit control, in document order.
3. **Shift+Tab** moves focus backward without trapping the user in a positive `tabindex` cycle.
4. **Enter** on a text field activates form submit (or surfaces validation) on forms that declare `enterSubmits`.
5. Interactive controls show a **focus-visible** outline (`styles.css` 3px ring).
6. The authenticated shell exposes **Skip to content** targeting `#main-content`.

Core forms covered:

| Form | Path | Accessible form name |
| --- | --- | --- |
| Login | `/login` | Employee sign-in |
| Create customer | `/customers` | Create customer form |
| Record consent | `/customers/:id` | Record consent |
| Create product | `/products` | Create product form |
| Create segment | `/segments` | Create segment form |
| Campaign builder | `/campaign-builder` | Campaign builder form |

## Journey

```text
Open a core form → Tab forward → Shift+Tab backward → Submit with Enter → Visible focus
```

## Implementation map

| Layer | Path |
| --- | --- |
| Contract + tab-order catalog | `frontend/src/features/a11y/keyboardNavigationFlow.ts` |
| Unit tests | `frontend/src/features/a11y/keyboardNavigationFlow.test.ts` |
| Login labels | `frontend/src/pages/LoginPage.tsx` |
| Shell skip link | `frontend/src/components/AppLayout.tsx` |
| Focus CSS | `frontend/src/app/styles.css` (+ `styles.test.ts`) |
| Integration | `frontend/src/test/integration/keyboardNavigation.integration.test.tsx` |
| Playwright UI | `frontend/tests/e2e/keyboard-navigation.spec.ts` |

## Running tests

```bash
cd frontend
npm test -- --run src/features/a11y/keyboardNavigationFlow.test.ts
npm test -- --run src/test/integration/keyboardNavigation.integration.test.tsx
npm test -- --run src/app/styles.test.ts
npx playwright test tests/e2e/keyboard-navigation.spec.ts
```

Do **not** run these when a backlog item says “do not run any tests” unless execution is explicitly requested.

## Related backlog

| Item | Topic |
| --- | --- |
| **587** | Add keyboard navigation support (implementation foundation) |
| **598–606** | Core UI workflow flows that must remain keyboard-operable |
| **607** | Role-based menu |
| **608** | Keyboard navigation works for core forms (this document) |
| **609** | Main screens pass basic accessibility checks — see [ui-main-screens-accessibility.md](ui-main-screens-accessibility.md) |
| **610** | UI style notes — see [ui-style-notes.md](../development/ui-style-notes.md) |
| **611** | Accessibility notes — see [accessibility-notes.md](../development/accessibility-notes.md) |
